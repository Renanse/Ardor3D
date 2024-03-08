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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Set;

/**
 * A simple Multiset implementation using an EnumMap to count the occurrences of elements.
 */
public class SimpleEnumMultiset<K extends Enum<K>> implements Multiset<K> {
    private final EnumMap<K, Integer> _counts;
    private final Class<K> _elementType;

    /**
     * Creates a new multiset for the given enum type.
     *
     * @param elementType the enum type
     */
    public SimpleEnumMultiset(Class<K> elementType) {
        _elementType = elementType;
        _counts = new EnumMap<>(elementType);

    }

    public void add(K element) {
        _counts.merge(element, 1, Integer::sum);
    }

    @Override
    public void setCount(K element, int count) {
        if (count <= 0) {
            _counts.remove(element);
        } else {
            _counts.put(element, count);
        }
    }

    public boolean remove(K element) {
        Integer count = _counts.get(element);
        if (count == null) {
            return false;
        }
        if (count > 1) {
            _counts.put(element, count - 1);
        } else {
            _counts.remove(element);
        }
        return true;
    }

    public int count(K element) {
        return _counts.getOrDefault(element, 0);
    }

    public Set<K> elementSet() {
        return Collections.unmodifiableSet(_counts.keySet());
    }

    public void removeAll(K element) {
        _counts.remove(element);
    }

    public int size() {
        return _counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isEmpty() {
        return _counts.isEmpty();
    }

    public void clear() {
        _counts.clear();
    }

    @Override
    public SimpleEnumMultiset<K> copyOf() {
        var multiset = new SimpleEnumMultiset<K>(_elementType);
        multiset._counts.putAll(_counts);
        return multiset;
    }
}
