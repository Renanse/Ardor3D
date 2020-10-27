/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

/**
 * <code>Timer</code> is a ReadOnlyTimer implementation with nanosecond resolution.
 */
public class Timer implements ReadOnlyTimer {

  protected static final long TIMER_RESOLUTION = 1000000000L;
  protected static final double INVERSE_TIMER_RESOLUTION = 1.0 / Timer.TIMER_RESOLUTION;

  protected long _startTime;
  protected long _previousFrameTime;
  protected double _tpf;
  protected double _fps;

  public Timer() {
    _startTime = System.nanoTime();
  }

  @Override
  public double getTimeInSeconds() { return getTime() * Timer.INVERSE_TIMER_RESOLUTION; }

  @Override
  public long getTime() { return System.nanoTime() - _startTime; }

  @Override
  public long getResolution() { return Timer.TIMER_RESOLUTION; }

  @Override
  public double getFrameRate() { return _fps; }

  @Override
  public double getTimePerFrame() { return _tpf; }

  @Override
  public long getPreviousFrameTime() { return _previousFrameTime; }

  /**
   * Update should be called once per frame to correctly update "time per frame" and "frame rate
   * (fps)"
   */
  public void update() {
    final long time = getTime();
    _tpf = (time - _previousFrameTime) * Timer.INVERSE_TIMER_RESOLUTION;
    _fps = 1.0 / _tpf;
    _previousFrameTime = time;
  }

  /**
   * Reset this timer, so that {@link #getTime()} and {@link #getTimeInSeconds()} reflects the time
   * spend from this call.
   */
  public void reset() {
    _startTime = System.nanoTime();
    _previousFrameTime = getTime();
  }
}
