/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.event;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIDrawer;
import com.ardor3d.extension.ui.UIDrawer.DrawerEdge;
import com.ardor3d.math.Rectangle2;

public class DrawerDragListener implements DragListener {
  int oldX = 0;
  int oldY = 0;
  protected final UIDrawer uiDrawer;

  public DrawerDragListener(final UIDrawer drawer) {
    uiDrawer = drawer;
  }

  @Override
  public void startDrag(final int mouseX, final int mouseY) {
    oldX = mouseX;
    oldY = mouseY;
  }

  @Override
  public void drag(final int mouseX, final int mouseY) {
    if (!uiDrawer.isDraggable()) {
      return;
    }
    // check if we are off the edge... if so, flag for redraw (part of the frame may have been hidden)
    if (!smallerThanWindow()) {
      uiDrawer.fireComponentDirty();
    }

    if (uiDrawer.getEdge() == DrawerEdge.BOTTOM || uiDrawer.getEdge() == DrawerEdge.TOP) {
      uiDrawer.addTranslation(mouseX - oldX, 0, 0);
    } else {
      uiDrawer.addTranslation(0, mouseY - oldY, 0);
    }
    oldX = mouseX;
    oldY = mouseY;

    // check if we are off the edge now... if so, flag for redraw (part of the frame may have been
    // hidden)
    if (!smallerThanWindow()) {
      uiDrawer.fireComponentDirty();
    }
  }

  /**
   * @return true if this frame can be fully contained by the hud.
   */
  public boolean smallerThanWindow() {
    final int dispWidth = uiDrawer.getHud().getWidth();
    final int dispHeight = uiDrawer.getHud().getHeight();
    final Rectangle2 rect = uiDrawer.getRelativeComponentBounds(null);
    return rect.getWidth() <= dispWidth && rect.getHeight() <= dispHeight;
  }

  /**
   * Do nothing.
   */
  @Override
  public void endDrag(final UIComponent component, final int mouseX, final int mouseY) {}

  /**
   * Check if we are dragging's the frames title bar label.
   */
  @Override
  public boolean isDragHandle(final UIComponent component, final int mouseX, final int mouseY) {
    return component == uiDrawer.getTitleBar().getTitleLabel();
  }
}
