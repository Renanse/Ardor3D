/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.Focus;

/**
 * Describes the interface to implement to keep track of focus changes.
 */
public interface FocusWrapper {
    public boolean getAndClearFocusLost();

    void init();
}
