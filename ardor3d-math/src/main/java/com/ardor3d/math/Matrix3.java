/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
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
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Matrix3 represents a double precision 3x3 matrix.
 * 
 * Note: some algorithms in this class were ported from Eberly, Wolfram, Game Gems and others to Java.
 */
public class Matrix3 implements Cloneable, Savable, Externalizable, ReadOnlyMatrix3, Poolable {

    /** Used with equals method to determine if two Matrix3 objects are close enough to be considered equal. */
    public static final double ALLOWED_DEVIANCE = 0.00000001;

    private static final long serialVersionUID = 1L;

    private static final ObjectPool<Matrix3> MAT_POOL = ObjectPool.create(Matrix3.class, MathConstants.maxMathPoolSize);

    /**
     * <pre>
     * 1, 0, 0
     * 0, 1, 0
     * 0, 0, 1
     * </pre>
     */
    public final static ReadOnlyMatrix3 IDENTITY = new Matrix3(1, 0, 0, 0, 1, 0, 0, 0, 1);

    protected double _m00, _m01, _m02, //
            _m10, _m11, _m12, //
            _m20, _m21, _m22;

    /**
     * Constructs a new, mutable matrix set to identity.
     */
    public Matrix3() {
        this(Matrix3.IDENTITY);
    }

    /**
     * Constructs a new, mutable matrix using the given matrix values (names are mRC = m[ROW][COL])
     * 
     * @param m00
     * @param m01
     * @param m02
     * @param m10
     * @param m11
     * @param m12
     * @param m20
     * @param m21
     * @param m22
     */
    public Matrix3(final double m00, final double m01, final double m02, final double m10, final double m11,
            final double m12, final double m20, final double m21, final double m22) {

        _m00 = m00;
        _m01 = m01;
        _m02 = m02;
        _m10 = m10;
        _m11 = m11;
        _m12 = m12;
        _m20 = m20;
        _m21 = m21;
        _m22 = m22;
    }

    /**
     * Constructs a new, mutable matrix using the values from the given matrix
     * 
     * @param source
     */
    public Matrix3(final ReadOnlyMatrix3 source) {
        set(source);
    }

    /**
     * @param row
     * @param column
     * @return the value stored in this matrix at row, column.
     * @throws IllegalArgumentException
     *             if row and column are not in bounds [0, 2]
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
     *             if row and column are not in bounds [0, 2]
     */
    @Override
    public float getValuef(final int row, final int column) {
        return (float) getValue(row, column);
    }

    @Override
    public double getM00() {
        return _m00;
    }

    @Override
    public double getM01() {
        return _m01;
    }

    @Override
    public double getM02() {
        return _m02;
    }

    @Override
    public double getM10() {
        return _m10;
    }

    @Override
    public double getM11() {
        return _m11;
    }

    @Override
    public double getM12() {
        return _m12;
    }

    @Override
    public double getM20() {
        return _m20;
    }

    @Override
    public double getM21() {
        return _m21;
    }

    @Override
    public double getM22() {
        return _m22;
    }

    public void setM00(final double m00) {
        _m00 = m00;
    }

    public void setM01(final double m01) {
        _m01 = m01;
    }

    public void setM02(final double m02) {
        _m02 = m02;
    }

    public void setM10(final double m10) {
        _m10 = m10;
    }

    public void setM11(final double m11) {
        _m11 = m11;
    }

    public void setM12(final double m12) {
        _m12 = m12;
    }

    public void setM20(final double m20) {
        _m20 = m20;
    }

    public void setM21(final double m21) {
        _m21 = m21;
    }

    public void setM22(final double m22) {
        _m22 = m22;
    }

    /**
     * Same as set(IDENTITY)
     * 
     * @return this matrix for chaining
     */
    public Matrix3 setIdentity() {
        return set(Matrix3.IDENTITY);
    }

    /**
     * @return true if this matrix equals the 3x3 identity matrix
     */
    @Override
    public boolean isIdentity() {
        return strictEquals(Matrix3.IDENTITY);
    }

