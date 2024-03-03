/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.framework;

import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.renderer.Renderable;

/**
 * Owns all the data that is related to the scene. This class should not really know anything about
 * rendering or the screen, it's just the scene data.
 */
public interface Scene extends Renderable {
  /**
   * A scene should be able to handle a pick execution as it is the only thing that has a complete
   * picture of the scenegraph(s).
   *
   * @param pickRay
   */
  PickResults doPick(Ray3 pickRay);
}
