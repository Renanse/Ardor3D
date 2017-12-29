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
import com.ardor3d.math.Rectangle2;

@Immutable
public class SwipeGestureEvent extends PanGestureEvent {

    public SwipeGestureEvent(final Rectangle2 bounds, final int touches, final int xDirection, final int yDirection) {
        this(System.nanoTime(), bounds, touches, xDirection, yDirection);
    }

    public SwipeGestureEvent(final long nanos, final Rectangle2 bounds, final int touches, final int xDirection,
            final int yDirection) {
        super(nanos, false, bounds, touches, xDirection, yDirection);
    }

    @Override
    public String toString() {
        return MessageFormat.format("SwipeGestureEvent:  touches: {0}  x: {1}  y: {2}", _touches, _xDirection,
                _yDirection);
    }

}
