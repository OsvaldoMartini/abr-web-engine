package com.allinweb.ch.executors;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ExecutorsManager implements AutoCloseable {

    public enum Pool {
        SCHEDULER, // general scheduled tasks
        SCREENSHOT_SCHEDULER,
        PRELAUNCH // background tasks
    }

    // Shared pools/schedulers
    private final Map<Pool, ExecutorService> pools = new ConcurrentHashMap<>();
    private final Map<Pool, ScheduledExecutorService> schedulers = new ConcurrentHashMap<>();

    // Per-session WebSocket executors (single-thread, ordered)
    private final ConcurrentHashMap<String, ExecutorService> wsBySession = new ConcurrentHashMap<>();

    public ExecutorsManager() {
        pools.put(
                Pool.PRELAUNCH,
                newFixed("prelaunch", Math.max(2, Runtime.getRuntime().availableProcessors() / 2)));

        schedulers.put(Pool.SCHEDULER, newScheduled("scheduler", 1));
        schedulers.put(Pool.SCREENSHOT_SCHEDULER, newScheduled("screenshot", 1));
    }

    /** Shared executor pools (PRELAUNCH, etc.) */
    public ExecutorService executor(Pool pool) {
        ExecutorService es = pools.get(pool);
        if (es == null) throw new IllegalArgumentException("Unknown pool " + pool);
        return es;
    }

    /** Shared schedulers */
    public ScheduledExecutorService scheduler(Pool pool) {
        ScheduledExecutorService ses = schedulers.get(pool);
        if (ses == null) throw new IllegalArgumentException("Unknown scheduler " + pool);
        return ses;
    }

    /**
     * WebSocket executor for a session.
     * - single-thread: guarantees message order per session
     * - dedicated: avoids one session blocking another
     */
    public ExecutorService websocketExecutor(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");
        return wsBySession.computeIfAbsent(sessionId, sid -> newSingle("ws-" + sid));
    }

    /**
     * Call when a WebSocket session is permanently done (closed, failed, etc.)
     * so we don't leak executors.
     */
    public void releaseWebsocketExecutor(String sessionId) {
        if (sessionId == null) return;
        ExecutorService es = wsBySession.remove(sessionId);
        shutdownNowQuietly(es);
    }

    private static ExecutorService newSingle(String name) {
        return Executors.newSingleThreadExecutor(namedThreads(name));
    }

    private static ExecutorService newFixed(String name, int nThreads) {
        return Executors.newFixedThreadPool(nThreads, namedThreads(name));
    }

    private static ScheduledExecutorService newScheduled(String name, int nThreads) {
        return Executors.newScheduledThreadPool(nThreads, namedThreads(name));
    }

    private static ThreadFactory namedThreads(String prefix) {
        AtomicInteger idx = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r);
            t.setName(prefix + "-" + idx.getAndIncrement());
            t.setDaemon(true); // keep if you want JVM to exit without waiting; otherwise set false
            t.setUncaughtExceptionHandler((th, ex) -> System.err.println("Uncaught in " + th.getName() + ": " + ex));
            return t;
        };
    }

    @Override
    public void close() {
        // Stop scheduled first (so they don't keep submitting)
        schedulers.values().forEach(ExecutorsManager::shutdownNowQuietly);
        pools.values().forEach(ExecutorsManager::shutdownNowQuietly);

        // Stop all per-session WS executors
        wsBySession.values().forEach(ExecutorsManager::shutdownNowQuietly);
        wsBySession.clear();
    }

    private static void shutdownNowQuietly(ExecutorService es) {
        if (es == null) return;
        es.shutdownNow();
        try {
            es.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
