/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.functions;

import com.ardor3d.math.MathUtils;

/**
 * Based on Multifractal code originally written by F. Kenton "Doc Mojo" Musgrave, 1998. Modified by jas for use with
 * libnoise, then modified for use in Ardor3D in 2009.
 */
public class RidgeFunction3D implements Function3D {

    public static final int MAX_OCTAVES = 32;

    private Function3D _source;
    private double _octaves = 6;
    private double _frequency = 1;
    private double _lacunarity = 2;
    private double _gain = 2;
    private double _offset = 1;
    private double _h = 1;
    private final double[] _spectralWeights = new double[MAX_OCTAVES];

    public RidgeFunction3D() {
        setSource(Functions.simplexNoise());
        updateWeights();
    }

    public RidgeFunction3D(final Function3D source, final double octaves, final double frequency,
            final double lacunarity) {
        setSource(source);
        setOctaves(octaves);
        setFrequency(frequency);
        // to not trigger weight calc
        _lacunarity = lacunarity;
        updateWeights();
    }

    public double eval(final double x, final double y, final double z) {
        double value = 0, signal = 0, weight = 1;
        double dx = x * _frequency, dy = y * _frequency, dz = z * _frequency;
        for (int i = 0; i < _octaves; i++) {
            signal = _source.eval(dx, dy, dz);

            signal = Math.abs(signal);
            signal = _offset - signal;

            // Square the signal to increase the sharpness of the ridges.
            signal *= signal;

            // The weighting from the previous octave is applied to the signal.
            // Larger values have higher weights, producing sharp points along the
            // ridges.
            signal *= weight;

            // Weight successive contributions by the previous signal. (clamp to [0, 1])
            weight = MathUtils.clamp(signal * _gain, 0, 1);

            // Add the signal to the output value.
            value += signal * _spectralWeights[i];

            // Next!
            dx *= _lacunarity;
            dy *= _lacunarity;
            dz *= _lacunarity;
        }

        return (value * 1.25) - 1.0;
    }

    public Function3D getSource() {
        return _source;
    }

    public void setSource(final Function3D source) {
        _source = source;
    }

    public double getOctaves() {
        return _octaves;
    }

    public void setOctaves(final double octaves) {
        _octaves = octaves;
    }

    public double getFrequency() {
        return _frequency;
    }

    public void setFrequency(final double frequency) {
        _frequency = frequency;
    }

    public double getLacunarity() {
        return _lacunarity;
    }

    public void setLacunarity(final double lacunarity) {
        _lacunarity = lacunarity;
        updateWeights();
    }

    public double getGain() {
        return _gain;
    }

    public void setGain(final double gain) {
        _gain = gain;
    }

    public double getOffset() {
        return _offset;
    }

    public void setOffset(final double offset) {
        _offset = offset;
    }

    public double getH() {
        return _h;
    }

    public void setH(final double h) {
        _h = h;
        updateWeights();
    }

    private void updateWeights() {
        double dFreq = 1;
        for (int i = 0; i < MAX_OCTAVES; i++) {
            // Compute weight for each frequency.
            _spectralWeights[i] = Math.pow(dFreq, -_h);
            dFreq *= _lacunarity;
        }
    }
}
