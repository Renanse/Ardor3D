/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.logical;

import com.ardor3d.input.MouseState;
import com.ardor3d.input.MouseWrapper;
import com.google.common.collect.PeekingIterator;

/**
 * A "do-nothing" implementation of MouseWrapper useful when you want to ignore (or do not need) mouse events.
 */
public class DummyMouseWrapper implements MouseWrapper {
    public static final DummyMouseWrapper INSTANCE = new DummyMouseWrapper();

    PeekingIterator<MouseState> empty = new PeekingIterator<MouseState>() {

        public boolean hasNext() {
            return false;
        }

        public void remove() {}

        public MouseState peek() {
            return null;
        }

        public MouseState next() {
            return null;
        }
    };

    public PeekingIterator<MouseState> getEvents() {
        return empty;
    }

    public void init() {
        ; // ignore, does nothing
    }

    @Override
    public void setIgnoreInput(final boolean ignore) {}

    @Override
    public boolean isIgnoreInput() {
        return true;
    }
}
