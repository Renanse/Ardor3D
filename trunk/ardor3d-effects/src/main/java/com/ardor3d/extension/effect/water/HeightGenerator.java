/**
 * Copyright (c) 2008 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.water;

/**
 * <code>HeightGenerator</code> Base interface for all waterheight generators used by the projected grid mesh.
 */
public interface HeightGenerator {
    /**
     * How to animate/set heights on a grid
     * 
     * @param x
     *            x position to get height for
     * @param z
     *            z position to get height for
     * @param time
     *            time to get height for
     * @return height for specified position
     */
    public double getHeight(double x, double z, double time);

    public double getMaximumHeight();
}
