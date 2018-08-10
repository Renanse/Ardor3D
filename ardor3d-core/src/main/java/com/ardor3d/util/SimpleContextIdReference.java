/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.List;

import com.google.common.collect.Lists;

public class SimpleContextIdReference<T> extends PhantomReference<T> {

    /**
     * Keep a string reference to these objects until their reference is cleared.
     */
    private static final List<SimpleContextIdReference<?>> REFS = Lists.newLinkedList();

    private final int _id;
    private final Object _glContext;

    public SimpleContextIdReference(final T reference, final ReferenceQueue<? super T> queue, final int id,
            final Object glContext) {
        super(reference, queue);
        REFS.add(this);
        _id = id;
        _glContext = glContext;
    }

    @Override
    public void clear() {
        super.clear();
        REFS.remove(this);
    }

    public int getId() {
        return _id;
    }

    public Object getGlContext() {
        return _glContext;
    }
}
