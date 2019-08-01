/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text;

import com.ardor3d.input.InputState;
import com.ardor3d.input.keyboard.Key;

public interface UIKeyHandler {

    public static double KeyRepeatIntervalTime = 1 / 25.0;
    public static double KeyRepeatStartTime = 1.0;

    boolean keyPressed(Key key, InputState state);

    boolean keyReleased(Key key, InputState state);

    boolean keyHeld(Key key, InputState state);

    boolean characterReceived(char value, InputState state);
}
