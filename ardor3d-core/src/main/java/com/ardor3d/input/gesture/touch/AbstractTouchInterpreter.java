/**
 * Copyright (c) 2008-2017 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.gesture.touch;

import java.util.List;

import com.ardor3d.input.gesture.event.AbstractGestureEvent;
import com.ardor3d.math.Rectangle2;

public abstract class AbstractTouchInterpreter {
    protected final int _touches;
    protected final String[] _lastArmedIds;
    protected long _lastArmed;
    protected final Rectangle2 _lastBounds = new Rectangle2();

    protected ArmState _state = ArmState.Ready;

    protected enum ArmState {
        Ready, Armed, Triggered, Unknown
    }

    protected AbstractTouchInterpreter(final int touches) {
        _touches = touches;
        _lastArmedIds = new String[touches];
    }

    public abstract AbstractGestureEvent examine(final List<TouchHistory> touchInfo, int up, int valid);

    public AbstractGestureEvent update() {
        return null;
    }

    public AbstractGestureEvent touchEnd() {
        return null;
    }

}
