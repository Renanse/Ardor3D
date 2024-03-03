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

import com.ardor3d.math.util.MathUtils;

/**
 * An implementation of an Archimedean spiral which supports any number of spiral arms and optional
 * turbulence.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Archimedean_spiral">wikipedia entry</a>
 */
public class ArchimedeanSpiralFunction3D implements Function3D {

  private static final int DEFAULT_ROUGHNESS = 1;
  private static final double DEFAULT_FREQUENCY = 0.2;
  private static final Function3D DEFAULT_TURBULENCE =
      new FbmFunction3D(Functions.simplexNoise(), DEFAULT_ROUGHNESS, DEFAULT_FREQUENCY, 0.5, 2.0);

  private final int _numArms;
  private final Function3D _turbulenceFunction;

  /**
   * Create the function for the specified number of arms, and optionally use the default turbulence.
   *
   * @param numArms
   *          The number of arms of the spiral (1 or more).
   * @param useDefaultTurbulence
   *          True if the default turbulence should be used; false for no turbulence.
   */
  public ArchimedeanSpiralFunction3D(final int numArms, final boolean useDefaultTurbulence) {
    this(numArms, useDefaultTurbulence ? DEFAULT_TURBULENCE : null);
  }

  /**
   * Create the function for the specified number of arms, with the specified turbulence.
   *
   * @param numArms
   *          The number of arms of the spiral (1 or more).
   * @param turbulenceFunction
   *          The turbulence function to use (can be null).
   */
  public ArchimedeanSpiralFunction3D(final int numArms, final Function3D turbulenceFunction) {
    _numArms = numArms;
    _turbulenceFunction = turbulenceFunction;
  }

  /**
   * Evaluate the function.
   *
   * @return A result which is generally, but not always, in the -1 to 1 range.
   */
  @Override
  public double eval(final double x, final double y, final double z) {
    final double radius = Math.sqrt(x * x + y * y);

    double phi;
    if (radius == 0.0) {
      phi = 0.0;
    } else {
      if (x < 0.0) {
        phi = 3.0 * MathUtils.HALF_PI - Math.asin(y / radius);
      } else {
        phi = MathUtils.HALF_PI + Math.asin(y / radius);
      }
    }

    final double turbulence = (_turbulenceFunction != null) ? _turbulenceFunction.eval(x, y, z) : 0.0;

    double value = z + radius + ((_numArms * phi) / MathUtils.TWO_PI) + turbulence;

    value = value % 1.0;
    value = (value * 2.0) - 1.0;

    return value;
  }
}
