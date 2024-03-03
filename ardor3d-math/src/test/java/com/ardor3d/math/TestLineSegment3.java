/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
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

import com.ardor3d.math.util.MathUtils;

public class TestLineSegment3 {

  @Test
  public void testGetSet() {
    final LineSegment3 seg1 = new LineSegment3();
    assertEquals(Vector3.ZERO, seg1.getOrigin());
    assertEquals(Vector3.UNIT_Z, seg1.getDirection());
    assertTrue(seg1.getExtent() == 0.5);

    seg1.setOrigin(Vector3.NEG_ONE);
    seg1.setDirection(Vector3.UNIT_X);
    seg1.setExtent(42.0);
    assertEquals(Vector3.NEG_ONE, seg1.getOrigin());
    assertEquals(Vector3.UNIT_X, seg1.getDirection());
    assertTrue(seg1.getExtent() == 42.0);

    final LineSegment3 seg2 = new LineSegment3(seg1);
    assertEquals(Vector3.NEG_ONE, seg2.getOrigin());
    assertEquals(Vector3.UNIT_X, seg2.getDirection());
    assertTrue(seg2.getExtent() == 42.0);

    final LineSegment3 seg3 = new LineSegment3(Vector3.ONE, Vector3.UNIT_Y, 2.5);
    assertEquals(Vector3.ONE, seg3.getOrigin());
    assertEquals(Vector3.UNIT_Y, seg3.getDirection());
    assertTrue(seg3.getExtent() == 2.5);

    final LineSegment3 seg4 = new LineSegment3(new Vector3(9, 2, 2), new Vector3(5, 2, 2));
    assertEquals(new Vector3(7, 2, 2), seg4.getOrigin());
    assertEquals(Vector3.NEG_UNIT_X, seg4.getDirection());
    assertTrue(seg4.getExtent() == 2);

    assertEquals(new Vector3(9, 2, 2), seg4.getNegativeEnd(null));
    assertEquals(new Vector3(1, -1.5, 1), seg3.getNegativeEnd(new Vector3()));
    assertEquals(new Vector3(5, 2, 2), seg4.getPositiveEnd(null));
    assertEquals(new Vector3(1, 3.5, 1), seg3.getPositiveEnd(new Vector3()));

    assertFalse(seg3.equals(new LineSegment3(Vector3.ONE, Vector3.ONE, 42)));
    assertFalse(seg3.equals(new LineSegment3(Vector3.ONE, Vector3.UNIT_Y, 42)));
  }

  @Test
  public void testEquals() {
    // couple of equals validity tests
    final LineSegment3 seg1 = new LineSegment3();
    assertEquals(seg1, seg1);
    assertFalse(seg1.equals(null));

    // throw in a couple pool accesses for coverage
    final LineSegment3 seg2 = LineSegment3.fetchTempInstance();
    seg2.set(seg1);
    assertEquals(seg1, seg2);
    assertNotSame(seg1, seg2);
    LineSegment3.releaseTempInstance(seg2);

    // cover more of equals
    assertFalse(seg1.equals(new LineSegment3(Vector3.ZERO, Vector3.UNIT_X, 2)));
    assertFalse(seg1.equals(new LineSegment3(Vector3.ZERO, Vector3.UNIT_Z, 2)));
  }

  @Test
  public void testSimpleHash() {
    // Just a simple sanity check.
    final LineSegment3 seg1 = new LineSegment3(Vector3.ZERO, Vector3.UNIT_Y, 2);
    final LineSegment3 seg2 = new LineSegment3(Vector3.ZERO, Vector3.UNIT_Y, 2);
    final LineSegment3 seg3 = new LineSegment3(Vector3.ZERO, Vector3.UNIT_Y, 4);

    assertTrue(seg1.hashCode() == seg2.hashCode());
    assertTrue(seg1.hashCode() != seg3.hashCode());
  }

  @Test
  public void testClone() {
    final LineSegment3 seg1 = new LineSegment3();
    final LineSegment3 seg2 = seg1.clone();
    assertEquals(seg1, seg2);
    assertNotSame(seg1, seg2);
  }

  @Test
  public void testRandom() {
    MathUtils.setRandomSeed(0);
    final LineSegment3 seg1 = new LineSegment3();
    final Vector3 store = seg1.random(null);
    assertEquals(new Vector3(0.0, 0.0, 0.23096778737665702), store);

    seg1.random(store);
    assertEquals(new Vector3(0.0, 0.0, -0.25946358432851413), store);
  }

  @Test
  public void testFinite() {
    final LineSegment3 seg1 = new LineSegment3();
    final LineSegment3 seg2 = new LineSegment3(new Vector3(Double.NaN, 0, 0), Vector3.UNIT_Z, 0.5);
    final LineSegment3 seg3 = new LineSegment3(Vector3.ZERO, new Vector3(Double.NaN, 0, 0), 0.5);
    final LineSegment3 seg4 = new LineSegment3(Vector3.ZERO, Vector3.UNIT_Z, Double.NaN);
    final LineSegment3 seg5 = new LineSegment3(Vector3.ZERO, Vector3.UNIT_Z, Double.POSITIVE_INFINITY);

    assertTrue(LineSegment3.isFinite(seg1));
    assertFalse(LineSegment3.isFinite(seg2));
    assertFalse(LineSegment3.isFinite(seg3));
    assertFalse(LineSegment3.isFinite(seg4));
    assertFalse(LineSegment3.isFinite(seg5));

    seg5.setExtent(1);
    assertTrue(LineSegment3.isFinite(seg5));

    assertFalse(LineSegment3.isFinite(null));
  }

  @Test
  public void testDistance() {
    final LineSegment3 seg1 = new LineSegment3(Vector3.ZERO, Vector3.UNIT_Z, 1.0);
    final Vector3 store = new Vector3();
    assertTrue(16.0 == seg1.distanceSquared(new Vector3(0, 0, 5), store));
    assertEquals(Vector3.UNIT_Z, store);
    assertTrue(9.0 == seg1.distanceSquared(new Vector3(0, 0, -4), store));
    assertEquals(Vector3.NEG_UNIT_Z, store);
    assertTrue(4.0 == seg1.distanceSquared(new Vector3(2, 0, 0), store));
    assertEquals(Vector3.ZERO, store);
    assertTrue(1.0 == seg1.distanceSquared(Vector3.NEG_UNIT_X, null));
  }

}
