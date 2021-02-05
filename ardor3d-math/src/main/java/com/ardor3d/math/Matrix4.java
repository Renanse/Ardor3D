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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.BufferOverflowException;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.type.ReadOnlyVector4;
import com.ardor3d.math.util.EqualsUtil;
import com.ardor3d.math.util.HashUtil;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Matrix4 represents a double precision 4x4 matrix and contains a flag, set at object creation,
 * indicating if the given Matrix4 object is mutable.
 *
 * Note: some algorithms in this class were ported from Eberly, Wolfram, Game Gems and others to
 * Java.
 */
public class Matrix4 implements Cloneable, Savable, Externalizable, ReadOnlyMatrix4, Poolable {
  /**
   * Used with equals method to determine if two Matrix4 objects are close enough to be considered
   * equal.
   */
  public static final double ALLOWED_DEVIANCE = 0.00000001;

  private static final long serialVersionUID = 1L;

  private static final ObjectPool<Matrix4> MAT_POOL = ObjectPool.create(Matrix4.class, MathConstants.maxMathPoolSize);

  /**
   * <pre>
   * 1, 0, 0, 0
   * 0, 1, 0, 0
   * 0, 0, 1, 0
   * 0, 0, 0, 1
   * </pre>
   */
  public final static ReadOnlyMatrix4 IDENTITY = new Matrix4(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);

  protected double _m00, _m01, _m02, _m03, //
      _m10, _m11, _m12, _m13, //
      _m20, _m21, _m22, _m23, //
      _m30, _m31, _m32, _m33;

  /**
   * Constructs a new matrix set to identity.
   */
  public Matrix4() {
    this(Matrix4.IDENTITY);
  }

  /**
   * Constructs a new matrix set to the given matrix values. (names are mRC = m[ROW][COL])
   *
   * @param m00
   * @param m01
   * @param m02
   * @param m03
   * @param m10
   * @param m11
   * @param m12
   * @param m13
   * @param m20
   * @param m21
   * @param m22
   * @param m23
   * @param m30
   * @param m31
   * @param m32
   * @param m33
   */
  public Matrix4(final double m00, final double m01, final double m02, final double m03, final double m10,
    final double m11, final double m12, final double m13, final double m20, final double m21, final double m22,
    final double m23, final double m30, final double m31, final double m32, final double m33) {

    _m00 = m00;
    _m01 = m01;
    _m02 = m02;
    _m03 = m03;
    _m10 = m10;
    _m11 = m11;
    _m12 = m12;
    _m13 = m13;
    _m20 = m20;
    _m21 = m21;
    _m22 = m22;
    _m23 = m23;
    _m30 = m30;
    _m31 = m31;
    _m32 = m32;
    _m33 = m33;
  }

  /**
   * Constructs a new matrix set to the values of the given matrix.
   *
   * @param source
   */
  public Matrix4(final ReadOnlyMatrix4 source) {
    set(source);
  }

  /**
   * @param row
   * @param column
   * @return the value stored in this matrix at row, column.
   * @throws IllegalArgumentException
   *           if row and column are not in bounds [0, 3]
   */
  @Override
  public double getValue(final int row, final int column) {
    switch (row) {
      case 0:
        switch (column) {
          case 0:
            return _m00;
          case 1:
            return _m01;
          case 2:
            return _m02;
          case 3:
            return _m03;
        }
        break;
      case 1:
        switch (column) {
          case 0:
            return _m10;
          case 1:
            return _m11;
          case 2:
            return _m12;
          case 3:
            return _m13;
        }
        break;

      case 2:
        switch (column) {
          case 0:
            return _m20;
          case 1:
            return _m21;
          case 2:
            return _m22;
          case 3:
            return _m23;
        }
        break;

      case 3:
        switch (column) {
          case 0:
            return _m30;
          case 1:
            return _m31;
          case 2:
            return _m32;
          case 3:
            return _m33;
        }
        break;
    }
    throw new IllegalArgumentException();
  }

  /**
   * @param row
   * @param column
   * @return the value stored in this matrix at row, column, pre-cast to a float for convenience.
   * @throws IllegalArgumentException
   *           if row and column are not in bounds [0, 2]
   */
  @Override
  public float getValuef(final int row, final int column) {
    return (float) getValue(row, column);
  }

  @Override
  public double getM00() { return _m00; }

  @Override
  public double getM01() { return _m01; }

  @Override
  public double getM02() { return _m02; }

  @Override
  public double getM03() { return _m03; }

  @Override
  public double getM10() { return _m10; }

  @Override
  public double getM11() { return _m11; }

  @Override
  public double getM12() { return _m12; }

  @Override
  public double getM13() { return _m13; }

  @Override
  public double getM20() { return _m20; }

  @Override
  public double getM21() { return _m21; }

  @Override
  public double getM22() { return _m22; }

  @Override
  public double getM23() { return _m23; }

  @Override
  public double getM30() { return _m30; }

  @Override
  public double getM31() { return _m31; }

  @Override
  public double getM32() { return _m32; }

  @Override
  public double getM33() { return _m33; }

  public void setM00(final double m00) { _m00 = m00; }

  public void setM01(final double m01) { _m01 = m01; }

  public void setM02(final double m02) { _m02 = m02; }

  public void setM03(final double m03) { _m03 = m03; }

  public void setM10(final double m10) { _m10 = m10; }

  public void setM11(final double m11) { _m11 = m11; }

  public void setM12(final double m12) { _m12 = m12; }

  public void setM13(final double m13) { _m13 = m13; }

  public void setM20(final double m20) { _m20 = m20; }

  public void setM21(final double m21) { _m21 = m21; }

  public void setM22(final double m22) { _m22 = m22; }

  public void setM23(final double m23) { _m23 = m23; }

  public void setM30(final double m30) { _m30 = m30; }

  public void setM31(final double m31) { _m31 = m31; }

  public void setM32(final double m32) { _m32 = m32; }

  public void setM33(final double m33) { _m33 = m33; }

  /**
   * Same as set(IDENTITY)
   *
   * @return this matrix for chaining
   */
  public Matrix4 setIdentity() {
    return set(Matrix4.IDENTITY);
  }

  /**
   * @return true if this matrix equals the 4x4 identity matrix
   */
  @Override
  public boolean isIdentity() { return strictEquals(Matrix4.IDENTITY); }

  /**
   * Sets the value of this matrix at row, column to the given value.
   *
   * @param row
   * @param column
   * @param value
   * @return this matrix for chaining
   * @throws IllegalArgumentException
   *           if row and column are not in bounds [0, 3]
   */
  public Matrix4 setValue(final int row, final int column, final double value) {
    switch (row) {
      case 0:
        switch (column) {
          case 0:
            _m00 = value;
            break;
          case 1:
            _m01 = value;
            break;
          case 2:
            _m02 = value;
            break;
          case 3:
            _m03 = value;
            break;
          default:
            throw new IllegalArgumentException();
        }
        break;

      case 1:
        switch (column) {
          case 0:
            _m10 = value;
            break;
          case 1:
            _m11 = value;
            break;
          case 2:
            _m12 = value;
            break;
          case 3:
            _m13 = value;
            break;
          default:
            throw new IllegalArgumentException();
        }
        break;

      case 2:
        switch (column) {
          case 0:
            _m20 = value;
            break;
          case 1:
            _m21 = value;
            break;
          case 2:
            _m22 = value;
            break;
          case 3:
            _m23 = value;
            break;
          default:
            throw new IllegalArgumentException();
        }
        break;

      case 3:
        switch (column) {
          case 0:
            _m30 = value;
            break;
          case 1:
            _m31 = value;
            break;
          case 2:
            _m32 = value;
            break;
          case 3:
            _m33 = value;
            break;
          default:
            throw new IllegalArgumentException();
        }
        break;

      default:
        throw new IllegalArgumentException();
    }

    return this;
  }

  /**
   * Sets the values of this matrix to the values given.
   *
   * @param m00
   * @param m01
   * @param m02
   * @param m03
   * @param m10
   * @param m11
   * @param m12
   * @param m13
   * @param m20
   * @param m21
   * @param m22
   * @param m23
   * @param m30
   * @param m31
   * @param m32
   * @param m33
   * @return this matrix for chaining
   */
  public Matrix4 set(final double m00, final double m01, final double m02, final double m03, final double m10,
      final double m11, final double m12, final double m13, final double m20, final double m21, final double m22,
      final double m23, final double m30, final double m31, final double m32, final double m33) {

    _m00 = m00;
    _m01 = m01;
    _m02 = m02;
    _m03 = m03;
    _m10 = m10;
    _m11 = m11;
    _m12 = m12;
    _m13 = m13;
    _m20 = m20;
    _m21 = m21;
    _m22 = m22;
    _m23 = m23;
    _m30 = m30;
    _m31 = m31;
    _m32 = m32;
    _m33 = m33;

    return this;
  }

