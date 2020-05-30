/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.functions;

/**
 * Function which creates a diagonal grid of rounded 'holes'. The term "Mesh" is used here in its
 * common form, and does not refer to 3D geometry... the resulting pattern simply resembles a screen
 * or sieve.
 */
public class MeshFunction3D implements Function3D {

  private final double _lineSize;

  /**
   * Create a MeshFunction3D with a default lineSize of 0.5.
   */
  public MeshFunction3D() {
    this(0.5);
  }

  /**
   * Create a MeshFunction3D with the specified lineSize. Lower lineSize values will result in thinner
   * lines.
   * 
   * @param lineSize
   *          The line size, which should be greater than zero.
   */
  public MeshFunction3D(final double lineSize) {
    _lineSize = lineSize;
  }

  /**
   * Evaluate the function.
   */
  @Override
  public double eval(final double x, final double y, final double z) {
    final double value = (Math.sin(x) + Math.sin(y) + Math.sin(z)) / _lineSize;
    return ((value * value) * 2.0) - 1.0;
  }
}
