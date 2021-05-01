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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ardor3d.intersection.IntersectionRecord;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;

public class TestRayBounding {
  @Test
  public void testRayAABBIntersection() throws Exception {
    final BoundingBox obb = new BoundingBox();
    obb.setCenter(Vector3.ZERO);
    obb.setXExtent(1);
    obb.setYExtent(1);
    obb.setZExtent(1);

    Ray3 ray = new Ray3(new Vector3(2, -10, 0), Vector3.UNIT_Y);
    assertFalse(obb.intersects(ray));
    IntersectionRecord record = obb.intersectsWhere(ray);
    assertEquals(null, record);

    final Quaternion rotation = new Quaternion();
    rotation.fromAngleAxis(MathUtils.QUARTER_PI, Vector3.UNIT_Z);
    final Transform transform = new Transform();
    transform.setRotation(rotation);
    obb.transform(transform, obb);

    ray = new Ray3(new Vector3(1, -10, 0), Vector3.UNIT_Y);
    assertTrue(obb.intersects(ray));
    record = obb.intersectsWhere(ray);
    assertEquals(2, record.getNumberOfIntersections());
  }

  @Test
  public void testRayOBBIntersection() throws Exception {
    final OrientedBoundingBox obb = new OrientedBoundingBox();
    obb.setCenter(Vector3.ZERO);
    obb.setExtent(Vector3.ONE);

    Ray3 ray = new Ray3(new Vector3(1.2, -10, 0), Vector3.UNIT_Y);
    assertFalse(obb.intersects(ray));
    IntersectionRecord record = obb.intersectsWhere(ray);
    assertEquals(null, record);

    final Quaternion rotation = new Quaternion();
    rotation.fromAngleAxis(MathUtils.QUARTER_PI, Vector3.UNIT_Z);
    final Transform transform = new Transform();
    transform.setRotation(rotation);
    obb.transform(transform, obb);

    ray = new Ray3(new Vector3(1.2, -10, 0), Vector3.UNIT_Y);
    assertTrue(obb.intersects(ray));
    record = obb.intersectsWhere(ray);
    assertEquals(2, record.getNumberOfIntersections());
  }

  @Test
  public void testRaySphereIntersection() throws Exception {
    final BoundingSphere bs = new BoundingSphere();
    bs.setCenter(Vector3.ZERO);
    bs.setRadius(1);

    final Ray3 ray = new Ray3(new Vector3(2, -3, 0), Vector3.UNIT_Y);
    assertFalse(bs.intersects(ray));
    IntersectionRecord record = bs.intersectsWhere(ray);
    assertEquals(null, record);

    final Transform transform = new Transform();
    transform.setTranslation(2, 0, .5);
    bs.transform(transform, bs);

    assertTrue(bs.intersects(ray));
    record = bs.intersectsWhere(ray);
    assertEquals(2, record.getNumberOfIntersections());
  }
}
