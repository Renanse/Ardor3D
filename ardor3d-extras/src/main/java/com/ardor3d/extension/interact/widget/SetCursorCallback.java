/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.interact.widget;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.MouseManager;
import com.ardor3d.input.MouseState;

public class SetCursorCallback implements InteractMouseOverCallback {

    private final MouseCursor _cursor;
    private Canvas _lastEnteredCanvas;

    public SetCursorCallback(final MouseCursor cursor) {
        _cursor = cursor;
    }

    @Override
    public void mouseEntered(final Canvas source, final MouseState current, final InteractManager manager) {
        if (source == null) {
            return;
        }

        final MouseManager mm = source.getMouseManager();
        if (mm != null) {
            _lastEnteredCanvas = source;
            mm.setCursor(_cursor);
        }
    }

    @Override
    public void mouseDeparted(Canvas source, final MouseState current, final InteractManager manager) {
        if (source == null) {
            if (_lastEnteredCanvas != null) {
                source = _lastEnteredCanvas;
            } else {
                return;
            }
        }

        final MouseManager mm = source.getMouseManager();
        if (mm != null) {
            mm.setCursor(null);
        }
        _lastEnteredCanvas = null;
    }

}
