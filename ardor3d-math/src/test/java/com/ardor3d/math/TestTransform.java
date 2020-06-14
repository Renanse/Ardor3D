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

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import org.junit.Test;

public class TestTransform {

  @Test
  public void testGetSet() {
    final Transform trans = new Transform();
    assertEquals(Transform.IDENTITY, trans);

    final Transform immutable = new Transform(new Matrix3().applyRotationX(MathUtils.QUARTER_PI),
        new Vector3(0, -1, -2), new Vector3(1, 2, 3), true, true, true);
    assertTrue(true == immutable.isIdentity());
    assertTrue(true == immutable.isRotationMatrix());
    assertTrue(true == immutable.isUniformScale());
    assertEquals(new Matrix3().applyRotationX(MathUtils.QUARTER_PI), immutable.getMatrix());
    assertEquals(new Vector3(0, -1, -2), immutable.getScale());
    assertEquals(new Vector3(1, 2, 3), immutable.getTranslation());

    final Transform trans2 = new Transform(immutable);
    assertEquals(immutable, trans2);
    trans2.updateFlags(false);

    trans.set(immutable);
    assertEquals(Transform.IDENTITY, trans); // because of shortcut flags.

    trans.set(trans2);
    assertEquals(trans2, trans);

    trans.setIdentity();
    assertEquals(Transform.IDENTITY, trans);

    final double a = MathUtils.QUARTER_PI;
    trans.setRotation(new Quaternion().fromAngleAxis(a, Vector3.UNIT_Y));

    assertEquals(new Matrix3( //
        Math.cos(a), 0, Math.sin(a), //
        0, 1, 0, //
        -Math.sin(a), 0, Math.cos(a)), trans.getMatrix());

    trans2.setRotation(new Matrix3().fromAngleAxis(a, Vector3.UNIT_Y));
    assertEquals(trans.getMatrix(), trans2.getMatrix());

    trans.setScale(1.0);
    assertEquals(Vector3.ONE, trans.getScale());

    trans.setScale(new Vector3(1, 2, 3));
    assertEquals(new Vector3(1, 2, 3), trans.getScale());

    trans.setScale(-1, 5, -3);
    assertEquals(new Vector3(-1, 5, -3), trans.getScale());

    trans.setTranslation(new Vector3(10, 20, 30));
    assertEquals(new Vector3(10, 20, 30), trans.getTranslation());

    trans.setTranslation(-10, 50, -30);
    assertEquals(new Vector3(-10, 50, -30), trans.getTranslation());

    trans.setIdentity();
    trans.setRotation(new Matrix3().fromAngleAxis(a, Vector3.UNIT_Y));
    trans.setScale(2, 3, 4);
    trans.setTranslation(5, 10, 15);

    final Matrix4 mat4 = trans.getHomogeneousMatrix(null);
    assertEquals(new Matrix4( //
        2 * Math.cos(a), 2 * 0, 2 * Math.sin(a), 5, //
        3 * 0, 3 * 1, 3 * 0, 10, //
        4 * -Math.sin(a), 4 * 0, 4 * Math.cos(a), 15, //
        0, 0, 0, 1), mat4);

    trans2.fromHomogeneousMatrix(mat4);
    trans2.getHomogeneousMatrix(mat4);
    assertEquals(new Matrix4( //
        2 * Math.cos(a), 2 * 0, 2 * Math.sin(a), 5, //
        3 * 0, 3 * 1, 3 * 0, 10, //
        4 * -Math.sin(a), 4 * 0, 4 * Math.cos(a), 15, //
        0, 0, 0, 1), mat4);

    trans.setIdentity();
    trans.setRotation(new Matrix3(0, 1, 2, 3, 4, 5, 6, 7, 8));
    trans.setTranslation(10, 11, 12);
    trans.getHomogeneousMatrix(mat4);
    assertEquals(new Matrix4( //
        0, 1, 2, 10, //
        3, 4, 5, 11, //
        6, 7, 8, 12, //
        0, 0, 0, 1), mat4);

  }

