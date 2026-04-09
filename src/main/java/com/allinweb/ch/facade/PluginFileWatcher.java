package com.allinweb.ch.facade;

import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * Watches the plugins directory for changes to {@code .min.js} build files.
 * When a change is detected, all plugin caches are cleared so the next
 * injection reads the fresh file from disk.
 *
 * <p>Runs as a daemon thread — starts once via {@link #start()} and stops
 * automatically when the JVM exits, or explicitly via {@link #stop()}.</p>
 *
 * <p>Hot reload loop:
 * <ol>
 *   <li>Developer edits plugin source</li>
 *   <li>esbuild watch rebuilds {@code .min.js}</li>
 *   <li>This watcher detects the file change</li>
 *   <li>All plugin caches are cleared</li>
 *   <li>Next browser injection uses the new code</li>
 * </ol>
 */
@Slf4j
public class PluginFileWatcher {

    private static volatile PluginFileWatcher instance;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread watcherThread;
    private WatchService watchService;

    private PluginFileWatcher() {}

    public static PluginFileWatcher getInstance() {
        if (instance == null) {
            synchronized (PluginFileWatcher.class) {
                if (instance == null) {
                    instance = new PluginFileWatcher();
                }
            }
        }
        return instance;
    }

    /**
     * Start watching the plugins directory. Safe to call multiple times —
     * only the first call starts the thread.
     */
    public void start() {
        if (running.getAndSet(true)) {
            log.info("PluginFileWatcher — already running");
            return;
        }

        String pluginsDir = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.PATH_PLUGINS);

        if (pluginsDir == null || pluginsDir.isBlank()) {
            log.warn("PluginFileWatcher — path_plugins not configured, file watcher disabled");
            running.set(false);
            return;
        }

        Path pluginsPath = Paths.get(pluginsDir);
        if (!Files.isDirectory(pluginsPath)) {
            log.warn("PluginFileWatcher — plugins directory does not exist: {}", pluginsDir);
            running.set(false);
            return;
        }

        watcherThread = new Thread(() -> runWatcher(pluginsPath), "plugin-file-watcher");
        watcherThread.setDaemon(true);
        watcherThread.start();

        log.info("PluginFileWatcher — watching {} for .min.js changes", pluginsPath);
    }

    /**
     * Stop the file watcher.
     */
    public void stop() {
        running.set(false);
        try {
            if (watchService != null) watchService.close();
        } catch (IOException ignored) {
        }
        if (watcherThread != null) watcherThread.interrupt();
        log.info("PluginFileWatcher — stopped");
    }

    public boolean isRunning() {
        return running.get();
    }

    // ── Watcher loop ───────────────────────────────────────────────────────

    private void runWatcher(Path pluginsPath) {
        try {
            watchService = FileSystems.getDefault().newWatchService();

            // Register the build/ subdirectory of each plugin
            Map<WatchKey, Path> keyPathMap = new HashMap<>();
            registerBuildDirs(pluginsPath, keyPathMap);

            while (running.get()) {
                WatchKey key;
                try {
                    key = watchService.take(); // blocks until an event
                } catch (InterruptedException | ClosedWatchServiceException e) {
                    break;
                }

                Path dir = keyPathMap.get(key);
                boolean cacheCleared = false;

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                    Path changed = (Path) event.context();
                    if (changed != null && changed.toString().endsWith(".min.js")) {
                        Path fullPath = dir != null ? dir.resolve(changed) : changed;
                        log.info("PluginFileWatcher — detected change: {} ({})", fullPath, kind.name());

                        if (!cacheCleared) {
                            PerformPreLoad.reloadAllPlugins();
                            cacheCleared = true;
                            log.info("PluginFileWatcher — plugin caches cleared, "
                                    + "next injection will use updated scripts");
                        }
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    keyPathMap.remove(key);
                    if (keyPathMap.isEmpty()) {
                        log.warn("PluginFileWatcher — all watched directories became invalid, stopping");
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.error("PluginFileWatcher — error: {}", e.getMessage(), e);
        } finally {
            running.set(false);
            try {
                if (watchService != null) watchService.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Walk the plugins directory and register every {@code build/} subdirectory.
     * This catches: pageScanner/build/, hoverPick/build/, actionExecutor/build/, etc.
     */
    private void registerBuildDirs(Path pluginsPath, Map<WatchKey, Path> keyPathMap) throws IOException {
        Files.walkFileTree(pluginsPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.getFileName().toString().equals("build") || dir.equals(pluginsPath)) {
                    WatchKey key = dir.register(
                            watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
                    keyPathMap.put(key, dir);
                    log.debug("PluginFileWatcher — watching: {}", dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        log.info("PluginFileWatcher — registered {} directories", keyPathMap.size());
    }
}
