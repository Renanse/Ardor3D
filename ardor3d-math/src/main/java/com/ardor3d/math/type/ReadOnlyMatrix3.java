/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.type;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;

public interface ReadOnlyMatrix3 {

  double getValue(int row, int column);

  float getValuef(int row, int column);

  boolean isIdentity();

  Vector3 getColumn(int index, Vector3 store);

  Vector3 getRow(int index, Vector3 store);

  DoubleBuffer toDoubleBuffer(DoubleBuffer store, boolean rowMajor);

  FloatBuffer toFloatBuffer(FloatBuffer store, boolean rowMajor);

  double[] toArray(double[] store);

  double[] toArray(double[] store, boolean rowMajor);

  double[] toAngles(double[] store);

  Matrix3 multiply(ReadOnlyMatrix3 matrix, Matrix3 store);

  Vector3 applyPre(ReadOnlyVector3 vec, Vector3 store);

  Vector3 applyPost(ReadOnlyVector3 vec, Vector3 store);

  Matrix3 multiplyDiagonalPre(ReadOnlyVector3 vec, Matrix3 store);

  Matrix3 multiplyDiagonalPost(ReadOnlyVector3 vec, Matrix3 store);

  Matrix3 add(ReadOnlyMatrix3 matrix, Matrix3 store);

  Matrix3 subtract(ReadOnlyMatrix3 matrix, Matrix3 store);

  Matrix3 scale(ReadOnlyVector3 scale, Matrix3 store);

  Matrix3 transpose(Matrix3 store);

  Matrix3 invert(Matrix3 store);

  Matrix3 adjugate(Matrix3 store);

  double determinant();

  boolean isOrthonormal();

  Matrix3 clone();

  double getM00();

  double getM01();

  double getM02();

  double getM10();

  double getM11();

  double getM12();

  double getM20();

  double getM21();

  double getM22();
}
