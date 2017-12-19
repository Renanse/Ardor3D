/**
 * Copyright (c) 2008-2017 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.logical;

import static com.google.common.base.Preconditions.checkNotNull;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.InputState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseState;
import com.ardor3d.math.MathUtils;
import com.google.common.base.Predicate;

/**
 * A condition that is true if a given button was pressed when going from the previous input state to the current one.
 */
@Immutable
public final class MouseButtonLongPressedCondition implements Predicate<TwoInputStates> {
    private final MouseButton _button;
    private final long _triggerTimeMS;
    private final double _maxDrift;

    private boolean _armed;
    private int _armedX, _armedY;
    private long _armedAt;

    /**
     * Construct a new MouseButtonLongPressedCondition.
     *
     * @param button
     *            the button that should be pressed to trigger this condition
     * @param triggerTimeMS
     *            how long, in ms, a button needs to be down before triggering the condition.
     * @param maxDrift
     *            how far the mouse / cursor can move from the initial long press location before invalidating the long
     *            press.
     * @throws NullPointerException
     *             if the supplied button argument is null
     */
    public MouseButtonLongPressedCondition(final MouseButton button, final long triggerTimeMS, final double maxDrift) {
        checkNotNull(button);

        _button = button;
        _triggerTimeMS = triggerTimeMS;
        _maxDrift = maxDrift;
    }

    public boolean apply(final TwoInputStates states) {
        final InputState currentState = states.getCurrent();
        final InputState previousState = states.getPrevious();

        // we need non-null states
        if (currentState == null || previousState == null) {
            _armed = false;
            return false;
        }

        final MouseState mouseState = currentState.getMouseState();
        final long now = System.currentTimeMillis();

        // if we were not armed before...
        if (!_armed) {
            // only arm if we are pressing the button anew
            if (mouseState.getButtonsPressedSince(previousState.getMouseState()).contains(_button)) {
                _armed = true;
                _armedX = mouseState.getX();
                _armedY = mouseState.getY();
                _armedAt = now;
            }
            return false;
        }

        if (mouseState.getButtonsPressedSince(previousState.getMouseState()).size() > 0) {
            _armed = false;
            return false;
        }

        final float dx = _armedX - mouseState.getX();
        final float dy = _armedY - mouseState.getY();
        // we must be armed, so check if we should still be armed... Button should be down and we should not have
        // drifted too far from our initial pressed location.
        if (mouseState.getButtonState(_button) != ButtonState.DOWN || MathUtils.sqrt(dx * dx + dy * dy) > _maxDrift) {
            _armed = false;
            return false;
        }

        // armed, and not drifting. Return if enough time has passed.
        if (now - _armedAt > _triggerTimeMS) {
            _armed = false;
            return true;
        }

        return false;
    }
}
