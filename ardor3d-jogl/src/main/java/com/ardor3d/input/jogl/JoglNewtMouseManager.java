/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.jogl;

import java.nio.ByteBuffer;

import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.PixelFormat;
import javax.media.nativewindow.util.PixelRectangle;

import com.ardor3d.framework.jogl.NewtWindowContainer;
import com.ardor3d.image.Image;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.MouseManager;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.opengl.GLWindow;

public class JoglNewtMouseManager implements MouseManager {

    /** our current grabbed state */
    private GrabbedState _grabbedState;

    private final GLWindow _newtWindow;

    private PointerIcon _previousPointerIcon;

    public JoglNewtMouseManager(final NewtWindowContainer newtWindowContainer) {
        _newtWindow = newtWindowContainer.getNewtWindow();
    }

    @Override
    public void setCursor(final MouseCursor cursor) {
        final Image image = cursor.getImage();
        final DimensionImmutable size = new Dimension(image.getWidth(), image.getHeight());
        final ByteBuffer pixels = image.getData(0);

        PixelFormat pixFormat = null;
        for (final PixelFormat pf : PixelFormat.values()) {
            if (pf.componentCount == image.getDataFormat().getComponents()
                    && pf.bytesPerPixel() == image.getDataType().getBytesPerPixel(pf.componentCount)) {
                pixFormat = pf;
                break;
            }
        }

        final PixelRectangle.GenericPixelRect rec = new PixelRectangle.GenericPixelRect(pixFormat, size, 0, false,
                pixels);
        _newtWindow.getScreen().getDisplay().createPointerIcon(rec, cursor.getHotspotX(), cursor.getHotspotY());
    }

    @Override
    public void setPosition(final int x, final int y) {
        _newtWindow.warpPointer(x, _newtWindow.getHeight() - y);
    }

    @Override
    public void setGrabbed(final GrabbedState grabbedState) {
        // check if we should be here.
        if (_grabbedState == grabbedState) {
            return;
        }

        // remember our grabbed state mode.
        _grabbedState = grabbedState;
        if (grabbedState == GrabbedState.GRABBED) {
            // remember our old cursor
            _previousPointerIcon = _newtWindow.getPointerIcon();
            // set our cursor to be invisible
            _newtWindow.setPointerVisible(false);
        } else {
            // restore our old cursor
            _newtWindow.setPointerIcon(_previousPointerIcon);
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
