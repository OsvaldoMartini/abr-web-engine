package com.allinweb.ch.util;

@FunctionalInterface
interface SingletonSupplier<T> {
    T get();
}
