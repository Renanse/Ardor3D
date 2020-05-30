/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.border;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.renderer.Renderer;

/**
 * A simple border that does not paint in the border area. This border can be used for adding fixed
 * size padding or spacing between components.
 */
public class EmptyBorder extends UIBorder {

  /**
   * Create a new border. All sides are zero size.
   */
  public EmptyBorder() {
    this(0, 0, 0, 0);
  }

  /**
   * Create a new border with the given edge sizes
   */
  public EmptyBorder(final int top, final int left, final int bottom, final int right) {
    super(top, left, bottom, right);
  }

  @Override
  public void draw(final Renderer renderer, final UIComponent comp) {
    // nothing to do here
  }

}
