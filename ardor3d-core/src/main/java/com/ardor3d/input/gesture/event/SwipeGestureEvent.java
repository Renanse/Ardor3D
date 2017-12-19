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
public class SwipeGestureEvent extends PanGestureEvent {

    public SwipeGestureEvent(final boolean startOfGesture, final int xDirection, final int yDirection) {
        this(System.nanoTime(), startOfGesture, xDirection, yDirection);
    }

    public SwipeGestureEvent(final long nanos, final boolean startOfGesture, final int xDirection,
            final int yDirection) {
        super(nanos, startOfGesture, xDirection, yDirection);
    }

    @Override
    public String toString() {
        return MessageFormat.format("SwipeGestureEvent:  x: {0}  y: {1}", _xDirection, _yDirection);
    }

}
