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
 * BoundingPickResults implements the addPick of PickResults to use PickData objects that calculate bounding volume
 * level accurate ray picks.
 */
public class BoundingPickResults extends PickResults {
    @Override
    public void addPick(final Ray3 ray, final Pickable p) {
        if (p.intersectsWorldBound(ray)) {
            addPickData(new PickData(ray, p, willCheckDistance()));
        }
    }
}
