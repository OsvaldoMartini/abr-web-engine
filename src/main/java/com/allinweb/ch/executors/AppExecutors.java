package com.allinweb.ch.executors;

public final class AppExecutors {
    private static final ExecutorsManager INSTANCE = new ExecutorsManager();

    private AppExecutors() {}

    public static ExecutorsManager get() {
        return INSTANCE;
    }
}
