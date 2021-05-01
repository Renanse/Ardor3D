/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.visitor;

import com.ardor3d.scenegraph.Spatial;

public class SetSpatialLayer implements Visitor {
  private final int _layer;

  public SetSpatialLayer(final int layer) {
    _layer = layer;
  }

  @Override
  public void visit(final Spatial spatial) {
    spatial.setLayer(_layer);
  }
}
