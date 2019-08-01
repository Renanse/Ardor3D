/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
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

    private static final long TIMER_RESOLUTION = 1000000000L;
    private static final double INVERSE_TIMER_RESOLUTION = 1.0 / TIMER_RESOLUTION;

    private long _startTime;
    private long _previousTime;
    private double _tpf;
    private double _fps;

    public Timer() {
        _startTime = System.nanoTime();
    }

    public double getTimeInSeconds() {
        return getTime() * INVERSE_TIMER_RESOLUTION;
    }

    public long getTime() {
        return System.nanoTime() - _startTime;
    }

    public long getResolution() {
        return TIMER_RESOLUTION;
    }

    public double getFrameRate() {
        return _fps;
    }

    public double getTimePerFrame() {
        return _tpf;
    }

    /**
     * Update should be called once per frame to correctly update "time per frame" and "frame rate (fps)"
     */
    public void update() {
        final long time = getTime();
        _tpf = (time - _previousTime) * INVERSE_TIMER_RESOLUTION;
        _fps = 1.0 / _tpf;
        _previousTime = time;
    }

    /**
     * Reset this timer, so that {@link #getTime()} and {@link #getTimeInSeconds()} reflects the time spend from this
     * call.
     */
    public void reset() {
        _startTime = System.nanoTime();
        _previousTime = getTime();
    }
}
