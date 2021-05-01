/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.controller;

import com.ardor3d.scenegraph.Spatial;

public interface SpatialController<T extends Spatial> {

  /**
   * @param time
   *          The time in seconds between the last call to update and the current one
   * @param caller
   *          The spatial currently executing this controller.
   */
  void update(double time, T caller);

}
