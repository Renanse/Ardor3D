/**
 * Copyright (c) 2008-2017 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.gestures;

import java.text.MessageFormat;

import com.ardor3d.annotation.Immutable;

@Immutable
public class PinchGestureEvent extends AbstractGestureEvent {

    protected final double _scale;

    public PinchGestureEvent(final double scale) {
        this(System.nanoTime(), scale);
    }

    public PinchGestureEvent(final long nanos, final double scale) {
        super(nanos);
        _scale = scale;
    }

    public double getScale() {
        return _scale;
    }

    @Override
    public String toString() {
        return MessageFormat.format("PinchGestureEvent: {0,percent}%", _scale);
    }

}
