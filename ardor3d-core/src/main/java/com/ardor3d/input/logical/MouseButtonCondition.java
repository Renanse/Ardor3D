/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.logical;

import java.util.EnumMap;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.InputState;
import com.ardor3d.input.MouseButton;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

/**
 * A condition that checks the state of the two most commonly used mouse buttons.
 */
@Immutable
public final class MouseButtonCondition implements Predicate<TwoInputStates> {
    private final EnumMap<MouseButton, ButtonState> _states = Maps.newEnumMap(MouseButton.class);

    public MouseButtonCondition(final EnumMap<MouseButton, ButtonState> states) {
        _states.putAll(states);
    }

    public MouseButtonCondition(final ButtonState left, final ButtonState right, final ButtonState middle) {
        if (left != ButtonState.UNDEFINED) {
            _states.put(MouseButton.LEFT, left);
        }
        if (right != ButtonState.UNDEFINED) {
            _states.put(MouseButton.RIGHT, right);
        }
        if (middle != ButtonState.UNDEFINED) {
            _states.put(MouseButton.MIDDLE, middle);
        }
    }

    public boolean apply(final TwoInputStates states) {
        final InputState currentState = states.getCurrent();

        if (currentState == null) {
            return false;
        }

        for (final MouseButton button : _states.keySet()) {
            final ButtonState required = _states.get(button);
            if (required != ButtonState.UNDEFINED) {
                if (currentState.getMouseState().getButtonState(button) != required) {
                    return false;
                }
            }
        }
        return true;
    }
}
