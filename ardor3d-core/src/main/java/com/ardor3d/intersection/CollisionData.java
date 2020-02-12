/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.intersection;

import java.util.List;

import com.ardor3d.scenegraph.Mesh;

/**
 * CollisionData contains information about a collision between two Mesh objects. The mesh that was hit by the relevant
 * Mesh (the one making the collision check) is referenced as well as an ArrayList for the triangles that collided.
 */
public class CollisionData {

    private final Mesh _targetMesh;
    private final Mesh _sourceMesh;

    private final List<PrimitiveKey> _sourcePrimitives;
    private final List<PrimitiveKey> _targetPrimitives;

    /**
     * instantiates a new CollisionData object.
     * 
     * @param sourceMesh
     *            the relevant Geometry
     * @param targetMesh
     *            the mesh the source Mesh collided with.
     */
    public CollisionData(final Mesh sourceMesh, final Mesh targetMesh) {
        this(sourceMesh, targetMesh, null, null);
    }

    /**
     * instantiates a new CollisionData object.
     * 
     * @param sourceMesh
     *            the relevant Mesh
     * @param targetMesh
     *            the mesh the source Mesh collided with.
     * @param sourcePrimitives
     *            the primitives of the source Mesh that made contact.
     * @param targetPrimitives
     *            the primitives of the second mesh that made contact.
     */
    public CollisionData(final Mesh sourceMesh, final Mesh targetMesh, final List<PrimitiveKey> sourcePrimitives,
            final List<PrimitiveKey> targetPrimitives) {
        _targetMesh = targetMesh;
        _sourceMesh = sourceMesh;
        _targetPrimitives = targetPrimitives;
        _sourcePrimitives = sourcePrimitives;
    }

    public Mesh getTargetMesh() {
        return _targetMesh;
    }

    public Mesh getSourceMesh() {
        return _sourceMesh;
    }

    public List<PrimitiveKey> getSourcePrimitives() {
        return _sourcePrimitives;
    }

    public List<PrimitiveKey> getTargetPrimitives() {
        return _targetPrimitives;
    }
}