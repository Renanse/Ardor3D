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

public class TestRing {

  @Test
  public void testGetSet() {
    final Ring ring = new Ring();
    assertEquals(Vector3.ZERO, ring.getCenter());
    assertEquals(Vector3.UNIT_Y, ring.getUp());
    assertTrue(ring.getInnerRadius() == 0.0);
    assertTrue(ring.getOuterRadius() == 1.0);

    ring.setCenter(Vector3.ONE);
    ring.setUp(Vector3.UNIT_X);
    ring.setInnerRadius(1.5);
    ring.setOuterRadius(3.0);
    assertEquals(Vector3.ONE, ring.getCenter());
    assertEquals(Vector3.UNIT_X, ring.getUp());
    assertTrue(ring.getInnerRadius() == 1.5);
    assertTrue(ring.getOuterRadius() == 3.0);

    final Ring ring2 = new Ring(ring);
    assertEquals(Vector3.ONE, ring2.getCenter());
    assertEquals(Vector3.UNIT_X, ring2.getUp());
    assertTrue(ring2.getInnerRadius() == 1.5);
    assertTrue(ring2.getOuterRadius() == 3.0);

    final Ring ring3 = new Ring(Vector3.NEG_ONE, Vector3.UNIT_Z, 12.0, 42.0);
    assertEquals(Vector3.NEG_ONE, ring3.getCenter());
    assertEquals(Vector3.UNIT_Z, ring3.getUp());
    assertTrue(ring3.getInnerRadius() == 12.0);
    assertTrue(ring3.getOuterRadius() == 42.0);
  }

  @Test
  public void testFinite() {
    assertTrue(Ring.isFinite(new Ring()));
    assertFalse(Ring.isFinite(null));
    assertFalse(Ring.isFinite(new Ring(Vector3.ZERO, Vector3.UNIT_Y, 0.0, Double.NaN)));
    assertFalse(Ring.isFinite(new Ring(Vector3.ZERO, Vector3.UNIT_Y, 0.0, Double.POSITIVE_INFINITY)));
    assertFalse(Ring.isFinite(new Ring(Vector3.ZERO, Vector3.UNIT_Y, Double.NaN, 1.0)));
    assertFalse(Ring.isFinite(new Ring(Vector3.ZERO, Vector3.UNIT_Y, Double.NEGATIVE_INFINITY, 1.0)));
    assertFalse(Ring.isFinite(new Ring(Vector3.ZERO, new Vector3(Double.NaN, 0, 0), 0.0, 1.0)));
    assertFalse(Ring.isFinite(new Ring(Vector3.ZERO, new Vector3(Double.POSITIVE_INFINITY, 0, 0), 0.0, 1.0)));
    assertFalse(Ring.isFinite(new Ring(new Vector3(Double.NaN, 0, 0), Vector3.UNIT_Y, 0.0, 1.0)));
    assertFalse(Ring.isFinite(new Ring(new Vector3(Double.NEGATIVE_INFINITY, 0, 0), Vector3.UNIT_Y, 0.0, 1.0)));

    // couple of equals validity tests
    final Ring ring1 = new Ring();
    assertEquals(ring1, ring1);
    assertFalse(ring1.equals(null));

    // throw in a couple pool accesses for coverage
    final Ring ring2 = Ring.fetchTempInstance();
    ring2.set(ring1);
    assertEquals(ring1, ring2);
    assertNotSame(ring1, ring2);
    Ring.releaseTempInstance(ring2);

    // cover more of equals
    assertFalse(ring1.equals(new Ring(Vector3.UNIT_X, Vector3.UNIT_X, -1.0, 2.0)));
    assertFalse(ring1.equals(new Ring(Vector3.UNIT_X, Vector3.UNIT_X, -1.0, 1.0)));
    assertFalse(ring1.equals(new Ring(Vector3.UNIT_X, Vector3.UNIT_X, 0.0, 1.0)));
    assertFalse(ring1.equals(new Ring(Vector3.UNIT_X, Vector3.UNIT_Y, 0.0, 1.0)));
    assertTrue(ring1.equals(new Ring(Vector3.ZERO, Vector3.UNIT_Y, 0.0, 1.0)));
  }

  @Test
  public void testSimpleHash() {
    // Just a simple sanity check.
    final Ring ring1 = new Ring(Vector3.ZERO, Vector3.UNIT_Y, 0.0, 2.0);
    final Ring ring2 = new Ring(Vector3.ZERO, Vector3.UNIT_Y, 0.0, 2.0);
    final Ring ring3 = new Ring(Vector3.ZERO, Vector3.UNIT_Y, 0.0, 3.0);

    assertTrue(ring1.hashCode() == ring2.hashCode());
    assertTrue(ring1.hashCode() != ring3.hashCode());
  }

  @Test
  public void testClone() {
    final Ring ring1 = new Ring();
    final Ring ring2 = ring1.clone();
    assertEquals(ring1, ring2);
    assertNotSame(ring1, ring2);
  }

  @Test
  public void testRandom() {
    MathUtils.setRandomSeed(0);
    final Ring ring1 = new Ring();
    final Vector3 store = ring1.random(null);
    assertTrue(new Vector3(0.7454530390475868, 0.0, -0.4186496466746111).equals(store, MathUtils.EPSILON));

    ring1.setUp(Vector3.UNIT_X);
    ring1.random(store);
    assertTrue(new Vector3(0.0, 0.3038618643402747, -0.38497319274818265).equals(store, MathUtils.EPSILON));
  }
}
