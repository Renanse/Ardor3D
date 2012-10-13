/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.logical;

import com.ardor3d.input.KeyEvent;
import com.ardor3d.input.KeyboardWrapper;
import com.google.common.collect.PeekingIterator;

/**
 * A "do-nothing" implementation of KeyboardWrapper useful when you want to ignore (or do not need) key events.
 */
public class DummyKeyboardWrapper implements KeyboardWrapper {
    public static final DummyKeyboardWrapper INSTANCE = new DummyKeyboardWrapper();

    PeekingIterator<KeyEvent> empty = new PeekingIterator<KeyEvent>() {

        public boolean hasNext() {
            return false;
        }

        public void remove() {}

        public KeyEvent peek() {
            return null;
        }

        public KeyEvent next() {
            return null;
        }
    };

    public PeekingIterator<KeyEvent> getEvents() {
        return empty;
    }

    public void init() {
        ; // ignore, does nothing.
    }

}
