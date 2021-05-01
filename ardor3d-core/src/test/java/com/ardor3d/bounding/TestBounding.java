/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.bounding;

import org.junit.Test;

import com.ardor3d.math.Vector3;

public class TestBounding {
  @Test
  public void testBoundingBoxMerge() throws Exception {
    final BoundingBox obb = new BoundingBox();
    obb.setCenter(Vector3.ZERO);
    obb.setXExtent(1);
    obb.setYExtent(1);
    obb.setZExtent(1);

    // final ReadOnlyVector3 center = sceneBounds.getCenter();
    // for (int i = 0; i < _corners.length; i++) {
    // _corners[i].set(center);
    // }
    //
    // if (sceneBounds instanceof BoundingBox) {
    // final BoundingBox bbox = (BoundingBox) sceneBounds;
    // bbox.getExtent(_extents);
    // } else if (sceneBounds instanceof BoundingSphere) {
    // final BoundingSphere bsphere = (BoundingSphere) sceneBounds;
    // _extents.set(bsphere.getRadius(), bsphere.getRadius(), bsphere.getRadius());
    // }

  }
}
