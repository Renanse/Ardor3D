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

import com.ardor3d.math.util.MathUtils;

public class TestMatrix4 {

  @Test
  public void testGetSet() {
    final Matrix4 mat4A = new Matrix4();
    assertEquals(Matrix4.IDENTITY, mat4A);

    mat4A.setM00(0.0);
    mat4A.setM01(0.1);
    mat4A.setM02(0.2);
    mat4A.setM03(0.3);
    mat4A.setM10(1.0);
    mat4A.setM11(1.1);
    mat4A.setM12(1.2);
    mat4A.setM13(1.3);
    mat4A.setM20(2.0);
    mat4A.setM21(2.1);
    mat4A.setM22(2.2);
    mat4A.setM23(2.3);
    mat4A.setM30(3.0);
    mat4A.setM31(3.1);
    mat4A.setM32(3.2);
    mat4A.setM33(3.3);

    assertTrue(0.0 == mat4A.getM00());
    assertTrue(0.1 == mat4A.getM01());
    assertTrue(0.2 == mat4A.getM02());
    assertTrue(0.3 == mat4A.getM03());
    assertTrue(1.0 == mat4A.getM10());
    assertTrue(1.1 == mat4A.getM11());
    assertTrue(1.2 == mat4A.getM12());
    assertTrue(1.3 == mat4A.getM13());
    assertTrue(2.0 == mat4A.getM20());
    assertTrue(2.1 == mat4A.getM21());
    assertTrue(2.2 == mat4A.getM22());
    assertTrue(2.3 == mat4A.getM23());
    assertTrue(3.0 == mat4A.getM30());
    assertTrue(3.1 == mat4A.getM31());
    assertTrue(3.2 == mat4A.getM32());
    assertTrue(3.3 == mat4A.getM33());

    final Matrix4 mat4B = new Matrix4(mat4A);
    assertTrue(0.0 == mat4B.getM00());
    assertTrue(0.1 == mat4B.getM01());
    assertTrue(0.2 == mat4B.getM02());
    assertTrue(0.3 == mat4B.getM03());
    assertTrue(1.0 == mat4B.getM10());
    assertTrue(1.1 == mat4B.getM11());
    assertTrue(1.2 == mat4B.getM12());
    assertTrue(1.3 == mat4B.getM13());
    assertTrue(2.0 == mat4B.getM20());
    assertTrue(2.1 == mat4B.getM21());
    assertTrue(2.2 == mat4B.getM22());
    assertTrue(2.3 == mat4B.getM23());
    assertTrue(3.0 == mat4B.getM30());
    assertTrue(3.1 == mat4B.getM31());
    assertTrue(3.2 == mat4B.getM32());
    assertTrue(3.3 == mat4B.getM33());

    final Matrix4 mat4C = new Matrix4(0.0, 0.1, 0.2, 0.3, 1.0, 1.1, 1.2, 1.3, 2.0, 2.1, 2.2, 2.3, 3.0, 3.1, 3.2, 3.3);
    assertTrue(0.0 == mat4C.getM00());
    assertTrue(0.1 == mat4C.getM01());
    assertTrue(0.2 == mat4C.getM02());
    assertTrue(0.3 == mat4C.getM03());
    assertTrue(1.0 == mat4C.getM10());
    assertTrue(1.1 == mat4C.getM11());
    assertTrue(1.2 == mat4C.getM12());
    assertTrue(1.3 == mat4C.getM13());
    assertTrue(2.0 == mat4C.getM20());
    assertTrue(2.1 == mat4C.getM21());
    assertTrue(2.2 == mat4C.getM22());
    assertTrue(2.3 == mat4C.getM23());
    assertTrue(3.0 == mat4C.getM30());
    assertTrue(3.1 == mat4C.getM31());
    assertTrue(3.2 == mat4C.getM32());
    assertTrue(3.3 == mat4C.getM33());

    mat4C.setIdentity();
    assertTrue(mat4C.isIdentity());

    for (int x = 0; x < 4; x++) {
      for (int y = 0; y < 4; y++) {
        final double value = (10 * x + y) / 10.;
        mat4C.setValue(x, y, value);
        assertTrue(value == mat4C.getValue(x, y));
      }
    }

    mat4C.setIdentity();
    mat4C.set(0.0, 0.1, 0.2, 0.3, 2.0, 2.1, 2.2, 2.3, 4.0, 4.1, 4.2, 4.3, 6.0, 6.1, 6.2, 6.3);
    for (int x = 0; x < 4; x++) {
      for (int y = 0; y < 4; y++) {
        assertTrue((20 * x + y) / 10.f == mat4C.getValuef(x, y));
      }
    }

    final Matrix4 store = new Matrix4(mat4C);
    // catch a few expected exceptions
    try {
      mat4C.getValue(-1, 0);
      fail("getValue(-1, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4C.getValue(0, 4);
      fail("getValue(0, 4) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4C.getValue(1, -1);
      fail("getValue(1, -1) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4C.getValue(2, 4);
      fail("getValue(2, 4) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4C.getValue(3, -1);
      fail("getValue(3, -1) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4C.getValue(4, 0);
      fail("getValue(4, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}

    try {
      mat4C.setValue(-1, 0, 0);
      fail("setValue(-1, 0, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4C.setValue(0, -1, 0);
      fail("setValue(0, -1, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4C.setValue(1, 4, 0);
      fail("setValue(1, 4, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4C.setValue(2, -1, 0);
      fail("setValue(2, -1, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4C.setValue(3, 4, 0);
      fail("setValue(3, 4, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4C.setValue(4, 0, 0);
      fail("setValue(4, 0, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    // above exceptions shouldn't have altered mat4C
    assertEquals(store, mat4C);
  }

  @Test
  public void testColumns() {
    final Matrix4 mat4A = new Matrix4();
    mat4A.setColumn(0, new Vector4(0, 4, 8, 12));
    mat4A.setColumn(1, new Vector4(1, 5, 9, 13));
    mat4A.setColumn(2, new Vector4(2, 6, 10, 14));
    mat4A.setColumn(3, new Vector4(3, 7, 11, 15));
    assertEquals(new Vector4(0, 4, 8, 12), mat4A.getColumn(0, new Vector4()));
    assertEquals(new Vector4(1, 5, 9, 13), mat4A.getColumn(1, null));
    assertEquals(new Vector4(2, 6, 10, 14), mat4A.getColumn(2, null));
    assertEquals(new Vector4(3, 7, 11, 15), mat4A.getColumn(3, null));
    try {
      mat4A.getColumn(-1, null);
      fail("getColumn(-1, null) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4A.getColumn(4, null);
      fail("getColumn(4, null) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4A.setColumn(-1, new Vector4());
      fail("setColumn(-1, double[]) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4A.setColumn(4, new Vector4());
      fail("setColumn(4, double[]) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
  }

  @Test
  public void testRows() {
    final Matrix4 mat4A = new Matrix4();
    mat4A.setRow(0, new Vector4(0, 1, 2, 3));
    mat4A.setRow(1, new Vector4(4, 5, 6, 7));
    mat4A.setRow(2, new Vector4(8, 9, 10, 11));
    mat4A.setRow(3, new Vector4(12, 13, 14, 15));
    assertEquals(new Vector4(0, 1, 2, 3), mat4A.getRow(0, new Vector4()));
    assertEquals(new Vector4(4, 5, 6, 7), mat4A.getRow(1, null));
    assertEquals(new Vector4(8, 9, 10, 11), mat4A.getRow(2, null));
    assertEquals(new Vector4(12, 13, 14, 15), mat4A.getRow(3, null));
    try {
      mat4A.getRow(-1, null);
      fail("getRow(-1, null) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4A.getRow(4, null);
      fail("getRow(4, null) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4A.setRow(-1, new Vector4());
      fail("setRow(-1, double[]) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      mat4A.setRow(4, new Vector4());
      fail("setRow(4, double[]) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
  }

  @Test
  public void testSetRotation() {
    final Matrix4 mat4A = new Matrix4(0.0, 0.1, 0.2, 0.3, 1.0, 1.1, 1.2, 1.3, 2.0, 2.1, 2.2, 2.3, 3.0, 3.1, 3.2, 3.3);
    mat4A.set(Matrix3.IDENTITY);
    assertTrue(1.0 == mat4A.getM00());
    assertTrue(0.0 == mat4A.getM01());
    assertTrue(0.0 == mat4A.getM02());
    assertTrue(0.3 == mat4A.getM03());
    assertTrue(0.0 == mat4A.getM10());
    assertTrue(1.0 == mat4A.getM11());
    assertTrue(0.0 == mat4A.getM12());
    assertTrue(1.3 == mat4A.getM13());
    assertTrue(0.0 == mat4A.getM20());
    assertTrue(0.0 == mat4A.getM21());
    assertTrue(1.0 == mat4A.getM22());
    assertTrue(2.3 == mat4A.getM23());
    assertTrue(3.0 == mat4A.getM30());
    assertTrue(3.1 == mat4A.getM31());
    assertTrue(3.2 == mat4A.getM32());
    assertTrue(3.3 == mat4A.getM33());

    mat4A.setIdentity();
    // rotate identity 90 degrees around Y
    final double a = MathUtils.HALF_PI;
    final Quaternion quaternion = new Quaternion();
    quaternion.fromAngleAxis(a, Vector3.UNIT_Y);
    mat4A.set(quaternion);

    assertEquals(new Matrix4( //
        Math.cos(a), 0, Math.sin(a), 0, //
        0, 1, 0, 0, //
        -Math.sin(a), 0, Math.cos(a), 0, //
        0, 0, 0, 1), mat4A);
  }

  @Test
  public void testFromBuffer() {
    final FloatBuffer fb = FloatBuffer.allocate(16);
    fb.put(new float[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
    fb.flip();
    // row major
    final Matrix4 mat4A = new Matrix4().fromFloatBuffer(fb);
    assertTrue(0 == mat4A.getM00());
    assertTrue(1 == mat4A.getM01());
    assertTrue(2 == mat4A.getM02());
    assertTrue(3 == mat4A.getM03());
    assertTrue(4 == mat4A.getM10());
    assertTrue(5 == mat4A.getM11());
    assertTrue(6 == mat4A.getM12());
    assertTrue(7 == mat4A.getM13());
    assertTrue(8 == mat4A.getM20());
    assertTrue(9 == mat4A.getM21());
    assertTrue(10 == mat4A.getM22());
    assertTrue(11 == mat4A.getM23());
    assertTrue(12 == mat4A.getM30());
    assertTrue(13 == mat4A.getM31());
    assertTrue(14 == mat4A.getM32());
    assertTrue(15 == mat4A.getM33());

    // column major
    fb.rewind();
    mat4A.setIdentity();
    mat4A.fromFloatBuffer(fb, false);
    assertTrue(0 == mat4A.getM00());
    assertTrue(4 == mat4A.getM01());
    assertTrue(8 == mat4A.getM02());
    assertTrue(12 == mat4A.getM03());
    assertTrue(1 == mat4A.getM10());
    assertTrue(5 == mat4A.getM11());
    assertTrue(9 == mat4A.getM12());
    assertTrue(13 == mat4A.getM13());
    assertTrue(2 == mat4A.getM20());
    assertTrue(6 == mat4A.getM21());
    assertTrue(10 == mat4A.getM22());
    assertTrue(14 == mat4A.getM23());
    assertTrue(3 == mat4A.getM30());
    assertTrue(7 == mat4A.getM31());
    assertTrue(11 == mat4A.getM32());
    assertTrue(15 == mat4A.getM33());

    final DoubleBuffer db = DoubleBuffer.allocate(16);
    db.put(new double[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
    db.flip();
    // row major
    mat4A.setIdentity();
    mat4A.fromDoubleBuffer(db);
    assertTrue(0 == mat4A.getM00());
    assertTrue(1 == mat4A.getM01());
    assertTrue(2 == mat4A.getM02());
    assertTrue(3 == mat4A.getM03());
    assertTrue(4 == mat4A.getM10());
    assertTrue(5 == mat4A.getM11());
    assertTrue(6 == mat4A.getM12());
    assertTrue(7 == mat4A.getM13());
    assertTrue(8 == mat4A.getM20());
    assertTrue(9 == mat4A.getM21());
    assertTrue(10 == mat4A.getM22());
    assertTrue(11 == mat4A.getM23());
    assertTrue(12 == mat4A.getM30());
    assertTrue(13 == mat4A.getM31());
    assertTrue(14 == mat4A.getM32());
    assertTrue(15 == mat4A.getM33());

    // column major
    db.rewind();
    mat4A.setIdentity();
    mat4A.fromDoubleBuffer(db, false);
    assertTrue(0 == mat4A.getM00());
    assertTrue(4 == mat4A.getM01());
    assertTrue(8 == mat4A.getM02());
    assertTrue(12 == mat4A.getM03());
    assertTrue(1 == mat4A.getM10());
    assertTrue(5 == mat4A.getM11());
    assertTrue(9 == mat4A.getM12());
    assertTrue(13 == mat4A.getM13());
    assertTrue(2 == mat4A.getM20());
    assertTrue(6 == mat4A.getM21());
    assertTrue(10 == mat4A.getM22());
    assertTrue(14 == mat4A.getM23());
    assertTrue(3 == mat4A.getM30());
    assertTrue(7 == mat4A.getM31());
    assertTrue(11 == mat4A.getM32());
    assertTrue(15 == mat4A.getM33());
  }

  @Test
  public void testToBuffer() {
    final double[] values = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    final double[] colmajor = {0, 4, 8, 12, 1, 5, 9, 13, 2, 6, 10, 14, 3, 7, 11, 15};

    final Matrix4 mat4A = new Matrix4().fromArray(values);

    // row major
    final FloatBuffer fb = mat4A.toFloatBuffer(FloatBuffer.allocate(16));
    fb.flip();
    for (int i = 0; i < 16; i++) {
      assertTrue(values[i] == fb.get());
    }

    // column major
    fb.rewind();
    mat4A.toFloatBuffer(fb, false);
    fb.flip();
    for (int i = 0; i < 16; i++) {
      assertTrue(colmajor[i] == fb.get());
    }

    // row major
    final DoubleBuffer db = mat4A.toDoubleBuffer(DoubleBuffer.allocate(16));
    db.flip();
    for (int i = 0; i < 16; i++) {
      assertTrue(values[i] == db.get());
    }

    // column major
    db.rewind();
    mat4A.toDoubleBuffer(db, false);
    db.flip();
    for (int i = 0; i < 16; i++) {
      assertTrue(colmajor[i] == db.get());
    }
  }

  @Test
  public void testFromArray() {
    final double[] values = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    final Matrix4 mat4A = new Matrix4();

    // row major
    mat4A.setIdentity();
    mat4A.fromArray(values);
    assertTrue(0 == mat4A.getM00());
    assertTrue(1 == mat4A.getM01());
    assertTrue(2 == mat4A.getM02());
    assertTrue(3 == mat4A.getM03());
    assertTrue(4 == mat4A.getM10());
    assertTrue(5 == mat4A.getM11());
    assertTrue(6 == mat4A.getM12());
    assertTrue(7 == mat4A.getM13());
    assertTrue(8 == mat4A.getM20());
    assertTrue(9 == mat4A.getM21());
    assertTrue(10 == mat4A.getM22());
    assertTrue(11 == mat4A.getM23());
    assertTrue(12 == mat4A.getM30());
    assertTrue(13 == mat4A.getM31());
    assertTrue(14 == mat4A.getM32());
    assertTrue(15 == mat4A.getM33());

    // column major
    mat4A.setIdentity();
    mat4A.fromArray(values, false);
    assertTrue(0 == mat4A.getM00());
    assertTrue(4 == mat4A.getM01());
    assertTrue(8 == mat4A.getM02());
    assertTrue(12 == mat4A.getM03());
    assertTrue(1 == mat4A.getM10());
    assertTrue(5 == mat4A.getM11());
    assertTrue(9 == mat4A.getM12());
    assertTrue(13 == mat4A.getM13());
    assertTrue(2 == mat4A.getM20());
    assertTrue(6 == mat4A.getM21());
    assertTrue(10 == mat4A.getM22());
    assertTrue(14 == mat4A.getM23());
    assertTrue(3 == mat4A.getM30());
    assertTrue(7 == mat4A.getM31());
    assertTrue(11 == mat4A.getM32());
    assertTrue(15 == mat4A.getM33());
  }

  @Test
  public void testToArray() {
    final double[] values = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    final Matrix4 mat4A = new Matrix4().fromArray(values);

    // row major
    final double[] dbls1 = mat4A.toArray(null);
    for (int i = 0; i < 16; i++) {
      assertTrue(values[i] == dbls1[i]);
    }

    // column major
    final double[] colmajor = {0, 4, 8, 12, 1, 5, 9, 13, 2, 6, 10, 14, 3, 7, 11, 15};
    mat4A.toArray(dbls1, false);
    for (int i = 0; i < 16; i++) {
      assertTrue(colmajor[i] == dbls1[i]);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadArray() {
    final Matrix4 mat4A = new Matrix4();
    mat4A.toArray(new double[9]);
  }

  @Test
  public void testAngleAxis() {
    final Matrix4 mat4A = new Matrix4();
    // rotate identity 90 degrees around X
    final double angle = MathUtils.HALF_PI;
    mat4A.fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_X);
    assertEquals(new Matrix4( //
        1, 0, 0, 0, //
        0, Math.cos(angle), -Math.sin(angle), 0, //
        0, Math.sin(angle), Math.cos(angle), 0, //
        0, 0, 0, 1), mat4A);
  }

  @Test
  public void testRotations() {
    final Vector4 rotated = new Vector4();
    final Vector4 expected = new Vector4();
    final Matrix4 worker = new Matrix4();

    // test axis rotation methods against general purpose
    // X AXIS
    expected.set(1, 1, 1, 1);
    rotated.set(1, 1, 1, 1);
    worker.setIdentity().applyRotationX(MathUtils.QUARTER_PI).applyPost(expected, expected);
    worker.setIdentity().applyRotation(MathUtils.QUARTER_PI, 1, 0, 0).applyPost(rotated, rotated);
    assertTrue(rotated.distance(expected) <= MathUtils.EPSILON);

    // Y AXIS
    expected.set(1, 1, 1, 1);
    rotated.set(1, 1, 1, 1);
    worker.setIdentity().applyRotationY(MathUtils.QUARTER_PI).applyPost(expected, expected);
    worker.setIdentity().applyRotation(MathUtils.QUARTER_PI, 0, 1, 0).applyPost(rotated, rotated);
    assertTrue(rotated.distance(expected) <= MathUtils.EPSILON);

    // Z AXIS
    expected.set(1, 1, 1, 1);
    rotated.set(1, 1, 1, 1);
    worker.setIdentity().applyRotationZ(MathUtils.QUARTER_PI).applyPost(expected, expected);
    worker.setIdentity().applyRotation(MathUtils.QUARTER_PI, 0, 0, 1).applyPost(rotated, rotated);
    assertTrue(rotated.distance(expected) <= MathUtils.EPSILON);
  }

  @Test
  public void testTranslation() {
    final Matrix4 src = new Matrix4();
    src.applyRotation(MathUtils.QUARTER_PI, 1, 0, 0);

    final Matrix4 trans = new Matrix4();
    trans.setColumn(3, new Vector4(1, 2, 3, 1));
    final Matrix4 transThenRotate = trans.multiply(src, null);
    final Matrix4 rotateThenTrans = src.multiply(trans, null);

    final Matrix4 pre1 = new Matrix4(src).applyTranslationPre(1, 2, 3);
    final Matrix4 post1 = new Matrix4(src).applyTranslationPost(1, 2, 3);

    assertEquals(transThenRotate, pre1);
    assertEquals(rotateThenTrans, post1);
  }

  @Test
  public void testMultiplyDiagonal() {
    final Matrix4 mat4A = new Matrix4();
    Matrix4 result = mat4A.multiplyDiagonalPost(new Vector4(2, 4, 6, 8), null);
    assertEquals(new Matrix4( //
        2, 0, 0, 0, //
        0, 4, 0, 0, //
        0, 0, 6, 0, //
        0, 0, 0, 8), result);
    mat4A.multiplyDiagonalPre(new Vector4(-2, -4, -6, -8), result);
    assertEquals(new Matrix4( //
        -2, 0, 0, 0, //
        0, -4, 0, 0, //
        0, 0, -6, 0, //
        0, 0, 0, -8), result);

    final double a = MathUtils.HALF_PI;
    mat4A.applyRotationY(a);
    mat4A.multiplyDiagonalPost(new Vector4(2, 4, 6, 8), result);
    assertEquals(new Matrix4( //
        2 * Math.cos(a), 4 * 0, 6 * Math.sin(a), 8 * 0, //
        2 * 0, 4 * 1, 6 * 0, 8 * 0, //
        2 * -Math.sin(a), 4 * 0, 6 * Math.cos(a), 8 * 0, //
        2 * 0, 4 * 0, 6 * 0, 8 * 1), result);
    result = mat4A.multiplyDiagonalPre(new Vector4(-2, -4, -6, -8), null);
    assertEquals(new Matrix4( //
        -2 * Math.cos(a), -2 * 0, -2 * Math.sin(a), -2 * 0, //
        -4 * 0, -4 * 1, -4 * 0, -4 * 0, //
        -6 * -Math.sin(a), -6 * 0, -6 * Math.cos(a), -6 * 0, //
        -8 * 0, -8 * 0, -8 * 0, -8 * 1), result);
  }

  @Test
  public void testMultiply() {
    final Matrix4 mat4A = new Matrix4( //
        0.01, 0.1, 0.2, 0.3, //
        1.0, 1.1, 1.2, 1.3, //
        2.0, 2.1, 2.2, 2.3, //
        3.0, 3.1, 3.2, 3.3);
    mat4A.multiplyLocal(2);
    assertEquals(new Matrix4( //
        0.02, 0.2, 0.4, 0.6, //
        2.0, 2.2, 2.4, 2.6, //
        4.0, 4.2, 4.4, 4.6, //
        6.0, 6.2, 6.4, 6.6), mat4A);

    final Matrix4 mat4B = new Matrix4( //
        0.5, 1, 2, 3, //
        4, 5, 6, 7, //
        8, 9, 10, 11, //
        12, 13, 14, 15);
    final Matrix4 result = mat4A.multiply(mat4B, null);
    assertTrue(0.02 * 0.5 + 0.2 * 4 + 0.4 * 8 + 0.6 * 12 == result.getM00());
    assertTrue(0.02 * 1 + 0.2 * 5 + 0.4 * 9 + 0.6 * 13 == result.getM01());
    assertTrue(0.02 * 2 + 0.2 * 6 + 0.4 * 10 + 0.6 * 14 == result.getM02());
    assertTrue(0.02 * 3 + 0.2 * 7 + 0.4 * 11 + 0.6 * 15 == result.getM03());
    assertTrue(2.0 * 0.5 + 2.2 * 4 + 2.4 * 8 + 2.6 * 12 == result.getM10());
    assertTrue(2.0 * 1 + 2.2 * 5 + 2.4 * 9 + 2.6 * 13 == result.getM11());
    assertTrue(2.0 * 2 + 2.2 * 6 + 2.4 * 10 + 2.6 * 14 == result.getM12());
    assertTrue(2.0 * 3 + 2.2 * 7 + 2.4 * 11 + 2.6 * 15 == result.getM13());
    assertTrue(4.0 * 0.5 + 4.2 * 4 + 4.4 * 8 + 4.6 * 12 == result.getM20());
    assertTrue(4.0 * 1 + 4.2 * 5 + 4.4 * 9 + 4.6 * 13 == result.getM21());
    assertTrue(4.0 * 2 + 4.2 * 6 + 4.4 * 10 + 4.6 * 14 == result.getM22());
    assertTrue(4.0 * 3 + 4.2 * 7 + 4.4 * 11 + 4.6 * 15 == result.getM23());
    assertTrue(6.0 * 0.5 + 6.2 * 4 + 6.4 * 8 + 6.6 * 12 == result.getM30());
    assertTrue(6.0 * 1 + 6.2 * 5 + 6.4 * 9 + 6.6 * 13 == result.getM31());
    assertTrue(6.0 * 2 + 6.2 * 6 + 6.4 * 10 + 6.6 * 14 == result.getM32());
    assertTrue(6.0 * 3 + 6.2 * 7 + 6.4 * 11 + 6.6 * 15 == result.getM33());
    mat4A.multiplyLocal(mat4B);
    assertTrue(0.02 * 0.5 + 0.2 * 4 + 0.4 * 8 + 0.6 * 12 == mat4A.getM00());
    assertTrue(0.02 * 1 + 0.2 * 5 + 0.4 * 9 + 0.6 * 13 == mat4A.getM01());
    assertTrue(0.02 * 2 + 0.2 * 6 + 0.4 * 10 + 0.6 * 14 == mat4A.getM02());
    assertTrue(0.02 * 3 + 0.2 * 7 + 0.4 * 11 + 0.6 * 15 == mat4A.getM03());
    assertTrue(2.0 * 0.5 + 2.2 * 4 + 2.4 * 8 + 2.6 * 12 == mat4A.getM10());
    assertTrue(2.0 * 1 + 2.2 * 5 + 2.4 * 9 + 2.6 * 13 == mat4A.getM11());
    assertTrue(2.0 * 2 + 2.2 * 6 + 2.4 * 10 + 2.6 * 14 == mat4A.getM12());
    assertTrue(2.0 * 3 + 2.2 * 7 + 2.4 * 11 + 2.6 * 15 == mat4A.getM13());
    assertTrue(4.0 * 0.5 + 4.2 * 4 + 4.4 * 8 + 4.6 * 12 == mat4A.getM20());
    assertTrue(4.0 * 1 + 4.2 * 5 + 4.4 * 9 + 4.6 * 13 == mat4A.getM21());
    assertTrue(4.0 * 2 + 4.2 * 6 + 4.4 * 10 + 4.6 * 14 == mat4A.getM22());
    assertTrue(4.0 * 3 + 4.2 * 7 + 4.4 * 11 + 4.6 * 15 == mat4A.getM23());
    assertTrue(6.0 * 0.5 + 6.2 * 4 + 6.4 * 8 + 6.6 * 12 == mat4A.getM30());
    assertTrue(6.0 * 1 + 6.2 * 5 + 6.4 * 9 + 6.6 * 13 == mat4A.getM31());
    assertTrue(6.0 * 2 + 6.2 * 6 + 6.4 * 10 + 6.6 * 14 == mat4A.getM32());
    assertTrue(6.0 * 3 + 6.2 * 7 + 6.4 * 11 + 6.6 * 15 == mat4A.getM33());
  }

  @Test
  public void testAddSubtract() {
    final Matrix4 mat4A = new Matrix4( //
        0.0, 0.1, 0.2, 0.3, //
        1.0, 1.1, 1.2, 1.3, //
        2.0, 2.1, 2.2, 2.3, //
        3.0, 3.1, 3.2, 3.3);

    final Matrix4 result1 = mat4A.add(new Matrix4(//
        1, 2, 3, 4, //
        5, 6, 7, 8, //
        9, 10, 11, 12, //
        13, 14, 15, 16), null);
    assertEquals(new Matrix4( //
        1.0, 2.1, 3.2, 4.3, //
        6.0, 7.1, 8.2, 9.3, //
        11.0, 12.1, 13.2, 14.3, //
        16.0, 17.1, 18.2, 19.3), result1);

    final Matrix4 result2 = result1.subtract(new Matrix4(//
        1, 2, 3, 4, //
        5, 6, 7, 8, //
        9, 10, 11, 12, //
        13, 14, 15, 16), null);
    assertEquals(mat4A, result2);
    result2.addLocal(Matrix4.IDENTITY);
    assertEquals(new Matrix4( //
        1.0, 0.1, 0.2, 0.3, //
        1.0, 2.1, 1.2, 1.3, //
        2.0, 2.1, 3.2, 2.3, //
        3.0, 3.1, 3.2, 4.3), result2);

    result1.subtractLocal(Matrix4.IDENTITY);
    assertEquals(new Matrix4( //
        0.0, 2.1, 3.2, 4.3, //
        6.0, 6.1, 8.2, 9.3, //
        11.0, 12.1, 12.2, 14.3, //
        16.0, 17.1, 18.2, 18.3), result1);
  }

  @Test
  public void testScale() {
    final Matrix4 mat4A = new Matrix4( //
        0.01, 0.1, 0.2, 0.3, //
        1.0, 1.1, 1.2, 1.3, //
        2.0, 2.1, 2.2, 2.3, //
        3.0, 3.1, 3.2, 3.3);
    final Matrix4 result = mat4A.scale(new Vector4(-1, 2, 3, 4), null);
    assertEquals(new Matrix4( //
        0.01 * -1, 0.1 * 2, 0.2 * 3, 0.3 * 4, //
        1.0 * -1, 1.1 * 2, 1.2 * 3, 1.3 * 4, //
        2.0 * -1, 2.1 * 2, 2.2 * 3, 2.3 * 4, //
        3.0 * -1, 3.1 * 2, 3.2 * 3, 3.3 * 4), result);

    result.scaleLocal(new Vector4(-1, 0.5, 1 / 3., .25));
    assertEquals(mat4A, result);
  }

  @Test
  public void testTranspose() {
    final Matrix4 mat4A = new Matrix4( //
        0.01, 0.1, 0.2, 0.3, //
        1.0, 1.1, 1.2, 1.3, //
        2.0, 2.1, 2.2, 2.3, //
        3.0, 3.1, 3.2, 3.3);
    final Matrix4 result = mat4A.transpose(null);
    assertEquals(new Matrix4( //
        0.01, 1.0, 2.0, 3.0, //
        0.1, 1.1, 2.1, 3.1, //
        0.2, 1.2, 2.2, 3.2, //
        0.3, 1.3, 2.3, 3.3), result);
    assertEquals(new Matrix4( //
        0.01, 0.1, 0.2, 0.3, //
        1.0, 1.1, 1.2, 1.3, //
        2.0, 2.1, 2.2, 2.3, //
        3.0, 3.1, 3.2, 3.3), result.transposeLocal());
    // coverage
    final Matrix4 result2 = result.transposeLocal().transpose(new Matrix4());
    assertEquals(mat4A, result2);
  }

  @Test
  public void testInvert() {
    final Matrix4 mat4A = new Matrix4().applyRotationX(MathUtils.QUARTER_PI).applyTranslationPost(1, 2, 3);
    final Matrix4 inverted = mat4A.invert(null);
    assertEquals(Matrix4.IDENTITY, mat4A.multiply(inverted, null));
    assertEquals(mat4A, inverted.invertLocal());
  }

  @Test(expected = ArithmeticException.class)
  public void testBadInvert() {
    final Matrix4 mat4A = new Matrix4(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    mat4A.invertLocal();
  }

  @Test
  public void testAdjugate() {
    final double //
    a = -3, b = 2, c = -5, d = 2, //
        e = -1, f = 0, g = -2, h = 3, //
        i = 1, j = -3, k = -4, l = 0, //
        m = 4, n = 2, o = 3, p = 1;

    final Matrix4 mat4A = new Matrix4( //
        a, b, c, d, //
        e, f, g, h, //
        i, j, k, l, //
        m, n, o, p);

    // prepare sections
    final Matrix3 m00 = new Matrix3(f, g, h, j, k, l, n, o, p);
    final Matrix3 m01 = new Matrix3(b, c, d, j, k, l, n, o, p);
    final Matrix3 m02 = new Matrix3(b, c, d, f, g, h, n, o, p);
    final Matrix3 m03 = new Matrix3(b, c, d, f, g, h, j, k, l);
    final Matrix3 m10 = new Matrix3(e, g, h, i, k, l, m, o, p);
    final Matrix3 m11 = new Matrix3(a, c, d, i, k, l, m, o, p);
    final Matrix3 m12 = new Matrix3(a, c, d, e, g, h, m, o, p);
    final Matrix3 m13 = new Matrix3(a, c, d, e, g, h, i, k, l);
    final Matrix3 m20 = new Matrix3(e, f, h, i, j, l, m, n, p);
    final Matrix3 m21 = new Matrix3(a, b, d, i, j, l, m, n, p);
    final Matrix3 m22 = new Matrix3(a, b, d, e, f, h, m, n, p);
    final Matrix3 m23 = new Matrix3(a, b, d, e, f, h, i, j, l);
    final Matrix3 m30 = new Matrix3(e, f, g, i, j, k, m, n, o);
    final Matrix3 m31 = new Matrix3(a, b, c, i, j, k, m, n, o);
    final Matrix3 m32 = new Matrix3(a, b, c, e, f, g, m, n, o);
    final Matrix3 m33 = new Matrix3(a, b, c, e, f, g, i, j, k);

    // generate adjugate
    final Matrix4 testValue = new Matrix4( //
        m00.determinant(), -m01.determinant(), m02.determinant(), -m03.determinant(), //
        -m10.determinant(), m11.determinant(), -m12.determinant(), m13.determinant(), //
        m20.determinant(), -m21.determinant(), m22.determinant(), -m23.determinant(), //
        -m30.determinant(), m31.determinant(), -m32.determinant(), m33.determinant());

    assertEquals(testValue, mat4A.adjugate(null));
    assertEquals(testValue, mat4A.adjugateLocal());
  }

  @Test
  public void testDeterminant() {
    {
      final double //
      a = -3, b = 2, c = -5, d = 2, //
          e = -1, f = 0, g = -2, h = 3, //
          i = 1, j = -3, k = -4, l = 0, //
          m = 4, n = 2, o = 3, p = 1;

      final Matrix4 mat4A = new Matrix4( //
          a, b, c, d, //
          e, f, g, h, //
          i, j, k, l, //
          m, n, o, p);

      // prepare sections
      final double m00 = new Matrix3(f, g, h, j, k, l, n, o, p).determinant();
      final double m01 = new Matrix3(e, g, h, i, k, l, m, o, p).determinant();
      final double m02 = new Matrix3(e, f, h, i, j, l, m, n, p).determinant();
      final double m03 = new Matrix3(e, f, g, i, j, k, m, n, o).determinant();
      final double determinant = a * m00 - b * m01 + c * m02 - d * m03;

      assertTrue(Math.abs(determinant - mat4A.determinant()) <= MathUtils.EPSILON);
    }

    {
      final double //
      a = -1.2, b = 4, c = -2.5, d = 1.7, //
          e = 2, f = -3, g = -2, h = 3.2, //
          i = 3.1, j = -1, k = 2, l = 1.15, //
          m = 1, n = 2, o = 3.14, p = 1.4;

      final Matrix4 mat4A = new Matrix4( //
          a, b, c, d, //
          e, f, g, h, //
          i, j, k, l, //
          m, n, o, p);

      // prepare sections
      final double m00 = new Matrix3(f, g, h, j, k, l, n, o, p).determinant();
      final double m01 = new Matrix3(e, g, h, i, k, l, m, o, p).determinant();
      final double m02 = new Matrix3(e, f, h, i, j, l, m, n, p).determinant();
      final double m03 = new Matrix3(e, f, g, i, j, k, m, n, o).determinant();
      final double determinant = a * m00 - b * m01 + c * m02 - d * m03;

      assertTrue(Math.abs(determinant - mat4A.determinant()) <= MathUtils.EPSILON);
    }
  }

  @Test
  public void testClone() {
    final Matrix4 mat1 = new Matrix4();
    final Matrix4 mat2 = mat1.clone();
    assertEquals(mat1, mat2);
    assertNotSame(mat1, mat2);
  }

  @Test
  public void testFinite() {
    final Matrix4 mat4 = new Matrix4();
    assertTrue(Matrix4.isFinite(mat4));
    for (int i = 0; i < 16; i++) {
      mat4.setIdentity();
      mat4.setValue(i / 4, i % 4, Double.NaN);
      assertFalse(Matrix4.isFinite(mat4));
      mat4.setIdentity();
      mat4.setValue(i / 4, i % 4, Double.POSITIVE_INFINITY);
      assertFalse(Matrix4.isFinite(mat4));
    }

    mat4.setIdentity();
    assertTrue(Matrix4.isFinite(mat4));

    assertFalse(Matrix4.isFinite(null));

    // couple of equals validity tests
    assertEquals(mat4, mat4);
    assertTrue(mat4.strictEquals(mat4));
    assertFalse(mat4.equals(null));
    assertFalse(mat4.strictEquals(null));
    assertFalse(mat4.strictEquals(new Vector2()));

    // throw in a couple pool accesses for coverage
    final Matrix4 matTemp = Matrix4.fetchTempInstance();
    matTemp.set(mat4);
    assertEquals(mat4, matTemp);
    assertNotSame(mat4, matTemp);
    Matrix4.releaseTempInstance(matTemp);

    // cover more of equals
    mat4.set(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
    final Matrix4 comp = new Matrix4(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
    assertFalse(mat4.equals(comp));
    assertFalse(mat4.strictEquals(comp));
    for (int i = 0; i < 15; i++) {
      comp.setValue(i / 4, i % 4, i);
      assertFalse(mat4.equals(comp));
      assertFalse(mat4.strictEquals(comp));
    }
  }

  @Test
  public void testSimpleHash() {
    // Just a simple sanity check.
    final Matrix4 mat1 = new Matrix4(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
    final Matrix4 mat2 = new Matrix4(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
    final Matrix4 mat3 = new Matrix4(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0);

    assertTrue(mat1.hashCode() == mat2.hashCode());
    assertTrue(mat1.hashCode() != mat3.hashCode());
  }

  @Test
  public void testOrthonormal() {
    final Matrix4 mat4 = new Matrix4();
    assertTrue(mat4.isOrthonormal());
    // just rotation
    mat4.set(new Matrix3().applyRotationX(MathUtils.QUARTER_PI));
    assertTrue(mat4.isOrthonormal());
    // non-uniform scale
    mat4.set(new Matrix3().scaleLocal(new Vector3(1, 2, 3)).applyRotationX(MathUtils.QUARTER_PI));
    assertFalse(mat4.isOrthonormal());
    // uniform scale
    mat4.set(new Matrix3().scaleLocal(new Vector3(2, 2, 2)).applyRotationX(MathUtils.QUARTER_PI));
    assertFalse(mat4.isOrthonormal());
    // uniform scale 1
    mat4.set(new Matrix3().scaleLocal(new Vector3(1, 1, 1)).applyRotationX(MathUtils.QUARTER_PI));
    assertTrue(mat4.isOrthonormal());
  }

  @Test
  public void testApplyVector4() {
    final Matrix4 mat4 = new Matrix4().applyRotationX(MathUtils.HALF_PI);
    final Vector4 vec4 = new Vector4(0, 1, 0, 1);
    final Vector4 result = mat4.applyPost(vec4, null);
    assertTrue(Math.abs(new Vector4(0, 0, 1, 1).distance(result)) <= MathUtils.EPSILON);
    vec4.set(0, 1, 1, 1);
    mat4.applyPost(vec4, result);
    assertTrue(Math.abs(new Vector4(0, -1, 1, 1).distance(result)) <= MathUtils.EPSILON);

    vec4.set(0, 1, 1, 1);
    mat4.applyPre(vec4, result);
    assertTrue(Math.abs(new Vector4(0, 1, -1, 1).distance(result)) <= MathUtils.EPSILON);

    vec4.set(1, 1, 1, 1);
    assertTrue(Math.abs(new Vector4(1, 1, -1, 1).distance(mat4.applyPre(vec4, null))) <= MathUtils.EPSILON);
  }

  @Test
  public void testApplyVector3() {
    final Matrix4 mat4 = new Matrix4().applyRotationX(MathUtils.HALF_PI).applyTranslationPre(1, 2, 3);
    final Vector3 vec3 = new Vector3(0, 1, 0);
    final Vector3 result = mat4.applyPostPoint(vec3, null);
    assertTrue(Math.abs(new Vector3(1, 2, 4).distance(result)) <= MathUtils.EPSILON);
    vec3.set(0, 1, 1);
    mat4.applyPostPoint(vec3, result);
    assertTrue(Math.abs(new Vector3(1, 1, 4).distance(result)) <= MathUtils.EPSILON);

    vec3.set(0, 1, 1);
    mat4.applyPostVector(vec3, result);
    assertTrue(Math.abs(new Vector3(0, -1, 1).distance(result)) <= MathUtils.EPSILON);

    vec3.set(1, 1, 1);
    assertTrue(Math.abs(new Vector3(1, -1, 1).distance(mat4.applyPostVector(vec3, null))) <= MathUtils.EPSILON);
  }
}
