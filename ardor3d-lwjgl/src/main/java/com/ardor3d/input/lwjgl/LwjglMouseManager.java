/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.lwjgl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.MouseManager;
import com.ardor3d.util.geom.BufferUtils;

/**
 * LWJGL-specific implementation of the {@link com.ardor3d.input.MouseManager} interface. No methods in this class
 * should be called before the LWJGL Display has been initialized, since this class is dependent on being able to
 * initialize the {@link org.lwjgl.input.Mouse} class.
 */
public class LwjglMouseManager implements MouseManager {
    private boolean _inited = false;

    private void init() {
        if (!_inited) {
            if (!Mouse.isCreated()) {
                try {
                    Mouse.create();
                } catch (final Exception e) {
                    // this typically happens if the Display hasn't been initialized.
                    throw new RuntimeException("Unable to initialise mouse manager", e);
                }
            }
            _inited = true;
        }
    }

    @MainThread
    public void setCursor(final MouseCursor cursor) {
        init();

        try {
            final Cursor lwjglCursor = createLwjglCursor(cursor);

            if (lwjglCursor == null || !lwjglCursor.equals(Mouse.getNativeCursor())) {
                Mouse.setNativeCursor(lwjglCursor);
            }
        } catch (final LWJGLException e) {
            throw new RuntimeException("Unable to set cursor", e);
        }
    }

    private Cursor createLwjglCursor(final MouseCursor cursor) throws LWJGLException {
        if (cursor == MouseCursor.SYSTEM_DEFAULT || cursor == null) {
            return null; // setting the cursor to null in LWJGL means using the system default one
        }

        final boolean eightBitAlpha = (Cursor.getCapabilities() & Cursor.CURSOR_8_BIT_ALPHA) != 0;

        final Image image = cursor.getImage();

        final boolean isRgba = image.getDataFormat() == ImageDataFormat.RGBA;
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();

        final ByteBuffer imageData = image.getData(0);
        imageData.rewind();
        final IntBuffer imageDataCopy = BufferUtils.createIntBuffer(imageWidth * imageHeight);

        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                final int index = y * imageWidth + x;

                int r = imageData.get() & 0xff;
                int g = imageData.get() & 0xff;
                int b = imageData.get() & 0xff;
                int a = 0xff;
                if (isRgba) {
                    a = imageData.get() & 0xff;
                    if (!eightBitAlpha) {
                        if (a < 0x7f) {
                            a = 0x00;
                            // small hack to prevent triggering "reverse screen" on windows.
                            r = g = b = 0;
                        } else {
                            a = 0xff;
                        }
                    }
                }

                imageDataCopy.put(index, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return new Cursor(imageWidth, imageHeight, cursor.getHotspotX(), cursor.getHotspotY(), 1, imageDataCopy, null);
    }

    public void setPosition(final int x, final int y) {
        init();

        Mouse.setCursorPosition(x, y);
    }

    public void setGrabbed(final GrabbedState grabbedState) {
        init();

        switch (grabbedState) {
            case GRABBED:
                Mouse.setGrabbed(true);
                break;
            case NOT_GRABBED:
                Mouse.setGrabbed(false);
                break;
            default:
                throw new IllegalStateException("Unhandled GrabbedState: " + grabbedState);
        }
    }

    public boolean isSetPositionSupported() {
        return true;
    }

    public boolean isSetGrabbedSupported() {
        return true;
    }

    public GrabbedState getGrabbed() {
        return Mouse.isGrabbed() ? GrabbedState.GRABBED : GrabbedState.NOT_GRABBED;
    }
}
