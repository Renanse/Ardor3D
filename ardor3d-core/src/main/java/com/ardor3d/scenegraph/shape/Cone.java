/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.shape;

public class Cone extends Cylinder {

    public Cone() {}

    public Cone(final String name, final int axisSamples, final int radialSamples, final float radius,
            final float height) {
        this(name, axisSamples, radialSamples, radius, height, true);
    }

    public Cone(final String name, final int axisSamples, final int radialSamples, final float radius,
            final float height, final boolean closed) {
        super(name, axisSamples, radialSamples, radius, height, closed);
        setRadius2(0);
    }

    public void setHalfAngle(final float radians) {
        setRadius1(Math.tan(radians));
    }
}
