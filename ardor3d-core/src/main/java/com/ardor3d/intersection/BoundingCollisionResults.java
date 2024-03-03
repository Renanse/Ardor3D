/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.intersection;

import com.ardor3d.scenegraph.Mesh;

/**
 * BoundingCollisionResults creates a CollisionResults object that only cares about bounding volume
 * accuracy. CollisionData objects are added to the collision list as they happen, these data
 * objects only refer to the two meshes, not their triangle lists. While BoundingCollisionResults
 * defines a processCollisions method, it is empty and should be further defined by the user if so
 * desired.
 */
public class BoundingCollisionResults extends CollisionResults {

  /**
   * adds a CollisionData object to this results list, the objects only refer to the collision meshes,
   * not the triangles.
   * 
   * @see com.ardor3d.intersection.CollisionResults#addCollision(com.ardor3d.scene.Geometry,
   *      com.ardor3d.scene.Geometry)
   */
  @Override
  public void addCollision(final Mesh s, final Mesh t) {
    final CollisionData data = new CollisionData(s, t);
    addCollisionData(data);
  }

  /**
   * empty implementation, it is highly recommended that you override this method to handle any
   * collisions as needed.
   * 
   * @see com.ardor3d.intersection.CollisionResults#processCollisions()
   */
  @Override
  public void processCollisions() {

  }

}