    /**
     * Sets the value of this matrix at row, column to the given value.
     * 
     * @param row
     * @param column
     * @param value
     * @return this matrix for chaining
     * @throws IllegalArgumentException
     *             if row and column are not in bounds [0, 2]
     */
    public Matrix3 setValue(final int row, final int column, final double value) {
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
     * @param m10
     * @param m11
     * @param m12
     * @param m20
     * @param m21
     * @param m22
     * @return this matrix for chaining
     */
    public Matrix3 set(final double m00, final double m01, final double m02, final double m10, final double m11,
            final double m12, final double m20, final double m21, final double m22) {

        _m00 = m00;
        _m01 = m01;
        _m02 = m02;
        _m10 = m10;
        _m11 = m11;
        _m12 = m12;
        _m20 = m20;
        _m21 = m21;
        _m22 = m22;

        return this;
    }

    /**
     * Sets the values of this matrix to the values of the provided source matrix.
     * 
     * @param source
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Matrix3 set(final ReadOnlyMatrix3 source) {
        _m00 = source.getM00();
        _m10 = source.getM10();
        _m20 = source.getM20();

        _m01 = source.getM01();
        _m11 = source.getM11();
        _m21 = source.getM21();

        _m02 = source.getM02();
        _m12 = source.getM12();
        _m22 = source.getM22();

        return this;
    }

    /**
     * Sets the values of this matrix to the rotational value of the given quaternion.
     * 
     * @param quaternion
     * @return this matrix for chaining
     */
    public Matrix3 set(final ReadOnlyQuaternion quaternion) {
        return quaternion.toRotationMatrix(this);
    }

    /**
     * @param source
     *            the buffer to read our matrix data from.
     * @return this matrix for chaining.
     */
    public Matrix3 fromDoubleBuffer(final DoubleBuffer source) {
        return fromDoubleBuffer(source, true);
    }

    /**
     * @param source
     *            the buffer to read our matrix data from.
     * @param rowMajor
     *            if true, data is stored row by row. Otherwise it is stored column by column.
     * @return this matrix for chaining.
     */
    public Matrix3 fromDoubleBuffer(final DoubleBuffer source, final boolean rowMajor) {
        if (rowMajor) {
            _m00 = source.get();
            _m01 = source.get();
            _m02 = source.get();
            _m10 = source.get();
            _m11 = source.get();
            _m12 = source.get();
            _m20 = source.get();
            _m21 = source.get();
            _m22 = source.get();
        } else {
            _m00 = source.get();
            _m10 = source.get();
            _m20 = source.get();
            _m01 = source.get();
            _m11 = source.get();
            _m21 = source.get();
            _m02 = source.get();
            _m12 = source.get();
            _m22 = source.get();
        }

        return this;
    }

    /**
     * Note: data is cast to floats.
     * 
     * @param store
     *            the buffer to read our matrix data from.
     * @return this matrix for chaining.
     */
    public Matrix3 fromFloatBuffer(final FloatBuffer source) {
        return fromFloatBuffer(source, true);
    }

    /**
     * Note: data is cast to floats.
     * 
     * @param store
     *            the buffer to read our matrix data from.
     * @param rowMajor
     *            if true, data is stored row by row. Otherwise it is stored column by column.
     * @return this matrix for chaining.
     */
    public Matrix3 fromFloatBuffer(final FloatBuffer source, final boolean rowMajor) {
        if (rowMajor) {
            _m00 = source.get();
            _m01 = source.get();
            _m02 = source.get();
            _m10 = source.get();
            _m11 = source.get();
            _m12 = source.get();
            _m20 = source.get();
            _m21 = source.get();
            _m22 = source.get();
        } else {
            _m00 = source.get();
            _m10 = source.get();
            _m20 = source.get();
            _m01 = source.get();
            _m11 = source.get();
            _m21 = source.get();
            _m02 = source.get();
            _m12 = source.get();
            _m22 = source.get();
        }

        return this;
    }

    /**
     * Sets the values of this matrix to the values of the provided double array.
     * 
     * @param source
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if source is null.
     * @throws ArrayIndexOutOfBoundsException
     *             if source array has a length less than 9.
     */
    public Matrix3 fromArray(final double[] source) {
        return fromArray(source, true);
    }

    /**
     * Sets the values of this matrix to the values of the provided double array.
     * 
     * @param source
     * @param rowMajor
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if source is null.
     * @throws ArrayIndexOutOfBoundsException
     *             if source array has a length less than 9.
     */
    public Matrix3 fromArray(final double[] source, final boolean rowMajor) {
        if (rowMajor) {
            _m00 = source[0];
            _m01 = source[1];
            _m02 = source[2];
            _m10 = source[3];
            _m11 = source[4];
            _m12 = source[5];
            _m20 = source[6];
            _m21 = source[7];
            _m22 = source[8];
        } else {
            _m00 = source[0];
            _m10 = source[1];
            _m20 = source[2];
            _m01 = source[3];
            _m11 = source[4];
            _m21 = source[5];
            _m02 = source[6];
            _m12 = source[7];
            _m22 = source[8];
        }
        return this;
    }

    /**
     * Replaces a column in this matrix with the values of the given vector.
     * 
     * @param columnIndex
     * @param columnData
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if columnData is null.
     * @throws IllegalArgumentException
     *             if columnIndex is not in [0, 2]
     */
    public Matrix3 setColumn(final int columnIndex, final ReadOnlyVector3 columnData) {
        switch (columnIndex) {
            case 0:
                _m00 = columnData.getX();
                _m10 = columnData.getY();
                _m20 = columnData.getZ();
                break;
            case 1:
                _m01 = columnData.getX();
                _m11 = columnData.getY();
                _m21 = columnData.getZ();
                break;
            case 2:
                _m02 = columnData.getX();
                _m12 = columnData.getY();
                _m22 = columnData.getZ();
                break;
            default:
                throw new IllegalArgumentException("Bad columnIndex: " + columnIndex);
        }
        return this;
    }

    /**
     * Replaces a row in this matrix with the values of the given vector.
     * 
     * @param rowIndex
     * @param rowData
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if rowData is null.
     * @throws IllegalArgumentException
     *             if rowIndex is not in [0, 2]
     */
    public Matrix3 setRow(final int rowIndex, final ReadOnlyVector3 rowData) {
        switch (rowIndex) {
            case 0:
                _m00 = rowData.getX();
                _m01 = rowData.getY();
                _m02 = rowData.getZ();
                break;
            case 1:
                _m10 = rowData.getX();
                _m11 = rowData.getY();
                _m12 = rowData.getZ();
                break;
            case 2:
                _m20 = rowData.getX();
                _m21 = rowData.getY();
                _m22 = rowData.getZ();
                break;
            default:
                throw new IllegalArgumentException("Bad rowIndex: " + rowIndex);
        }
        return this;
    }

    /**
     * Set the values of this matrix from the axes (columns) provided.
     * 
     * @param uAxis
     * @param vAxis
     * @param wAxis
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if any of the axes are null.
     */
    public Matrix3 fromAxes(final ReadOnlyVector3 uAxis, final ReadOnlyVector3 vAxis, final ReadOnlyVector3 wAxis) {
        setColumn(0, uAxis);
        setColumn(1, vAxis);
        setColumn(2, wAxis);
        return this;
    }

    /**
     * Sets this matrix to the rotation indicated by the given angle and axis of rotation. Note: This method creates an
     * object, so use fromAngleNormalAxis when possible, particularly if your axis is already normalized.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation.
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if axis is null.
     */
    public Matrix3 fromAngleAxis(final double angle, final ReadOnlyVector3 axis) {
        final Vector3 normAxis = Vector3.fetchTempInstance();
        axis.normalize(normAxis);
        fromAngleNormalAxis(angle, normAxis);
        Vector3.releaseTempInstance(normAxis);
        return this;
    }

    /**
     * Sets this matrix to the rotation indicated by the given angle and a unit-length axis of rotation.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation (already normalized).
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if axis is null.
     */
    public Matrix3 fromAngleNormalAxis(final double angle, final ReadOnlyVector3 axis) {
        final double fCos = MathUtils.cos(angle);
        final double fSin = MathUtils.sin(angle);
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

    /**
     * XXX: Need to redo this again... or at least correct the terms. YRP are arbitrary terms, based on a specific frame
     * of axis.
     * 
     * Updates this matrix from the given Euler rotation angles (y,r,p). Note that we are applying in order: roll,
     * pitch, yaw but we've ordered them in x, y, and z for convenience. See:
     * http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToMatrix/index.htm
     * 
     * @param yaw
     *            the Euler yaw of rotation (in radians). (aka Bank, often rot around x)
     * @param roll
     *            the Euler roll of rotation (in radians). (aka Heading, often rot around y)
     * @param pitch
     *            the Euler pitch of rotation (in radians). (aka Attitude, often rot around z)
     * @return this matrix for chaining
     */
    public Matrix3 fromAngles(final double yaw, final double roll, final double pitch) {
        final double ch = Math.cos(roll);
        final double sh = Math.sin(roll);
        final double cp = Math.cos(pitch);
        final double sp = Math.sin(pitch);
        final double cy = Math.cos(yaw);
        final double sy = Math.sin(yaw);

        _m00 = ch * cp;
        _m01 = sh * sy - ch * sp * cy;
        _m02 = ch * sp * sy + sh * cy;
        _m10 = sp;
        _m11 = cp * cy;
        _m12 = -cp * sy;
        _m20 = -sh * cp;
        _m21 = sh * sp * cy + ch * sy;
        _m22 = -sh * sp * sy + ch * cy;
        return this;
    }

    public Matrix3 applyRotation(final double angle, final double x, final double y, final double z) {
        final double m00 = _m00, m01 = _m01, m02 = _m02, //
        m10 = _m10, m11 = _m11, m12 = _m12, //
        m20 = _m20, m21 = _m21, m22 = _m22;

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

        _m10 = m10 * r00 + m11 * r10 + m12 * r20;
        _m11 = m10 * r01 + m11 * r11 + m12 * r21;
        _m12 = m10 * r02 + m11 * r12 + m12 * r22;

        _m20 = m20 * r00 + m21 * r10 + m22 * r20;
        _m21 = m20 * r01 + m21 * r11 + m22 * r21;
        _m22 = m20 * r02 + m21 * r12 + m22 * r22;

        return this;
    }

    /**
     * Apply rotation around X (Mrx * this)
     * 
     * @param angle
     * @return
     */
    public Matrix3 applyRotationX(final double angle) {
        final double m01 = _m01, m02 = _m02, //
        m11 = _m11, m12 = _m12, //
        m21 = _m21, m22 = _m22;

        final double cosAngle = Math.cos(angle);
        final double sinAngle = Math.sin(angle);

        _m01 = m01 * cosAngle + m02 * sinAngle;
        _m02 = m02 * cosAngle - m01 * sinAngle;

        _m11 = m11 * cosAngle + m12 * sinAngle;
        _m12 = m12 * cosAngle - m11 * sinAngle;

        _m21 = m21 * cosAngle + m22 * sinAngle;
        _m22 = m22 * cosAngle - m21 * sinAngle;

        return this;
    }

    /**
     * Apply rotation around Y (Mry * this)
     * 
     * @param angle
     * @return
     */
    public Matrix3 applyRotationY(final double angle) {
        final double m00 = _m00, m02 = _m02, //
        m10 = _m10, m12 = _m12, //
        m20 = _m20, m22 = _m22;

        final double cosAngle = Math.cos(angle);
        final double sinAngle = Math.sin(angle);

        _m00 = m00 * cosAngle - m02 * sinAngle;
        _m02 = m00 * sinAngle + m02 * cosAngle;

        _m10 = m10 * cosAngle - m12 * sinAngle;
        _m12 = m10 * sinAngle + m12 * cosAngle;

        _m20 = m20 * cosAngle - m22 * sinAngle;
        _m22 = m20 * sinAngle + m22 * cosAngle;

        return this;
    }

    /**
     * Apply rotation around Z (Mrz * this)
     * 
     * @param angle
     * @return
     */
    public Matrix3 applyRotationZ(final double angle) {
        final double m00 = _m00, m01 = _m01, //
        m10 = _m10, m11 = _m11, //
        m20 = _m20, m21 = _m21;

        final double cosAngle = Math.cos(angle);
        final double sinAngle = Math.sin(angle);

        _m00 = m00 * cosAngle + m01 * sinAngle;
        _m01 = m01 * cosAngle - m00 * sinAngle;

        _m10 = m10 * cosAngle + m11 * sinAngle;
        _m11 = m11 * cosAngle - m10 * sinAngle;

        _m20 = m20 * cosAngle + m21 * sinAngle;
        _m21 = m21 * cosAngle - m20 * sinAngle;

        return this;
    }

    /**
     * @param index
     * @param store
     *            the vector object to store the result in. if null, a new one is created.
     * @return the column specified by the index.
     * @throws IllegalArgumentException
     *             if index is not in bounds [0, 2]
     */
    @Override
    public Vector3 getColumn(final int index, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        switch (index) {
            case 0:
                result.setX(_m00);
                result.setY(_m10);
                result.setZ(_m20);
                break;
            case 1:
                result.setX(_m01);
                result.setY(_m11);
                result.setZ(_m21);
                break;
            case 2:
                result.setX(_m02);
                result.setY(_m12);
                result.setZ(_m22);
                break;
            default:
                throw new IllegalArgumentException("invalid column index: " + index);
        }

        return result;
    }

    /**
     * @param index
     * @param store
     *            the vector object to store the result in. if null, a new one is created.
     * @return the row specified by the index.
     * @throws IllegalArgumentException
     *             if index is not in bounds [0, 2]
     */
    @Override
    public Vector3 getRow(final int index, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }
        switch (index) {
            case 0:
                result.setX(_m00);
                result.setY(_m01);
                result.setZ(_m02);
                break;
            case 1:
                result.setX(_m10);
                result.setY(_m11);
                result.setZ(_m12);
                break;
            case 2:
                result.setX(_m20);
                result.setY(_m21);
                result.setZ(_m22);
                break;
            default:
                throw new IllegalArgumentException("invalid row index: " + index);
        }
        return result;
    }

    /**
     * @param store
     *            the buffer to store our matrix data in. Must not be null. Data is entered starting at current buffer
     *            position.
     * @return matrix data as a DoubleBuffer in row major order. The position is at the end of the inserted data.
     * @throws NullPointerException
     *             if store is null.
     * @throws BufferOverflowException
     *             if there is not enough room left in the buffer to write all 9 values.
     */
    @Override
    public DoubleBuffer toDoubleBuffer(final DoubleBuffer store) {
        return toDoubleBuffer(store, true);
    }

    /**
     * @param store
     *            the buffer to store our matrix data in. Must not be null. Data is entered starting at current buffer
     *            position.
     * @param rowMajor
     *            if true, data is stored row by row. Otherwise it is stored column by column.
     * @return matrix data as a DoubleBuffer in the specified order. The position is at the end of the inserted data.
     * @throws NullPointerException
     *             if store is null.
     * @throws BufferOverflowException
     *             if there is not enough room left in the buffer to write all 9 values.
     */
    @Override
    public DoubleBuffer toDoubleBuffer(final DoubleBuffer store, final boolean rowMajor) {
        if (rowMajor) {
            store.put(_m00);
            store.put(_m01);
            store.put(_m02);
            store.put(_m10);
            store.put(_m11);
            store.put(_m12);
            store.put(_m20);
            store.put(_m21);
            store.put(_m22);
        } else {
            store.put(_m00);
            store.put(_m10);
            store.put(_m20);
            store.put(_m01);
            store.put(_m11);
            store.put(_m21);
            store.put(_m02);
            store.put(_m12);
            store.put(_m22);
        }

        return store;
    }

    /**
     * Note: data is cast to floats.
     * 
     * @param store
     *            the buffer to store our matrix data in. Must not be null. Data is entered starting at current buffer
     *            position.
     * @return matrix data as a FloatBuffer in row major order. The position is at the end of the inserted data.
     * @throws NullPointerException
     *             if store is null.
     * @throws BufferOverflowException
     *             if there is not enough room left in the buffer to write all 9 values.
     */
    @Override
    public FloatBuffer toFloatBuffer(final FloatBuffer store) {
        return toFloatBuffer(store, true);
    }

    /**
     * Note: data is cast to floats.
     * 
     * @param store
     *            the buffer to store our matrix data in. Must not be null. Data is entered starting at current buffer
     *            position.
     * @param rowMajor
     *            if true, data is stored row by row. Otherwise it is stored column by column.
     * @return matrix data as a FloatBuffer in the specified order. The position is at the end of the inserted data.
     * @throws NullPointerException
     *             if store is null.
     * @throws BufferOverflowException
     *             if there is not enough room left in the buffer to write all 9 values.
     */
    @Override
    public FloatBuffer toFloatBuffer(final FloatBuffer store, final boolean rowMajor) {
        if (rowMajor) {
            store.put((float) _m00);
            store.put((float) _m01);
            store.put((float) _m02);
            store.put((float) _m10);
            store.put((float) _m11);
            store.put((float) _m12);
            store.put((float) _m20);
            store.put((float) _m21);
            store.put((float) _m22);
        } else {
            store.put((float) _m00);
            store.put((float) _m10);
            store.put((float) _m20);
            store.put((float) _m01);
            store.put((float) _m11);
            store.put((float) _m21);
            store.put((float) _m02);
            store.put((float) _m12);
            store.put((float) _m22);
        }

        return store;
    }

    /**
     * @param store
     *            the double array to store our matrix data in. If null, a new array is created.
     * @return matrix data as a double array in row major order.
     * @throws IllegalArgumentException
     *             if the store is non-null and has a length < 9
     */
    @Override
    public double[] toArray(final double[] store) {
        return toArray(store, true);
    }

    /**
     * @param store
     *            the double array to store our matrix data in. If null, a new array is created.
     * @param rowMajor
     *            if true, data is stored row by row. Otherwise it is stored column by column.
     * @return matrix data as a double array in the specified order.
     * @throws IllegalArgumentException
     *             if the store is non-null and has a length < 9
     */
    @Override
    public double[] toArray(final double[] store, final boolean rowMajor) {
        double[] result = store;
        if (result == null) {
            result = new double[9];
        } else if (result.length < 9) {
            throw new IllegalArgumentException("store must be at least length 9.");
        }

        if (rowMajor) {
            result[0] = _m00;
            result[1] = _m01;
            result[2] = _m02;
            result[3] = _m10;
            result[4] = _m11;
            result[5] = _m12;
            result[6] = _m20;
            result[7] = _m21;
            result[8] = _m22;
        } else {
            result[0] = _m00;
            result[1] = _m10;
            result[2] = _m20;
            result[3] = _m01;
            result[4] = _m11;
            result[5] = _m21;
            result[6] = _m02;
            result[7] = _m12;
            result[8] = _m22;
        }

        return result;
    }

    /**
     * converts this matrix to Euler rotation angles (yaw, roll, pitch). See
     * http://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToEuler/index.htm
     * 
     * @param store
     *            the double[] array to store the computed angles in. If null, a new double[] will be created
     * @return the double[] array.
     * @throws IllegalArgumentException
     *             if non-null store is not at least length 3
     * @see #fromAngles(double, double, double)
     */
    @Override
    public double[] toAngles(final double[] store) {
        double[] result = store;
        if (result == null) {
            result = new double[3];
        } else if (result.length < 3) {
            throw new IllegalArgumentException("store array must have at least three elements");
        }

        double heading, attitude, bank;
        if (_m10 > 1 - MathUtils.ZERO_TOLERANCE) { // singularity at north pole
            heading = Math.atan2(_m02, _m22);
            attitude = Math.PI / 2;
            bank = 0;
        } else if (_m10 < -1 + MathUtils.ZERO_TOLERANCE) { // singularity at south pole
            heading = Math.atan2(_m02, _m22);
            attitude = -Math.PI / 2;
            bank = 0;
        } else {
            heading = Math.atan2(-_m20, _m00);
            bank = Math.atan2(-_m12, _m11);
            attitude = Math.asin(_m10);
        }
        result[0] = bank;
        result[1] = heading;
        result[2] = attitude;

        return result;
    }

    /**
     * @param matrix
     * @return This matrix for chaining, modified internally to reflect multiplication against the given matrix
     * @throws NullPointerException
     *             if matrix is null
     */
    public Matrix3 multiplyLocal(final ReadOnlyMatrix3 matrix) {
        return multiply(matrix, this);
    }

    /**
     * @param matrix
     * @param store
     *            a matrix to store the result in. if null, a new matrix is created. It is safe for the given matrix and
     *            this parameter to be the same object.
     * @return this matrix multiplied by the given matrix.
     * @throws NullPointerException
     *             if matrix is null.
     */
    @Override
    public Matrix3 multiply(final ReadOnlyMatrix3 matrix, final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }
        final double temp00 = _m00 * matrix.getM00() + _m01 * matrix.getM10() + _m02 * matrix.getM20();
        final double temp01 = _m00 * matrix.getM01() + _m01 * matrix.getM11() + _m02 * matrix.getM21();
        final double temp02 = _m00 * matrix.getM02() + _m01 * matrix.getM12() + _m02 * matrix.getM22();
        final double temp10 = _m10 * matrix.getM00() + _m11 * matrix.getM10() + _m12 * matrix.getM20();
        final double temp11 = _m10 * matrix.getM01() + _m11 * matrix.getM11() + _m12 * matrix.getM21();
        final double temp12 = _m10 * matrix.getM02() + _m11 * matrix.getM12() + _m12 * matrix.getM22();
        final double temp20 = _m20 * matrix.getM00() + _m21 * matrix.getM10() + _m22 * matrix.getM20();
        final double temp21 = _m20 * matrix.getM01() + _m21 * matrix.getM11() + _m22 * matrix.getM21();
        final double temp22 = _m20 * matrix.getM02() + _m21 * matrix.getM12() + _m22 * matrix.getM22();

        result.set(temp00, temp01, temp02, temp10, temp11, temp12, temp20, temp21, temp22);

        return result;
    }

