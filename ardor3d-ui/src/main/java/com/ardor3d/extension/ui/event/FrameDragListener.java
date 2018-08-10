/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
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
import com.ardor3d.math.Rectangle2;

/**
 * The drag listener responsible for allowing a frame to be moved around the screen with the mouse.
 */
public class FrameDragListener implements DragListener {
    int oldX = 0;
    int oldY = 0;
    protected final UIFrame uiFrame;

    public FrameDragListener(final UIFrame uiFrame) {
        this.uiFrame = uiFrame;
    }

    public void startDrag(final int mouseX, final int mouseY) {
        oldX = mouseX;
        oldY = mouseY;
    }

    public void drag(final int mouseX, final int mouseY) {
        if (!uiFrame.isDraggable()) {
            return;
        }
        // check if we are off the edge... if so, flag for redraw (part of the frame may have been hidden)
        if (!smallerThanWindow()) {
            uiFrame.fireComponentDirty();
        }

        uiFrame.addTranslation(mouseX - oldX, mouseY - oldY, 0);
        oldX = mouseX;
        oldY = mouseY;

        // check if we are off the edge now... if so, flag for redraw (part of the frame may have been hidden)
        if (!smallerThanWindow()) {
            uiFrame.fireComponentDirty();
        }
    }

    /**
     * @return true if this frame can be fully contained by the hud.
     */
    public boolean smallerThanWindow() {
        final int dispWidth = uiFrame.getHud().getWidth();
        final int dispHeight = uiFrame.getHud().getHeight();
        final Rectangle2 rect = uiFrame.getRelativeComponentBounds(null);
        return rect.getWidth() <= dispWidth && rect.getHeight() <= dispHeight;
    }

    /**
     * Do nothing.
     */
    public void endDrag(final UIComponent component, final int mouseX, final int mouseY) {}

    /**
     * Check if we are dragging's the frames title bar label.
     */
    public boolean isDragHandle(final UIComponent component, final int mouseX, final int mouseY) {
        return component == uiFrame.getTitleBar().getTitleLabel();
    }
}
