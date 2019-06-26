/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

import java.text.MessageFormat;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.character.CharacterInputState;
import com.ardor3d.input.controller.ControllerState;
import com.ardor3d.input.gesture.GestureState;
import com.ardor3d.input.keyboard.KeyboardState;
import com.ardor3d.input.mouse.MouseState;

/**
 * The total input state of the devices that are being handled.
 */
@Immutable
public class InputState {
    public static final InputState LOST_FOCUS = new InputState(KeyboardState.NOTHING, MouseState.NOTHING,
            ControllerState.NOTHING, GestureState.NOTHING, CharacterInputState.NOTHING);
    public static final InputState EMPTY = new InputState(KeyboardState.NOTHING, MouseState.NOTHING,
            ControllerState.NOTHING, GestureState.NOTHING, CharacterInputState.NOTHING);

    private final KeyboardState _keyboardState;
    private final MouseState _mouseState;
    private final ControllerState _controllerState;
    private final GestureState _gestureState;
    private final CharacterInputState _characterState;

    /**
     * Creates a new instance.
     *
     * @param keyboardState
     *            a non-null KeyboardState instance
     * @param mouseState
     *            a non-null MouseState instance
     * @param controllerState
     *            a non-null ControllerState instance
     * @param gestureState
     *            a non-null GestureState instance
     * @param characterState
     *            a non-null CharacterInputState instance
     * @throws NullPointerException
     *             if any parameter is null
     */
    public InputState(final KeyboardState keyboardState, final MouseState mouseState,
            final ControllerState controllerState, final GestureState gestureState,
            final CharacterInputState characterState) {
        if (keyboardState == null) {
            throw new NullPointerException("Keyboard state");
        }

        if (mouseState == null) {
            throw new NullPointerException("Mouse state");
        }

        if (controllerState == null) {
            throw new NullPointerException("Controller state");
        }

        if (gestureState == null) {
            throw new NullPointerException("Gesture state");
        }

        _keyboardState = keyboardState;
        _mouseState = mouseState;
        _controllerState = controllerState;
        _gestureState = gestureState;
        _characterState = characterState;
    }

    public KeyboardState getKeyboardState() {
        return _keyboardState;
    }

    public MouseState getMouseState() {
        return _mouseState;
    }

    public ControllerState getControllerState() {
        return _controllerState;
    }

    public GestureState getGestureState() {
        return _gestureState;
    }

    public CharacterInputState getCharacterState() {
        return _characterState;
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "InputState[keyboardState={0}, mouseState={1}, controllerState={2}, gestureState={3}, characterState={4}]",
                _keyboardState, _mouseState, _controllerState, _gestureState, _characterState);
    }
}
