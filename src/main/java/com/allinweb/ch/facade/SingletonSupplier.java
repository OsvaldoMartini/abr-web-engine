package com.allinweb.ch.facade;

@FunctionalInterface
public interface SingletonSupplier<T> {
    T get();
}
