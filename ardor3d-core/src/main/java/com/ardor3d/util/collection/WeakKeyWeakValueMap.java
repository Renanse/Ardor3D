/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.collection;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;

public class WeakKeyWeakValueMap<K, V> {
    private final Map<K, WeakReference<V>> weakMap = Collections.synchronizedMap(new WeakHashMap<K, WeakReference<V>>());
    private final ReferenceQueue<V> queue = new ReferenceQueue<>();

    private void processQueue() {
        WeakReference<? extends V> ref;
        while ((ref = (WeakReference<? extends V>) queue.poll()) != null) {
            weakMap.values().remove(ref);
        }
    }

    public V get(K key) {
        processQueue();
        WeakReference<V> ref = weakMap.get(key);
        return ref == null ? null : ref.get();
    }

    public void put(K key, V value) {
        processQueue();
        weakMap.put(key, new WeakReference<>(value, queue));
    }

    public V remove(K key) {
        processQueue();
        WeakReference<V> ref = weakMap.remove(key);
        return ref == null ? null : ref.get();
    }

    public Set<K> keySet() {
        processQueue();
        return weakMap.keySet();
    }

    public Collection<WeakReference<V>> values() {
        processQueue();
        return weakMap.values();
    }
}
