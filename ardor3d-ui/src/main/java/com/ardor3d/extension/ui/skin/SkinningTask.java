/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.skin;

import com.ardor3d.extension.ui.UIComponent;

/**
 * A simple call-back useful for skins in cases where skinning happens later, such as during dynamic
 * construction of a menu.
 */
public interface SkinningTask {

  /**
   * Perform skinning tasks on the given component
   * 
   * @param component
   *          the component to be skinned.
   */
  void skinComponent(UIComponent component);

}