  @Test(expected = TransformException.class)
  public void testFailScale1A() {
    final Transform trans = new Transform(new Matrix3(), new Vector3(), new Vector3(), false, false, false);
    trans.setScale(Vector3.ONE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailScale1B() {
    final Transform trans = new Transform();
    trans.setScale(Vector3.ZERO);
  }

  @Test(expected = TransformException.class)
  public void testFailScale2A() {
    final Transform trans = new Transform(new Matrix3(), new Vector3(), new Vector3(), false, false, false);
    trans.setScale(1, 1, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailScale2B() {
    final Transform trans = new Transform();
    trans.setScale(0, 0, 0);
  }

  @Test(expected = TransformException.class)
  public void testFailScale3A() {
    final Transform trans = new Transform(new Matrix3(), new Vector3(), new Vector3(), false, false, false);
    trans.setScale(1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailScale3B() {
    final Transform trans = new Transform();
    trans.setScale(0);
  }

  @Test
  public void testTranslate() {
    final Transform trans = new Transform();
    trans.translate(1, 3, 5);
    assertEquals(new Vector3(1, 3, 5), trans.getTranslation());
    trans.translate(trans.getTranslation().negate(null));
    assertEquals(Vector3.ZERO, trans.getTranslation());

    trans.translate(new Vector3(1, 3, 5));
    assertEquals(new Vector3(1, 3, 5), trans.getTranslation());
    trans.translate(-1, -3, -5);
    assertEquals(Vector3.ZERO, trans.getTranslation());
  }

  @Test
  public void testApplyVector3() {
    final Transform trans =
        new Transform().setRotation(new Matrix3().applyRotationX(MathUtils.HALF_PI)).translate(1, 2, 3);
    final Vector3 vec3 = new Vector3(0, 1, 0);

    final Vector3 result = trans.applyForward(vec3, null);
    assertTrue(Math.abs(new Vector3(1, 2, 4).distance(result)) <= MathUtils.EPSILON);
    trans.applyForward(vec3, result);
    assertTrue(Math.abs(new Vector3(1, 2, 4).distance(result)) <= MathUtils.EPSILON);
    trans.applyForward(vec3);
    assertTrue(Math.abs(new Vector3(1, 2, 4).distance(vec3)) <= MathUtils.EPSILON);

    vec3.set(0, 1, 1);
    final Vector3 result2 = trans.applyForwardVector(vec3, null);
    assertTrue(Math.abs(new Vector3(0, -1, 1).distance(result2)) <= MathUtils.EPSILON);
    trans.applyForwardVector(vec3, result2);
    assertTrue(Math.abs(new Vector3(0, -1, 1).distance(result2)) <= MathUtils.EPSILON);
    trans.applyForwardVector(vec3);
    assertTrue(Math.abs(new Vector3(0, -1, 1).distance(vec3)) <= MathUtils.EPSILON);

    vec3.set(0, 1, 0);
    final Vector3 result3 = trans.applyInverse(vec3, null);
    assertTrue(Math.abs(new Vector3(-1, -3, 1).distance(result3)) <= MathUtils.EPSILON);
    trans.applyInverse(vec3, result3);
    assertTrue(Math.abs(new Vector3(-1, -3, 1).distance(result3)) <= MathUtils.EPSILON);
    trans.applyInverse(vec3);
    assertTrue(Math.abs(new Vector3(-1, -3, 1).distance(vec3)) <= MathUtils.EPSILON);

    vec3.set(0, 1, 1);
    final Vector3 result4 = trans.applyInverseVector(vec3, null);
    assertTrue(Math.abs(new Vector3(0, 1, -1).distance(result4)) <= MathUtils.EPSILON);
    trans.applyInverseVector(vec3, result4);
    assertTrue(Math.abs(new Vector3(0, 1, -1).distance(result4)) <= MathUtils.EPSILON);
    trans.applyInverseVector(vec3);
    assertTrue(Math.abs(new Vector3(0, 1, -1).distance(vec3)) <= MathUtils.EPSILON);

    trans.setRotation(new Matrix3().applyRotationY(MathUtils.PI)).translate(2, 3, -1);

    vec3.set(1, 2, 3).normalizeLocal();
    final Vector3 orig = new Vector3(vec3);
    trans.applyForward(vec3);
    trans.applyInverse(vec3);
    assertTrue(Math.abs(orig.distance(vec3)) <= 10 * MathUtils.EPSILON); // accumulated error

    vec3.set(orig);
    trans.applyForwardVector(vec3);
    trans.applyInverseVector(vec3);
    assertTrue(Math.abs(orig.distance(vec3)) <= 10 * MathUtils.EPSILON); // accumulated error

    vec3.set(orig);
    trans.setIdentity();
    trans.applyForward(vec3);
    assertEquals(orig, vec3);
    trans.applyForwardVector(vec3);
    assertEquals(orig, vec3);
    trans.applyInverse(vec3);
    assertEquals(orig, vec3);
    trans.applyInverseVector(vec3);
    assertEquals(orig, vec3);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testApplyFail1() {
    final Transform trans = new Transform();
    trans.applyForward(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testApplyFail2() {
    final Transform trans = new Transform();
    trans.applyForwardVector(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testApplyFail3() {
    final Transform trans = new Transform();
    trans.applyInverse(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testApplyFail4() {
    final Transform trans = new Transform();
    trans.applyInverseVector(null);
  }

  @Test
  public void testMultiply() {
    final Transform trans1 = new Transform();
    final Transform trans2 = new Transform();
    assertEquals(Transform.IDENTITY, trans1.multiply(trans2, null));

    trans1.setTranslation(1, 2, 3);
    final Transform trans3 = trans1.multiply(trans2, null);
    assertEquals(trans1, trans3);

    trans2.setTranslation(-1, -2, -3);
    trans1.multiply(trans2, trans3);
    assertEquals(Transform.IDENTITY, trans3);
    assertTrue(trans3.isRotationMatrix());
    assertTrue(trans3.isIdentity());
    assertTrue(trans3.isUniformScale());

    trans2.setScale(1, 2, 1);
    trans1.multiply(trans2, trans3);
    assertEquals(new Transform().setScale(1, 2, 1), trans3);
    assertTrue(trans3.isRotationMatrix());
    assertFalse(trans3.isIdentity());
    assertFalse(trans3.isUniformScale());

    trans1.setScale(1, 2, 1);
    trans1.multiply(trans2, trans3);
    assertEquals(new Transform().setRotation(new Matrix3(1, 0, 0, 0, 4, 0, 0, 0, 1)).setTranslation(0, -2, 0), trans3);
    assertFalse(trans3.isRotationMatrix());
    assertFalse(trans3.isIdentity());
    assertFalse(trans3.isUniformScale());
  }

  @Test
  public void testInvert() {
    final Transform trans1 = new Transform();
    trans1.setRotation(new Matrix3().applyRotationZ(3 * MathUtils.QUARTER_PI));
    final Transform trans2 = trans1.invert(null);
    assertEquals(Transform.IDENTITY, trans1.multiply(trans2, null));

    trans1.setIdentity().invert(trans1);
    assertEquals(Transform.IDENTITY, trans1);
  }

  @Test
  public void testClone() {
    final Transform trans1 = new Transform();
    final Transform trans2 = trans1.clone();
    assertEquals(trans1, trans2);
    assertNotSame(trans1, trans2);
  }

  @Test
  public void testValid() {
    final Transform trans = new Transform();
    assertTrue(Transform.isValid(trans));
    trans.setIdentity();
    trans.setRotation(new Matrix3(Double.NaN, 0, 0, 0, 0, 0, 0, 0, 0));
    assertFalse(Transform.isValid(trans));
    trans.setIdentity();
    trans.setScale(Double.NaN, 0, 0);
    assertFalse(Transform.isValid(trans));
    trans.setScale(Double.NaN);
    assertFalse(Transform.isValid(trans));
    trans.setIdentity();
    trans.setTranslation(Double.NaN, 0, 0);
    assertFalse(Transform.isValid(trans));

    trans.setIdentity();
    assertTrue(Transform.isValid(trans));

    assertFalse(Transform.isValid(null));

    // couple of equals validity tests
    assertEquals(trans, trans);
    assertTrue(trans.strictEquals(trans));
    assertFalse(trans.equals(null));
    assertFalse(trans.strictEquals(null));
    assertFalse(trans.strictEquals(new Vector2()));

    // throw in a couple pool accesses for coverage
    final Transform transTemp = Transform.fetchTempInstance();
    transTemp.set(trans);
    assertEquals(trans, transTemp);
    assertNotSame(trans, transTemp);
    Transform.releaseTempInstance(transTemp);

    // cover more of equals
    trans.setScale(1, 2, 3);
    trans.setRotation(new Matrix3(0, 1, 2, 3, 4, 5, 6, 7, 8));
    trans.setTranslation(1, 2, 3);
    final Transform comp = new Transform();
    final Matrix3 mat3 = new Matrix3(-1, -1, -1, -1, -1, -1, -1, -1, -1);
    comp.setScale(-1, -1, -1);
    comp.setRotation(mat3);
    comp.setTranslation(-1, -1, -1);
    assertFalse(trans.equals(comp));
    assertFalse(trans.strictEquals(comp));
    for (int i = 0; i < 8; i++) {
      mat3.setValue(i / 3, i % 3, i);
      comp.setRotation(mat3);
      assertFalse(trans.equals(comp));
      assertFalse(trans.strictEquals(comp));
    }
    // test translation
    trans.setRotation(Matrix3.IDENTITY);
    comp.setRotation(Matrix3.IDENTITY);
    comp.setTranslation(1, -1, -1);
    assertFalse(trans.equals(comp));
    assertFalse(trans.strictEquals(comp));
    comp.setTranslation(1, 2, -1);
    assertFalse(trans.equals(comp));
    assertFalse(trans.strictEquals(comp));
    comp.setTranslation(1, 2, 3);
    assertFalse(trans.equals(comp));
    assertFalse(trans.strictEquals(comp));

    // test scale
    comp.setScale(1, -1, -1);
    assertFalse(trans.equals(comp));
    assertFalse(trans.strictEquals(comp));
    comp.setScale(1, 2, -1);
    assertFalse(trans.equals(comp));
    assertFalse(trans.strictEquals(comp));
    comp.setScale(1, 2, 3);
    assertTrue(trans.equals(comp));
    assertTrue(trans.strictEquals(comp));
  }

  @Test
  public void testSimpleHash() {
    // Just a simple sanity check.
    final Transform trans1 = new Transform().setTranslation(1, 2, 3);
    final Transform trans2 = new Transform().setTranslation(1, 2, 3);
    final Transform trans3 = new Transform().setTranslation(1, 2, 0);

    assertTrue(trans1.hashCode() == trans2.hashCode());
    assertTrue(trans1.hashCode() != trans3.hashCode());
  }

  @Test
  public void testGLApplyMatrix() {
    final Transform trans = new Transform();

    // non-rotational
    trans.setRotation(new Matrix3(0, 1, 2, 3, 4, 5, 6, 7, 8));
    trans.setTranslation(10, 11, 12);
    final DoubleBuffer db = DoubleBuffer.allocate(16);
    trans.getGLApplyMatrix(db);
    db.flip();
    for (final double val : new double[] {0, 3, 6, 0, 1, 4, 7, 0, 2, 5, 8, 0, 10, 11, 12, 1}) {
      assertTrue(val == db.get());
    }
    final FloatBuffer fb = FloatBuffer.allocate(16);
    trans.getGLApplyMatrix(fb);
    fb.flip();
    for (final float val : new float[] {0, 3, 6, 0, 1, 4, 7, 0, 2, 5, 8, 0, 10, 11, 12, 1}) {
      assertTrue(val == fb.get());
    }

    // rotational
    final double a = MathUtils.QUARTER_PI;
    trans.setRotation(new Matrix3().applyRotationY(a));
    trans.setTranslation(10, 11, 12);
    trans.setScale(2, 3, 4);
    db.rewind();
    trans.getGLApplyMatrix(db);
    db.flip();
    for (final double val : new double[] {2 * Math.cos(a), 2 * 0, 2 * -Math.sin(a), 0, //
        3 * 0, 3 * 1, 3 * 0, 0, //
        4 * Math.sin(a), 4 * 0, 4 * Math.cos(a), 0, //
        10, 11, 12, 1}) {
      assertTrue(val == db.get());
    }
    fb.rewind();
    trans.getGLApplyMatrix(fb);
    fb.flip();
    for (final float val : new float[] {(float) (2 * Math.cos(a)), 2 * 0, (float) (2 * -Math.sin(a)), 0, //
        3 * 0, 3 * 1, 3 * 0, 0, //
        (float) (4 * Math.sin(a)), 4 * 0, (float) (4 * Math.cos(a)), 0, //
        10, 11, 12, 1}) {
      assertTrue(val == fb.get());
    }
  }
}
