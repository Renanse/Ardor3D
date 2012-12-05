/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.intersection;

import com.ardor3d.math.Ray3;

/**
 * PickData contains information about a picking operation (or Ray/Volume intersection). This data contains the mesh the
 * ray hit, the triangles it hit, and the ray itself.
 */
public class PickData {

    private final Ray3 _ray;
    private final Pickable _target;
    protected IntersectionRecord _intersectionRecord;

    /**
     * instantiates a new PickData object. Note: subclasses may want to make calc points false to prevent this extra
     * work.
     */
    public PickData(final Ray3 ray, final Pickable target, final boolean calcPoints) {
        _ray = ray;
        _target = target;

        if (calcPoints) {
            _intersectionRecord = target.intersectsWorldBoundsWhere(ray);
        }
    }

    /**
     * @return the pickable hit by the ray.
     */
    public Pickable getTarget() {
        return _target;
    }

    /**
     * @return the ray used in the test.
     */
    public Ray3 getRay() {
        return _ray;
    }

    /**
     * @return the intersection record generated for this pick. Will be null if calcPoints was false.
     */
    public IntersectionRecord getIntersectionRecord() {
        return _intersectionRecord;
    }
}