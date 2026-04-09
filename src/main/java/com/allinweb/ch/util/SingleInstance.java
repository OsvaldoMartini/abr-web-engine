package com.allinweb.ch.util;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SingleInstance {
    private static FileChannel channel;
    private static FileLock lock;
    private static Path currentLockFile;

    /**
     * Acquire the lock in the specified directory.
     * If no directory given, uses user.dir.
     */
    public static boolean acquire(String appId) {
        return acquire(appId, System.getProperty("user.dir"));
    }

    public static boolean acquire(String appId, String directory) {
        try {
            Path dir = Paths.get(directory);
            if (!Files.isDirectory(dir)) {
                Files.createDirectories(dir);
            }
            Path lockFile = dir.resolve("." + appId + ".lock");
            channel = FileChannel.open(lockFile, CREATE, WRITE);
            lock = channel.tryLock();
            if (lock != null) {
                currentLockFile = lockFile;
                log.info("Lock acquired: {}", lockFile.toAbsolutePath());
                return true;
            }
            return false;
        } catch (OverlappingFileLockException | IOException e) {
            return false;
        }
    }

    public static Path getLockFilePath() {
        return currentLockFile;
    }

    public static void release() {
        try {
            if (lock != null) lock.release();
        } catch (IOException ignored) {
        }
        try {
            if (channel != null) channel.close();
        } catch (IOException ignored) {
        }
        if (currentLockFile != null) {
            try {
                Files.deleteIfExists(currentLockFile);
            } catch (IOException ignored) {
            }
        }
        lock = null;
        channel = null;
        currentLockFile = null;
    }
}
