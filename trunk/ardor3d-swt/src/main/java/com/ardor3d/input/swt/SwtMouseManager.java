/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import com.ardor3d.image.util.SWTImageUtil;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.MouseManager;

/**
 * Implementation of the {@link com.ardor3d.input.MouseManager} interface for use with SWT. This implementation supports
 * the optional {@link #setGrabbed(com.ardor3d.input.GrabbedState)} and {@link #setPosition(int, int)} methods. The
 * constructor takes an SWT {@link org.eclipse.swt.widgets.Control} instance, for which the cursor is set. In a
 * multi-canvas application, each canvas can have its own SwtMouseManager instance, or it is possible to use a single
 * one for the SWT container that includes the control.
 */
public class SwtMouseManager implements MouseManager {

    private static Cursor _transparentCursor;

    private final Control _control;

    /** our current grabbed state */
    private GrabbedState _grabbedState;

    /** Our cursor prior to a setGrabbed(GRABBED) operation. Stored to be used when cursor is "ungrabbed" */
    private Cursor _pregrabCursor;

    public SwtMouseManager(final Control control) {
        _control = control;
    }

    public void setCursor(final MouseCursor cursor) {
        if (cursor == MouseCursor.SYSTEM_DEFAULT) {
            _control.setCursor(null);
            return;
        }

        final ImageData imageData = SWTImageUtil.convertToSWT(cursor.getImage()).get(0);

        final Cursor swtCursor = new Cursor(_control.getDisplay(), imageData, cursor.getHotspotX(), cursor
                .getHotspotY());

        _control.setCursor(swtCursor);
    }

    public void setPosition(final int x, final int y) {
        final Point p = new Point(x, _control.getSize().y - y);
        _control.getDisplay().setCursorLocation(_control.toDisplay(p));
    }

    public void setGrabbed(final GrabbedState grabbedState) {
        if (!isSetGrabbedSupported()) {
            throw new UnsupportedOperationException();
        }

        // check if we should be here.
        if (_grabbedState == grabbedState) {
            return;
        }

        // remember our grabbed state mode.
        _grabbedState = grabbedState;

        if (grabbedState == GrabbedState.GRABBED) {
            // remember our old cursor
            _pregrabCursor = _control.getCursor();

            // set our cursor to be invisible
            _control.setCursor(getTransparentCursor());
        } else {
            // restore our old cursor
            _control.setCursor(_pregrabCursor);
        }
    }

    private final Cursor getTransparentCursor() {
        if (_transparentCursor == null) {
            final Display display = _control.getDisplay();
            final Color white = display.getSystemColor(SWT.COLOR_WHITE);
            final Color black = display.getSystemColor(SWT.COLOR_BLACK);
            final PaletteData palette = new PaletteData(new RGB[] { white.getRGB(), black.getRGB() });
            final ImageData sourceData = new ImageData(16, 16, 1, palette);
            sourceData.transparentPixel = 0;
            _transparentCursor = new Cursor(display, sourceData, 0, 0);
        }
        return _transparentCursor;
    }

    public boolean isSetPositionSupported() {
        return true;
    }

    public boolean isSetGrabbedSupported() {
        return true;
    }

    public GrabbedState getGrabbed() {
        return _grabbedState;
    }
}