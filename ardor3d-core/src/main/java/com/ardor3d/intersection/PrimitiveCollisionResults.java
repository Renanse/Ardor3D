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

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.scenegraph.Mesh;

/**
 * PrimitiveCollisionResults creates a CollisionResults object that calculates collisions to the
 * primitive (line, triangle, etc.) accuracy. CollisionData objects are added to the collision list
 * as they happen, these data objects only refer to the two meshes, not their primitive lists. While
 * PrimitiveCollisionResults defines a processCollisions method, it is empty and should be further
 * defined by the user if so desired.
 *
 * NOTE: Only Mesh objects may obtain primitive accuracy, all others will result in Bounding
 * accuracy.
 */
public class PrimitiveCollisionResults extends CollisionResults {
  /*
   * (non-Javadoc)
   *
   * @see com.ardor3d.intersection.CollisionResults#addCollision(com.ardor3d.scene.Geometry,
   * com.ardor3d.scene.Geometry)
   */
  @Override
  public void addCollision(final Mesh s, final Mesh t) {
    // find the triangle that is being hit.
    // add this node and the triangle to the CollisionResults list.
    final List<PrimitiveKey> a = new ArrayList<>();
    final List<PrimitiveKey> b = new ArrayList<>();
    PickingUtil.findPrimitiveCollision(s, t, a, b);
    final CollisionData data = new CollisionData(s, t, a, b);
    addCollisionData(data);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.ardor3d.intersection.CollisionResults#processCollisions()
   */
  @Override
  public void processCollisions() {

  }

}