  /**
   * Sets the values of this matrix to the values of the provided source matrix.
   *
   * @param source
   * @return this matrix for chaining
   * @throws NullPointerException
   *           if source is null.
   */
  public Matrix4 set(final ReadOnlyMatrix4 source) {
    _m00 = source.getM00();
    _m01 = source.getM01();
    _m02 = source.getM02();
    _m03 = source.getM03();

    _m10 = source.getM10();
    _m11 = source.getM11();
    _m12 = source.getM12();
    _m13 = source.getM13();

    _m20 = source.getM20();
    _m21 = source.getM21();
    _m22 = source.getM22();
    _m23 = source.getM23();

    _m30 = source.getM30();
    _m31 = source.getM31();
    _m32 = source.getM32();
    _m33 = source.getM33();

    return this;
  }

  /**
   * Sets the 3x3 rotation part of this matrix to the values of the provided source matrix.
   *
   * @param source
   * @return this matrix for chaining
   * @throws NullPointerException
   *           if source is null.
   */
  public Matrix4 set(final ReadOnlyMatrix3 source) {
    _m00 = source.getM00();
    _m01 = source.getM01();
    _m02 = source.getM02();
    _m10 = source.getM10();
    _m11 = source.getM11();
    _m12 = source.getM12();
    _m20 = source.getM20();
    _m21 = source.getM21();
    _m22 = source.getM22();
    return this;
  }

  /**
   * Sets the values of this matrix to the rotational value of the given quaternion. Only modifies the
   * 3x3 rotation part of this matrix.
   *
   * @param quaternion
   * @return this matrix for chaining
   */
  public Matrix4 set(final ReadOnlyQuaternion quaternion) {
    return quaternion.toRotationMatrix(this);
  }

  /**
   * @param source
   *          the buffer to read our matrix data from.
   * @return this matrix for chaining.
   */
  public Matrix4 fromDoubleBuffer(final DoubleBuffer source) {
    return fromDoubleBuffer(source, true);
  }

  /**
   * @param source
   *          the buffer to read our matrix data from.
   * @param rowMajor
   *          if true, data is stored row by row. Otherwise it is stored column by column.
   * @return this matrix for chaining.
   */
  public Matrix4 fromDoubleBuffer(final DoubleBuffer source, final boolean rowMajor) {
    if (rowMajor) {
      _m00 = source.get();
      _m01 = source.get();
      _m02 = source.get();
      _m03 = source.get();
      _m10 = source.get();
      _m11 = source.get();
      _m12 = source.get();
      _m13 = source.get();
      _m20 = source.get();
      _m21 = source.get();
      _m22 = source.get();
      _m23 = source.get();
      _m30 = source.get();
      _m31 = source.get();
      _m32 = source.get();
      _m33 = source.get();
    } else {
      _m00 = source.get();
      _m10 = source.get();
      _m20 = source.get();
      _m30 = source.get();
      _m01 = source.get();
      _m11 = source.get();
      _m21 = source.get();
      _m31 = source.get();
      _m02 = source.get();
      _m12 = source.get();
      _m22 = source.get();
      _m32 = source.get();
      _m03 = source.get();
      _m13 = source.get();
      _m23 = source.get();
      _m33 = source.get();
    }

    return this;
  }

  /**
   * Note: data is cast to floats.
   *
   * @param store
   *          the buffer to read our matrix data from.
   * @return this matrix for chaining.
   */
  public Matrix4 fromFloatBuffer(final FloatBuffer source) {
    return fromFloatBuffer(source, true);
  }

  /**
   * Note: data is cast to floats.
   *
   * @param store
   *          the buffer to read our matrix data from.
   * @param rowMajor
   *          if true, data is stored row by row. Otherwise it is stored column by column.
   * @return this matrix for chaining.
   */
  public Matrix4 fromFloatBuffer(final FloatBuffer source, final boolean rowMajor) {
    if (rowMajor) {
      _m00 = source.get();
      _m01 = source.get();
      _m02 = source.get();
      _m03 = source.get();
      _m10 = source.get();
      _m11 = source.get();
      _m12 = source.get();
      _m13 = source.get();
      _m20 = source.get();
      _m21 = source.get();
      _m22 = source.get();
      _m23 = source.get();
      _m30 = source.get();
      _m31 = source.get();
      _m32 = source.get();
      _m33 = source.get();
    } else {
      _m00 = source.get();
      _m10 = source.get();
      _m20 = source.get();
      _m30 = source.get();
      _m01 = source.get();
      _m11 = source.get();
      _m21 = source.get();
      _m31 = source.get();
      _m02 = source.get();
      _m12 = source.get();
      _m22 = source.get();
      _m32 = source.get();
      _m03 = source.get();
      _m13 = source.get();
      _m23 = source.get();
      _m33 = source.get();
    }

    return this;
  }

  /**
   * Sets the values of this matrix to the values of the provided double array.
   *
   * @param source
   * @return this matrix for chaining
   * @throws NullPointerException
   *           if source is null.
   * @throws ArrayIndexOutOfBoundsException
   *           if source array has a length less than 16.
   */
  public Matrix4 fromArray(final double[] source) {
    return fromArray(source, true);
  }

  /**
   * Sets the values of this matrix to the values of the provided double array.
   *
   * @param source
   * @param rowMajor
   * @return this matrix for chaining
   * @throws NullPointerException
   *           if source is null.
   * @throws ArrayIndexOutOfBoundsException
   *           if source array has a length less than 16.
   */
  public Matrix4 fromArray(final double[] source, final boolean rowMajor) {
    if (rowMajor) {
      _m00 = source[0];
      _m01 = source[1];
      _m02 = source[2];
      _m03 = source[3];
      _m10 = source[4];
      _m11 = source[5];
      _m12 = source[6];
      _m13 = source[7];
      _m20 = source[8];
      _m21 = source[9];
      _m22 = source[10];
      _m23 = source[11];
      _m30 = source[12];
      _m31 = source[13];
      _m32 = source[14];
      _m33 = source[15];
    } else {
      _m00 = source[0];
      _m10 = source[1];
      _m20 = source[2];
      _m30 = source[3];
      _m01 = source[4];
      _m11 = source[5];
      _m21 = source[6];
      _m31 = source[7];
      _m02 = source[8];
      _m12 = source[9];
      _m22 = source[10];
      _m32 = source[11];
      _m03 = source[12];
      _m13 = source[13];
      _m23 = source[14];
      _m33 = source[15];
    }
    return this;
  }

  /**
   * Replaces a column in this matrix with the values of the given array.
   *
   * @param columnIndex
   * @param columnData
   * @return this matrix for chaining
   * @throws NullPointerException
   *           if columnData is null.
   * @throws IllegalArgumentException
   *           if columnData has a length < 4
   * @throws IllegalArgumentException
   *           if columnIndex is not in [0, 3]
   */
  public Matrix4 setColumn(final int columnIndex, final Vector4 columnData) {
    switch (columnIndex) {
      case 0:
        _m00 = columnData.getX();
        _m10 = columnData.getY();
        _m20 = columnData.getZ();
        _m30 = columnData.getW();
        break;
      case 1:
        _m01 = columnData.getX();
        _m11 = columnData.getY();
        _m21 = columnData.getZ();
        _m31 = columnData.getW();
        break;
      case 2:
        _m02 = columnData.getX();
        _m12 = columnData.getY();
        _m22 = columnData.getZ();
        _m32 = columnData.getW();
        break;
      case 3:
        _m03 = columnData.getX();
        _m13 = columnData.getY();
        _m23 = columnData.getZ();
        _m33 = columnData.getW();
        break;
      default:
        throw new IllegalArgumentException("Bad columnIndex: " + columnIndex);
    }
    return this;
  }

  /**
   * Replaces a row in this matrix with the values of the given array.
   *
   * @param rowIndex
   * @param rowData
   * @return this matrix for chaining
   * @throws NullPointerException
   *           if rowData is null.
   * @throws IllegalArgumentException
   *           if rowData has a length < 4
   * @throws IllegalArgumentException
   *           if rowIndex is not in [0, 3]
   */
  public Matrix4 setRow(final int rowIndex, final Vector4 rowData) {
    switch (rowIndex) {
      case 0:
        _m00 = rowData.getX();
        _m01 = rowData.getY();
        _m02 = rowData.getZ();
        _m03 = rowData.getW();
        break;
      case 1:
        _m10 = rowData.getX();
        _m11 = rowData.getY();
        _m12 = rowData.getZ();
        _m13 = rowData.getW();
        break;
      case 2:
        _m20 = rowData.getX();
        _m21 = rowData.getY();
        _m22 = rowData.getZ();
        _m23 = rowData.getW();
        break;
      case 3:
        _m30 = rowData.getX();
        _m31 = rowData.getY();
        _m32 = rowData.getZ();
        _m33 = rowData.getW();
        break;
      default:
        throw new IllegalArgumentException("Bad rowIndex: " + rowIndex);
    }
    return this;
  }

