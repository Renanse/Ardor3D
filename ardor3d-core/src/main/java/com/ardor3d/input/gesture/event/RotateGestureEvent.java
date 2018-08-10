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
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Rectangle2;

@Immutable
public class RotateGestureEvent extends AbstractGestureEvent {

    protected final double _currentAngle;
    protected final double _deltaRadians;
    protected final double _totalRadians;

    public RotateGestureEvent(final boolean startOfGesture, final Rectangle2 bounds, final double currentAngle,
            final double deltaRadians, final double totalRadians) {
        this(System.nanoTime(), startOfGesture, bounds, currentAngle, deltaRadians, totalRadians);
    }

    public RotateGestureEvent(final long nanos, final boolean startOfGesture, final Rectangle2 bounds,
            final double currentAngle, final double deltaRadians, final double totalRadians) {
        super(nanos, startOfGesture, bounds);

        _currentAngle = currentAngle;
        _deltaRadians = deltaRadians;
        _totalRadians = totalRadians;
    }

    public double getCurrentAngle() {
        return _currentAngle;
    }

    public double getDeltaRadians() {
        return _deltaRadians;
    }

    public double getTotalRadians() {
        return _totalRadians;
    }

    @Override
    public String toString() {
        return MessageFormat.format("RotateGestureEvent (initial: {3}): {0} angle, {1} delta, {2} total",
                _currentAngle * MathUtils.RAD_TO_DEG, _deltaRadians, _totalRadians, isStartOfGesture());
    }

}
