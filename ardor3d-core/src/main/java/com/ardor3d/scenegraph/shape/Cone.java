/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.shape;

public class Cone extends Cylinder {

  public Cone() {}

  public Cone(final String name, final int axisSamples, final int radialSamples, final double radius,
    final double height) {
    this(name, axisSamples, radialSamples, radius, height, true);
  }

  public Cone(final String name, final int axisSamples, final int radialSamples, final double radius,
    final double height, final boolean closed) {
    super(name, axisSamples, radialSamples, radius, height, closed);
    setRadius2(0);
  }

  public void setHalfAngle(final double radians) {
    setRadius1(Math.tan(radians));
  }
}