    /**
     * Multiplies this matrix by the diagonal matrix formed by the given vector (v^D * M). If supplied, the result is
     * stored into the supplied "store" matrix.
     * 
     * @param vec
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created. Note that it IS safe for
     *            vec and store to be the same object.
     * @return the store matrix, or a new matrix if store is null.
     * @throws NullPointerException
     *             if vec is null
     */
    @Override
    public Matrix3 multiplyDiagonalPre(final ReadOnlyVector3 vec, final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        result.set( //
                vec.getX() * _m00, vec.getX() * _m01, vec.getX() * _m02, //
                vec.getY() * _m10, vec.getY() * _m11, vec.getY() * _m12, //
                vec.getZ() * _m20, vec.getZ() * _m21, vec.getZ() * _m22);

        return result;
    }

    /**
     * Multiplies this matrix by the diagonal matrix formed by the given vector (M * v^D). If supplied, the result is
     * stored into the supplied "store" matrix.
     * 
     * @param vec
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created. Note that it IS safe for
     *            vec and store to be the same object.
     * @return the store matrix, or a new matrix if store is null.
     * @throws NullPointerException
     *             if vec is null
     */
    @Override
    public Matrix3 multiplyDiagonalPost(final ReadOnlyVector3 vec, final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        result.set( //
                vec.getX() * _m00, vec.getY() * _m01, vec.getZ() * _m02, //
                vec.getX() * _m10, vec.getY() * _m11, vec.getZ() * _m12, //
                vec.getX() * _m20, vec.getY() * _m21, vec.getZ() * _m22);

        return result;
    }

