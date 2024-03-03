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

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import org.junit.Test;

import com.ardor3d.math.util.MathUtils;

public class TestMatrix3 {

  @Test
  public void testGetSet() {
    final Matrix3 mat3A = new Matrix3();
    assertEquals(Matrix3.IDENTITY, mat3A);

    mat3A.setM00(0.0);
    mat3A.setM01(0.1);
    mat3A.setM02(0.2);
    mat3A.setM10(1.0);
    mat3A.setM11(1.1);
    mat3A.setM12(1.2);
    mat3A.setM20(2.0);
    mat3A.setM21(2.1);
    mat3A.setM22(2.2);

    assertTrue(0.0 == mat3A.getM00());
    assertTrue(0.1 == mat3A.getM01());
    assertTrue(0.2 == mat3A.getM02());
    assertTrue(1.0 == mat3A.getM10());
    assertTrue(1.1 == mat3A.getM11());
    assertTrue(1.2 == mat3A.getM12());
    assertTrue(2.0 == mat3A.getM20());
    assertTrue(2.1 == mat3A.getM21());
    assertTrue(2.2 == mat3A.getM22());

    final Matrix3 mat3B = new Matrix3(mat3A);
    assertTrue(0.0 == mat3B.getM00());
    assertTrue(0.1 == mat3B.getM01());
    assertTrue(0.2 == mat3B.getM02());
    assertTrue(1.0 == mat3B.getM10());
    assertTrue(1.1 == mat3B.getM11());
    assertTrue(1.2 == mat3B.getM12());
    assertTrue(2.0 == mat3B.getM20());
    assertTrue(2.1 == mat3B.getM21());
    assertTrue(2.2 == mat3B.getM22());

    final Matrix3 mat3C = new Matrix3(0.0, 0.1, 0.2, 1.0, 1.1, 1.2, 2.0, 2.1, 2.2);
    assertTrue(0.0 == mat3C.getM00());
    assertTrue(0.1 == mat3C.getM01());
    assertTrue(0.2 == mat3C.getM02());
    assertTrue(1.0 == mat3C.getM10());
    assertTrue(1.1 == mat3C.getM11());
    assertTrue(1.2 == mat3C.getM12());
    assertTrue(2.0 == mat3C.getM20());
    assertTrue(2.1 == mat3C.getM21());
    assertTrue(2.2 == mat3C.getM22());

    mat3C.setIdentity();
    assertTrue(mat3C.isIdentity());

    for (int x = 0; x < 3; x++) {
      for (int y = 0; y < 3; y++) {
        final double value = (10 * x + y) / 10.;
        mat3C.setValue(x, y, value);
        assertTrue(value == mat3C.getValue(x, y));
      }
    }

    mat3C.setIdentity();
    mat3C.set(0.0, 0.1, 0.2, 2.0, 2.1, 2.2, 4.0, 4.1, 4.2);
    for (int x = 0; x < 3; x++) {
      for (int y = 0; y < 3; y++) {
        assertTrue((20 * x + y) / 10.f == mat3C.getValuef(x, y));
      }
    }

    final Matrix3 store = new Matrix3(mat3C);
    // catch a few expected exceptions
    try {
      mat3C.getValue(-1, 0);
      fail("getValue(-1, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3C.getValue(0, 3);
      fail("getValue(0, 3) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3C.getValue(1, -1);
      fail("getValue(1, -1) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3C.getValue(2, 3);
      fail("getValue(2, 3) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3C.getValue(3, 0);
      fail("getValue(3, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}

    try {
      mat3C.setValue(-1, 0, 0);
      fail("setValue(-1, 0, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3C.setValue(0, -1, 0);
      fail("setValue(0, -1, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3C.setValue(1, 3, 0);
      fail("setValue(1, 3, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3C.setValue(2, -1, 0);
      fail("setValue(2, -1, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3C.setValue(3, 0, 0);
      fail("setValue(3, 0, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    // above exceptions shouldn't have altered mat3C
    assertEquals(store, mat3C);
  }

  @Test
  public void testColumns() {
    final Matrix3 mat3A = new Matrix3();
    mat3A.setColumn(0, new Vector3(0, 3, 6));
    mat3A.setColumn(1, new Vector3(1, 4, 7));
    mat3A.setColumn(2, new Vector3(2, 5, 8));
    assertEquals(new Vector3(0, 3, 6), mat3A.getColumn(0, new Vector3()));
    assertEquals(new Vector3(1, 4, 7), mat3A.getColumn(1, null));
    assertEquals(new Vector3(2, 5, 8), mat3A.getColumn(2, null));
    try {
      mat3A.getColumn(-1, null);
      fail("getColumn(-1, null) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3A.getColumn(3, null);
      fail("getColumn(3, null) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3A.setColumn(-1, new Vector3());
      fail("setColumn(-1, Vector3) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3A.setColumn(4, new Vector3());
      fail("setColumn(4, Vector3) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}

    mat3A.fromAxes(new Vector3(1, 2, 3), new Vector3(4, 5, 6), new Vector3(7, 8, 9));
    mat3A.setColumn(0, new Vector3(1, 2, 3));
    mat3A.setColumn(1, new Vector3(4, 5, 6));
    mat3A.setColumn(2, new Vector3(7, 8, 9));
  }

  @Test
  public void testRows() {
    final Matrix3 mat3A = new Matrix3();
    mat3A.setRow(0, new Vector3(0, 1, 2));
    mat3A.setRow(1, new Vector3(3, 4, 5));
    mat3A.setRow(2, new Vector3(6, 7, 8));
    assertEquals(new Vector3(0, 1, 2), mat3A.getRow(0, new Vector3()));
    assertEquals(new Vector3(3, 4, 5), mat3A.getRow(1, null));
    assertEquals(new Vector3(6, 7, 8), mat3A.getRow(2, null));
    try {
      mat3A.getRow(-1, null);
      fail("getRow(-1, null) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3A.getRow(3, null);
      fail("getRow(3, null) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3A.setRow(-1, new Vector3());
      fail("setRow(-1, Vector3) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat3A.setRow(3, new Vector3());
      fail("setRow(3, Vector3]) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
  }

  @Test
  public void testSetRotation() {
    final Matrix3 mat3A = new Matrix3();
    // rotate identity 90 degrees around Y
    final double a = MathUtils.HALF_PI;
    final Quaternion quaternion = new Quaternion();
    quaternion.fromAngleAxis(a, Vector3.UNIT_Y);
    mat3A.set(quaternion);

    assertEquals(new Matrix3( //
        Math.cos(a), 0, Math.sin(a), //
        0, 1, 0, //
        -Math.sin(a), 0, Math.cos(a)), mat3A);
  }

  @Test
  public void testFromBuffer() {
    final FloatBuffer fb = FloatBuffer.allocate(9);
    fb.put(new float[] {0, 1, 2, 3, 4, 5, 6, 7, 8});
    fb.flip();
    // row major
    final Matrix3 mat3A = new Matrix3().fromFloatBuffer(fb, true);
    assertTrue(0 == mat3A.getM00());
    assertTrue(1 == mat3A.getM01());
    assertTrue(2 == mat3A.getM02());
    assertTrue(3 == mat3A.getM10());
    assertTrue(4 == mat3A.getM11());
    assertTrue(5 == mat3A.getM12());
    assertTrue(6 == mat3A.getM20());
    assertTrue(7 == mat3A.getM21());
    assertTrue(8 == mat3A.getM22());

    // column major
    fb.rewind();
    mat3A.setIdentity();
    mat3A.fromFloatBuffer(fb, false);
    assertTrue(0 == mat3A.getM00());
    assertTrue(3 == mat3A.getM01());
    assertTrue(6 == mat3A.getM02());
    assertTrue(1 == mat3A.getM10());
    assertTrue(4 == mat3A.getM11());
    assertTrue(7 == mat3A.getM12());
    assertTrue(2 == mat3A.getM20());
    assertTrue(5 == mat3A.getM21());
    assertTrue(8 == mat3A.getM22());

    final DoubleBuffer db = DoubleBuffer.allocate(16);
    db.put(new double[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
    db.flip();
    // row major
    mat3A.setIdentity();
    mat3A.fromDoubleBuffer(db, true);
    assertTrue(0 == mat3A.getM00());
    assertTrue(1 == mat3A.getM01());
    assertTrue(2 == mat3A.getM02());
    assertTrue(3 == mat3A.getM10());
    assertTrue(4 == mat3A.getM11());
    assertTrue(5 == mat3A.getM12());
    assertTrue(6 == mat3A.getM20());
    assertTrue(7 == mat3A.getM21());
    assertTrue(8 == mat3A.getM22());

    // column major
    db.rewind();
    mat3A.setIdentity();
    mat3A.fromDoubleBuffer(db, false);
    assertTrue(0 == mat3A.getM00());
    assertTrue(3 == mat3A.getM01());
    assertTrue(6 == mat3A.getM02());
    assertTrue(1 == mat3A.getM10());
    assertTrue(4 == mat3A.getM11());
    assertTrue(7 == mat3A.getM12());
    assertTrue(2 == mat3A.getM20());
    assertTrue(5 == mat3A.getM21());
    assertTrue(8 == mat3A.getM22());
  }

  @Test
  public void testToBuffer() {
    final double[] values = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    final double[] colmajor = {0, 3, 6, 1, 4, 7, 2, 5, 8};

    final Matrix3 mat3A = new Matrix3().fromArray(values);

    // row major
    final FloatBuffer fb = mat3A.toFloatBuffer(FloatBuffer.allocate(9), true);
    fb.flip();
    for (int i = 0; i < 9; i++) {
      assertTrue(values[i] == fb.get());
    }

    // column major
    fb.rewind();
    mat3A.toFloatBuffer(fb, false);
    fb.flip();
    for (int i = 0; i < 9; i++) {
      assertTrue(colmajor[i] == fb.get());
    }

    // row major
    final DoubleBuffer db = mat3A.toDoubleBuffer(DoubleBuffer.allocate(9), true);
    db.flip();
    for (int i = 0; i < 9; i++) {
      assertTrue(values[i] == db.get());
    }

    // column major
    db.rewind();
    mat3A.toDoubleBuffer(db, false);
    db.flip();
    for (int i = 0; i < 9; i++) {
      assertTrue(colmajor[i] == db.get());
    }
  }

  @Test
  public void testFromArray() {
    final double[] values = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    final Matrix3 mat3A = new Matrix3();

    // row major
    mat3A.fromArray(values);
    assertTrue(0 == mat3A.getM00());
    assertTrue(1 == mat3A.getM01());
    assertTrue(2 == mat3A.getM02());
    assertTrue(3 == mat3A.getM10());
    assertTrue(4 == mat3A.getM11());
    assertTrue(5 == mat3A.getM12());
    assertTrue(6 == mat3A.getM20());
    assertTrue(7 == mat3A.getM21());
    assertTrue(8 == mat3A.getM22());

    // column major
    mat3A.setIdentity();
    mat3A.fromArray(values, false);
    assertTrue(0 == mat3A.getM00());
    assertTrue(3 == mat3A.getM01());
    assertTrue(6 == mat3A.getM02());
    assertTrue(1 == mat3A.getM10());
    assertTrue(4 == mat3A.getM11());
    assertTrue(7 == mat3A.getM12());
    assertTrue(2 == mat3A.getM20());
    assertTrue(5 == mat3A.getM21());
    assertTrue(8 == mat3A.getM22());
  }

  @Test
  public void testToArray() {
    final double[] values = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    final Matrix3 mat3A = new Matrix3().fromArray(values);

    // row major
    final double[] dbls1 = mat3A.toArray(null);
    for (int i = 0; i < 9; i++) {
      assertTrue(values[i] == dbls1[i]);
    }

    // column major
    final double[] colmajor = {0, 3, 6, 1, 4, 7, 2, 5, 8};
    mat3A.toArray(dbls1, false);
    for (int i = 0; i < 9; i++) {
      assertTrue(colmajor[i] == dbls1[i]);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadArray() {
    final Matrix3 mat3A = new Matrix3();
    mat3A.toArray(new double[4]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadAnglesArray() {
    final Matrix3 mat3A = new Matrix3();
    mat3A.toAngles(new double[2]);
  }

  @Test
  public void testAngleAxis() {
    final Matrix3 mat3A = new Matrix3();
    // rotate identity 90 degrees around X
    final double angle = MathUtils.HALF_PI;
    mat3A.fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_X);
    assertEquals(new Matrix3( //
        1, 0, 0, //
        0, Math.cos(angle), -Math.sin(angle), //
        0, Math.sin(angle), Math.cos(angle)), mat3A);
  }

  @Test
  public void testRotations() {
    final Vector3 rotated = new Vector3(1, 1, 1);
    final Vector3 expected = new Vector3(1, 1, 1);

    // rotated
    final Matrix3 mat3A = new Matrix3().fromAngles(MathUtils.HALF_PI, MathUtils.QUARTER_PI, MathUtils.PI);
    mat3A.applyPost(rotated, rotated);

    // expected - post
    final Matrix3 worker = new Matrix3().fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_X);
    worker.applyPost(expected, expected);
    worker.fromAngleAxis(MathUtils.PI, Vector3.UNIT_Z);
    worker.applyPost(expected, expected);
    worker.fromAngleAxis(MathUtils.QUARTER_PI, Vector3.UNIT_Y);
    worker.applyPost(expected, expected);

    // test how close it came out
    assertTrue(rotated.distance(expected) <= MathUtils.EPSILON);

    // Try a new way with new angles...
    final Matrix3 mat3B = new Matrix3().fromAngles(MathUtils.QUARTER_PI, MathUtils.PI, MathUtils.HALF_PI);
    rotated.set(1, 1, 1);
    mat3B.applyPost(rotated, rotated);

    // expected
    expected.set(1, 1, 1);
    worker.setIdentity();
    // put together matrix, then apply to vector, so YZX
    worker.applyRotationY(MathUtils.PI);
    worker.applyRotationZ(MathUtils.HALF_PI);
    worker.applyRotationX(MathUtils.QUARTER_PI);
    worker.applyPost(expected, expected);

    // test how close it came out
    assertTrue(rotated.distance(expected) <= MathUtils.EPSILON);

    // test axis rotation methods against general purpose
    // X AXIS
    expected.set(1, 1, 1);
    rotated.set(1, 1, 1);
    worker.setIdentity().applyRotationX(MathUtils.QUARTER_PI).applyPost(expected, expected);
    worker.setIdentity().applyRotation(MathUtils.QUARTER_PI, 1, 0, 0).applyPost(rotated, rotated);
    assertTrue(rotated.distance(expected) <= MathUtils.EPSILON);

    // Y AXIS
    expected.set(1, 1, 1);
    rotated.set(1, 1, 1);
    worker.setIdentity().applyRotationY(MathUtils.QUARTER_PI).applyPost(expected, expected);
    worker.setIdentity().applyRotation(MathUtils.QUARTER_PI, 0, 1, 0).applyPost(rotated, rotated);
    assertTrue(rotated.distance(expected) <= MathUtils.EPSILON);

    // Z AXIS
    expected.set(1, 1, 1);
    rotated.set(1, 1, 1);
    worker.setIdentity().applyRotationZ(MathUtils.QUARTER_PI).applyPost(expected, expected);
    worker.setIdentity().applyRotation(MathUtils.QUARTER_PI, 0, 0, 1).applyPost(rotated, rotated);
    assertTrue(rotated.distance(expected) <= MathUtils.EPSILON);

    // test toAngles - not necessarily the same values as fromAngles, but should be same resulting
    // Matrix.
    mat3A.fromAngles(MathUtils.HALF_PI, MathUtils.QUARTER_PI, MathUtils.PI);
    final double[] angles = mat3A.toAngles(null);
    worker.fromAngles(angles[0], angles[1], angles[2]);
    assertEquals(mat3A, worker);

    mat3A.fromAngles(MathUtils.HALF_PI, MathUtils.QUARTER_PI, MathUtils.HALF_PI);
    mat3A.toAngles(angles);
    worker.fromAngles(angles[0], angles[1], angles[2]);
    assertEquals(mat3A, worker);

    mat3A.fromAngles(MathUtils.HALF_PI, MathUtils.QUARTER_PI, -MathUtils.HALF_PI);
    mat3A.toAngles(angles);
    worker.fromAngles(angles[0], angles[1], angles[2]);
    assertEquals(mat3A, worker);
  }

  @Test
  public void testMultiplyDiagonal() {
    final Matrix3 mat3A = new Matrix3();
    Matrix3 result = mat3A.multiplyDiagonalPost(new Vector3(2, 4, 6), null);
    assertEquals(new Matrix3( //
        2, 0, 0, //
        0, 4, 0, //
        0, 0, 6), result);
    mat3A.multiplyDiagonalPre(new Vector3(-2, -4, -6), result);
    assertEquals(new Matrix3( //
        -2, 0, 0, //
        0, -4, 0, //
        0, 0, -6), result);

    final double a = MathUtils.HALF_PI;
    mat3A.applyRotationY(a);
    mat3A.multiplyDiagonalPost(new Vector3(2, 4, 6), result);
    assertEquals(new Matrix3( //
        2 * Math.cos(a), 4 * 0, 6 * Math.sin(a), //
        2 * 0, 4 * 1, 6 * 0, //
        2 * -Math.sin(a), 4 * 0, 6 * Math.cos(a)), result);
    result = mat3A.multiplyDiagonalPre(new Vector3(-2, -4, -6), null);
    assertEquals(new Matrix3( //
        -2 * Math.cos(a), -2 * 0, -2 * Math.sin(a), //
        -4 * 0, -4 * 1, -4 * 0, //
        -6 * -Math.sin(a), -6 * 0, -6 * Math.cos(a)), result);
  }

  @Test
  public void testMultiply() {
    final Matrix3 mat3A = new Matrix3( //
        0.01, 0.1, 0.2, //
        1.0, 1.1, 1.2, //
        2.0, 2.1, 2.2);
    mat3A.multiplyLocal(2);
    assertEquals(new Matrix3( //
        0.02, 0.2, 0.4, //
        2.0, 2.2, 2.4, //
        4.0, 4.2, 4.4), mat3A);

    final Matrix3 mat3B = new Matrix3( //
        0.5, 1, 2, //
        4, 5, 6, //
        8, 9, 10);
    final Matrix3 result = mat3A.multiply(mat3B, null);
    assertTrue(0.02 * 0.5 + 0.2 * 4 + 0.4 * 8 == result.getM00());
    assertTrue(0.02 * 1 + 0.2 * 5 + 0.4 * 9 == result.getM01());
    assertTrue(0.02 * 2 + 0.2 * 6 + 0.4 * 10 == result.getM02());
    assertTrue(2.0 * 0.5 + 2.2 * 4 + 2.4 * 8 == result.getM10());
    assertTrue(2.0 * 1 + 2.2 * 5 + 2.4 * 9 == result.getM11());
    assertTrue(2.0 * 2 + 2.2 * 6 + 2.4 * 10 == result.getM12());
    assertTrue(4.0 * 0.5 + 4.2 * 4 + 4.4 * 8 == result.getM20());
    assertTrue(4.0 * 1 + 4.2 * 5 + 4.4 * 9 == result.getM21());
    assertTrue(4.0 * 2 + 4.2 * 6 + 4.4 * 10 == result.getM22());
    mat3A.multiplyLocal(mat3B);
    assertTrue(0.02 * 0.5 + 0.2 * 4 + 0.4 * 8 == mat3A.getM00());
    assertTrue(0.02 * 1 + 0.2 * 5 + 0.4 * 9 == mat3A.getM01());
    assertTrue(0.02 * 2 + 0.2 * 6 + 0.4 * 10 == mat3A.getM02());
    assertTrue(2.0 * 0.5 + 2.2 * 4 + 2.4 * 8 == mat3A.getM10());
    assertTrue(2.0 * 1 + 2.2 * 5 + 2.4 * 9 == mat3A.getM11());
    assertTrue(2.0 * 2 + 2.2 * 6 + 2.4 * 10 == mat3A.getM12());
    assertTrue(4.0 * 0.5 + 4.2 * 4 + 4.4 * 8 == mat3A.getM20());
    assertTrue(4.0 * 1 + 4.2 * 5 + 4.4 * 9 == mat3A.getM21());
    assertTrue(4.0 * 2 + 4.2 * 6 + 4.4 * 10 == mat3A.getM22());
  }

  @Test
  public void testAddSubtract() {
    final Matrix3 mat3A = new Matrix3( //
        0.0, 0.1, 0.2, //
        1.0, 1.1, 1.2, //
        2.0, 2.1, 2.2);

    final Matrix3 result1 = mat3A.add(new Matrix3(//
        1, 2, 3, //
        5, 6, 7, //
        9, 10, 11), null);
    assertEquals(new Matrix3( //
        1.0, 2.1, 3.2, //
        6.0, 7.1, 8.2, //
        11.0, 12.1, 13.2), result1);

    final Matrix3 result2 = result1.subtract(new Matrix3(//
        1, 2, 3, //
        5, 6, 7, //
        9, 10, 11), null);
    assertEquals(mat3A, result2);
    result2.addLocal(Matrix3.IDENTITY);
    assertEquals(new Matrix3( //
        1.0, 0.1, 0.2, //
        1.0, 2.1, 1.2, //
        2.0, 2.1, 3.2), result2);

    result1.subtractLocal(Matrix3.IDENTITY);
    assertEquals(new Matrix3( //
        0.0, 2.1, 3.2, //
        6.0, 6.1, 8.2, //
        11.0, 12.1, 12.2), result1);
  }

  @Test
  public void testScale() {
    final Matrix3 mat3A = new Matrix3( //
        0.01, 0.1, 0.2, //
        1.0, 1.1, 1.2, //
        2.0, 2.1, 2.2);
    final Matrix3 result = mat3A.scale(new Vector3(-1, 2, 3), null);
    assertEquals(new Matrix3( //
        0.01 * -1, 0.1 * 2, 0.2 * 3, //
        1.0 * -1, 1.1 * 2, 1.2 * 3, //
        2.0 * -1, 2.1 * 2, 2.2 * 3), result);

    result.scaleLocal(new Vector3(-1, 0.5, 1 / 3.));
    assertEquals(mat3A, result);
  }

  @Test
  public void testTranspose() {
    final Matrix3 mat3A = new Matrix3( //
        0.01, 0.1, 0.2, //
        1.0, 1.1, 1.2, //
        2.0, 2.1, 2.2);
    final Matrix3 result = mat3A.transpose(null);
    assertEquals(new Matrix3( //
        0.01, 1.0, 2.0, //
        0.1, 1.1, 2.1, //
        0.2, 1.2, 2.2), result);
    assertEquals(new Matrix3( //
        0.01, 0.1, 0.2, //
        1.0, 1.1, 1.2, //
        2.0, 2.1, 2.2), result.transposeLocal());
    // coverage
    final Matrix3 result2 = result.transposeLocal().transpose(new Matrix3());
    assertEquals(mat3A, result2);
  }

  @Test
  public void testInvert() {
    final Matrix3 mat3A = new Matrix3().applyRotationX(MathUtils.QUARTER_PI);
    final Matrix3 inverted = mat3A.invert(null);
    assertEquals(Matrix3.IDENTITY, mat3A.multiply(inverted, null));
    assertEquals(mat3A, inverted.invertLocal());
  }

  @Test(expected = ArithmeticException.class)
  public void testBadInvert() {
    final Matrix3 mat3A = new Matrix3(0, 0, 0, 0, 0, 0, 0, 0, 0);
    mat3A.invertLocal();
  }

  @Test
  public void testAdjugate() {
    final double //
    a = -3, b = 2, c = -5, //
        d = -1, e = 0, f = -2, //
        g = 3, h = -4, i = 1;

    final Matrix3 mat3A = new Matrix3( //
        a, b, c, //
        d, e, f, //
        g, h, i);

    final Matrix3 testValue = new Matrix3( //
        e * i - h * f, -(b * i - h * c), b * f - e * c, //
        -(d * i - g * f), a * i - g * c, -(a * f - d * c), //
        d * h - g * e, -(a * h - g * b), a * e - d * b);

    assertEquals(testValue, mat3A.adjugate(null));
    assertEquals(testValue, mat3A.adjugateLocal());
  }

  @Test
  public void testDeterminant() {
    {
      final double //
      a = -3, b = 2, c = -5, //
          d = -1, e = 0, f = -2, //
          g = 3, h = -4, i = 1;

      final Matrix3 mat3A = new Matrix3( //
          a, b, c, //
          d, e, f, //
          g, h, i);
      final double determinant = a * e * i + b * f * g + c * d * h - c * e * g - b * d * i - a * f * h;
      assertTrue(determinant == mat3A.determinant());
    }

    {
      final double //
      a = -1, b = 2, c = -3, //
          d = 4, e = -5, f = 6, //
          g = -7, h = 8, i = -9;

      final Matrix3 mat3A = new Matrix3( //
          a, b, c, //
          d, e, f, //
          g, h, i);
      final double determinant = a * e * i + b * f * g + c * d * h - c * e * g - b * d * i - a * f * h;
      assertTrue(determinant == mat3A.determinant());
    }
  }

  @Test
  public void testClone() {
    final Matrix3 mat1 = new Matrix3();
    final Matrix3 mat2 = mat1.clone();
    assertEquals(mat1, mat2);
    assertNotSame(mat1, mat2);
  }

  @Test
  public void testFinite() {
    final Matrix3 mat3 = new Matrix3();
    assertTrue(Matrix3.isFinite(mat3));
    for (int i = 0; i < 9; i++) {
      mat3.setIdentity();
      mat3.setValue(i / 3, i % 3, Double.NaN);
      assertFalse(Matrix3.isFinite(mat3));
      mat3.setIdentity();
      mat3.setValue(i / 3, i % 3, Double.POSITIVE_INFINITY);
      assertFalse(Matrix3.isFinite(mat3));
    }

    mat3.setIdentity();
    assertTrue(Matrix3.isFinite(mat3));

    assertFalse(Matrix3.isFinite(null));

    // couple of equals validity tests
    assertEquals(mat3, mat3);
    assertTrue(mat3.strictEquals(mat3));
    assertFalse(mat3.equals(null));
    assertFalse(mat3.strictEquals(null));
    assertFalse(mat3.strictEquals(new Vector2()));

    // throw in a couple pool accesses for coverage
    final Matrix3 matTemp = Matrix3.fetchTempInstance();
    matTemp.set(mat3);
    assertEquals(mat3, matTemp);
    assertNotSame(mat3, matTemp);
    Matrix3.releaseTempInstance(matTemp);

    // cover more of equals
    mat3.set(0, 1, 2, 3, 4, 5, 6, 7, 8);
    final Matrix3 comp = new Matrix3(-1, -1, -1, -1, -1, -1, -1, -1, -1);
    assertFalse(mat3.equals(comp));
    assertFalse(mat3.strictEquals(comp));
    for (int i = 0; i < 8; i++) {
      comp.setValue(i / 3, i % 3, i);
      assertFalse(mat3.equals(comp));
      assertFalse(mat3.strictEquals(comp));
    }
  }

  @Test
  public void testSimpleHash() {
    // Just a simple sanity check.
    final Matrix3 mat1 = new Matrix3(1, 2, 3, 4, 5, 6, 7, 8, 9);
    final Matrix3 mat2 = new Matrix3(1, 2, 3, 4, 5, 6, 7, 8, 9);
    final Matrix3 mat3 = new Matrix3(1, 2, 3, 4, 5, 6, 7, 8, 0);

    assertTrue(mat1.hashCode() == mat2.hashCode());
    assertTrue(mat1.hashCode() != mat3.hashCode());
  }

  @Test
  public void testOrthonormal() {
    final Matrix3 mat3 = new Matrix3();
    assertTrue(mat3.isOrthonormal());
    // just rotation
    mat3.applyRotationX(MathUtils.QUARTER_PI);
    assertTrue(mat3.isOrthonormal());
    // non-uniform scale
    mat3.setIdentity();
    mat3.scaleLocal(new Vector3(1, 2, 3));
    assertFalse(mat3.isOrthonormal());
    // non-uniform scale + rotation
    mat3.setIdentity();
    mat3.scaleLocal(new Vector3(1, 2, 3));
    mat3.applyRotationX(MathUtils.QUARTER_PI);
    assertFalse(mat3.isOrthonormal());
  }

  @Test
  public void testApplyVector3() {
    final Matrix3 mat3 = new Matrix3().applyRotationX(MathUtils.HALF_PI);
    final Vector3 vec3 = new Vector3(0, 1, 0);
    final Vector3 result = mat3.applyPost(vec3, null);
    assertTrue(Math.abs(new Vector3(0, 0, 1).distance(result)) <= MathUtils.EPSILON);
    vec3.set(0, 1, 1);
    mat3.applyPost(vec3, result);
    assertTrue(Math.abs(new Vector3(0, -1, 1).distance(result)) <= MathUtils.EPSILON);

    vec3.set(0, 1, 1);
    mat3.applyPre(vec3, result);
    assertTrue(Math.abs(new Vector3(0, 1, -1).distance(result)) <= MathUtils.EPSILON);

    vec3.set(1, 1, 1);
    assertTrue(Math.abs(new Vector3(1, 1, -1).distance(mat3.applyPre(vec3, null))) <= MathUtils.EPSILON);
  }

  @Test
  public void testStartEnd() {
    final Matrix3 mat3 = new Matrix3();
    mat3.fromStartEndLocal(Vector3.UNIT_X, Vector3.UNIT_Y); // should be a 90 degree turn around Z
    assertEquals(new Vector3(-1, 1, 1), mat3.applyPost(new Vector3(1, 1, 1), null));

    // coverage
    mat3.fromStartEndLocal(new Vector3(1, 0, 0), new Vector3(1 + Double.MIN_VALUE, 0, 0));
    assertTrue(mat3.applyPost(Vector3.ONE, null).distance(Vector3.ONE) < MathUtils.ZERO_TOLERANCE);
    mat3.fromStartEndLocal(new Vector3(0, 1, 0), new Vector3(0, 1 + Double.MIN_VALUE, 0));
    assertTrue(mat3.applyPost(Vector3.ONE, null).distance(Vector3.ONE) < MathUtils.ZERO_TOLERANCE);
    mat3.fromStartEndLocal(new Vector3(0, 0, 1), new Vector3(0, 0, 1 + Double.MIN_VALUE));
    assertTrue(mat3.applyPost(Vector3.ONE, null).distance(Vector3.ONE) < MathUtils.ZERO_TOLERANCE);
  }

  @Test
  public void testLookAt() {
    final Vector3 direction = new Vector3(-1, 0, 0);
    final Matrix3 mat3 = new Matrix3().lookAt(direction, Vector3.UNIT_Y);
    assertEquals(direction, mat3.applyPost(Vector3.UNIT_Z, null));

    direction.set(1, 1, 1).normalizeLocal();
    mat3.lookAt(direction, Vector3.UNIT_Y);
    assertEquals(direction, mat3.applyPost(Vector3.UNIT_Z, null));

    direction.set(-1, 2, -1).normalizeLocal();
    mat3.lookAt(direction, Vector3.UNIT_Y);
    assertEquals(direction, mat3.applyPost(Vector3.UNIT_Z, new Vector3()));
  }
}
