/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
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

import com.ardor3d.math.type.ReadOnlyRay3;

public class TestRay3 {
  @Test
  public void testData() {
    final Ray3 ray = new Ray3();
    assertEquals(Vector3.UNIT_Z, ray.getDirection());
    assertEquals(Vector3.ZERO, ray.getOrigin());

    ray.setDirection(Vector3.NEG_UNIT_X);
    assertEquals(Vector3.NEG_UNIT_X, ray.getDirection());
    ray.setOrigin(Vector3.ONE);
    assertEquals(Vector3.ONE, ray.getOrigin());

    final Ray3 ray2 = new Ray3(ray);
    assertEquals(Vector3.NEG_UNIT_X, ray2.getDirection());
    assertEquals(Vector3.ONE, ray2.getOrigin());

    ray.set(new Ray3());
    assertEquals(Vector3.UNIT_Z, ray.getDirection());
    assertEquals(Vector3.ZERO, ray.getOrigin());
  }

  @Test
  public void testFinite() {
    final Ray3 ray1 = new Ray3(new Vector3(0, 0, 0), new Vector3(0, 0, 1));
    final Ray3 ray2 = new Ray3(new Vector3(Double.POSITIVE_INFINITY, 0, 0), new Vector3(0, 0, 1));
    final Ray3 ray3 = new Ray3(new Vector3(0, 0, 0), new Vector3(Double.POSITIVE_INFINITY, 0, 1));

    assertTrue(Ray3.isFinite(ray1));
    assertFalse(Ray3.isFinite(ray2));
    assertFalse(Ray3.isFinite(ray3));

    assertFalse(Ray3.isFinite(null));

    // couple if equals validity tests
    assertEquals(ray1, ray1);
    assertFalse(ray1.equals(null));

    // throw in a couple pool accesses for coverage
    final Ray3 ray4 = Ray3.fetchTempInstance();
    ray4.set(ray1);
    assertEquals(ray1, ray4);
    assertNotSame(ray1, ray4);
    Ray3.releaseTempInstance(ray4);

    // cover more of equals
    assertFalse(ray1.equals(new Ray3(Vector3.ZERO, Vector3.NEG_UNIT_X)));
  }

  @Test
  public void testClone() {
    final Ray3 ray1 = new Ray3();
    final Ray3 ray2 = ray1.clone();
    assertEquals(ray1, ray2);
    assertNotSame(ray1, ray2);
  }

  @Test
  public void testDistance() {
    final Ray3 ray1 = new Ray3();
    assertTrue(25.0 == ray1.distanceSquared(new Vector3(0, 5, 3), null));

    final Vector3 store = new Vector3();
    assertTrue(9.0 == ray1.distanceSquared(new Vector3(0, 3, 3), store));
    assertEquals(new Vector3(0, 0, 3), store);
    assertTrue(18.0 == ray1.distanceSquared(new Vector3(0, 3, -3), store));
    assertEquals(new Vector3(0, 0, 0), store);
  }