    /**
     * Internally scales all values of this matrix by the given scalar.
     * 
     * @param scalar
     * @return this matrix for chaining.
     */
    public Matrix3 multiplyLocal(final double scalar) {
        _m00 *= scalar;
        _m01 *= scalar;
        _m02 *= scalar;
        _m10 *= scalar;
        _m11 *= scalar;
        _m12 *= scalar;
        _m20 *= scalar;
        _m21 *= scalar;
        _m22 *= scalar;
        return this;
    }

    /**
     * @param matrix
     *            the matrix to add to this.
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created. Note that it IS safe for
     *            matrix and store to be the same object.
     * @return the result.
     * @throws NullPointerException
     *             if matrix is null
     */
    @Override
    public Matrix3 add(final ReadOnlyMatrix3 matrix, final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        result._m00 = _m00 + matrix.getM00();
        result._m01 = _m01 + matrix.getM01();
        result._m02 = _m02 + matrix.getM02();
        result._m10 = _m10 + matrix.getM10();
        result._m11 = _m11 + matrix.getM11();
        result._m12 = _m12 + matrix.getM12();
        result._m20 = _m20 + matrix.getM20();
        result._m21 = _m21 + matrix.getM21();
        result._m22 = _m22 + matrix.getM22();

        return result;
    }

