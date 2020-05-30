/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.controller.interpolation;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Spatial;

/**
 * LinearVector3InterpolationController class interpolates a {@link Spatial}s vectors using
 * {@link Vector3#lerpLocal(ReadOnlyVector3, ReadOnlyVector3, double)}
 */
public class LinearVector3InterpolationController extends Vector3InterpolationController {

  /** Serial UID */
  private static final long serialVersionUID = 1L;

  @Override
  protected Vector3 interpolateVectors(final ReadOnlyVector3 from, final ReadOnlyVector3 to, final double delta,
      final Vector3 target) {

    assert (null != from) : "parameter 'from' can not be null";
    assert (null != to) : "parameter 'to' can not be null";

    return target.lerpLocal(from, to, delta);
  }

}
