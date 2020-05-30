/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.backdrop;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.renderer.Renderer;

/**
 * Defines a class responsible for drawing a "backdrop" or screen/background that lays behind a UI
 * component.
 */
public abstract class UIBackdrop {

  /**
   * Draw this backdrop. Override this method to do the actual work.
   * 
   * @param renderer
   *          the renderer to use in drawing.
   * @param component
   *          the component we are drawing the background for.
   */
  public abstract void draw(final Renderer renderer, final UIComponent comp);

  /**
   * Get the height that a backdrop should cover for a given component. The height is the component's
   * current content area height, plus any padding on the top and bottom.
   * 
   * @param component
   *          the component to check against
   * @return the height as described above
   */
  public static int getBackdropHeight(final UIComponent component) {
    if (component.getPadding() == null) {
      return component.getContentHeight();
    } else {
      return component.getContentHeight() + component.getPadding().getTop() + component.getPadding().getBottom();
    }
  }

  /**
   * Get the width that a backdrop should cover for a given component. The width is the component's
   * current content area width, plus any padding on the left and right.
   * 
   * @param component
   *          the component to check against
   * @return the width as described above
   */
  public static int getBackdropWidth(final UIComponent component) {
    if (component.getPadding() == null) {
      return component.getContentWidth();
    } else {
      return component.getContentWidth() + component.getPadding().getRight() + component.getPadding().getLeft();
    }
  }
}
