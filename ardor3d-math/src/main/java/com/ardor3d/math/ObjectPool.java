/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Object pool for use with our Math Library to help reduce object creation during calculations. This class uses
 * a ThreadLocal pool of objects to allow for fast multi-threaded use.
 * 
 * @param <T>
 *            the type.
 */
public abstract class ObjectPool<T extends Poolable> {
    private final ThreadLocal<List<T>> _pool = new ThreadLocal<List<T>>() {
        @Override
        protected List<T> initialValue() {
            return new ArrayList<T>(_maxSize);
        }
    };

    private final int _maxSize;

    protected ObjectPool(final int maxSize) {
        _maxSize = maxSize;
    }

    protected abstract T newInstance();

    public final T fetch() {
        final List<T> objects = _pool.get();
        return objects.isEmpty() ? newInstance() : objects.remove(objects.size() - 1);
    }

    public final void release(final T object) {
        if (object == null) {
            throw new RuntimeException("Should not release null objects into ObjectPool.");
        }

        final List<T> objects = _pool.get();
        if (objects.size() < _maxSize) {
            objects.add(object);
        }
    }

    public static <T extends Poolable> ObjectPool<T> create(final Class<T> clazz, final int maxSize) {
        return new ObjectPool<T>(maxSize) {
            @Override
            protected T newInstance() {
                try {
                    return clazz.newInstance();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
