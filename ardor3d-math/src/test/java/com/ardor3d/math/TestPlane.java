/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ardor3d.math.type.ReadOnlyPlane.Side;

public class TestPlane {

  @Test
  public void testGetSet() {
    final Plane plane = new Plane();
    assertEquals(Vector3.UNIT_Y, plane.getNormal());
    assertTrue(plane.getConstant() == 0.0);

    plane.setNormal(Vector3.UNIT_X);
    plane.setConstant(1.0);
    assertEquals(Vector3.UNIT_X, plane.getNormal());
    assertTrue(plane.getConstant() == 1.0);

    final Plane plane2 = new Plane(plane);
    assertEquals(Vector3.UNIT_X, plane2.getNormal());
    assertTrue(plane.getConstant() == 1.0);

    final Plane plane3 = new Plane(Vector3.NEG_UNIT_Z, 2.5);
    assertEquals(Vector3.NEG_UNIT_Z, plane3.getNormal());
    assertTrue(plane3.getConstant() == 2.5);

    final Plane plane4 = new Plane().setPlanePoints(new Vector3(1, 1, 1), new Vector3(2, 1, 1), new Vector3(2, 2, 1));
    assertEquals(Vector3.UNIT_Z, plane4.getNormal());
    assertTrue(plane4.getConstant() == 1.0);
  }

  @Test
  public void testEquals() {
    // couple of equals validity tests
    final Plane plane1 = new Plane();
    assertEquals(plane1, plane1);
    assertFalse(plane1.equals(null));

    // throw in a couple pool accesses for coverage
    final Plane plane2 = Plane.fetchTempInstance();
    plane2.set(plane1);
    assertEquals(plane1, plane2);
    assertNotSame(plane1, plane2);
    Plane.releaseTempInstance(plane2);

    // cover more of equals
    assertFalse(plane1.equals(new Plane(Vector3.UNIT_X, 0)));
  }

  @Test
  public void testSimpleHash() {
    // Just a simple sanity check.
    final Plane plane1 = new Plane(Vector3.UNIT_Y, 2);
    final Plane plane2 = new Plane(Vector3.UNIT_Y, 2);
    final Plane plane3 = new Plane(Vector3.UNIT_Z, 2);

    assertTrue(plane1.hashCode() == plane2.hashCode());
    assertTrue(plane1.hashCode() != plane3.hashCode());
  }

  @Test
  public void testClone() {
    final Plane plane1 = new Plane();
    final Plane plane2 = plane1.clone();
    assertEquals(plane1, plane2);
    assertNotSame(plane1, plane2);
  }

  @Test
  public void testFinite() {
    final Plane plane1 = new Plane();
    final Plane plane2 = new Plane(new Vector3(Double.NaN, 0, 0), 0.5);
    final Plane plane3 = new Plane(Vector3.UNIT_X, Double.NaN);
    final Plane plane4 = new Plane(Vector3.UNIT_X, Double.POSITIVE_INFINITY);

    assertTrue(Plane.isFinite(plane1));
    assertFalse(Plane.isFinite(plane2));
    assertFalse(Plane.isFinite(plane3));
    assertFalse(Plane.isFinite(plane4));

    plane4.setConstant(1);
    assertTrue(Plane.isFinite(plane4));

    assertFalse(Plane.isFinite(null));
  }

  @Test
  public void testDistance() {
    final Plane plane1 = new Plane(Vector3.UNIT_Y, 1.0);
    final Vector3 point = new Vector3(0, 5, 0);
    assertTrue(4.0 == plane1.pseudoDistance(point));
    assertEquals(Side.Outside, plane1.whichSide(point));

    point.set(0, -4, 0);
    assertTrue(-5.0 == plane1.pseudoDistance(point));
    assertEquals(Side.Inside, plane1.whichSide(point));

    point.set(1, 1, 1);
    assertTrue(0.0 == plane1.pseudoDistance(point));
    assertEquals(Side.Neither, plane1.whichSide(point));
  }

  @Test
  public void testReflect() {
    final Plane plane1 = new Plane(Vector3.UNIT_X, 5.0);
    assertEquals(new Vector3(), plane1.reflectVector(new Vector3(), new Vector3()));
    assertEquals(new Vector3(-1, 0, 0), plane1.reflectVector(new Vector3(1, 0, 0), null));
    assertEquals(new Vector3(-1, 1, 1).normalizeLocal(),
        plane1.reflectVector(new Vector3(1, 1, 1).normalizeLocal(), null));
    assertEquals(new Vector3(-3, 2, -1).normalizeLocal(),
        plane1.reflectVector(new Vector3(3, 2, -1).normalizeLocal(), null));

    final Plane plane2 = new Plane(Vector3.UNIT_Z, 1.0);
    assertEquals(new Vector3(), plane2.reflectVector(new Vector3(), new Vector3()));
    assertEquals(new Vector3(0, 0, -1), plane2.reflectVector(new Vector3(0, 0, 1), null));
    assertEquals(new Vector3(1, 1, -1).normalizeLocal(),
        plane2.reflectVector(new Vector3(1, 1, 1).normalizeLocal(), null));
    assertEquals(new Vector3(3, 2, 1).normalizeLocal(),
        plane2.reflectVector(new Vector3(3, 2, -1).normalizeLocal(), null));
  }
}