    /**
     * Internally adds the values of the given matrix to this matrix.
     * 
     * @param matrix
     *            the matrix to add to this.
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if matrix is null
     */
    public Matrix3 addLocal(final ReadOnlyMatrix3 matrix) {
        return add(matrix, this);
    }

    /**
     * @param matrix
     *            the matrix to subtract from this.
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created. Note that it IS safe for
     *            matrix and store to be the same object.
     * @return the result.
     * @throws NullPointerException
     *             if matrix is null
     */
    @Override
    public Matrix3 subtract(final ReadOnlyMatrix3 matrix, final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        result._m00 = _m00 - matrix.getM00();
        result._m01 = _m01 - matrix.getM01();
        result._m02 = _m02 - matrix.getM02();
        result._m10 = _m10 - matrix.getM10();
        result._m11 = _m11 - matrix.getM11();
        result._m12 = _m12 - matrix.getM12();
        result._m20 = _m20 - matrix.getM20();
        result._m21 = _m21 - matrix.getM21();
        result._m22 = _m22 - matrix.getM22();

        return result;
    }

    /**
     * Internally subtracts the values of the given matrix from this matrix.
     * 
     * @param matrix
     *            the matrix to subtract from this.
     * @return this matrix for chaining
     * @throws NullPointerException
     *             if matrix is null
     */
    public Matrix3 subtractLocal(final ReadOnlyMatrix3 matrix) {
        return subtract(matrix, this);
    }

    /**
     * Applies the given scale to this matrix and returns the result as a new matrix
     * 
     * @param scale
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created.
     * @return the new matrix
     * @throws NullPointerException
     *             if scale is null.
     */
    @Override
    public Matrix3 scale(final ReadOnlyVector3 scale, final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        return result.set( //
                _m00 * scale.getX(), _m01 * scale.getY(), _m02 * scale.getZ(), //
                _m10 * scale.getX(), _m11 * scale.getY(), _m12 * scale.getZ(), //
                _m20 * scale.getX(), _m21 * scale.getY(), _m22 * scale.getZ());
    }

    /**
     * Applies the given scale to this matrix values internally
     * 
     * @param scale
     * @return this matrix for chaining.
     * @throws NullPointerException
     *             if scale is null.
     */
    public Matrix3 scaleLocal(final ReadOnlyVector3 scale) {
        return scale(scale, this);
    }

    /**
     * transposes this matrix as a new matrix, basically flipping it across the diagonal
     * 
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created.
     * @return this matrix for chaining.
     * @see <a href="http://en.wikipedia.org/wiki/Transpose">wikipedia.org-Transpose</a>
     */
    @Override
    public Matrix3 transpose(final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }
        result._m00 = _m00;
        result._m01 = _m10;
        result._m02 = _m20;
        result._m10 = _m01;
        result._m11 = _m11;
        result._m12 = _m21;
        result._m20 = _m02;
        result._m21 = _m12;
        result._m22 = _m22;

