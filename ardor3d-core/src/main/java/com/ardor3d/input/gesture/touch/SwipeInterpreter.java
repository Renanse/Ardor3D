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
import com.ardor3d.input.gesture.event.SwipeGestureEvent;
import com.ardor3d.math.MathUtils;

public class SwipeInterpreter extends AbstractTouchInterpreter {

    private final double _minVelocity;
    private final long _maxRestTimeMS;

    private int _prevX, _prevY, _currX, _currY;
    private long _prevTime, _currTime;

    public SwipeInterpreter(final int touches, final double minVelocity, final long maxRestTimeMS) {
        super(touches);
        _minVelocity = minVelocity;
        _maxRestTimeMS = maxRestTimeMS;
    }

    @Override
    public AbstractGestureEvent examine(final List<TouchHistory> touchInfo, final int up, final int valid) {
        if (valid == _touches && touchInfo.size() == valid && _state != ArmState.Unknown) {
            boolean down = false;
            int currX = 0, currY = 0, prevX = 0, prevY = 0;
            long maxTime = 0;
            for (int i = 0; i < valid; i++) {
                final TouchHistory t = touchInfo.get(i);
                down |= (t.currState == TouchStatus.Down);
                currX += t.currX;
                currY += t.currY;
                prevX += t.prevX;
                prevY += t.prevY;
                maxTime = Math.max(maxTime, t.currTime);
            }
            currX /= valid;
            currY /= valid;
            prevX /= valid;
            prevY /= valid;

            if (maxTime != _currTime && (down || prevX != currX || prevY != currY)) {
                _state = _state == ArmState.Ready ? ArmState.Armed : ArmState.Triggered;
                _prevTime = _currTime;
                _currTime = maxTime;
                _prevX = _currX;
                _prevY = _currY;
                _currX = currX;
                _currY = currY;
            }
        }

        return null;
    }

    @Override
    public AbstractGestureEvent touchEnd() {
        final boolean checkAndSend = _state == ArmState.Triggered;
        _state = ArmState.Ready;

        if (checkAndSend && _currTime > _prevTime) {
            // check our wait time
            if (System.currentTimeMillis() - _currTime > _maxRestTimeMS) {
                return null;
            }

            // check our velocity
            final int dx = _currX - _prevX;
            final int dy = _currY - _prevY;

            final double dist = MathUtils.sqrt(dx * dx + dy * dy);
            final double velocity = dist / (_currTime - _prevTime);
            if (velocity < _minVelocity) {
                return null;
            }

            return new SwipeGestureEvent(_lastBounds, _touches, dx, -dy);
        }

        return null;
    }
}
