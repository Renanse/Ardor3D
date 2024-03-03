/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
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
 * A simple backdrop that does not alter the content area.
 */
public class EmptyBackdrop extends UIBackdrop {
  public EmptyBackdrop() {}

  @Override
  public void draw(final Renderer renderer, final UIComponent comp) {
    // We did not affect the content area, so set it as "virgin"
    comp.setVirginContentArea(true);
  }
}
