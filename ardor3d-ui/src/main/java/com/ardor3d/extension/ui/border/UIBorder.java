/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.border;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.renderer.Renderer;

/**
 * Defines a class responsible for drawing a "border" or "picture frame" drawn around the edges of a
 * UI component.
 */
public abstract class UIBorder extends Insets {

  /**
   * Construct a border with the given edge thicknesses.
   * 
   * @param top
   * @param left
   * @param bottom
   * @param right
   */
  public UIBorder(final int top, final int left, final int bottom, final int right) {
    super(top, left, bottom, right);
  }

  /**
   * Draw this border. Override this method to do the actual work.
   * 
   * @param renderer
   *          the renderer to use in drawing.
   * @param component
   *          the UIComponent we are drawing the border for.
   */
  public abstract void draw(Renderer renderer, UIComponent component);

  /**
   * Calculate the total height of a border for a given component. This is the current inner height of
   * the component plus the top and bottom paddings and border thicknesses.
   * 
   * @param comp
   *          the component to check against
   * @return the height
   */
  public static int getBorderHeight(final UIComponent component) {
    int height = component.getContentHeight();
    if (component.getPadding() != null) {
      height += component.getPadding().getTop() + component.getPadding().getBottom();
    }
    if (component.getBorder() != null) {
      height += component.getBorder().getTop() + component.getBorder().getBottom();
    }
    return height;
  }

  /**
   * Calculate the total width of a border for a given component. This is the current inner width of
   * the component plus the left and right paddings and border thicknesses.
   * 
   * @param comp
   *          the component to check against
   * @return the width
   */
  public static int getBorderWidth(final UIComponent component) {
    int height = component.getContentWidth();
    if (component.getPadding() != null) {
      height += component.getPadding().getLeft() + component.getPadding().getRight();
    }
    if (component.getBorder() != null) {
      height += component.getBorder().getLeft() + component.getBorder().getRight();
    }
    return height;
  }
}
