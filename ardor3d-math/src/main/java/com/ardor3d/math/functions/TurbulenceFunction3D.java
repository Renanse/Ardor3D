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
 * Function that uses a fBm function to distort incoming coordinates on each of the 3 axis before
 * feeding them to a source function for evaluation. The amount of distortion depends on the given
 * power, roughness and frequency.
 */
public class TurbulenceFunction3D implements Function3D {

  private double _power;
  private Function3D _source;
  private final FbmFunction3D _distortModule;

  /**
   * Construct a new turbulence function with the given values.
   *
   * @param source
   *          the source function to send our distorted coordinates to.
   * @param power
   *          a scalar that controls how much the distortion is applied to the input coordinates. 0 ==
   *          no distortion.
   * @param roughness
   *          how "choppy" the distortion is. Lower values produce more gradual shifts in distortion
   *          whereas higher values produce more choppy transitions.
   * @param frequency
   *          how rapidly the distortion value changes.
   */
  public TurbulenceFunction3D(final Function3D source, final double power, final int roughness,
    final double frequency) {
    _power = power;
    _source = source;
    _distortModule = new FbmFunction3D(Functions.simplexNoise(), roughness, frequency, 0.5, 2.0);
  }

  @Override
  public double eval(final double x, final double y, final double z) {
    // tweak the incoming x, y, and z with some magic numbers to prevent singularities as integer
    // boundaries.
    final double x0 = x + .1985;
    final double y0 = y + .9958;
    final double z0 = z + .5284;

    final double x1 = x + .4106;
    final double y1 = y + .2672;
    final double z1 = z + .9529;

    final double x2 = x + .8297;
    final double y2 = y + .1921;
    final double z2 = z + .7123;

    // Use tweaked values to feed our distortion module. Use our power field to scale the amount of
    // distortion and
    // add it to the original values.
    final double xDistort = x + (_distortModule.eval(x0, y0, z0) * _power);
    final double yDistort = y + (_distortModule.eval(x1, y1, z1) * _power);
    final double zDistort = z + (_distortModule.eval(x2, y2, z2) * _power);

    // Get the output value at the distorted location
    return _source.eval(xDistort, yDistort, zDistort);
  }

  public double getPower() { return _power; }

  public void setPower(final double power) { _power = power; }

  public Function3D getSource() { return _source; }

  public void setSource(final Function3D source) { _source = source; }

  public void setRoughness(final int roughness) {
    _distortModule.setOctaves(roughness);
  }

  public void setFrequency(final double frequency) {
    _distortModule.setFrequency(frequency);
  }
}
