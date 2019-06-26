/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.dummy;

import com.ardor3d.input.Focus.FocusWrapper;

/**
 * A "do-nothing" implementation of FocusWrapper useful when you want to ignore (or do not need) focus events.
 */
public class DummyFocusWrapper implements FocusWrapper {
    public static final DummyFocusWrapper INSTANCE = new DummyFocusWrapper();

    public void init() {
        ; // ignore, does nothing
    }

    public boolean getAndClearFocusLost() {
        return false;
    }
}
