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
import com.ardor3d.input.gesture.event.LongPressGestureEvent;

public class LongPressInterpreter extends AbstractTouchInterpreter {

    public static final int DEFAULT_TOUCHES = 1;

    public static final int DEFAULT_PRESS_TIME = 1200;
    private final int _pressTime;

    public static final double DEFAULT_MAX_DRIFT = 10.0;
    private final double _maxDrift;

    public LongPressInterpreter() {
        this(DEFAULT_TOUCHES);
    }

    public LongPressInterpreter(final int touches) {
        this(touches, DEFAULT_PRESS_TIME, DEFAULT_MAX_DRIFT);
    }

    public LongPressInterpreter(final int touches, final int pressTimeMS, final double maxDrift) {
        super(touches);
        _pressTime = pressTimeMS;
        _maxDrift = maxDrift;
    }

    @Override
    public AbstractGestureEvent examine(final List<TouchHistory> touchInfo, final int up, final int valid) {

        if (valid == _touches) {
            switch (_state) {
                case Ready:
                    if (up != 0) {
                        // switch to spent if a touch was lifted or untracked
                        _state = ArmState.Unknown;
                    } else if (valid == _touches) {
                        // switch to Armed if valid touches match
                        _state = ArmState.Armed;
                        _lastArmed = System.currentTimeMillis();
                        for (int i = 0, maxI = touchInfo.size(); i < maxI; i++) {
                            _lastArmedIds[i] = touchInfo.get(i).id;
                        }
                    }
                    break;
                case Armed:
                    // make sure we haven't done anything to disarm
                    for (int i = 0, maxI = touchInfo.size(); i < maxI; i++) {
                        final TouchHistory t = touchInfo.get(i);

                        // check for drift
                        if (t.distanceFromInitialSq() > _maxDrift * _maxDrift) {
                            _state = ArmState.Unknown;
                            break;
                        }

                        // make sure ids didn't suddenly change - seems unlikely though
                        boolean found = false;
                        for (final String id : _lastArmedIds) {
                            if (t.id.equals(id)) {
                                found = true;
                            }
                        }
                        if (!found) {
                            _state = ArmState.Unknown;
                            break;
                        }
                    }
                    break;
                case Triggered:
                case Unknown:
                default:
                    break;
            }
        } else if (_state == ArmState.Armed) {
            _state = ArmState.Unknown;
        }

        return null;
    }

    @Override
    public AbstractGestureEvent touchEnd() {
        _state = ArmState.Ready;
        return null;
    }

    @Override
    public AbstractGestureEvent update() {
        if (_state == ArmState.Armed) {
            final long time = System.currentTimeMillis();
            if (time >= _lastArmed + _pressTime) {
                _state = ArmState.Triggered;
                return new LongPressGestureEvent(_touches);
            }
        }
        return null;
    }
}
