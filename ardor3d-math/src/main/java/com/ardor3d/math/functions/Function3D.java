/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math.functions;

/**
 * Simple interface describing a function that receives a 3 value tuple and returns a value.
 */
public interface Function3D {

    /**
     * @param x
     *            the 1st value in our tuple
     * @param y
     *            the 2nd value in our tuple
     * @param z
     *            the 3rd value in our tuple
     * @return some value, generally (but not necessarily) in [-1, 1]
     */
    double eval(double x, double y, double z);

}