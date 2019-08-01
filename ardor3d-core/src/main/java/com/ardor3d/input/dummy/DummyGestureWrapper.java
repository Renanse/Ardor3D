/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.dummy;

import com.ardor3d.input.gesture.GestureWrapper;
import com.ardor3d.input.gesture.event.AbstractGestureEvent;
import com.google.common.collect.PeekingIterator;

public class DummyGestureWrapper implements GestureWrapper {

    public static final DummyGestureWrapper INSTANCE = new DummyGestureWrapper();

    PeekingIterator<AbstractGestureEvent> empty = new PeekingIterator<AbstractGestureEvent>() {
        public boolean hasNext() {
            return false;
        }

        public void remove() {}

        public AbstractGestureEvent peek() {
            return null;
        }

        public AbstractGestureEvent next() {
            return null;
        }
    };

    public PeekingIterator<AbstractGestureEvent> getGestureEvents() {
        return empty;
    }

    public void init() {
        ; // ignore, does nothing
    }

}
