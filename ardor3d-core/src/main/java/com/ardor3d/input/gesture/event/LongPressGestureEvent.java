/**
 * Copyright (c) 2008-2017 Bird Dog Games, Inc..
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
import com.ardor3d.math.Rectangle2;

@Immutable
public class LongPressGestureEvent extends AbstractGestureEvent {

    public int _touches;

    public LongPressGestureEvent(final Rectangle2 bounds, final int touches) {
        this(System.nanoTime(), bounds, touches);
    }

    public LongPressGestureEvent(final long nanos, final Rectangle2 bounds, final int touches) {
        super(nanos, false, bounds);
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
