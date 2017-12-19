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

import com.ardor3d.annotation.Immutable;

@Immutable
public abstract class AbstractGestureEvent {

    private final long _nanos;
    private final boolean _startOfGesture;

    public AbstractGestureEvent(final long nanos, final boolean startOfGesture) {
        _nanos = nanos;
        _startOfGesture = startOfGesture;
    }

    public long getNanos() {
        return _nanos;
    }

    public boolean isStartOfGesture() {
        return _startOfGesture;
    }
}
