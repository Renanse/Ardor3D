/**
 * Copyright (c) 2008-2011 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.jogl;

import com.ardor3d.framework.jogl.NewtWindowContainer;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.MouseManager;
import com.jogamp.newt.opengl.GLWindow;


public class JoglNewtMouseManager implements MouseManager{

    /** our current grabbed state */
    private GrabbedState _grabbedState;
    
    private GLWindow _newtWindow;
    
    public JoglNewtMouseManager(final NewtWindowContainer newtWindowContainer) {
        _newtWindow = newtWindowContainer.getNewtWindow();
    }

    @Override
    public void setCursor(MouseCursor cursor) {
        //FIXME not supported by NEWT
    }

    @Override
    public void setPosition(final int x, final int y) {
        _newtWindow.warpPointer(x, _newtWindow.getHeight() - y);
    }

    @Override
    public void setGrabbed(GrabbedState grabbedState) {
        // check if we should be here.
        if (_grabbedState == grabbedState) {
            return;
        }
        
        // remember our grabbed state mode.
        _grabbedState = grabbedState;
        if (grabbedState == GrabbedState.GRABBED) {
            //FIXME remember our old cursor
            
            // set our cursor to be invisible
            _newtWindow.setPointerVisible(false);
        }
        else {
            //FIXME restore our old cursor
        
            // set our cursor to be visible
            _newtWindow.setPointerVisible(true);
        }
    }

    @Override
    public GrabbedState getGrabbed() {
        return _grabbedState;
    }

    @Override
    public boolean isSetPositionSupported() {
        return true;
    }

    @Override
    public boolean isSetGrabbedSupported() {
        return true;
    }

}
