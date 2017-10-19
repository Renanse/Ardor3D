/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.event;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIFrameStatusBar;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;

public final class FrameResizeListener implements DragListener {
    private int _initialX;
    private int _initialY;
    private int _initialLocalComponentWidth;
    private int _initialLocalComponentHeight;
    private final Transform _initFrameTransform = new Transform();
    private final UIFrameStatusBar _uiFrameStatusBar;

    public FrameResizeListener(final UIFrameStatusBar uiFrameStatusBar) {
        _uiFrameStatusBar = uiFrameStatusBar;
    }

    public void startDrag(final int mouseX, final int mouseY) {
        final Vector3 vec = Vector3.fetchTempInstance();
        vec.set(mouseX, mouseY, 0);
        _uiFrameStatusBar.getWorldTransform().applyInverseVector(vec);

        _initialX = Math.round(vec.getXf());
        _initialY = Math.round(vec.getYf());
        Vector3.releaseTempInstance(vec);

        final UIFrame frame = UIFrame.findParentFrame(_uiFrameStatusBar);
        _initialLocalComponentWidth = frame.getLocalComponentWidth();
        _initialLocalComponentHeight = frame.getLocalComponentHeight();

        _initFrameTransform.set(frame.getWorldTransform());
    }

    public void drag(final int mouseX, final int mouseY) {
        resizeFrameByPosition(mouseX, mouseY);
    }

    public void endDrag(final UIComponent component, final int mouseX, final int mouseY) {
        resizeFrameByPosition(mouseX, mouseY);
    }

    private void resizeFrameByPosition(final int mouseX, final int mouseY) {

        final Vector3 vec = Vector3.fetchTempInstance();
        vec.set(mouseX, mouseY, 0);
        _uiFrameStatusBar.getWorldTransform().applyInverseVector(vec);

        final int x = Math.round(vec.getXf());
        final int y = Math.round(vec.getYf());

        final UIFrame frame = UIFrame.findParentFrame(_uiFrameStatusBar);

        // Set the new width to the initial width + the change in mouse x position.
        int newWidth = _initialLocalComponentWidth + x - _initialX;
        if (newWidth < frame.getMinimumLocalComponentWidth()) {
            // don't let us get smaller than frame min size
            newWidth = frame.getMinimumLocalComponentWidth();
        }
        if (newWidth > frame.getMaximumLocalComponentWidth()) {
            // don't let us get bigger than frame max size
            newWidth = frame.getMaximumLocalComponentWidth();
        }

        // Set the new height to the initial height + the change in mouse y position.
        int newHeight = _initialLocalComponentHeight - (y - _initialY);
        if (newHeight < frame.getMinimumLocalComponentHeight()) {
            // don't let us get smaller than frame min size
            newHeight = frame.getMinimumLocalComponentHeight();
        }
        if (newHeight > frame.getMaximumLocalComponentHeight()) {
            // don't let us get bigger than frame max size
            newHeight = frame.getMaximumLocalComponentHeight();
        }

        frame.setLocalComponentSize(newWidth, newHeight);

        vec.set(0, _initialLocalComponentHeight - newHeight, 0);
        _initFrameTransform.applyForwardVector(vec);
        frame.setTransform(_initFrameTransform);
        frame.addTranslation(vec);
        Vector3.releaseTempInstance(vec);

        frame.layout();
    }

    public boolean isDragHandle(final UIComponent component, final int mouseX, final int mouseY) {
        return component == _uiFrameStatusBar.getResizeButton();
    }
}
