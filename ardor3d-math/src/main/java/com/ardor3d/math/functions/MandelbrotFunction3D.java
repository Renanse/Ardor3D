/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.functions;

/**
 * A function that returns the famous Mandelbrot set. This is not really 3d... The z factor is
 * ignored.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Mandelbrot_set">wikipedia entry</a>
 */
public class MandelbrotFunction3D implements Function3D {

  private int _iterations;

  public MandelbrotFunction3D(final int iterations) {
    setIterations(iterations);
  }

  @Override
  public double eval(final double x, final double y, final double z) {
    double dx = 0;
    double dy = 0;

    int iteration = 0;

    while (dx * dx + dy * dy <= (2 * 2) && iteration < _iterations) {
      final double xtemp = dx * dx - dy * dy + x;
      dy = 2 * dx * dy + y;
      dx = xtemp;
      iteration++;
    }

    if (iteration == _iterations) {
      return 1;
    } else {
      // returns a value in [-1, 1]
      return 2 * (iteration / (double) _iterations) - 1;
    }
  }

  public void setIterations(final int iterations) { _iterations = iterations; }

  public int getIterations() { return _iterations; }
}
