/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.jogl;

import com.ardor3d.framework.jogl.NewtWindowContainer;
import com.ardor3d.input.FocusWrapper;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;


public class JoglNewtFocusWrapper extends WindowAdapter implements FocusWrapper {

    protected volatile boolean _focusLost = false;

    protected final GLWindow _newtWindow;

    public JoglNewtFocusWrapper(final NewtWindowContainer newtWindowContainer) {
        _newtWindow = newtWindowContainer.getNewtWindow();
    }
    
    @Override
    public void windowGainedFocus(final WindowEvent e) {
        // do nothing
    }

    @Override
    public void windowLostFocus(final WindowEvent e) {
        _focusLost = true;
    }

    @Override
    public boolean getAndClearFocusLost() {
        final boolean result = _focusLost;

        _focusLost = false;

        return result;
    }

    @Override
    public void init() {
        _newtWindow.addWindowListener(this);
    }

}