        return result;
    }

    /**
     * transposes this matrix in place
     * 
     * @return this matrix for chaining.
     * @see <a href="http://en.wikipedia.org/wiki/Transpose">wikipedia.org-Transpose</a>
     */
    public Matrix3 transposeLocal() {
        final double m01 = _m01;
        final double m02 = _m02;
        final double m12 = _m12;
        _m01 = _m10;
        _m02 = _m20;
        _m12 = _m21;
        _m10 = m01;
        _m20 = m02;
        _m21 = m12;
        return this;
    }

    /**
     * @param store
     *            a matrix to store the result in. If store is null, a new matrix is created. Note that it IS safe for
     *            store == this.
     * @return a matrix that represents this matrix, inverted.
     * 
     *         if store is not null and is read only
     * @throws ArithmeticException
     *             if this matrix can not be inverted.
     */
    @Override
    public Matrix3 invert(final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        final double det = determinant();
        if (Math.abs(det) <= MathUtils.EPSILON) {
            throw new ArithmeticException("This matrix cannot be inverted.");
        }

        final double temp00 = _m11 * _m22 - _m12 * _m21;
        final double temp01 = _m02 * _m21 - _m01 * _m22;
        final double temp02 = _m01 * _m12 - _m02 * _m11;
        final double temp10 = _m12 * _m20 - _m10 * _m22;
        final double temp11 = _m00 * _m22 - _m02 * _m20;
        final double temp12 = _m02 * _m10 - _m00 * _m12;
        final double temp20 = _m10 * _m21 - _m11 * _m20;
        final double temp21 = _m01 * _m20 - _m00 * _m21;
        final double temp22 = _m00 * _m11 - _m01 * _m10;
        result.set(temp00, temp01, temp02, temp10, temp11, temp12, temp20, temp21, temp22);
        result.multiplyLocal(1.0 / det);
        return result;
    }

    /**
     * Inverts this matrix locally.
     * 
     * @return this matrix inverted internally.
     * @throws ArithmeticException
     *             if this matrix can not be inverted.
     */
    public Matrix3 invertLocal() {
        return invert(this);
    }

    /**
     * @param store
     *            The matrix to store the result in. If null, a new matrix is created.
     * @return The adjugate, or classical adjoint, of this matrix
     * @see <a href="http://en.wikipedia.org/wiki/Adjugate_matrix">wikipedia.org-Adjugate_matrix</a>
     */
    @Override
    public Matrix3 adjugate(final Matrix3 store) {
        Matrix3 result = store;
        if (result == null) {
            result = new Matrix3();
        }

        final double temp00 = _m11 * _m22 - _m12 * _m21;
        final double temp01 = _m02 * _m21 - _m01 * _m22;
        final double temp02 = _m01 * _m12 - _m02 * _m11;
        final double temp10 = _m12 * _m20 - _m10 * _m22;
        final double temp11 = _m00 * _m22 - _m02 * _m20;
        final double temp12 = _m02 * _m10 - _m00 * _m12;
        final double temp20 = _m10 * _m21 - _m11 * _m20;
        final double temp21 = _m01 * _m20 - _m00 * _m21;
        final double temp22 = _m00 * _m11 - _m01 * _m10;

        return result.set(temp00, temp01, temp02, temp10, temp11, temp12, temp20, temp21, temp22);
    }

    /**
     * @return this matrix, modified to represent its adjugate, or classical adjoint
     * @see <a href="http://en.wikipedia.org/wiki/Adjugate_matrix">wikipedia.org-Adjugate_matrix</a>
     */
    public Matrix3 adjugateLocal() {
        return adjugate(this);
    }

    /**
     * @return the determinate of this 3x3 matrix (aei+bfg+cdh-ceg-bdi-afh)
     * @see <a href="http://en.wikipedia.org/wiki/Determinant">wikipedia.org-Determinant</a>
     */
    @Override
    public double determinant() {
        return _m00 * _m11 * _m22 + _m01 * _m12 * _m20 + _m02 * _m10 * _m21 - //
                _m02 * _m11 * _m20 - _m01 * _m10 * _m22 - _m00 * _m12 * _m21;
    }

    /**
     * A function for creating a rotation matrix that rotates a vector called "start" into another vector called "end".
     * 
     * @param start
     *            normalized non-zero starting vector
     * @param end
     *            normalized non-zero ending vector
     * @return this matrix, for chaining
     * @see "Tomas Möller, John Hughes 'Efficiently Building a Matrix to Rotate One Vector to Another' Journal of Graphics Tools, 4(4):1-4, 1999"
     */
    public Matrix3 fromStartEndLocal(final ReadOnlyVector3 start, final ReadOnlyVector3 end) {
        final Vector3 v = new Vector3();
        double e, h, f;

        start.cross(end, v);
        e = start.dot(end);
        f = e < 0 ? -e : e;

        // if "from" and "to" vectors are nearly parallel
        if (f > 1.0 - MathUtils.ZERO_TOLERANCE) {
            final Vector3 u = new Vector3();
            final Vector3 x = new Vector3();
            double c1, c2, c3; /* coefficients for later use */

            x.setX(start.getX() > 0.0 ? start.getX() : -start.getX());
            x.setY(start.getY() > 0.0 ? start.getY() : -start.getY());
            x.setZ(start.getZ() > 0.0 ? start.getZ() : -start.getZ());

            if (x.getX() < x.getY()) {
                if (x.getX() < x.getZ()) {
                    x.set(1.0, 0.0, 0.0);
                } else {
                    x.set(0.0, 0.0, 1.0);
                }
            } else {
                if (x.getY() < x.getZ()) {
                    x.set(0.0, 1.0, 0.0);
                } else {
                    x.set(0.0, 0.0, 1.0);
                }
            }

            u.set(x).subtractLocal(start);
            v.set(x).subtractLocal(end);

            c1 = 2.0 / u.dot(u);
            c2 = 2.0 / v.dot(v);
            c3 = c1 * c2 * u.dot(v);

            _m00 = -c1 * u.getX() * u.getX() - c2 * v.getX() * v.getX() + c3 * v.getX() * u.getX() + 1.0;
            _m01 = -c1 * u.getX() * u.getY() - c2 * v.getX() * v.getY() + c3 * v.getX() * u.getY();
            _m02 = -c1 * u.getX() * u.getZ() - c2 * v.getX() * v.getZ() + c3 * v.getX() * u.getZ();
            _m10 = -c1 * u.getY() * u.getX() - c2 * v.getY() * v.getX() + c3 * v.getY() * u.getX();
            _m11 = -c1 * u.getY() * u.getY() - c2 * v.getY() * v.getY() + c3 * v.getY() * u.getY() + 1.0;
            _m12 = -c1 * u.getY() * u.getZ() - c2 * v.getY() * v.getZ() + c3 * v.getY() * u.getZ();
            _m20 = -c1 * u.getZ() * u.getX() - c2 * v.getZ() * v.getX() + c3 * v.getZ() * u.getX();
            _m21 = -c1 * u.getZ() * u.getY() - c2 * v.getZ() * v.getY() + c3 * v.getZ() * u.getY();
            _m22 = -c1 * u.getZ() * u.getZ() - c2 * v.getZ() * v.getZ() + c3 * v.getZ() * u.getZ() + 1.0;
        } else {
            // the most common case, unless "start"="end", or "start"=-"end"
            double hvx, hvz, hvxy, hvxz, hvyz;
            h = 1.0 / (1.0 + e);
            hvx = h * v.getX();
            hvz = h * v.getZ();
            hvxy = hvx * v.getY();
            hvxz = hvx * v.getZ();
            hvyz = hvz * v.getY();
            _m00 = e + hvx * v.getX();
            _m01 = hvxy - v.getZ();
            _m02 = hvxz + v.getY();

            _m10 = hvxy + v.getZ();
            _m11 = e + h * v.getY() * v.getY();
            _m12 = hvyz - v.getX();

            _m20 = hvxz - v.getY();
            _m21 = hvyz + v.getX();
            _m22 = e + hvz * v.getZ();
        }
        return this;
    }

    /**
     * Multiplies the given vector by this matrix (v * M). If supplied, the result is stored into the supplied "store"
     * vector.
     * 
     * @param vec
     *            the vector to multiply this matrix by.
     * @param store
     *            a vector to store the result in. If store is null, a new vector is created. Note that it IS safe for
     *            vec and store to be the same object.
     * @return the store vector, or a new vector if store is null.
     * @throws NullPointerException
     *             if vec is null
     */
    @Override
    public Vector3 applyPre(final ReadOnlyVector3 vec, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        final double x = vec.getX();
        final double y = vec.getY();
        final double z = vec.getZ();

        result.setX(_m00 * x + _m10 * y + _m20 * z);
        result.setY(_m01 * x + _m11 * y + _m21 * z);
        result.setZ(_m02 * x + _m12 * y + _m22 * z);
        return result;
    }

    /**
     * Multiplies the given vector by this matrix (M * v). If supplied, the result is stored into the supplied "store"
     * vector.
     * 
     * @param vec
     *            the vector to multiply this matrix by.
     * @param store
     *            a vector to store the result in. If store is null, a new vector is created. Note that it IS safe for
     *            vec and store to be the same object.
     * @return the store vector, or a new vector if store is null.
     * @throws NullPointerException
     *             if vec is null
     */
    @Override
    public Vector3 applyPost(final ReadOnlyVector3 vec, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        final double x = vec.getX();
        final double y = vec.getY();
        final double z = vec.getZ();

        result.setX(_m00 * x + _m01 * y + _m02 * z);
        result.setY(_m10 * x + _m11 * y + _m12 * z);
        result.setZ(_m20 * x + _m21 * y + _m22 * z);
        return result;
    }

    /**
     * Modifies this matrix to equal the rotation required to point the z-axis at 'direction' and the y-axis to 'up'.
     * 
     * @param direction
     *            where to 'look' at
     * @param up
     *            a vector indicating the local up direction.
     * @return this matrix for chaining
     */
    public Matrix3 lookAt(final ReadOnlyVector3 direction, final ReadOnlyVector3 up) {
        final Vector3 xAxis = Vector3.fetchTempInstance();
        final Vector3 yAxis = Vector3.fetchTempInstance();
        final Vector3 zAxis = Vector3.fetchTempInstance();
        direction.normalize(zAxis);
        up.normalize(xAxis).crossLocal(zAxis).normalizeLocal();
        zAxis.cross(xAxis, yAxis);

        fromAxes(xAxis, yAxis, zAxis);

        Vector3.releaseTempInstance(xAxis);
        Vector3.releaseTempInstance(yAxis);
        Vector3.releaseTempInstance(zAxis);
        return this;
    }

    /**
     * Check a matrix... if it is null or its doubles are NaN or infinite, return false. Else return true.
     * 
     * @param matrix
     *            the vector to check
     * @return true or false as stated above.
     */
    public static boolean isValid(final ReadOnlyMatrix3 matrix) {
        if (matrix == null) {
            return false;
        }

        if (Double.isNaN(matrix.getM00()) || Double.isInfinite(matrix.getM00())) {
            return false;
        } else if (Double.isNaN(matrix.getM01()) || Double.isInfinite(matrix.getM01())) {
            return false;
        } else if (Double.isNaN(matrix.getM02()) || Double.isInfinite(matrix.getM02())) {
            return false;
        } else if (Double.isNaN(matrix.getM10()) || Double.isInfinite(matrix.getM10())) {
            return false;
        } else if (Double.isNaN(matrix.getM11()) || Double.isInfinite(matrix.getM11())) {
            return false;
        } else if (Double.isNaN(matrix.getM12()) || Double.isInfinite(matrix.getM12())) {
            return false;
        } else if (Double.isNaN(matrix.getM20()) || Double.isInfinite(matrix.getM20())) {
            return false;
        } else if (Double.isNaN(matrix.getM21()) || Double.isInfinite(matrix.getM21())) {
            return false;
        } else if (Double.isNaN(matrix.getM22()) || Double.isInfinite(matrix.getM22())) {
            return false;
        }

        return true;
    }

    /**
     * @return true if this Matrix is orthonormal - its rows are orthogonal, unit vectors.
     */
    @Override
    public boolean isOrthonormal() {
        if (Math.abs(_m00 * _m00 + _m01 * _m01 + _m02 * _m02 - 1.0) > MathUtils.ZERO_TOLERANCE) {
            return false;
        } else if (Math.abs(_m00 * _m10 + _m01 * _m11 + _m02 * _m12 - 0.0) > MathUtils.ZERO_TOLERANCE) {
            return false;
        } else if (Math.abs(_m00 * _m20 + _m01 * _m21 + _m02 * _m22 - 0.0) > MathUtils.ZERO_TOLERANCE) {
            return false;
        } else if (Math.abs(_m10 * _m00 + _m11 * _m01 + _m12 * _m02 - 0.0) > MathUtils.ZERO_TOLERANCE) {
            return false;
        } else if (Math.abs(_m10 * _m10 + _m11 * _m11 + _m12 * _m12 - 1.0) > MathUtils.ZERO_TOLERANCE) {
            return false;
        } else if (Math.abs(_m10 * _m20 + _m11 * _m21 + _m12 * _m22 - 0.0) > MathUtils.ZERO_TOLERANCE) {
            return false;
        } else if (Math.abs(_m20 * _m00 + _m21 * _m01 + _m22 * _m02 - 0.0) > MathUtils.ZERO_TOLERANCE) {
            return false;
        } else if (Math.abs(_m20 * _m10 + _m21 * _m11 + _m22 * _m12 - 0.0) > MathUtils.ZERO_TOLERANCE) {
            return false;
        } else if (Math.abs(_m20 * _m20 + _m21 * _m21 + _m22 * _m22 - 1.0) > MathUtils.ZERO_TOLERANCE) {
            return false;
        }

        return true;
    }

    /**
     * @return the string representation of this matrix.
     */
    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer("com.ardor3d.math.Matrix3\n[\n");
        result.append(" ");
        result.append(_m00);
        result.append(" ");
        result.append(_m01);
        result.append(" ");
        result.append(_m02);
        result.append(" \n");

        result.append(" ");
        result.append(_m10);
        result.append(" ");
        result.append(_m11);
        result.append(" ");
        result.append(_m12);
        result.append(" \n");

        result.append(" ");
        result.append(_m20);
        result.append(" ");
        result.append(_m21);
        result.append(" ");
        result.append(_m22);
        result.append(" \n");

        result.append("]");
        return result.toString();
    }

    /**
     * @return returns a unique code for this matrix object based on its values. If two matrices are numerically equal,
     *         they will return the same hash code value.
     */
    @Override
    public int hashCode() {
        int result = 17;

        long val = Double.doubleToLongBits(_m00);
        result += 31 * result + (int) (val ^ val >>> 32);
        val = Double.doubleToLongBits(_m01);
        result += 31 * result + (int) (val ^ val >>> 32);
        val = Double.doubleToLongBits(_m02);
        result += 31 * result + (int) (val ^ val >>> 32);

        val = Double.doubleToLongBits(_m10);
        result += 31 * result + (int) (val ^ val >>> 32);
        val = Double.doubleToLongBits(_m11);
        result += 31 * result + (int) (val ^ val >>> 32);
        val = Double.doubleToLongBits(_m12);
        result += 31 * result + (int) (val ^ val >>> 32);

        val = Double.doubleToLongBits(_m20);
        result += 31 * result + (int) (val ^ val >>> 32);
        val = Double.doubleToLongBits(_m21);
        result += 31 * result + (int) (val ^ val >>> 32);
        val = Double.doubleToLongBits(_m22);
        result += 31 * result + (int) (val ^ val >>> 32);

        return result;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this matrix and the provided matrix have the double values that are within the
     *         MathUtils.ZERO_TOLERANCE.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyMatrix3)) {
            return false;
        }
        final ReadOnlyMatrix3 comp = (ReadOnlyMatrix3) o;
        if (Math.abs(getM00() - comp.getM00()) > Matrix3.ALLOWED_DEVIANCE) {
            return false;
        } else if (Math.abs(getM01() - comp.getM01()) > Matrix3.ALLOWED_DEVIANCE) {
            return false;
        } else if (Math.abs(getM02() - comp.getM02()) > Matrix3.ALLOWED_DEVIANCE) {
            return false;
        } else if (Math.abs(getM10() - comp.getM10()) > Matrix3.ALLOWED_DEVIANCE) {
            return false;
        } else if (Math.abs(getM11() - comp.getM11()) > Matrix3.ALLOWED_DEVIANCE) {
            return false;
        } else if (Math.abs(getM12() - comp.getM12()) > Matrix3.ALLOWED_DEVIANCE) {
            return false;
        } else if (Math.abs(getM20() - comp.getM20()) > Matrix3.ALLOWED_DEVIANCE) {
            return false;
        } else if (Math.abs(getM21() - comp.getM21()) > Matrix3.ALLOWED_DEVIANCE) {
            return false;
        } else if (Math.abs(getM22() - comp.getM22()) > Matrix3.ALLOWED_DEVIANCE) {
            return false;
        }

        return true;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this matrix and the provided matrix have the exact same double values.
     */
    public boolean strictEquals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyMatrix3)) {
            return false;
        }
        final ReadOnlyMatrix3 comp = (ReadOnlyMatrix3) o;
        if (getM00() != comp.getM00()) {
            return false;
        } else if (getM01() != comp.getM01()) {
            return false;
        } else if (getM02() != comp.getM02()) {
            return false;
        } else if (getM10() != comp.getM10()) {
            return false;
        } else if (getM11() != comp.getM11()) {
            return false;
        } else if (getM12() != comp.getM12()) {
            return false;
        } else if (getM20() != comp.getM20()) {
            return false;
        } else if (getM21() != comp.getM21()) {
            return false;
        } else if (getM22() != comp.getM22()) {
            return false;
        }

        return true;
    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public Matrix3 clone() {
        return new Matrix3(this);
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends Matrix3> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_m00, "m00", 1.0);
        capsule.write(_m01, "m01", 0.0);
        capsule.write(_m02, "m02", 0.0);
        capsule.write(_m10, "m10", 0.0);
        capsule.write(_m11, "m11", 1.0);
        capsule.write(_m12, "m12", 0.0);
        capsule.write(_m20, "m20", 0.0);
        capsule.write(_m21, "m21", 0.0);
        capsule.write(_m22, "m22", 1.0);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        _m00 = capsule.readDouble("m00", 1.0);
        _m01 = capsule.readDouble("m01", 0.0);
        _m02 = capsule.readDouble("m02", 0.0);
        _m10 = capsule.readDouble("m10", 0.0);
        _m11 = capsule.readDouble("m11", 1.0);
        _m12 = capsule.readDouble("m12", 0.0);
        _m20 = capsule.readDouble("m20", 0.0);
        _m21 = capsule.readDouble("m21", 0.0);
        _m22 = capsule.readDouble("m22", 1.0);
    }

    // /////////////////
    // Methods for Externalizable
    // /////////////////

    /**
     * Used with serialization. Not to be called manually.
     * 
     * @param in
     *            ObjectInput
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        _m00 = in.readDouble();
        _m01 = in.readDouble();
        _m02 = in.readDouble();
        _m10 = in.readDouble();
        _m11 = in.readDouble();
        _m12 = in.readDouble();
        _m20 = in.readDouble();
        _m21 = in.readDouble();
        _m22 = in.readDouble();
    }

    /**
     * Used with serialization. Not to be called manually.
     * 
     * @param out
     *            ObjectOutput
     * @throws IOException
     */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeDouble(_m00);
        out.writeDouble(_m01);
        out.writeDouble(_m02);
        out.writeDouble(_m10);
        out.writeDouble(_m11);
        out.writeDouble(_m12);
        out.writeDouble(_m20);
        out.writeDouble(_m21);
        out.writeDouble(_m22);
    }

    // /////////////////
    // Methods for creating temp variables (pooling)
    // /////////////////

    /**
     * @return An instance of Matrix3 that is intended for temporary use in calculations and so forth. Multiple calls to
     *         the method should return instances of this class that are not currently in use.
     */
    public final static Matrix3 fetchTempInstance() {
        if (MathConstants.useMathPools) {
            return Matrix3.MAT_POOL.fetch();
        } else {
            return new Matrix3();
        }
    }

    /**
     * Releases a Matrix3 back to be used by a future call to fetchTempInstance. TAKE CARE: this Matrix3 object should
     * no longer have other classes referencing it or "Bad Things" will happen.
     * 
     * @param mat
     *            the Matrix3 to release.
     */
    public final static void releaseTempInstance(final Matrix3 mat) {
        if (MathConstants.useMathPools) {
            Matrix3.MAT_POOL.release(mat);
        }
    }
}
