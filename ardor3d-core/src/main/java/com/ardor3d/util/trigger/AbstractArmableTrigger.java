/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.trigger;

public abstract class AbstractArmableTrigger {

    protected enum State {
        Unarmed, Armed, Triggered
    }

    protected TriggerListener _listener;

    protected State _state = State.Unarmed;

    public boolean isUnarmed() {
        return _state == State.Unarmed;
    }

    public boolean isArmed() {
        return _state == State.Armed;
    }

    public boolean isTriggered() {
        return _state == State.Triggered;
    }

    public void arm() {
        _state = State.Armed;
        if (_listener != null) {
            _listener.onArm();
        }
    }

    public void trigger() {
        _state = State.Triggered;
        if (_listener != null) {
            _listener.onTrigger();
        }
    }

    public void disarm() {
        _state = State.Unarmed;
        if (_listener != null) {
            _listener.onDisarm();
        }
    }

    public abstract void checkTrigger();

}