  @Test
  public void testIntersectsTriangle() {
    final Vector3 v0 = new Vector3(-1, -1, -1);
    final Vector3 v1 = new Vector3(+1, -1, -1);
    final Vector3 v2 = new Vector3(+1, +1, -1);

    final Vector3 intersectionPoint = new Vector3();

    // inside triangle
    Ray3 pickRay = new Ray3(new Vector3(0.5, -0.5, 3), new Vector3(0, 0, -1));
    assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

    // horizontal edge
    pickRay = new Ray3(new Vector3(0, -1, 3), new Vector3(0, 0, -1));
    assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

    // diagonal edge
    pickRay = new Ray3(new Vector3(0, 0, 3), new Vector3(0, 0, -1));
    assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

    // vertical edge
    pickRay = new Ray3(new Vector3(+1, 0, 3), new Vector3(0, 0, -1));
    assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

    // v0
    pickRay = new Ray3(new Vector3(-1, -1, 3), new Vector3(0, 0, -1));
    assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

    // v1
    pickRay = new Ray3(new Vector3(+1, -1, 3), new Vector3(0, 0, -1));
    assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

    // v2
    pickRay = new Ray3(new Vector3(1, 1, 3), new Vector3(0, 0, -1));
    assertTrue(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

    // outside horizontal edge
    pickRay = new Ray3(new Vector3(0, -1.1, 3), new Vector3(0, 0, -1));
    assertFalse(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

    // outside diagonal edge
    pickRay = new Ray3(new Vector3(-0.1, 0.1, 3), new Vector3(0, 0, -1));
    assertFalse(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

    // outside vertical edge
    pickRay = new Ray3(new Vector3(+1.1, 0, 3), new Vector3(0, 0, -1));
    assertFalse(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

    // inside triangle but ray pointing other way
    pickRay = new Ray3(new Vector3(-0.5, -0.5, 3), new Vector3(0, 0, +1));
    assertFalse(pickRay.intersectsTriangle(v0, v1, v2, intersectionPoint));

    // test distance
    pickRay = new Ray3(new Vector3(0.5, -0.5, 3), new Vector3(0, 0, -1));
    assertTrue(4.0 == pickRay.getDistanceToPrimitive(new Vector3[] {v0, v1, v2}));

    // test intersect planar
    assertTrue(pickRay.intersectsTrianglePlanar(v0, v1, v2, intersectionPoint));

  }

  @Test
  public void testIntersectsPlane() {
    final Vector3 intersectionPoint = new Vector3();

    Plane plane = new Plane(new Vector3(0, 1, 0), 2);

    Ray3 pickRay = new Ray3(new Vector3(0, 3, 0), new Vector3(0, 0, 1));
    assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

    pickRay = new Ray3(new Vector3(0, 3, 0), new Vector3(0, 1, 0));
    assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

    pickRay = new Ray3(new Vector3(0, 2, 0), new Vector3(0, 1, 0));
    assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

    pickRay = new Ray3(new Vector3(0, 1, 0), new Vector3(0, 1, 0));
    assertTrue(pickRay.intersectsPlane(plane, intersectionPoint));

    pickRay = new Ray3(new Vector3(0, 0, 0), new Vector3(1, 0, 0));
    assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

    pickRay = new Ray3(new Vector3(0, -3, 0), new Vector3(0, 0, 1));
    assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

    pickRay = new Ray3(new Vector3(0, 3, 0), new Vector3(0, -1, 0));
    assertTrue(pickRay.intersectsPlane(plane, intersectionPoint));

    pickRay = new Ray3(new Vector3(0, -3, 0), new Vector3(1, 1, 1));
    assertTrue(pickRay.intersectsPlane(plane, intersectionPoint));

    pickRay = new Ray3(new Vector3(0, -3, 0), new Vector3(-1, -1, -1));
    assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

    plane = new Plane(new Vector3(1, 1, 1), -2);

    pickRay = new Ray3(new Vector3(0, 0, 0), new Vector3(1, -1, 1));
    assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

    pickRay = new Ray3(new Vector3(0, -1, 0), new Vector3(0, 1, 0));
    assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

    pickRay = new Ray3(new Vector3(0, -2, 0), new Vector3(0, 1, 0));
    assertFalse(pickRay.intersectsPlane(plane, intersectionPoint));

    pickRay = new Ray3(new Vector3(0, -3, 0), new Vector3(0, 1, 0));
    assertTrue(pickRay.intersectsPlane(plane, null));
  }

  @Test
  public void testIntersectsQuad() {
    final Vector3 v0 = new Vector3(0, 0, 0);
    final Vector3 v1 = new Vector3(5, 0, 0);
    final Vector3 v2 = new Vector3(5, 5, 0);
    final Vector3 v3 = new Vector3(0, 5, 0);

    Vector3 intersectionPoint = null;

    // inside quad
    final ReadOnlyRay3 pickRayA = new Ray3(new Vector3(2, 2, 10), new Vector3(0, 0, -1));
    final ReadOnlyRay3 pickRayB = new Ray3(new Vector3(2, 4, 10), new Vector3(0, 0, -1));
    assertTrue(pickRayA.intersectsQuad(v0, v1, v2, v3, intersectionPoint));
    assertTrue(pickRayB.intersectsQuad(v0, v1, v2, v3, intersectionPoint));

    // inside quad
    final Ray3 pickRay2 = new Ray3(new Vector3(-1, 0, 10), new Vector3(0, 0, -1));
    assertFalse(pickRay2.intersectsQuad(v0, v1, v2, v3, intersectionPoint));

    // test distance
    assertTrue(10.0 == pickRayA.getDistanceToPrimitive(new Vector3[] {v0, v1, v2, v3}));
    assertTrue(Double.POSITIVE_INFINITY == pickRay2.getDistanceToPrimitive(new Vector3[] {v0, v1, v2, v3}));

    // test unsupported pick
    assertFalse(pickRay2.intersects(new Vector3[] {v0, v1}, null));

    // test intersect planar
    assertFalse(
        new Ray3(new Vector3(0, 0, -1), Vector3.UNIT_Y).intersectsQuadPlanar(v0, v1, v2, v3, intersectionPoint));
    intersectionPoint = new Vector3();
    assertTrue(pickRayA.intersectsQuadPlanar(v0, v1, v2, v3, intersectionPoint));
    assertTrue(pickRayB.intersectsQuadPlanar(v0, v1, v2, v3, intersectionPoint));
  }
}
