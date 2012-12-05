/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math.functions;

import com.ardor3d.math.MathUtils;

/**
 * Function describing a set of concentric rings, centered around the origin on the x/z plane, each ring stretching
 * infinitely along the Y axis. The spacing between rings is controlled by the frequency. A higher frequency gives more
 * rings in the same amount of space.
 */
public class CylinderFunction3D implements Function3D {
    private double _frequency;

    /**
     * Construct a new CylinderFunction3D with the given frequency
     * 
     * @param frequency
     *            the number of rings per unit
     */
    public CylinderFunction3D(final double frequency) {
        setFrequency(frequency);
    }

    public double eval(final double x, final double y, final double z) {
        final double dx = x * _frequency;
        final double dz = z * _frequency;

        // get the radius to our point -- see the equation of a circle
        double radius = MathUtils.sqrt(dx * dx + dz * dz);

        // get fractional part
        radius = radius - MathUtils.floor(radius);

        // now get the distance to the closest integer, radius is now [0, .5]
        radius = Math.min(radius, 1 - radius);

        // return a value between -1 and 1, where 1 means the radius length was on an integer value and -1 means it was
        // halfway between two integers.
        return 1.0 - radius * 4; // [-1, 1]
    }

    public void setFrequency(final double frequency) {
        _frequency = frequency;
    }

    public double getFrequency() {
        return _frequency;
    }

}