  /**
   * Sets the 3x3 rotation portion of this matrix to the rotation indicated by the given angle and
   * axis of rotation. Note: This method creates an object, so use fromAngleNormalAxis when possible,
   * particularly if your axis is already normalized.
   *
   * @param angle
   *          the angle to rotate (in radians).
   * @param axis
   *          the axis of rotation.
   * @return this matrix for chaining
   * @throws NullPointerException
   *           if axis is null.
   */
  public Matrix4 fromAngleAxis(final double angle, final ReadOnlyVector3 axis) {
    final Vector3 normAxis = Vector3.fetchTempInstance();
    axis.normalize(normAxis);
    fromAngleNormalAxis(angle, normAxis);
    Vector3.releaseTempInstance(normAxis);
    return this;
  }

  /**
   * Sets the 3x3 rotation portion of this matrix to the rotation indicated by the given angle and a
   * unit-length axis of rotation.
   *
   * @param angle
   *          the angle to rotate (in radians).
   * @param axis
   *          the axis of rotation (already normalized).
   * @return this matrix for chaining
   * @throws NullPointerException
   *           if axis is null.
   */
  public Matrix4 fromAngleNormalAxis(final double angle, final ReadOnlyVector3 axis) {
    final double fCos = Math.cos(angle);
    final double fSin = Math.sin(angle);
    final double fOneMinusCos = 1.0 - fCos;
    final double fX2 = axis.getX() * axis.getX();
    final double fY2 = axis.getY() * axis.getY();
    final double fZ2 = axis.getZ() * axis.getZ();
    final double fXYM = axis.getX() * axis.getY() * fOneMinusCos;
    final double fXZM = axis.getX() * axis.getZ() * fOneMinusCos;
    final double fYZM = axis.getY() * axis.getZ() * fOneMinusCos;
    final double fXSin = axis.getX() * fSin;
    final double fYSin = axis.getY() * fSin;
    final double fZSin = axis.getZ() * fSin;

    _m00 = fX2 * fOneMinusCos + fCos;
    _m01 = fXYM - fZSin;
    _m02 = fXZM + fYSin;
    _m10 = fXYM + fZSin;
    _m11 = fY2 * fOneMinusCos + fCos;
    _m12 = fYZM - fXSin;
    _m20 = fXZM - fYSin;
    _m21 = fYZM + fXSin;
    _m22 = fZ2 * fOneMinusCos + fCos;

    return this;
  }

  public Matrix4 applyRotation(final double angle, final double x, final double y, final double z) {
    final double m00 = _m00, m01 = _m01, m02 = _m02, //
        m10 = _m10, m11 = _m11, m12 = _m12, //
        m20 = _m20, m21 = _m21, m22 = _m22, //
        m30 = _m30, m31 = _m31, m32 = _m32;

    final double cosAngle = Math.cos(angle);
    final double sinAngle = Math.sin(angle);
    final double oneMinusCos = 1.0f - cosAngle;
    final double xyOneMinusCos = x * y * oneMinusCos;
    final double xzOneMinusCos = x * z * oneMinusCos;
    final double yzOneMinusCos = y * z * oneMinusCos;
    final double xSin = x * sinAngle;
    final double ySin = y * sinAngle;
    final double zSin = z * sinAngle;

    final double r00 = x * x * oneMinusCos + cosAngle;
    final double r01 = xyOneMinusCos - zSin;
    final double r02 = xzOneMinusCos + ySin;
    final double r10 = xyOneMinusCos + zSin;
    final double r11 = y * y * oneMinusCos + cosAngle;
    final double r12 = yzOneMinusCos - xSin;
    final double r20 = xzOneMinusCos - ySin;
    final double r21 = yzOneMinusCos + xSin;
    final double r22 = z * z * oneMinusCos + cosAngle;

    _m00 = m00 * r00 + m01 * r10 + m02 * r20;
    _m01 = m00 * r01 + m01 * r11 + m02 * r21;
    _m02 = m00 * r02 + m01 * r12 + m02 * r22;
    // _m03 is unchanged

    _m10 = m10 * r00 + m11 * r10 + m12 * r20;
    _m11 = m10 * r01 + m11 * r11 + m12 * r21;
    _m12 = m10 * r02 + m11 * r12 + m12 * r22;
    // _m13 is unchanged

    _m20 = m20 * r00 + m21 * r10 + m22 * r20;
    _m21 = m20 * r01 + m21 * r11 + m22 * r21;
    _m22 = m20 * r02 + m21 * r12 + m22 * r22;
    // _m23 is unchanged

    _m30 = m30 * r00 + m31 * r10 + m32 * r20;
    _m31 = m30 * r01 + m31 * r11 + m32 * r21;
    _m32 = m30 * r02 + m31 * r12 + m32 * r22;
    // _m33 is unchanged

    return this;
  }

  public Matrix4 applyRotationX(final double angle) {
    final double m01 = _m01, m02 = _m02, //
        m11 = _m11, m12 = _m12, //
        m21 = _m21, m22 = _m22, //
        m31 = _m31, m32 = _m32;

    final double cosAngle = Math.cos(angle);
    final double sinAngle = Math.sin(angle);

    _m01 = m01 * cosAngle + m02 * sinAngle;
    _m02 = m02 * cosAngle - m01 * sinAngle;

    _m11 = m11 * cosAngle + m12 * sinAngle;
    _m12 = m12 * cosAngle - m11 * sinAngle;

    _m21 = m21 * cosAngle + m22 * sinAngle;
    _m22 = m22 * cosAngle - m21 * sinAngle;

    _m31 = m31 * cosAngle + m32 * sinAngle;
    _m32 = m32 * cosAngle - m31 * sinAngle;

    return this;
  }

  public Matrix4 applyRotationY(final double angle) {
    final double m00 = _m00, m02 = _m02, //
        m10 = _m10, m12 = _m12, //
        m20 = _m20, m22 = _m22, //
        m30 = _m30, m32 = _m32;

    final double cosAngle = Math.cos(angle);
    final double sinAngle = Math.sin(angle);

    _m00 = m00 * cosAngle - m02 * sinAngle;
    _m02 = m00 * sinAngle + m02 * cosAngle;

    _m10 = m10 * cosAngle - m12 * sinAngle;
    _m12 = m10 * sinAngle + m12 * cosAngle;

    _m20 = m20 * cosAngle - m22 * sinAngle;
    _m22 = m20 * sinAngle + m22 * cosAngle;

    _m30 = m30 * cosAngle - m32 * sinAngle;
    _m32 = m30 * sinAngle + m32 * cosAngle;

    return this;
  }

  public Matrix4 applyRotationZ(final double angle) {
    final double m00 = _m00, m01 = _m01, //
        m10 = _m10, m11 = _m11, //
        m20 = _m20, m21 = _m21, //
        m30 = _m30, m31 = _m31;

    final double cosAngle = Math.cos(angle);
    final double sinAngle = Math.sin(angle);

    _m00 = m00 * cosAngle + m01 * sinAngle;
    _m01 = m01 * cosAngle - m00 * sinAngle;

    _m10 = m10 * cosAngle + m11 * sinAngle;
    _m11 = m11 * cosAngle - m10 * sinAngle;

    _m20 = m20 * cosAngle + m21 * sinAngle;
    _m21 = m21 * cosAngle - m20 * sinAngle;

    _m20 = m30 * cosAngle + m31 * sinAngle;
    _m21 = m31 * cosAngle - m30 * sinAngle;

    return this;
  }

  /**
   * M*T
   *
   * @param x
   * @param y
   * @param z
   * @return
   */
  public Matrix4 applyTranslationPost(final double x, final double y, final double z) {
    _m03 = _m00 * x + _m01 * y + _m02 * z + _m03;
    _m13 = _m10 * x + _m11 * y + _m12 * z + _m13;
    _m23 = _m20 * x + _m21 * y + _m22 * z + _m23;
    _m33 = _m30 * x + _m31 * y + _m32 * z + _m33;

    return this;
  }

  /**
   * T*M
   *
   * @param x
   * @param y
   * @param z
   * @return
   */
  public Matrix4 applyTranslationPre(final double x, final double y, final double z) {
    _m03 = x;
    _m13 = y;
    _m23 = z;

    return this;
  }

  /**
   * @param index
   * @param store
   *          the vector to store the result in. if null, a new one is created.
   * @return the column specified by the index.
   * @throws IllegalArgumentException
   *           if index is not in bounds [0, 3]
   */
  @Override
  public Vector4 getColumn(final int index, final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }

    switch (index) {
      case 0:
        result.setX(_m00);
        result.setY(_m10);
        result.setZ(_m20);
        result.setW(_m30);
        break;
      case 1:
        result.setX(_m01);
        result.setY(_m11);
        result.setZ(_m21);
        result.setW(_m31);
        break;
      case 2:
        result.setX(_m02);
        result.setY(_m12);
        result.setZ(_m22);
        result.setW(_m32);
        break;
      case 3:
        result.setX(_m03);
        result.setY(_m13);
        result.setZ(_m23);
        result.setW(_m33);
        break;
      default:
        throw new IllegalArgumentException("invalid column index: " + index);
    }

