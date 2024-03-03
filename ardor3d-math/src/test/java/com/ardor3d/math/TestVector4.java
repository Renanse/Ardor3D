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

public class TestVector4 {

  @Test
  public void testAdd() {
    final Vector4 vec1 = new Vector4();
    final Vector4 vec2 = new Vector4(Vector4.ONE);

    vec1.addLocal(1, 2, 3, 4);
    assertEquals(new Vector4(1, 2, 3, 4), vec1);
    vec1.addLocal(-1, -2, -3, -4);
    assertEquals(Vector4.ZERO, vec1);

    vec1.zero();
    vec1.addLocal(vec2);
    assertEquals(Vector4.ONE, vec1);

    vec1.zero();
    final Vector4 vec3 = vec1.add(vec2, new Vector4());
    assertEquals(Vector4.ZERO, vec1);
    assertEquals(Vector4.ONE, vec3);

    final Vector4 vec4 = vec1.add(0, 0, 0, 1, null);
    assertEquals(Vector4.ZERO, vec1);
    assertEquals(Vector4.UNIT_W, vec4);
  }

  @Test
  public void testSubtract() {
    final Vector4 vec1 = new Vector4();
    final Vector4 vec2 = new Vector4(Vector4.ONE);

    vec1.subtractLocal(1, 2, 3, 4);
    assertEquals(new Vector4(-1, -2, -3, -4), vec1);
    vec1.subtractLocal(-1, -2, -3, -4);
    assertEquals(Vector4.ZERO, vec1);

    vec1.zero();
    vec1.subtractLocal(vec2);
    assertEquals(Vector4.NEG_ONE, vec1);

    vec1.zero();
    final Vector4 vec3 = vec1.subtract(vec2, new Vector4());
    assertEquals(Vector4.ZERO, vec1);
    assertEquals(Vector4.NEG_ONE, vec3);

    final Vector4 vec4 = vec1.subtract(0, 0, 0, 1, null);
    assertEquals(Vector4.ZERO, vec1);
    assertEquals(Vector4.NEG_UNIT_W, vec4);
  }

