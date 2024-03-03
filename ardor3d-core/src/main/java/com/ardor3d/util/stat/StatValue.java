/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.stat;

public class StatValue {
  private double _accumulatedValue = 0;
  private double _averageValue = 0;
  private long _iterations;
  private boolean _averageDirty = true;

  public StatValue() {}

  public StatValue(final StatValue entry) {
    _accumulatedValue = entry._accumulatedValue;
    _averageValue = entry._averageValue;
    _averageDirty = entry._averageDirty;
    _iterations = entry._iterations;
  }

  public double getAccumulatedValue() { return _accumulatedValue; }

  public long getIterations() { return _iterations; }

  public double getAverageValue() {
    if (_averageDirty) {
      _averageValue = _iterations > 0 ? _accumulatedValue / _iterations : _accumulatedValue;
      _averageDirty = false;
    }
    return _averageValue;
  }

  public void incrementValue(final double statValue) {
    _accumulatedValue += statValue;
    _averageDirty = true;
  }

  public void incrementIterations() {
    _iterations++;
    _averageDirty = true;
  }

  public void setIterations(final long iterations) {
    _iterations = iterations;
    _averageDirty = true;
  }

  public void reset() {
    _accumulatedValue = 0;
    _iterations = 0;
    _averageValue = 0;
    _averageDirty = false;
  }
}
