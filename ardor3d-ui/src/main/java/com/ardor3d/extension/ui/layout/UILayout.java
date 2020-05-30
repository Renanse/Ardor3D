/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.layout;

import com.ardor3d.extension.ui.UIContainer;

/**
 * A class that can arrange the position and dimensions of the contents of a UI container.
 */
public abstract class UILayout {

  /**
   * Perform the actual layout of the contents in the given container.
   * 
   * @param container
   *          the container to layout
   */
  public abstract void layoutContents(UIContainer container);

  /**
   * Update the minimum size of this container, based on the contents of the provided container and
   * this layout.
   * 
   * @param container
   *          the container to update
   */
  public abstract void updateMinimumSizeFromContents(UIContainer container);
}
