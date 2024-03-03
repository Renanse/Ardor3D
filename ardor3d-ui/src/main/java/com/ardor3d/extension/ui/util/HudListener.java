/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.util;

import com.ardor3d.extension.ui.UIComponent;

/**
 * An interface for 3rd party objects interested in notifications about hud changes.
 */
public interface HudListener {
  /**
   * Called when a component is added to the UIHud.
   * 
   * @param component
   *          the added component
   */
  void componentAdded(UIComponent component);

  /**
   * Called when a component is removed from the UIHud.
   * 
   * @param component
   *          the removed component
   */
  void componentRemoved(UIComponent component);
}
