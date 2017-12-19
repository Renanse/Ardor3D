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
public class PanGestureEvent extends AbstractGestureEvent {

    protected final int _touches;
    protected final int _xDirection;
    protected final int _yDirection;

    public PanGestureEvent(final boolean startOfGesture, final int touches, final int xDirection,
            final int yDirection) {
        this(System.nanoTime(), startOfGesture, touches, xDirection, yDirection);
    }

    public PanGestureEvent(final long nanos, final boolean startOfGesture, final int touches, final int xDirection,
            final int yDirection) {
        super(nanos, startOfGesture);
        _touches = touches;
        _xDirection = xDirection;
        _yDirection = yDirection;
    }

    public int getTouches() {
        return _touches;
    }

    public int getXDirection() {
        return _xDirection;
    }

    public int getYDirection() {
        return _yDirection;
    }

    @Override
    public String toString() {
        return MessageFormat.format("PanGestureEvent:  touches: {0}  x: {1}  y: {2}", _touches, _xDirection,
                _yDirection);
    }

}
