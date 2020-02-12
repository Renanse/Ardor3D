/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.dummy;

import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.input.mouse.MouseWrapper;
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

    public PeekingIterator<MouseState> getMouseEvents() {
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
