/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

import com.ardor3d.annotation.Immutable;

/**
 * The total input state of the devices that are being handled.
 */
@Immutable
public class InputState {
    public static final InputState LOST_FOCUS = new InputState(KeyboardState.NOTHING, MouseState.NOTHING,
            ControllerState.NOTHING);
    public static final InputState EMPTY = new InputState(KeyboardState.NOTHING, MouseState.NOTHING,
            ControllerState.NOTHING);

    private final KeyboardState keyboardState;
    private final MouseState mouseState;
    private final ControllerState controllerState;

    /**
     * Creates a new instance.
     * 
     * @param keyboardState
     *            a non-null KeyboardState instance
     * @param mouseState
     *            a non-null MouseState instance
     * @throws NullPointerException
     *             if either parameter is null
     */
    public InputState(final KeyboardState keyboardState, final MouseState mouseState,
            final ControllerState controllerState) {
        if (keyboardState == null) {
            throw new NullPointerException("Keyboard state");
        }

        if (mouseState == null) {
            throw new NullPointerException("Mouse state");
        }

        if (controllerState == null) {
            throw new NullPointerException("Controller state");
        }

        this.keyboardState = keyboardState;
        this.mouseState = mouseState;
        this.controllerState = controllerState;
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

    @Override
    public String toString() {
        return "InputState{" + "keyboardState=" + keyboardState + ", mouseState=" + mouseState + ", controllerState="
                + controllerState + '}';
    }
}
