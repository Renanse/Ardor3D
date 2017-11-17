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
public class RotateGestureEvent extends AbstractGestureEvent {

    protected final double _radians;

    public RotateGestureEvent(final double radians) {
        this(System.nanoTime(), radians);
    }

    public RotateGestureEvent(final long nanos, final double radians) {
        super(nanos);
        _radians = radians;
    }

    public double getRadians() {
        return _radians;
    }

    @Override
    public String toString() {
        return MessageFormat.format("RotateGestureEvent: {0} radians", _radians);
    }

}
