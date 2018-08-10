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
public class PinchGestureEvent extends AbstractGestureEvent {

    protected final double _scale;

    public PinchGestureEvent(final boolean startOfGesture, final Rectangle2 bounds, final double scale) {
        this(System.nanoTime(), startOfGesture, bounds, scale);
    }

    public PinchGestureEvent(final long nanos, final boolean startOfGesture, final Rectangle2 bounds,
            final double scale) {
        super(nanos, startOfGesture, bounds);
        _scale = scale;
    }

    public double getScale() {
        return _scale;
    }

    @Override
    public String toString() {
        return MessageFormat.format("PinchGestureEvent: {0,number,percent}", _scale);
    }

}