  @Test
  public void testGetSet() {
    final Vector4 vec1 = new Vector4();
    vec1.setX(0);
    assertTrue(vec1.getX() == 0.0);
    vec1.setX(Double.POSITIVE_INFINITY);
    assertTrue(vec1.getX() == Double.POSITIVE_INFINITY);
    vec1.setX(Double.NEGATIVE_INFINITY);
    assertTrue(vec1.getX() == Double.NEGATIVE_INFINITY);
    assertTrue(vec1.getValue(0) == Double.NEGATIVE_INFINITY);

    vec1.setY(0);
    assertTrue(vec1.getY() == 0.0);
    vec1.setY(Double.POSITIVE_INFINITY);
    assertTrue(vec1.getY() == Double.POSITIVE_INFINITY);
    vec1.setY(Double.NEGATIVE_INFINITY);
    assertTrue(vec1.getY() == Double.NEGATIVE_INFINITY);
    assertTrue(vec1.getValue(1) == Double.NEGATIVE_INFINITY);

    vec1.setZ(0);
    assertTrue(vec1.getZ() == 0.0);
    vec1.setZ(Double.POSITIVE_INFINITY);
    assertTrue(vec1.getZ() == Double.POSITIVE_INFINITY);
    vec1.setZ(Double.NEGATIVE_INFINITY);
    assertTrue(vec1.getZ() == Double.NEGATIVE_INFINITY);
    assertTrue(vec1.getValue(2) == Double.NEGATIVE_INFINITY);

    vec1.setW(0);
    assertTrue(vec1.getW() == 0.0);
    vec1.setW(Double.POSITIVE_INFINITY);
    assertTrue(vec1.getW() == Double.POSITIVE_INFINITY);
    vec1.setW(Double.NEGATIVE_INFINITY);
    assertTrue(vec1.getW() == Double.NEGATIVE_INFINITY);
    assertTrue(vec1.getValue(3) == Double.NEGATIVE_INFINITY);

    vec1.set(Math.PI, Math.PI, Math.PI, Math.PI);
    assertTrue(vec1.getXf() == (float) Math.PI);
    assertTrue(vec1.getYf() == (float) Math.PI);
    assertTrue(vec1.getZf() == (float) Math.PI);
    assertTrue(vec1.getWf() == (float) Math.PI);

    final Vector4 vec2 = new Vector4();
    vec2.set(vec1);
    assertEquals(vec1, vec2);

    vec1.setValue(0, 0);
    vec1.setValue(1, 0);
    vec1.setValue(2, 0);
    vec1.setValue(3, 0);
    assertEquals(Vector4.ZERO, vec1);

    // catch a few expected exceptions
    try {
      vec2.getValue(4);
      fail("getValue(4) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      vec2.getValue(-1);
      fail("getValue(-1) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      vec2.setValue(-1, 0);
      fail("setValue(-1, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      vec2.setValue(4, 0);
      fail("setValue(4, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    // above exceptions shouldn't have altered vec2
    assertEquals(new Vector4(Math.PI, Math.PI, Math.PI, Math.PI), vec2);
  }

  @Test
  public void testToArray() {
    final Vector4 vec1 = new Vector4();
    vec1.set(Math.PI, Double.MAX_VALUE, 42, -1);
    final double[] array = vec1.toArray(null);
    final double[] array2 = vec1.toArray(new double[4]);
    assertNotNull(array);
    assertTrue(array.length == 4);
    assertTrue(array[0] == Math.PI);
    assertTrue(array[1] == Double.MAX_VALUE);
    assertTrue(array[2] == 42);
    assertTrue(array[3] == -1);
    assertNotNull(array2);
    assertTrue(array2.length == 4);
    assertTrue(array2[0] == Math.PI);
    assertTrue(array2[1] == Double.MAX_VALUE);
    assertTrue(array2[2] == 42);
    assertTrue(array2[3] == -1);

    try {
      vec1.toArray(new double[1]);
      fail("toArray(d[1]) should have thrown ArrayIndexOutOfBoundsException.");
    } catch (final ArrayIndexOutOfBoundsException e) {}
  }

  @Test
  public void testMultiply() {
    final Vector4 vec1 = new Vector4(1, -1, 2, -2);
    final Vector4 vec2 = vec1.multiply(2.0, null);
    final Vector4 vec2B = vec1.multiply(2.0, new Vector4());
    assertEquals(new Vector4(2.0, -2.0, 4.0, -4.0), vec2);
    assertEquals(new Vector4(2.0, -2.0, 4.0, -4.0), vec2B);

    vec2.multiplyLocal(0.5);
    assertEquals(new Vector4(1.0, -1.0, 2.0, -2.0), vec2);

    final Vector4 vec3 = vec1.multiply(vec2, null);
    final Vector4 vec3B = vec1.multiply(vec2, new Vector4());
    assertEquals(new Vector4(1, 1, 4, 4), vec3);
    assertEquals(new Vector4(1, 1, 4, 4), vec3B);

    final Vector4 vec4 = vec1.multiply(2, 3, 2, 3, null);
    final Vector4 vec4B = vec1.multiply(2, 3, 2, 3, new Vector4());
    assertEquals(new Vector4(2, -3, 4, -6), vec4);
    assertEquals(new Vector4(2, -3, 4, -6), vec4B);

    vec1.multiplyLocal(0.5, 0.5, 0.5, 0.5);
    assertEquals(new Vector4(0.5, -0.5, 1.0, -1.0), vec1);

    vec1.multiplyLocal(vec2);
    assertEquals(new Vector4(0.5, 0.5, 2.0, 2.0), vec1);
  }

  @Test
  public void testDivide() {
    final Vector4 vec1 = new Vector4(1, -1, 2, -2);
    final Vector4 vec2 = vec1.divide(2.0, null);
    final Vector4 vec2B = vec1.divide(2.0, new Vector4());
    assertEquals(new Vector4(0.5, -0.5, 1.0, -1.0), vec2);
    assertEquals(new Vector4(0.5, -0.5, 1.0, -1.0), vec2B);

    vec2.divideLocal(0.5);
    assertEquals(new Vector4(1.0, -1.0, 2.0, -2.0), vec2);

    final Vector4 vec3 = vec1.divide(vec2, null);
    final Vector4 vec3B = vec1.divide(vec2, new Vector4());
    assertEquals(Vector4.ONE, vec3);
    assertEquals(Vector4.ONE, vec3B);

    final Vector4 vec4 = vec1.divide(2, 3, 4, 5, null);
    final Vector4 vec4B = vec1.divide(2, 3, 4, 5, new Vector4());
    assertEquals(new Vector4(0.5, -1 / 3., 0.5, -0.4), vec4);
    assertEquals(new Vector4(0.5, -1 / 3., 0.5, -0.4), vec4B);

    vec1.divideLocal(0.5, 0.5, 0.5, 0.5);
    assertEquals(new Vector4(2, -2, 4, -4), vec1);

    vec1.divideLocal(vec2);
    assertEquals(new Vector4(2, 2, 2, 2), vec1);
  }

  @Test
  public void testScaleAdd() {
    final Vector4 vec1 = new Vector4(1, 1, 1, 1);
    final Vector4 vec2 = vec1.scaleAdd(2.0, new Vector4(1, 2, 3, 4), null);
    final Vector4 vec2B = vec1.scaleAdd(2.0, new Vector4(1, 2, 3, 4), new Vector4());
    assertEquals(new Vector4(3.0, 4.0, 5.0, 6.0), vec2);
    assertEquals(new Vector4(3.0, 4.0, 5.0, 6.0), vec2B);

    vec1.scaleAddLocal(2.0, new Vector4(1, 2, 3, 4));
    assertEquals(vec2, vec1);
  }

  @Test
  public void testNegate() {
    final Vector4 vec1 = new Vector4(3, 2, -1, 1);
    final Vector4 vec2 = vec1.negate(null);
    assertEquals(new Vector4(-3, -2, 1, -1), vec2);

    vec1.negateLocal();
    assertEquals(vec2, vec1);
  }

  @Test
  public void testNormalize() {
    final Vector4 vec1 = new Vector4(2, 1, 3, -1);
    assertTrue(vec1.length() == Math.sqrt(15));

    final Vector4 vec2 = vec1.normalize(null);
    final double invLength = MathUtils.inverseSqrt(2 * 2 + 1 * 1 + 3 * 3 + -1 * -1);
    assertEquals(new Vector4(2 * invLength, 1 * invLength, 3 * invLength, -1 * invLength), vec2);

    vec1.normalizeLocal();
    assertEquals(new Vector4(2 * invLength, 1 * invLength, 3 * invLength, -1 * invLength), vec1);

    vec1.zero();
    vec1.normalize(vec2);
    assertEquals(vec1, vec2);

    // ensure no exception thrown
    vec1.normalizeLocal();
    vec1.normalize(null);
  }

  @Test
  public void testDistance() {
    final Vector4 vec1 = new Vector4(0, 0, 0, 0);
    assertTrue(4.0 == vec1.distance(4, 0, 0, 0));
    assertTrue(3.0 == vec1.distance(0, 3, 0, 0));
    assertTrue(2.0 == vec1.distance(0, 0, 2, 0));
    assertTrue(1.0 == vec1.distance(0, 0, 0, 1));

    final Vector4 vec2 = new Vector4(1, 1, 1, 1);
    assertTrue(Math.sqrt(4) == vec1.distance(vec2));
  }

  @Test
  public void testLerp() {
    final Vector4 vec1 = new Vector4(8, 3, -2, 2);
    final Vector4 vec2 = new Vector4(2, 1, 0, -2);
    assertEquals(new Vector4(5, 2, -1, 0), vec1.lerp(vec2, 0.5, null));
    assertEquals(new Vector4(5, 2, -1, 0), vec1.lerp(vec2, 0.5, new Vector4()));
    assertEquals(new Vector4(5, 2, -1, 0), Vector4.lerp(vec1, vec2, 0.5, null));
    assertEquals(new Vector4(5, 2, -1, 0), Vector4.lerp(vec1, vec2, 0.5, new Vector4()));

    vec1.set(14, 5, 4, 2);
    vec1.lerpLocal(vec2, 0.25);
    assertEquals(new Vector4(11, 4, 3, 1), vec1);

    vec1.set(15, 7, 6, 8);
    final Vector4 vec3 = new Vector4(-1, -1, -1, -1);
    vec3.lerpLocal(vec1, vec2, 0.5);
    assertEquals(new Vector4(8.5, 4.0, 3.0, 3.0), vec3);

    // coverage
    assertEquals(vec1.lerp(vec1, .25, null), vec1);
    assertEquals(vec2.lerpLocal(vec2, .25), vec2);
    assertEquals(vec2.lerpLocal(vec2, vec2, .25), vec2);
    assertEquals(Vector4.lerp(vec1, vec1, .25, null), vec1);
  }

  @Test
  public void testDot() {
    final Vector4 vec1 = new Vector4(7, 2, 5, -1);
    assertTrue(35.0 == vec1.dot(3, 1, 2, -2));

    assertTrue(-11.0 == vec1.dot(new Vector4(-1, 1, -1, 1)));
  }

  @Test
  public void testClone() {
    final Vector4 vec1 = new Vector4(0, 0, 0, 0);
    final Vector4 vec2 = vec1.clone();
    assertEquals(vec1, vec2);
    assertNotSame(vec1, vec2);
  }

  @Test
  public void testFinite() {
    final Vector4 vec1 = new Vector4(0, 0, 0, 0);
    final Vector4 vec2A = new Vector4(Double.POSITIVE_INFINITY, 0, 0, 0);
    final Vector4 vec2B = new Vector4(0, Double.NEGATIVE_INFINITY, 0, 0);
    final Vector4 vec2C = new Vector4(0, 0, Double.POSITIVE_INFINITY, 0);
    final Vector4 vec2D = new Vector4(0, 0, 0, Double.POSITIVE_INFINITY);
    final Vector4 vec3A = new Vector4(Double.NaN, 0, 0, 0);
    final Vector4 vec3B = new Vector4(0, Double.NaN, 0, 0);
    final Vector4 vec3C = new Vector4(0, 0, Double.NaN, 0);
    final Vector4 vec3D = new Vector4(0, 0, 0, Double.NaN);

    assertTrue(Vector4.isFinite(vec1));
    assertFalse(Vector4.isFinite(vec2A));
    assertFalse(Vector4.isFinite(vec2B));
    assertFalse(Vector4.isFinite(vec2C));
    assertFalse(Vector4.isFinite(vec2D));
    assertFalse(Vector4.isFinite(vec3A));
    assertFalse(Vector4.isFinite(vec3B));
    assertFalse(Vector4.isFinite(vec3C));
    assertFalse(Vector4.isFinite(vec3D));

    vec3C.zero();
    assertTrue(Vector4.isFinite(vec3C));

    assertFalse(Vector4.isFinite(null));

    // couple of equals validity tests
    assertEquals(vec1, vec1);
    assertFalse(vec1.equals(null));

    // throw in a couple pool accesses for coverage
    final Vector4 vec6 = Vector4.fetchTempInstance();
    vec6.set(vec1);
    assertEquals(vec1, vec6);
    assertNotSame(vec1, vec6);
    Vector4.releaseTempInstance(vec6);

    // cover more of equals
    vec1.set(0, 1, 2, 3);
    assertFalse(vec1.equals(new Vector4(0, 4, 4, 4)));
    assertFalse(vec1.equals(new Vector4(0, 1, 4, 4)));
    assertFalse(vec1.equals(new Vector4(0, 1, 2, 4)));
  }

  @Test
  public void testSimpleHash() {
    // Just a simple sanity check.
    final Vector4 vec1 = new Vector4(1, 2, 3, 4);
    final Vector4 vec2 = new Vector4(1, 2, 3, 4);
    final Vector4 vec3 = new Vector4(2, 2, 2, 2);

    assertTrue(vec1.hashCode() == vec2.hashCode());
    assertTrue(vec1.hashCode() != vec3.hashCode());
  }
}