    return result;
  }

  /**
   * @param index
   * @param store
   *          the vector to store the result in. if null, a new one is created.
   * @return the row specified by the index.
   * @throws IllegalArgumentException
   *           if index is not in bounds [0, 3]
   */
  @Override
  public Vector4 getRow(final int index, final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }
    switch (index) {
      case 0:
        result.setX(_m00);
        result.setY(_m01);
        result.setZ(_m02);
        result.setW(_m03);
        break;
      case 1:
        result.setX(_m10);
        result.setY(_m11);
        result.setZ(_m12);
        result.setW(_m13);
        break;
      case 2:
        result.setX(_m20);
        result.setY(_m21);
        result.setZ(_m22);
        result.setW(_m23);
        break;
      case 3:
        result.setX(_m30);
        result.setY(_m31);
        result.setZ(_m32);
        result.setW(_m33);
        break;
      default:
        throw new IllegalArgumentException("invalid row index: " + index);
    }
    return result;
  }

  /**
   * @param store
   *          the Matrix to store our 3x3 portion to
   */
  @Override
  public void toMatrix3(final Matrix3 store) {
    store._m00 = _m00;
    store._m01 = _m01;
    store._m02 = _m02;
    store._m10 = _m10;
    store._m11 = _m11;
    store._m12 = _m12;
    store._m20 = _m20;
    store._m21 = _m21;
    store._m22 = _m22;
  }

  /**
   * @param store
   *          the buffer to store our matrix data in. Must not be null. Data is entered starting at
   *          current buffer
   * @return matrix data as a DoubleBuffer in row major order. The position is at the end of the
   *         inserted data.
   * @throws NullPointerException
   *           if store is null.
   * @throws BufferOverflowException
   *           if there is not enough room left in the buffer to write all 16 values.
   */
  @Override
  public DoubleBuffer toDoubleBuffer(final DoubleBuffer store) {
    return toDoubleBuffer(store, true);
  }

  /**
   * @param store
   *          the buffer to store our matrix data in. Must not be null. Data is entered starting at
   *          current buffer position.
   * @param rowMajor
   *          if true, data is stored row by row. Otherwise it is stored column by column.
   * @return matrix data as a DoubleBuffer in the specified order. The position is at the end of the
   *         inserted data.
   * @throws NullPointerException
   *           if store is null.
   * @throws BufferOverflowException
   *           if there is not enough room left in the buffer to write all 16 values.
   */
  @Override
  public DoubleBuffer toDoubleBuffer(final DoubleBuffer store, final boolean rowMajor) {
    if (rowMajor) {
      store.put(_m00);
      store.put(_m01);
      store.put(_m02);
      store.put(_m03);
      store.put(_m10);
      store.put(_m11);
      store.put(_m12);
      store.put(_m13);
      store.put(_m20);
      store.put(_m21);
      store.put(_m22);
      store.put(_m23);
      store.put(_m30);
      store.put(_m31);
      store.put(_m32);
      store.put(_m33);
    } else {
      store.put(_m00);
      store.put(_m10);
      store.put(_m20);
      store.put(_m30);
      store.put(_m01);
      store.put(_m11);
      store.put(_m21);
      store.put(_m31);
      store.put(_m02);
      store.put(_m12);
      store.put(_m22);
      store.put(_m32);
      store.put(_m03);
      store.put(_m13);
      store.put(_m23);
      store.put(_m33);
    }

    return store;
  }

  /**
   * Note: data is cast to floats.
   *
   * @param store
   *          the buffer to store our matrix data in. Must not be null. Data is entered starting at
   *          current buffer
   * @return matrix data as a FloatBuffer in row major order. The position is at the end of the
   *         inserted data.
   * @throws NullPointerException
   *           if store is null.
   * @throws BufferOverflowException
   *           if there is not enough room left in the buffer to write all 16 values.
   */
  @Override
  public FloatBuffer toFloatBuffer(final FloatBuffer store) {
    return toFloatBuffer(store, true);
  }

  /**
   * Note: data is cast to floats.
   *
   * @param store
   *          the buffer to store our matrix data in. Must not be null. Data is entered starting at
   *          current buffer
   * @param rowMajor
   *          if true, data is stored row by row. Otherwise it is stored column by column.
   * @return matrix data as a FloatBuffer in the specified order. The position is at the end of the
   *         inserted data.
   * @throws NullPointerException
   *           if store is null.
   * @throws BufferOverflowException
   *           if there is not enough room left in the buffer to write all 16 values.
   */
  @Override
  public FloatBuffer toFloatBuffer(final FloatBuffer store, final boolean rowMajor) {
    if (rowMajor) {
      store.put((float) _m00);
      store.put((float) _m01);
      store.put((float) _m02);
      store.put((float) _m03);
      store.put((float) _m10);
      store.put((float) _m11);
      store.put((float) _m12);
      store.put((float) _m13);
      store.put((float) _m20);
      store.put((float) _m21);
      store.put((float) _m22);
      store.put((float) _m23);
      store.put((float) _m30);
      store.put((float) _m31);
      store.put((float) _m32);
      store.put((float) _m33);
    } else {
      store.put((float) _m00);
      store.put((float) _m10);
      store.put((float) _m20);
      store.put((float) _m30);
      store.put((float) _m01);
      store.put((float) _m11);
      store.put((float) _m21);
      store.put((float) _m31);
      store.put((float) _m02);
      store.put((float) _m12);
      store.put((float) _m22);
      store.put((float) _m32);
      store.put((float) _m03);
      store.put((float) _m13);
      store.put((float) _m23);
      store.put((float) _m33);
    }

    return store;
  }

  /**
   * @param store
   *          the double array to store our matrix data in. If null, a new array is created.
   * @return matrix data as a double array in row major order.
   * @throws IllegalArgumentException
   *           if the store is non-null and has a length < 16
   */
  @Override
  public double[] toArray(final double[] store) {
    return toArray(store, true);
  }

  /**
   * @param store
   *          the double array to store our matrix data in. If null, a new array is created.
   * @param rowMajor
   *          if true, data is stored row by row. Otherwise it is stored column by column.
   * @return matrix data as a double array in the specified order.
   * @throws IllegalArgumentException
   *           if the store is non-null and has a length < 16
   */
  @Override
  public double[] toArray(final double[] store, final boolean rowMajor) {
    double[] result = store;
    if (result == null) {
      result = new double[16];
    } else if (result.length < 16) {
      throw new IllegalArgumentException("store must be at least length 16.");
    }

    if (rowMajor) {
      result[0] = _m00;
      result[1] = _m01;
      result[2] = _m02;
      result[3] = _m03;
      result[4] = _m10;
      result[5] = _m11;
      result[6] = _m12;
      result[7] = _m13;
      result[8] = _m20;
      result[9] = _m21;
      result[10] = _m22;
      result[11] = _m23;
      result[12] = _m30;
      result[13] = _m31;
      result[14] = _m32;
      result[15] = _m33;
    } else {
      result[0] = _m00;
      result[1] = _m10;
      result[2] = _m20;
      result[3] = _m30;
      result[4] = _m01;
      result[5] = _m11;
      result[6] = _m21;
      result[7] = _m31;
      result[8] = _m02;
      result[9] = _m12;
      result[10] = _m22;
      result[11] = _m32;
      result[12] = _m03;
      result[13] = _m13;
      result[14] = _m23;
      result[15] = _m33;
    }

    return result;
  }

  /**
   * Multiplies this matrix by the diagonal matrix formed by the given vector (v^D * M). If supplied,
   * the result is stored into the supplied "store" matrix.
   *
   * @param vec
   * @param store
   *          a matrix to store the result in. If store is null, a new matrix is created. Note that it
   *          IS safe for vec and store to be the same object.
   * @return the store matrix, or a new matrix if store is null.
   * @throws NullPointerException
   *           if vec is null
   */
  @Override
  public Matrix4 multiplyDiagonalPre(final ReadOnlyVector4 vec, final Matrix4 store) {
    Matrix4 result = store;
    if (result == null) {
      result = new Matrix4();
    }

    result.set( //
        vec.getX() * _m00, vec.getX() * _m01, vec.getX() * _m02, vec.getX() * _m03, //
        vec.getY() * _m10, vec.getY() * _m11, vec.getY() * _m12, vec.getY() * _m13, //
        vec.getZ() * _m20, vec.getZ() * _m21, vec.getZ() * _m22, vec.getZ() * _m23, //
        vec.getW() * _m30, vec.getW() * _m31, vec.getW() * _m32, vec.getW() * _m33);

    return result;
  }

  /**
   * Multiplies this matrix by the diagonal matrix formed by the given vector (M * v^D). If supplied,
   * the result is stored into the supplied "store" matrix.
   *
   * @param vec
   * @param store
   *          a matrix to store the result in. If store is null, a new matrix is created. Note that it
   *          IS safe for vec and store to be the same object.
   * @return the store matrix, or a new matrix if store is null.
   * @throws NullPointerException
   *           if vec is null
   */
  @Override
  public Matrix4 multiplyDiagonalPost(final ReadOnlyVector4 vec, final Matrix4 store) {
    Matrix4 result = store;
    if (result == null) {
      result = new Matrix4();
    }

    result.set( //
        vec.getX() * _m00, vec.getY() * _m01, vec.getZ() * _m02, vec.getW() * _m03, //
        vec.getX() * _m10, vec.getY() * _m11, vec.getZ() * _m12, vec.getW() * _m13, //
        vec.getX() * _m20, vec.getY() * _m21, vec.getZ() * _m22, vec.getW() * _m23, //
        vec.getX() * _m30, vec.getY() * _m31, vec.getZ() * _m32, vec.getW() * _m33);

    return result;
  }

  /**
   * @param matrix
   * @return This matrix for chaining, modified internally to reflect multiplication against the given
   *         matrix
   * @throws NullPointerException
   *           if matrix is null
   */
  public Matrix4 multiplyLocal(final ReadOnlyMatrix4 matrix) {
    return multiply(matrix, this);
  }

  /**
   * @param matrix
   * @param store
   *          a matrix to store the result in. if null, a new matrix is created. Note that it IS safe
   *          for matrix and store to be the same object.
   * @return this matrix multiplied by the given matrix.
   * @throws NullPointerException
   *           if matrix is null.
   */
  @Override
  public Matrix4 multiply(final ReadOnlyMatrix4 matrix, final Matrix4 store) {
    Matrix4 result = store;
    if (result == null) {
      result = new Matrix4();
    }

    final double temp00 =
        _m00 * matrix.getM00() + _m01 * matrix.getM10() + _m02 * matrix.getM20() + _m03 * matrix.getM30();
    final double temp01 =
        _m00 * matrix.getM01() + _m01 * matrix.getM11() + _m02 * matrix.getM21() + _m03 * matrix.getM31();
    final double temp02 =
        _m00 * matrix.getM02() + _m01 * matrix.getM12() + _m02 * matrix.getM22() + _m03 * matrix.getM32();
    final double temp03 =
        _m00 * matrix.getM03() + _m01 * matrix.getM13() + _m02 * matrix.getM23() + _m03 * matrix.getM33();

    final double temp10 =
        _m10 * matrix.getM00() + _m11 * matrix.getM10() + _m12 * matrix.getM20() + _m13 * matrix.getM30();
    final double temp11 =
        _m10 * matrix.getM01() + _m11 * matrix.getM11() + _m12 * matrix.getM21() + _m13 * matrix.getM31();
    final double temp12 =
        _m10 * matrix.getM02() + _m11 * matrix.getM12() + _m12 * matrix.getM22() + _m13 * matrix.getM32();
    final double temp13 =
        _m10 * matrix.getM03() + _m11 * matrix.getM13() + _m12 * matrix.getM23() + _m13 * matrix.getM33();

    final double temp20 =
        _m20 * matrix.getM00() + _m21 * matrix.getM10() + _m22 * matrix.getM20() + _m23 * matrix.getM30();
    final double temp21 =
        _m20 * matrix.getM01() + _m21 * matrix.getM11() + _m22 * matrix.getM21() + _m23 * matrix.getM31();
    final double temp22 =
        _m20 * matrix.getM02() + _m21 * matrix.getM12() + _m22 * matrix.getM22() + _m23 * matrix.getM32();
    final double temp23 =
        _m20 * matrix.getM03() + _m21 * matrix.getM13() + _m22 * matrix.getM23() + _m23 * matrix.getM33();

    final double temp30 =
        _m30 * matrix.getM00() + _m31 * matrix.getM10() + _m32 * matrix.getM20() + _m33 * matrix.getM30();
    final double temp31 =
        _m30 * matrix.getM01() + _m31 * matrix.getM11() + _m32 * matrix.getM21() + _m33 * matrix.getM31();
    final double temp32 =
        _m30 * matrix.getM02() + _m31 * matrix.getM12() + _m32 * matrix.getM22() + _m33 * matrix.getM32();
    final double temp33 =
        _m30 * matrix.getM03() + _m31 * matrix.getM13() + _m32 * matrix.getM23() + _m33 * matrix.getM33();

    result._m00 = temp00;
    result._m01 = temp01;
    result._m02 = temp02;
    result._m03 = temp03;
    result._m10 = temp10;
    result._m11 = temp11;
    result._m12 = temp12;
    result._m13 = temp13;
    result._m20 = temp20;
    result._m21 = temp21;
    result._m22 = temp22;
    result._m23 = temp23;
    result._m30 = temp30;
    result._m31 = temp31;
    result._m32 = temp32;
    result._m33 = temp33;

    return result;
  }

  /**
   * Internally scales all values of this matrix by the given scalar.
   *
   * @param scalar
   * @return this matrix for chaining.
   */
  public Matrix4 multiplyLocal(final double scalar) {
    _m00 *= scalar;
    _m01 *= scalar;
    _m02 *= scalar;
    _m03 *= scalar;
    _m10 *= scalar;
    _m11 *= scalar;
    _m12 *= scalar;
    _m13 *= scalar;
    _m20 *= scalar;
    _m21 *= scalar;
    _m22 *= scalar;
    _m23 *= scalar;
    _m30 *= scalar;
    _m31 *= scalar;
    _m32 *= scalar;
    _m33 *= scalar;

    return this;
  }

  /**
   * @param matrix
   *          the matrix to add to this.
   * @param store
   *          a matrix to store the result in. If store is null, a new matrix is created. Note that it
   *          IS safe for matrix and store to be the same object.
   * @return the result.
   * @throws NullPointerException
   *           if matrix is null
   */
  @Override
  public Matrix4 add(final ReadOnlyMatrix4 matrix, final Matrix4 store) {
    Matrix4 result = store;
    if (result == null) {
      result = new Matrix4();
    }

    result._m00 = _m00 + matrix.getM00();
    result._m01 = _m01 + matrix.getM01();
    result._m02 = _m02 + matrix.getM02();
    result._m03 = _m03 + matrix.getM03();
    result._m10 = _m10 + matrix.getM10();
    result._m11 = _m11 + matrix.getM11();
    result._m12 = _m12 + matrix.getM12();
    result._m13 = _m13 + matrix.getM13();
    result._m20 = _m20 + matrix.getM20();
    result._m21 = _m21 + matrix.getM21();
    result._m22 = _m22 + matrix.getM22();
    result._m23 = _m23 + matrix.getM23();
    result._m30 = _m30 + matrix.getM30();
    result._m31 = _m31 + matrix.getM31();
    result._m32 = _m32 + matrix.getM32();
    result._m33 = _m33 + matrix.getM33();

    return result;
  }

  /**
   * Internally adds the values of the given matrix to this matrix.
   *
   * @param matrix
   *          the matrix to add to this.
   * @return this matrix for chaining
   * @throws NullPointerException
   *           if matrix is null
   */
  public Matrix4 addLocal(final ReadOnlyMatrix4 matrix) {
    return add(matrix, this);
  }

  /**
   * @param matrix
   *          the matrix to subtract from this.
   * @param store
   *          a matrix to store the result in. If store is null, a new matrix is created. Note that it
   *          IS safe for matrix and store to be the same object.
   * @return the result.
   * @throws NullPointerException
   *           if matrix is null
   */
  @Override
  public Matrix4 subtract(final ReadOnlyMatrix4 matrix, final Matrix4 store) {
    Matrix4 result = store;
    if (result == null) {
      result = new Matrix4();
    }

    result._m00 = _m00 - matrix.getM00();
    result._m01 = _m01 - matrix.getM01();
    result._m02 = _m02 - matrix.getM02();
    result._m03 = _m03 - matrix.getM03();
    result._m10 = _m10 - matrix.getM10();
    result._m11 = _m11 - matrix.getM11();
    result._m12 = _m12 - matrix.getM12();
    result._m13 = _m13 - matrix.getM13();
    result._m20 = _m20 - matrix.getM20();
    result._m21 = _m21 - matrix.getM21();
    result._m22 = _m22 - matrix.getM22();
    result._m23 = _m23 - matrix.getM23();
    result._m30 = _m30 - matrix.getM30();
    result._m31 = _m31 - matrix.getM31();
    result._m32 = _m32 - matrix.getM32();
    result._m33 = _m33 - matrix.getM33();

    return result;
  }

  /**
   * Internally subtracts the values of the given matrix from this matrix.
   *
   * @param matrix
   *          the matrix to subtract from this.
   * @return this matrix for chaining
   * @throws NullPointerException
   *           if matrix is null
   */
  public Matrix4 subtractLocal(final ReadOnlyMatrix4 matrix) {
    return subtract(matrix, this);
  }

  /**
   * Applies the given scale to this matrix and returns the result as a new matrix
   *
   * @param scale
   * @param store
   *          a matrix to store the result in. If store is null, a new matrix is created.
   * @return the new matrix
   * @throws NullPointerException
   *           if scale is null.
   */
  @Override
  public Matrix4 scale(final ReadOnlyVector4 scale, final Matrix4 store) {
    Matrix4 result = store;
    if (result == null) {
      result = new Matrix4();
    }

    return result.set( //
        _m00 * scale.getX(), _m01 * scale.getY(), _m02 * scale.getZ(), _m03 * scale.getW(), //
        _m10 * scale.getX(), _m11 * scale.getY(), _m12 * scale.getZ(), _m13 * scale.getW(), //
        _m20 * scale.getX(), _m21 * scale.getY(), _m22 * scale.getZ(), _m23 * scale.getW(), //
        _m30 * scale.getX(), _m31 * scale.getY(), _m32 * scale.getZ(), _m33 * scale.getW());
  }

  /**
   * Applies the given scale to this matrix values internally
   *
   * @param scale
   * @return this matrix for chaining.
   * @throws NullPointerException
   *           if scale is null.
   */
  public Matrix4 scaleLocal(final ReadOnlyVector4 scale) {
    return scale(scale, this);
  }

  /**
   * transposes this matrix as a new matrix, basically flipping it across the diagonal
   *
   * @param store
   *          a matrix to store the result in. If store is null, a new matrix is created. It is NOT
   *          safe for store to == this.
   * @return this matrix for chaining.
   * @see <a href="http://en.wikipedia.org/wiki/Transpose">wikipedia.org-Transpose</a>
   */
  @Override
  public Matrix4 transpose(final Matrix4 store) {
    Matrix4 result = store;
    if (result == null) {
      result = new Matrix4();
    }

    result._m00 = _m00;
    result._m01 = _m10;
    result._m02 = _m20;
    result._m03 = _m30;
    result._m10 = _m01;
    result._m11 = _m11;
    result._m12 = _m21;
    result._m13 = _m31;
    result._m20 = _m02;
    result._m21 = _m12;
    result._m22 = _m22;
    result._m23 = _m32;
    result._m30 = _m03;
    result._m31 = _m13;
    result._m32 = _m23;
    result._m33 = _m33;

    return result;
  }

  /**
   * transposes this matrix in place
   *
   * @return this matrix for chaining.
   * @see <a href="http://en.wikipedia.org/wiki/Transpose">wikipedia.org-Transpose</a>
   */
  public Matrix4 transposeLocal() {
    final double m01 = _m01;
    final double m02 = _m02;
    final double m03 = _m03;
    final double m12 = _m12;
    final double m13 = _m13;
    final double m23 = _m23;
    _m01 = _m10;
    _m02 = _m20;
    _m03 = _m30;
    _m12 = _m21;
    _m13 = _m31;
    _m23 = _m32;
    _m10 = m01;
    _m20 = m02;
    _m30 = m03;
    _m21 = m12;
    _m31 = m13;
    _m32 = m23;
    return this;
  }

  /**
   * @param store
   *          a matrix to store the result in. If store is null, a new matrix is created. Note that it
   *          IS safe for store == this.
   * @return a matrix that represents this matrix, inverted.
   * @throws ArithmeticException
   *           if this matrix can not be inverted.
   */
  @Override
  public Matrix4 invert(final Matrix4 store) {
    Matrix4 result = store;
    if (result == null) {
      result = new Matrix4();
    }

    final double dA0 = _m00 * _m11 - _m01 * _m10;
    final double dA1 = _m00 * _m12 - _m02 * _m10;
    final double dA2 = _m00 * _m13 - _m03 * _m10;
    final double dA3 = _m01 * _m12 - _m02 * _m11;
    final double dA4 = _m01 * _m13 - _m03 * _m11;
    final double dA5 = _m02 * _m13 - _m03 * _m12;
    final double dB0 = _m20 * _m31 - _m21 * _m30;
    final double dB1 = _m20 * _m32 - _m22 * _m30;
    final double dB2 = _m20 * _m33 - _m23 * _m30;
    final double dB3 = _m21 * _m32 - _m22 * _m31;
    final double dB4 = _m21 * _m33 - _m23 * _m31;
    final double dB5 = _m22 * _m33 - _m23 * _m32;
    final double det = dA0 * dB5 - dA1 * dB4 + dA2 * dB3 + dA3 * dB2 - dA4 * dB1 + dA5 * dB0;

    if (Math.abs(det) <= MathUtils.EPSILON) {
      throw new ArithmeticException("This matrix cannot be inverted");
    }

    final double temp00 = +_m11 * dB5 - _m12 * dB4 + _m13 * dB3;
    final double temp10 = -_m10 * dB5 + _m12 * dB2 - _m13 * dB1;
    final double temp20 = +_m10 * dB4 - _m11 * dB2 + _m13 * dB0;
    final double temp30 = -_m10 * dB3 + _m11 * dB1 - _m12 * dB0;
    final double temp01 = -_m01 * dB5 + _m02 * dB4 - _m03 * dB3;
    final double temp11 = +_m00 * dB5 - _m02 * dB2 + _m03 * dB1;
    final double temp21 = -_m00 * dB4 + _m01 * dB2 - _m03 * dB0;
    final double temp31 = +_m00 * dB3 - _m01 * dB1 + _m02 * dB0;
    final double temp02 = +_m31 * dA5 - _m32 * dA4 + _m33 * dA3;
    final double temp12 = -_m30 * dA5 + _m32 * dA2 - _m33 * dA1;
    final double temp22 = +_m30 * dA4 - _m31 * dA2 + _m33 * dA0;
    final double temp32 = -_m30 * dA3 + _m31 * dA1 - _m32 * dA0;
    final double temp03 = -_m21 * dA5 + _m22 * dA4 - _m23 * dA3;
    final double temp13 = +_m20 * dA5 - _m22 * dA2 + _m23 * dA1;
    final double temp23 = -_m20 * dA4 + _m21 * dA2 - _m23 * dA0;
    final double temp33 = +_m20 * dA3 - _m21 * dA1 + _m22 * dA0;

    result.set(temp00, temp01, temp02, temp03, temp10, temp11, temp12, temp13, temp20, temp21, temp22, temp23, temp30,
        temp31, temp32, temp33);
    result.multiplyLocal(1.0 / det);

    return result;
  }

  /**
   * inverts this matrix locally.
   *
   * @return this matrix inverted internally.
   * @throws ArithmeticException
   *           if this matrix can not be inverted.
   */
  public Matrix4 invertLocal() {
    return invert(this);
  }

  /**
   * @param store
   *          The matrix to store the result in. If null, a new matrix is created.
   * @return The adjugate, or classical adjoint, of this matrix
   * @see <a href="http://en.wikipedia.org/wiki/Adjugate_matrix">wikipedia.org-Adjugate_matrix</a>
   */
  @Override
  public Matrix4 adjugate(final Matrix4 store) {
    Matrix4 result = store;
    if (result == null) {
      result = new Matrix4();
    }

    final double dA0 = _m00 * _m11 - _m01 * _m10;
    final double dA1 = _m00 * _m12 - _m02 * _m10;
    final double dA2 = _m00 * _m13 - _m03 * _m10;
    final double dA3 = _m01 * _m12 - _m02 * _m11;
    final double dA4 = _m01 * _m13 - _m03 * _m11;
    final double dA5 = _m02 * _m13 - _m03 * _m12;
    final double dB0 = _m20 * _m31 - _m21 * _m30;
    final double dB1 = _m20 * _m32 - _m22 * _m30;
    final double dB2 = _m20 * _m33 - _m23 * _m30;
    final double dB3 = _m21 * _m32 - _m22 * _m31;
    final double dB4 = _m21 * _m33 - _m23 * _m31;
    final double dB5 = _m22 * _m33 - _m23 * _m32;

    final double temp00 = +_m11 * dB5 - _m12 * dB4 + _m13 * dB3;
    final double temp10 = -_m10 * dB5 + _m12 * dB2 - _m13 * dB1;
    final double temp20 = +_m10 * dB4 - _m11 * dB2 + _m13 * dB0;
    final double temp30 = -_m10 * dB3 + _m11 * dB1 - _m12 * dB0;
    final double temp01 = -_m01 * dB5 + _m02 * dB4 - _m03 * dB3;
    final double temp11 = +_m00 * dB5 - _m02 * dB2 + _m03 * dB1;
    final double temp21 = -_m00 * dB4 + _m01 * dB2 - _m03 * dB0;
    final double temp31 = +_m00 * dB3 - _m01 * dB1 + _m02 * dB0;
    final double temp02 = +_m31 * dA5 - _m32 * dA4 + _m33 * dA3;
    final double temp12 = -_m30 * dA5 + _m32 * dA2 - _m33 * dA1;
    final double temp22 = +_m30 * dA4 - _m31 * dA2 + _m33 * dA0;
    final double temp32 = -_m30 * dA3 + _m31 * dA1 - _m32 * dA0;
    final double temp03 = -_m21 * dA5 + _m22 * dA4 - _m23 * dA3;
    final double temp13 = +_m20 * dA5 - _m22 * dA2 + _m23 * dA1;
    final double temp23 = -_m20 * dA4 + _m21 * dA2 - _m23 * dA0;
    final double temp33 = +_m20 * dA3 - _m21 * dA1 + _m22 * dA0;

    return result.set(temp00, temp01, temp02, temp03, temp10, temp11, temp12, temp13, temp20, temp21, temp22, temp23,
        temp30, temp31, temp32, temp33);
  }

  /**
   * @return this matrix, modified to represent its adjugate, or classical adjoint
   * @see <a href="http://en.wikipedia.org/wiki/Adjugate_matrix">wikipedia.org-Adjugate_matrix</a>
   */
  public Matrix4 adjugateLocal() {
    return adjugate(this);
  }

  /**
   * @return the determinate of this matrix
   * @see <a href="http://en.wikipedia.org/wiki/Determinant">wikipedia.org-Determinant</a>
   */
  @Override
  public double determinant() {
    final double val1 = _m11 * _m22 * _m33 + _m12 * _m23 * _m31 + _m13 * _m21 * _m32 - //
        _m13 * _m22 * _m31 - _m12 * _m21 * _m33 - _m11 * _m23 * _m32;
    final double val2 = _m10 * _m22 * _m33 + _m12 * _m23 * _m30 + _m13 * _m20 * _m32 - //
        _m13 * _m22 * _m30 - _m12 * _m20 * _m33 - _m10 * _m23 * _m32;
    final double val3 = _m10 * _m21 * _m33 + _m11 * _m23 * _m30 + _m13 * _m20 * _m31 - //
        _m13 * _m21 * _m30 - _m11 * _m20 * _m33 - _m10 * _m23 * _m31;
    final double val4 = _m10 * _m21 * _m32 + _m11 * _m22 * _m30 + _m12 * _m20 * _m31 - //
        _m12 * _m21 * _m30 - _m11 * _m20 * _m32 - _m10 * _m22 * _m31;

    return _m00 * val1 - _m01 * val2 + _m02 * val3 - _m03 * val4;
  }

  /**
   * Multiplies the given vector by this matrix (v * M). If supplied, the result is stored into the
   * supplied "store" vector.
   *
   * @param vector
   *          the vector to multiply this matrix by.
   * @param store
   *          the vector to store the result in. If store is null, a new vector is created. Note that
   *          it IS safe for vector and store to be the same object.
   * @return the store vector, or a new vector if store is null.
   * @throws NullPointerException
   *           if vector is null
   */
  @Override
  public Vector4 applyPre(final ReadOnlyVector4 vector, Vector4 store) {
    if (store == null) {
      store = new Vector4();
    }

    final double x = vector.getX();
    final double y = vector.getY();
    final double z = vector.getZ();
    final double w = vector.getW();

    store.setX(_m00 * x + _m10 * y + _m20 * z + _m30 * w);
    store.setY(_m01 * x + _m11 * y + _m21 * z + _m31 * w);
    store.setZ(_m02 * x + _m12 * y + _m22 * z + _m32 * w);
    store.setW(_m03 * x + _m13 * y + _m23 * z + _m33 * w);

    return store;
  }

  /**
   * Multiplies the given vector by this matrix (M * v). If supplied, the result is stored into the
   * supplied "store" vector.
   *
   * @param vector
   *          the vector to multiply this matrix by.
   * @param store
   *          the vector to store the result in. If store is null, a new vector is created. Note that
   *          it IS safe for vector and store to be the same object.
   * @return the store vector, or a new vector if store is null.
   * @throws NullPointerException
   *           if vector is null
   */
  @Override
  public Vector4 applyPost(final ReadOnlyVector4 vector, Vector4 store) {
    if (store == null) {
      store = new Vector4();
    }

    final double x = vector.getX();
    final double y = vector.getY();
    final double z = vector.getZ();
    final double w = vector.getW();

    store.setX(_m00 * x + _m01 * y + _m02 * z + _m03 * w);
    store.setY(_m10 * x + _m11 * y + _m12 * z + _m13 * w);
    store.setZ(_m20 * x + _m21 * y + _m22 * z + _m23 * w);
    store.setW(_m30 * x + _m31 * y + _m32 * z + _m33 * w);

    return store;
  }

  /**
   * Multiplies the given point by this matrix (M * p). If supplied, the result is stored into the
   * supplied "store" vector.
   *
   * @param point
   *          the point to multiply against this matrix.
   * @param store
   *          the point to store the result in. If store is null, a new Vector3 object is created.
   *          Note that it IS safe for point and store to be the same object.
   * @return the store object, or a new Vector3 if store is null.
   * @throws NullPointerException
   *           if point is null
   */
  @Override
  public Vector3 applyPostPoint(final ReadOnlyVector3 point, Vector3 store) {
    if (store == null) {
      store = new Vector3();
    }

    final double x = point.getX();
    final double y = point.getY();
    final double z = point.getZ();

    store.setX(_m00 * x + _m01 * y + _m02 * z + _m03);
    store.setY(_m10 * x + _m11 * y + _m12 * z + _m13);
    store.setZ(_m20 * x + _m21 * y + _m22 * z + _m23);

    return store;
  }

  /**
   * Multiplies the given vector by this matrix (M * v). If supplied, the result is stored into the
   * supplied "store" vector.
   *
   * @param vector
   *          the vector to multiply this matrix by.
   * @param store
   *          the vector to store the result in. If store is null, a new vector is created. Note that
   *          it IS safe for vector and store to be the same object.
   * @return the store vector, or a new vector if store is null.
   * @throws NullPointerException
   *           if vector is null
   */
  @Override
  public Vector3 applyPostVector(final ReadOnlyVector3 vector, Vector3 store) {
    if (store == null) {
      store = new Vector3();
    }

    final double x = vector.getX();
    final double y = vector.getY();
    final double z = vector.getZ();

    store.setX(_m00 * x + _m01 * y + _m02 * z);
    store.setY(_m10 * x + _m11 * y + _m12 * z);
    store.setZ(_m20 * x + _m21 * y + _m22 * z);

    return store;
  }

  /**
   * Check a matrix... if it is null or its doubles are NaN or infinite, return false. Else return
   * true.
   *
   * @param matrix
   *          the vector to check
   * @return true or false as stated above.
   */
  public static boolean isFinite(final ReadOnlyMatrix4 matrix) {
    if (matrix == null) {
      return false;
    }

    return Double.isFinite(matrix.getM00()) //
        && Double.isFinite(matrix.getM01()) //
        && Double.isFinite(matrix.getM02()) //
        && Double.isFinite(matrix.getM03()) //
        && Double.isFinite(matrix.getM10()) //
        && Double.isFinite(matrix.getM11()) //
        && Double.isFinite(matrix.getM12()) //
        && Double.isFinite(matrix.getM13()) //
        && Double.isFinite(matrix.getM20()) //
        && Double.isFinite(matrix.getM21()) //
        && Double.isFinite(matrix.getM22()) //
        && Double.isFinite(matrix.getM23()) //
        && Double.isFinite(matrix.getM30()) //
        && Double.isFinite(matrix.getM31()) //
        && Double.isFinite(matrix.getM32()) //
        && Double.isFinite(matrix.getM33());
  }

  /**
   * @return true if this Matrix is orthonormal
   * @see <a href="http://en.wikipedia.org/wiki/Orthogonal_matrix">wikipedia.org-Orthogonal matrix</a>
   */
  public boolean isOrthonormal() {
    final double m00 = _m00;
    final double m01 = _m01;
    final double m02 = _m02;
    final double m03 = _m03;
    final double m10 = _m10;
    final double m11 = _m11;
    final double m12 = _m12;
    final double m13 = _m13;
    final double m20 = _m20;
    final double m21 = _m21;
    final double m22 = _m22;
    final double m23 = _m23;
    final double m30 = _m30;
    final double m31 = _m31;
    final double m32 = _m32;
    final double m33 = _m33;

    if (Math.abs(m00 * m00 + m01 * m01 + m02 * m02 + m03 * m03 - 1.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }
    if (Math.abs(m00 * m10 + m01 * m11 + m02 * m12 + m03 * m13 - 0.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }
    if (Math.abs(m00 * m20 + m01 * m21 + m02 * m22 + m03 * m23 - 0.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }
    if (Math.abs(m00 * m30 + m01 * m31 + m02 * m32 + m03 * m33 - 0.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }

    if (Math.abs(m10 * m00 + m11 * m01 + m12 * m02 + m13 * m03 - 0.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }
    if (Math.abs(m10 * m10 + m11 * m11 + m12 * m12 + m13 * m13 - 1.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }
    if (Math.abs(m10 * m20 + m11 * m21 + m12 * m22 + m13 * m23 - 0.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }
    if (Math.abs(m10 * m30 + m11 * m31 + m12 * m32 + m13 * m33 - 0.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }

    if (Math.abs(m20 * m00 + m21 * m01 + m22 * m02 + m23 * m03 - 0.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }
    if (Math.abs(m20 * m10 + m21 * m11 + m22 * m12 + m23 * m13 - 0.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }
    if (Math.abs(m20 * m20 + m21 * m21 + m22 * m22 + m23 * m23 - 1.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }
    if (Math.abs(m20 * m30 + m21 * m31 + m22 * m32 + m23 * m33 - 0.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }

    if (Math.abs(m30 * m00 + m31 * m01 + m32 * m02 + m33 * m03 - 0.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }
    if (Math.abs(m30 * m10 + m31 * m11 + m32 * m12 + m33 * m13 - 0.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }
    if (Math.abs(m30 * m20 + m31 * m21 + m32 * m22 + m33 * m23 - 0.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }
    if (Math.abs(m30 * m30 + m31 * m31 + m32 * m32 + m33 * m33 - 1.0) > MathUtils.ZERO_TOLERANCE) {
      return false;
    }

    return true;
  }

  /**
   * @return the string representation of this matrix.
   */
  @Override
  public String toString() {
    final StringBuffer result = new StringBuffer("com.ardor3d.math.Matrix4\n[\n");
    result.append(' ');
    result.append(_m00);
    result.append(' ');
    result.append(_m01);
    result.append(' ');
    result.append(_m02);
    result.append(' ');
    result.append(_m03);
    result.append(" \n");

    result.append(' ');
    result.append(_m10);
    result.append(' ');
    result.append(_m11);
    result.append(' ');
    result.append(_m12);
    result.append(' ');
    result.append(_m13);
    result.append(" \n");

    result.append(' ');
    result.append(_m20);
    result.append(' ');
    result.append(_m21);
    result.append(' ');
    result.append(_m22);
    result.append(' ');
    result.append(_m23);
    result.append(" \n");

    result.append(' ');
    result.append(_m30);
    result.append(' ');
    result.append(_m31);
    result.append(' ');
    result.append(_m32);
    result.append(' ');
    result.append(_m33);
    result.append(" \n");

    result.append(']');
    return result.toString();
  }

  /**
   * @return returns a unique code for this matrix object based on its values. If two matrices are
   *         numerically equal, they will return the same hash code value.
   */
  @Override
  public int hashCode() {
    int result = 17;

    result = HashUtil.hash(result, _m00);
    result = HashUtil.hash(result, _m01);
    result = HashUtil.hash(result, _m02);
    result = HashUtil.hash(result, _m03);

    result = HashUtil.hash(result, _m10);
    result = HashUtil.hash(result, _m11);
    result = HashUtil.hash(result, _m12);
    result = HashUtil.hash(result, _m13);

    result = HashUtil.hash(result, _m20);
    result = HashUtil.hash(result, _m21);
    result = HashUtil.hash(result, _m22);
    result = HashUtil.hash(result, _m23);

    result = HashUtil.hash(result, _m30);
    result = HashUtil.hash(result, _m31);
    result = HashUtil.hash(result, _m32);
    result = HashUtil.hash(result, _m33);

    return result;
  }

  /**
   * @param o
   *          the object to compare for equality
   * @return true if this matrix and the provided matrix have the double values that are within the
   *         MathUtils.ZERO_TOLERANCE.
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReadOnlyMatrix4)) {
      return false;
    }
    final ReadOnlyMatrix4 comp = (ReadOnlyMatrix4) o;
    if (Math.abs(getM00() - comp.getM00()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM01() - comp.getM01()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM02() - comp.getM02()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM03() - comp.getM03()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM10() - comp.getM10()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM11() - comp.getM11()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM12() - comp.getM12()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM13() - comp.getM13()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM20() - comp.getM20()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM21() - comp.getM21()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM22() - comp.getM22()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM23() - comp.getM23()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM30() - comp.getM30()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM31() - comp.getM31()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM32() - comp.getM32()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    } else if (Math.abs(getM33() - comp.getM33()) > Matrix4.ALLOWED_DEVIANCE) {
      return false;
    }

    return true;
  }

  /**
   * @param o
   *          the object to compare for equality
   * @return true if this matrix and the provided matrix have the exact same double values.
   */
  public boolean strictEquals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReadOnlyMatrix4)) {
      return false;
    }
    final ReadOnlyMatrix4 comp = (ReadOnlyMatrix4) o;
    return EqualsUtil.areEqual(getM00(), comp.getM00()) //
        && EqualsUtil.areEqual(getM01(), comp.getM01()) //
        && EqualsUtil.areEqual(getM02(), comp.getM02()) //
        && EqualsUtil.areEqual(getM03(), comp.getM03()) //
        && EqualsUtil.areEqual(getM10(), comp.getM10()) //
        && EqualsUtil.areEqual(getM11(), comp.getM11()) //
        && EqualsUtil.areEqual(getM12(), comp.getM12()) //
        && EqualsUtil.areEqual(getM13(), comp.getM13()) //
        && EqualsUtil.areEqual(getM20(), comp.getM20()) //
        && EqualsUtil.areEqual(getM21(), comp.getM21()) //
        && EqualsUtil.areEqual(getM22(), comp.getM22()) //
        && EqualsUtil.areEqual(getM23(), comp.getM23()) //
        && EqualsUtil.areEqual(getM30(), comp.getM30()) //
        && EqualsUtil.areEqual(getM31(), comp.getM31()) //
        && EqualsUtil.areEqual(getM32(), comp.getM32()) //
        && EqualsUtil.areEqual(getM33(), comp.getM33());
  }

  // /////////////////
  // Method for Cloneable
  // /////////////////

  @Override
  public Matrix4 clone() {
    return new Matrix4(this);
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends Matrix4> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_m00, "m00", 1.0);
    capsule.write(_m01, "m01", 0.0);
    capsule.write(_m02, "m02", 0.0);
    capsule.write(_m03, "m03", 0.0);
    capsule.write(_m10, "m10", 0.0);
    capsule.write(_m11, "m11", 1.0);
    capsule.write(_m12, "m12", 0.0);
    capsule.write(_m13, "m13", 0.0);
    capsule.write(_m20, "m20", 0.0);
    capsule.write(_m21, "m21", 0.0);
    capsule.write(_m22, "m22", 1.0);
    capsule.write(_m23, "m23", 0.0);
    capsule.write(_m30, "m30", 0.0);
    capsule.write(_m31, "m31", 0.0);
    capsule.write(_m32, "m32", 0.0);
    capsule.write(_m33, "m33", 1.0);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _m00 = capsule.readDouble("m00", 1.0);
    _m01 = capsule.readDouble("m01", 0.0);
    _m02 = capsule.readDouble("m02", 0.0);
    _m03 = capsule.readDouble("m03", 0.0);
    _m10 = capsule.readDouble("m10", 0.0);
    _m11 = capsule.readDouble("m11", 1.0);
    _m12 = capsule.readDouble("m12", 0.0);
    _m13 = capsule.readDouble("m13", 0.0);
    _m20 = capsule.readDouble("m20", 0.0);
    _m21 = capsule.readDouble("m21", 0.0);
    _m22 = capsule.readDouble("m22", 1.0);
    _m23 = capsule.readDouble("m23", 0.0);
    _m30 = capsule.readDouble("m30", 0.0);
    _m31 = capsule.readDouble("m31", 0.0);
    _m32 = capsule.readDouble("m32", 0.0);
    _m33 = capsule.readDouble("m33", 1.0);
  }

  // /////////////////
  // Methods for Externalizable
  // /////////////////

  /**
   * Used with serialization. Not to be called manually.
   *
   * @param in
   *          ObjectInput
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    _m00 = in.readDouble();
    _m01 = in.readDouble();
    _m02 = in.readDouble();
    _m03 = in.readDouble();
    _m10 = in.readDouble();
    _m11 = in.readDouble();
    _m12 = in.readDouble();
    _m13 = in.readDouble();
    _m20 = in.readDouble();
    _m21 = in.readDouble();
    _m22 = in.readDouble();
    _m23 = in.readDouble();
    _m30 = in.readDouble();
    _m31 = in.readDouble();
    _m32 = in.readDouble();
    _m33 = in.readDouble();
  }

  /**
   * Used with serialization. Not to be called manually.
   *
   * @param out
   *          ObjectOutput
   * @throws IOException
   */
  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeDouble(_m00);
    out.writeDouble(_m01);
    out.writeDouble(_m02);
    out.writeDouble(_m03);
    out.writeDouble(_m10);
    out.writeDouble(_m11);
    out.writeDouble(_m12);
    out.writeDouble(_m13);
    out.writeDouble(_m20);
    out.writeDouble(_m21);
    out.writeDouble(_m22);
    out.writeDouble(_m23);
    out.writeDouble(_m30);
    out.writeDouble(_m31);
    out.writeDouble(_m32);
    out.writeDouble(_m33);
  }

  // /////////////////
  // Methods for creating temp variables (pooling)
  // /////////////////

  /**
   * @return An instance of Matrix4 that is intended for temporary use in calculations and so forth.
   *         Multiple calls to the method should return instances of this class that are not currently
   *         in use.
   */
  public final static Matrix4 fetchTempInstance() {
    if (MathConstants.useMathPools) {
      return Matrix4.MAT_POOL.fetch().setIdentity();
    } else {
      return new Matrix4();
    }
  }

  /**
   * Releases a Matrix4 back to be used by a future call to fetchTempInstance. TAKE CARE: this Matrix4
   * object should no longer have other classes referencing it or "Bad Things" will happen.
   *
   * @param mat
   *          the Matrix4 to release.
   */
  public final static void releaseTempInstance(final Matrix4 mat) {
    if (MathConstants.useMathPools) {
      Matrix4.MAT_POOL.release(mat);
    }
  }
}
