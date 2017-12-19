/**
 * Copyright (c) 2008-2017 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.gesture.event;

import java.text.MessageFormat;

import com.ardor3d.annotation.Immutable;

@Immutable
public class LongPressGestureEvent extends AbstractGestureEvent {

    public int _touches;

    public LongPressGestureEvent(final int touches) {
        this(touches, System.nanoTime());
    }

    public LongPressGestureEvent(final int touches, final long nanos) {
        super(nanos, false);
        _touches = touches;
    }

    public int getTouches() {
        return _touches;
    }

    @Override
    public String toString() {
        return MessageFormat.format("LongPressGestureEvent: touches={0}", _touches);
    }

}
