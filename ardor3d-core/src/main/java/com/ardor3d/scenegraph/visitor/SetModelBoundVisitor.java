/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.visitor;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;

public class SetModelBoundVisitor implements Visitor {
    private final BoundingVolume _bound;

    public SetModelBoundVisitor(final BoundingVolume bound) {
        _bound = bound;
    }

    public void visit(final Spatial spatial) {
        if (spatial instanceof Mesh) {
            ((Mesh) spatial).setModelBound(_bound.clone(null));
        }
    }
}