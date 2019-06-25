/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.gc;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ardor3d.renderer.RenderContext.RenderContextRef;
import com.ardor3d.util.Constants;
import com.google.common.collect.MapMaker;

/**
 * Context based reference to item T, holding value of type U.
 *
 * @param <T>
 *            type of object we are tied to the life of.
 * @param <U>
 *            type of value we are holding onto.
 */
public class ContextValueReference<T, U> extends PhantomReference<T> {

    /**
     * Keep a strong reference to these objects until their reference is cleared.
     */
    private static final List<ContextValueReference<?, ?>> REFS = new LinkedList<>();

    private final Map<RenderContextRef, U> _valueCache;
    private U _singleContextValue;

    public ContextValueReference(final T reference, final ReferenceQueue<? super T> queue) {
        super(reference, queue);
        if (Constants.useMultipleContexts) {
            _valueCache = new MapMaker().initialCapacity(2).weakKeys().makeMap();
        } else {
            _valueCache = null;
        }
        REFS.add(this);
    }

    public boolean containsKey(final RenderContextRef glContext) {
        if (Constants.useMultipleContexts) {
            return _valueCache.containsKey(glContext);
        } else {
            return true;
        }
    }

    public U getValue(final RenderContextRef glContext) {
        if (Constants.useMultipleContexts) {
            return _valueCache.get(glContext);
        } else {
            return _singleContextValue;
        }
    }

    public U removeValue(final RenderContextRef glContext) {
        if (Constants.useMultipleContexts) {
            return _valueCache.remove(glContext);
        } else {
            final U valueRemoved = _singleContextValue;
            _singleContextValue = null;
            return valueRemoved;
        }
    }

    public void put(final RenderContextRef glContext, final U value) {
        if (Constants.useMultipleContexts) {
            _valueCache.put(glContext, value);
        } else {
            _singleContextValue = value;
        }
    }

    public Set<RenderContextRef> getContextRefs() {
        if (Constants.useMultipleContexts) {
            return _valueCache.keySet();
        } else {
            return null;
        }
    }

    @Override
    public void clear() {
        super.clear();
        _singleContextValue = null;
        REFS.remove(this);
    }

    public static <T, U> ContextValueReference<T, U> newReference(final T reference,
            final ReferenceQueue<? super T> queue) {
        return new ContextValueReference<T, U>(reference, queue);
    }
}
