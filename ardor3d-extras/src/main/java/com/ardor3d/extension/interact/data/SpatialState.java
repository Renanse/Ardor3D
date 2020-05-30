/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.data;

import com.ardor3d.math.Transform;
import com.ardor3d.scenegraph.Spatial;

public class SpatialState {

  protected Transform _transform = new Transform();

  public SpatialState() {}

  /** copy constructor */
  public SpatialState(final SpatialState toCopy) {
    _transform.set(toCopy._transform);
  }

  public Transform getTransform() { return _transform; }

  public void copyState(final Spatial source) {
    _transform.set(source.getTransform());
  }

  public void applyState(final Spatial target) {
    target.setTransform(_transform);
  }

}
