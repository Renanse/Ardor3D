/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.visitor;

import com.ardor3d.scenegraph.Spatial;

public interface Visitor {

  /**
   * Execute our logic on the given Spatial
   * 
   * @param spatial
   */
  void visit(Spatial spatial);
}
