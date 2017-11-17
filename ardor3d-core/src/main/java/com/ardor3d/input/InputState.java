/**
 * Copyright (c) 2008-2017 Ardor Labs, Inc.
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
import com.ardor3d.input.gestures.GestureState;

/**
 * The total input state of the devices that are being handled.
 */
@Immutable
public class InputState {
    public static final InputState LOST_FOCUS = new InputState(KeyboardState.NOTHING, MouseState.NOTHING,
            ControllerState.NOTHING, GestureState.NOTHING);
    public static final InputState EMPTY = new InputState(KeyboardState.NOTHING, MouseState.NOTHING,
            ControllerState.NOTHING, GestureState.NOTHING);

    private final KeyboardState keyboardState;
    private final MouseState mouseState;
    private final ControllerState controllerState;
    private final GestureState gestureState;

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
     * @throws NullPointerException
     *             if any parameter is null
     */
    public InputState(final KeyboardState keyboardState, final MouseState mouseState,
            final ControllerState controllerState, final GestureState gestureState) {
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

        this.keyboardState = keyboardState;
        this.mouseState = mouseState;
        this.controllerState = controllerState;
        this.gestureState = gestureState;
    }

    public KeyboardState getKeyboardState() {
        return keyboardState;
    }

    public MouseState getMouseState() {
        return mouseState;
    }

    public ControllerState getControllerState() {
        return controllerState;
    }

    public GestureState getGestureState() {
        return gestureState;
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "InputState[keyboardState={0}, mouseState={1}, controllerState={2}, controllerState={3}]",
                keyboardState, mouseState, controllerState, gestureState);
    }
}
