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
 * An implementation of Fractional Brownian Motion (fBm), with configurable persistence and
 * lacunarity (or tightness of the fractal). This function basically recursively adds the given
 * source function (often a noise function) onto itself, shifting and scaling the sample point and
 * value on each iteration (or octave).
 *
 * @see <a href="http://en.wikipedia.org/wiki/Fractional_Brownian_motion">wikipedia entry</a>
 */
public class FbmFunction3D implements Function3D {

  private Function3D _source;
  private int _octaves;
  private double _frequency;
  private double _persistence;
  private double _lacunarity;

  /**
   * Construct a new FbmFunction with the given params.
   *
   * @param source
   *          the source function we will operate on.
   * @param octaves
   *          the number of iterations we will perform. Called octaves because the default for each
   *          octave is double the frequency of the previous octave, a property shared by music tones.
   *          Generally a value between 4 and 8 is good.
   * @param frequency
   *          a scale value applied to the incoming tuple before we apply fBm. It is the number of
   *          cycles per unit for the function. Default would be 1.
   * @param persistence
   *          a scale value that determines how fast the amplitude decreases for each octave.
   *          Generally should be in the range [0, 1]. A lower persistence value will decrease the
   *          effect of each octave. A traditional value is 0.5
   * @param lacunarity
   *          a scale value that determines how fast the frequency increases for each octave (freq =
   *          prevFreq * lac.) A traditional value is 2.
   */
  public FbmFunction3D(final Function3D source, final int octaves, final double frequency, final double persistence,
    final double lacunarity) {
    _source = source;
    _octaves = octaves;
    _frequency = frequency;
    _persistence = persistence;
    _lacunarity = lacunarity;
  }

  @Override
  public double eval(final double x, final double y, final double z) {
    double sum = 0;
    double dx = x * _frequency, dy = y * _frequency, dz = z * _frequency;
    double dPersistence = 1;
    for (int i = 0; i < _octaves; i++) {
      final double value = getValue(dx, dy, dz);

      sum += dPersistence * value;

      dPersistence *= _persistence;
      dx *= _lacunarity;
      dy *= _lacunarity;
      dz *= _lacunarity;
    }
    return sum;
  }

  protected double getValue(final double dx, final double dy, final double dz) {
    return _source.eval(dx, dy, dz);
  }

  public Function3D getSource() { return _source; }

  public void setSource(final Function3D source) { _source = source; }

  public int getOctaves() { return _octaves; }

  public void setOctaves(final int octaves) { _octaves = octaves; }

  public double getFrequency() { return _frequency; }

  public void setFrequency(final double frequency) { _frequency = frequency; }

  public double getPersistence() { return _persistence; }

  public void setPersistence(final double persistence) { _persistence = persistence; }

  public double getLacunarity() { return _lacunarity; }

  public void setLacunarity(final double lacunarity) { _lacunarity = lacunarity; }
}
