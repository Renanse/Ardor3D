/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.functions;

/**
 * A variation of fBm that uses absolute value to recast the extreme ends to the upper range and
 * values near 0 to the lower range. This bunches together values into clusters that can resemble
 * clouds or cotton balls.
 */
public class CloudsFunction3D extends FbmFunction3D {

  public static final int MAX_OCTAVES = 32;

  public CloudsFunction3D(final Function3D source, final int octaves, final double frequency, final double persistence,
    final double lacunarity) {
    super(source, octaves, frequency, persistence, lacunarity);
  }

  @Override
  protected double getValue(final double dx, final double dy, final double dz) {
    // Basically the "cloudy" |f(x)| you can find in perlin's docs and elsewhere, but rescaled to [-1,
    // 1] to keep it
    // in the range used by most of our function.
    return 2.0 * Math.abs(getSource().eval(dx, dy, dz)) - 1.0;
  }
}
