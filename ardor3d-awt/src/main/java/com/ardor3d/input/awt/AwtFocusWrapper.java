/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.awt;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import com.ardor3d.input.Focus.FocusWrapper;

/**
 * Focus listener class for use with AWT.
 */
public class AwtFocusWrapper implements FocusWrapper, FocusListener {
    protected volatile boolean _focusLost = false;

    protected final Component _component;

    public AwtFocusWrapper(final Component component) {
        _component = component;
    }

    public void focusGained(final FocusEvent e) {
    // do nothing
    }

    public void focusLost(final FocusEvent e) {
        _focusLost = true;
    }

    public boolean getAndClearFocusLost() {
        final boolean result = _focusLost;

        _focusLost = false;

        return result;
    }

    public void init() {
        _component.addFocusListener(this);
    }
}
