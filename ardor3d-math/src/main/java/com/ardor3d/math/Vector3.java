/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
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

import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Vector3 represents a point or vector in a three dimensional system. This implementation stores its data in
 * double-precision.
 */
public class Vector3 implements Cloneable, Savable, Externalizable, ReadOnlyVector3, Poolable {

    private static final long serialVersionUID = 1L;

    private static final ObjectPool<Vector3> VEC_POOL = ObjectPool.create(Vector3.class, MathConstants.maxMathPoolSize);

    /**
     * 0, 0, 0
     */
    public final static ReadOnlyVector3 ZERO = new Vector3(0, 0, 0);

    /**
     * 1, 1, 1
     */
    public final static ReadOnlyVector3 ONE = new Vector3(1, 1, 1);

    /**
     * -1, -1, -1
     */
    public final static ReadOnlyVector3 NEG_ONE = new Vector3(-1, -1, -1);

    /**
     * 1, 0, 0
     */
    public final static ReadOnlyVector3 UNIT_X = new Vector3(1, 0, 0);

    /**
     * -1, 0, 0
     */
    public final static ReadOnlyVector3 NEG_UNIT_X = new Vector3(-1, 0, 0);

    /**
     * 0, 1, 0
     */
    public final static ReadOnlyVector3 UNIT_Y = new Vector3(0, 1, 0);

    /**
     * 0, -1, 0
     */
    public final static ReadOnlyVector3 NEG_UNIT_Y = new Vector3(0, -1, 0);

    /**
     * 0, 0, 1
     */
    public final static ReadOnlyVector3 UNIT_Z = new Vector3(0, 0, 1);

    /**
     * 0, 0, -1
     */
    public final static ReadOnlyVector3 NEG_UNIT_Z = new Vector3(0, 0, -1);

    protected double _x = 0;
    protected double _y = 0;
    protected double _z = 0;

    /**
     * Constructs a new vector set to (0, 0, 0).
     */
    public Vector3() {
        this(0, 0, 0);
    }

    /**
     * Constructs a new vector set to the (x, y, z) values of the given source vector.
     *
     * @param src
     */
    public Vector3(final ReadOnlyVector3 src) {
        this(src.getX(), src.getY(), src.getZ());
    }

    /**
     * Constructs a new vector set to (x, y, z).
     *
     * @param x
     * @param y
     * @param z
     */
    public Vector3(final double x, final double y, final double z) {
        _x = x;
        _y = y;
        _z = z;
    }

    @Override
    public double getX() {
        return _x;
    }

    @Override
    public double getY() {
        return _y;
    }

    @Override
    public double getZ() {
        return _z;
    }

    /**
     * @return x as a float, to decrease need for explicit casts.
     */
    @Override
    public float getXf() {
        return (float) _x;
    }

    /**
     * @return y as a float, to decrease need for explicit casts.
     */
    @Override
    public float getYf() {
        return (float) _y;
    }

    /**
     * @return z as a float, to decrease need for explicit casts.
     */
    @Override
    public float getZf() {
        return (float) _z;
    }

    /**
     * @param index
     * @return x value if index == 0, y value if index == 1 or z value if index == 2
     * @throws IllegalArgumentException
     *             if index is not one of 0, 1, 2.
     */
    @Override
    public double getValue(final int index) {
        switch (index) {
            case 0:
                return getX();
            case 1:
                return getY();
            case 2:
                return getZ();
        }
        throw new IllegalArgumentException("index must be either 0, 1 or 2");
    }

    /**
     * @param index
     *            which field index in this vector to set.
     * @param value
     *            to set to one of x, y or z.
     * @throws IllegalArgumentException
     *             if index is not one of 0, 1, 2.
     */
    public void setValue(final int index, final double value) {
        switch (index) {
            case 0:
                setX(value);
                return;
            case 1:
                setY(value);
                return;
            case 2:
                setZ(value);
                return;
        }
        throw new IllegalArgumentException("index must be either 0, 1 or 2");
    }

    /**
     * Stores the double values of this vector in the given double array.
     *
     * @param store
     *            if null, a new double[3] array is created.
     * @return the double array
     * @throws ArrayIndexOutOfBoundsException
     *             if store is not at least length 3.
     */
    @Override
    public double[] toArray(double[] store) {
        if (store == null) {
            store = new double[3];
        }

        // do last first to ensure size is correct before any edits occur.
        store[2] = getZ();
        store[1] = getY();
        store[0] = getX();
        return store;
    }

    /**
     * Stores the double values of this vector in the given float array.
     *
     * @param store
     *            if null, a new float[3] array is created.
     * @return the float array
     * @throws NullPointerException
     *             if store is null.
     * @throws ArrayIndexOutOfBoundsException
     *             if store is not at least length 3.
     */
    public float[] toFloatArray(float[] store) {
        if (store == null) {
            store = new float[3];
        }

        // do last first to ensure size is correct before any edits occur.
        store[2] = (float) getZ();
        store[1] = (float) getY();
        store[0] = (float) getX();
        return store;
    }

    /**
     * Sets the first component of this vector to the given double value.
     *
     * @param x
     */
    public void setX(final double x) {
        _x = x;
    }

    /**
     * Sets the second component of this vector to the given double value.
     *
     * @param y
     */
    public void setY(final double y) {
        _y = y;
    }

    /**
     * Sets the third component of this vector to the given double value.
     *
     * @param z
     */
    public void setZ(final double z) {
        _z = z;
    }

    /**
     * Sets the value of this vector to (x, y, z)
     *
     * @param x
     * @param y
     * @param z
     * @return this vector for chaining
     */
    public Vector3 set(final double x, final double y, final double z) {
        setX(x);
        setY(y);
        setZ(z);
        return this;
    }

    /**
     * Sets the value of this vector to the (x, y, z) values of the provided source vector.
     *
     * @param source
     * @return this vector for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Vector3 set(final ReadOnlyVector3 source) {
        setX(source.getX());
        setY(source.getY());
        setZ(source.getZ());
        return this;
    }

    /**
     * Sets the value of this vector to (0, 0, 0)
     *
     * @return this vector for chaining
     */
    public Vector3 zero() {
        return set(0, 0, 0);
    }

    /**
     * Adds the given values to those of this vector and returns them in store * @param store the vector to store the
     * result in for return. If null, a new vector object is created and returned. .
     *
     * @param x
     * @param y
     * @param z
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return (this.x + x, this.y + y, this.z + z)
     */
    @Override
    public Vector3 add(final double x, final double y, final double z, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        return result.set(getX() + x, getY() + y, getZ() + z);
    }

    /**
     * Increments the values of this vector with the given x, y and z values.
     *
     * @param x
     * @param y
     * @param z
     * @return this vector for chaining
     */
    public Vector3 addLocal(final double x, final double y, final double z) {
        return set(getX() + x, getY() + y, getZ() + z);
    }

    /**
     * Adds the values of the given source vector to those of this vector and returns them in store.
     *
     * @param source
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return (this.x + source.x, this.y + source.y, this.z + source.z)
     * @throws NullPointerException
     *             if source is null.
     */
    @Override
    public Vector3 add(final ReadOnlyVector3 source, final Vector3 store) {
        return add(source.getX(), source.getY(), source.getZ(), store);
    }

    /**
     * Increments the values of this vector with the x, y and z values of the given vector.
     *
     * @param source
     * @return this vector for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Vector3 addLocal(final ReadOnlyVector3 source) {
        return addLocal(source.getX(), source.getY(), source.getZ());
    }

    /**
     * Subtracts the given values from those of this vector and returns them in store.
     *
     * @param x
     * @param y
     * @param z
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return (this.x - x, this.y - y, this.z - z)
     */
    @Override
    public Vector3 subtract(final double x, final double y, final double z, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        return result.set(getX() - x, getY() - y, getZ() - z);
    }

    /**
     * Decrements the values of this vector by the given x, y and z values.
     *
     * @param x
     * @param y
     * @param z
     * @return this vector for chaining
     */
    public Vector3 subtractLocal(final double x, final double y, final double z) {
        return set(getX() - x, getY() - y, getZ() - z);
    }

    /**
     * Subtracts the values of the given source vector from those of this vector and returns them in store.
     *
     * @param source
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned. It
     *            IS okay for source and store to be the same object.
     * @return (this.x - source.x, this.y - source.y, this.z - source.z)
     * @throws NullPointerException
     *             if source is null.
     */
    @Override
    public Vector3 subtract(final ReadOnlyVector3 source, final Vector3 store) {
        return subtract(source.getX(), source.getY(), source.getZ(), store);
    }

    /**
     * Decrements the values of this vector by the x, y and z values from the given source vector.
     *
     * @param source
     * @return this vector for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Vector3 subtractLocal(final ReadOnlyVector3 source) {
        return subtractLocal(source.getX(), source.getY(), source.getZ());
    }

    /**
     * Multiplies the values of this vector by the given scalar value and returns the result in store.
     *
     * @param scalar
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x * scalar, this.y * scalar, this.z * scalar)
     */
    @Override
    public Vector3 multiply(final double scalar, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        return result.set(getX() * scalar, getY() * scalar, getZ() * scalar);
    }

    /**
     * Internally modifies the values of this vector by multiplying them each by the given scalar value.
     *
     * @param scalar
     * @return this vector for chaining
     */
    public Vector3 multiplyLocal(final double scalar) {
        return set(getX() * scalar, getY() * scalar, getZ() * scalar);
    }

    /**
     * Multiplies the values of this vector by the given scale values and returns the result in store.
     *
     * @param scale
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x * scale.x, this.y * scale.y, this.z * scale.z)
     */
    @Override
    public Vector3 multiply(final ReadOnlyVector3 scale, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        return result.set(getX() * scale.getX(), getY() * scale.getY(), getZ() * scale.getZ());
    }

    /**
     * Internally modifies the values of this vector by multiplying them each by the given scale values.
     *
     * @param scalar
     * @return this vector for chaining
     */
    public Vector3 multiplyLocal(final ReadOnlyVector3 scale) {
        return set(getX() * scale.getX(), getY() * scale.getY(), getZ() * scale.getZ());
    }

    /**
     * Multiplies the values of this vector by the given scale values and returns the result in store.
     *
     * @param x
     * @param y
     * @param z
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x * scale.x, this.y * scale.y, this.z * scale.z)
     */
    @Override
    public Vector3 multiply(final double x, final double y, final double z, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        return result.set(getX() * x, getY() * y, getZ() * z);
    }

    /**
     * Internally modifies the values of this vector by multiplying them each by the given scale values.
     *
     * @param x
     * @param y
     * @param z
     * @return this vector for chaining
     */
    public Vector3 multiplyLocal(final double x, final double y, final double z) {
        return set(getX() * x, getY() * y, getZ() * z);
    }

    /**
     * Divides the values of this vector by the given scalar value and returns the result in store.
     *
     * @param scalar
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x / scalar, this.y / scalar, this.z / scalar)
     */
    @Override
    public Vector3 divide(final double scalar, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        return result.set(getX() / scalar, getY() / scalar, getZ() / scalar);
    }

    /**
     * Internally modifies the values of this vector by dividing them each by the given scalar value.
     *
     * @param scalar
     * @return this vector for chaining
     * @throws ArithmeticException
     *             if scalar is 0
     */
    public Vector3 divideLocal(final double scalar) {
        final double invScalar = 1.0 / scalar;

        return set(getX() * invScalar, getY() * invScalar, getZ() * invScalar);
    }

    /**
     * Divides the values of this vector by the given scale values and returns the result in store.
     *
     * @param scale
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x / scale.x, this.y / scale.y, this.z / scale.z)
     */
    @Override
    public Vector3 divide(final ReadOnlyVector3 scale, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        return result.set(getX() / scale.getX(), getY() / scale.getY(), getZ() / scale.getZ());
    }

    /**
     * Internally modifies the values of this vector by dividing them each by the given scale values.
     *
     * @param scale
     * @return this vector for chaining
     */
    public Vector3 divideLocal(final ReadOnlyVector3 scale) {
        return set(getX() / scale.getX(), getY() / scale.getY(), getZ() / scale.getZ());
    }

    /**
     * Divides the values of this vector by the given scale values and returns the result in store.
     *
     * @param x
     * @param y
     * @param z
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x / scale.x, this.y / scale.y, this.z / scale.z)
     */
    @Override
    public Vector3 divide(final double x, final double y, final double z, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        return result.set(getX() / x, getY() / y, getZ() / z);
    }

    /**
     * Internally modifies the values of this vector by dividing them each by the given scale values.
     *
     * @param x
     * @param y
     * @param z
     * @return this vector for chaining
     */
    public Vector3 divideLocal(final double x, final double y, final double z) {
        return set(getX() / x, getY() / y, getZ() / z);
    }

    /**
     *
     * Internally modifies this vector by multiplying its values with a given scale value, then adding a given "add"
     * value.
     *
     * @param scale
     *            the value to multiply this vector by.
     * @param add
     *            the value to add to the result
     * @return this vector for chaining
     */
    public Vector3 scaleAddLocal(final double scale, final ReadOnlyVector3 add) {
        _x = _x * scale + add.getX();
        _y = _y * scale + add.getY();
        _z = _z * scale + add.getZ();
        return this;
    }

    /**
     * Scales this vector by multiplying its values with a given scale value, then adding a given "add" value. The
     * result is store in the given store parameter.
     *
     * @param scale
     *            the value to multiply by.
     * @param add
     *            the value to add
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return the store variable
     */
    @Override
    public Vector3 scaleAdd(final double scale, final ReadOnlyVector3 add, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        result.setX(_x * scale + add.getX());
        result.setY(_y * scale + add.getY());
        result.setZ(_z * scale + add.getZ());
        return result;
    }

    /**
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return same as multiply(-1, store)
     */
    @Override
    public Vector3 negate(final Vector3 store) {
        return multiply(-1, store);
    }

    /**
     * @return same as multiplyLocal(-1)
     */
    public Vector3 negateLocal() {
        return multiplyLocal(-1);
    }

    /**
     * Creates a new unit length vector from this one by dividing by length. If the length is 0, (ie, if the vector is
     * 0, 0, 0) then a new vector (0, 0, 0) is returned.
     *
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new unit vector (or 0, 0, 0 if this unit is 0 length)
     */
    @Override
    public Vector3 normalize(final Vector3 store) {
        final double lengthSq = lengthSquared();
        if (Math.abs(lengthSq) > MathUtils.EPSILON) {
            return multiply(MathUtils.inverseSqrt(lengthSq), store);
        }

        return store != null ? store.set(Vector3.ZERO) : new Vector3(Vector3.ZERO);
    }

    /**
     * Converts this vector into a unit vector by dividing it internally by its length. If the length is 0, (ie, if the
     * vector is 0, 0, 0) then no action is taken.
     *
     * @return this vector for chaining
     */
    public Vector3 normalizeLocal() {
        final double lengthSq = lengthSquared();
        if (Math.abs(lengthSq) > MathUtils.EPSILON) {
            return multiplyLocal(MathUtils.inverseSqrt(lengthSq));
        }

        return this;
    }

    /**
     * Performs a linear interpolation between this vector and the given end vector, using the given scalar as a
     * percent. iow, if changeAmnt is closer to 0, the result will be closer to the current value of this vector and if
     * it is closer to 1, the result will be closer to the end value. The result is returned as a new vector object.
     *
     * @param endVec
     * @param scalar
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector as described above.
     * @throws NullPointerException
     *             if endVec is null.
     */
    @Override
    public Vector3 lerp(final ReadOnlyVector3 endVec, final double scalar, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        final double x = (1.0 - scalar) * getX() + scalar * endVec.getX();
        final double y = (1.0 - scalar) * getY() + scalar * endVec.getY();
        final double z = (1.0 - scalar) * getZ() + scalar * endVec.getZ();
        return result.set(x, y, z);
    }

    /**
     * Performs a linear interpolation between this vector and the given end vector, using the given scalar as a
     * percent. iow, if changeAmnt is closer to 0, the result will be closer to the current value of this vector and if
     * it is closer to 1, the result will be closer to the end value. The result is stored back in this vector.
     *
     * @param endVec
     * @param scalar
     * @return this vector for chaining
     * @throws NullPointerException
     *             if endVec is null.
     */
    public Vector3 lerpLocal(final ReadOnlyVector3 endVec, final double scalar) {
        setX((1.0 - scalar) * getX() + scalar * endVec.getX());
        setY((1.0 - scalar) * getY() + scalar * endVec.getY());
        setZ((1.0 - scalar) * getZ() + scalar * endVec.getZ());
        return this;
    }

    /**
     * Performs a linear interpolation between the given begin and end vectors, using the given scalar as a percent.
     * iow, if changeAmnt is closer to 0, the result will be closer to the begin value and if it is closer to 1, the
     * result will be closer to the end value. The result is returned as a new vector object.
     *
     * @param beginVec
     * @param endVec
     * @param scalar
     *            the scalar as a percent.
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned. It
     *            IS safe for store to be the same as the begin or end vector.
     * @return a new vector as described above.
     * @throws NullPointerException
     *             if beginVec or endVec are null.
     */
    public static Vector3 lerp(final ReadOnlyVector3 beginVec, final ReadOnlyVector3 endVec, final double scalar,
            final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        // Check for equality and skip operation if possible.
        if (!beginVec.equals(endVec)) {
            final double x = (1.0 - scalar) * beginVec.getX() + scalar * endVec.getX();
            final double y = (1.0 - scalar) * beginVec.getY() + scalar * endVec.getY();
            final double z = (1.0 - scalar) * beginVec.getZ() + scalar * endVec.getZ();
            return result.set(x, y, z);
        } else {
            return result.set(beginVec);
        }
    }

    /**
     * Performs a linear interpolation between the given begin and end vectors, using the given scalar as a percent.
     * iow, if changeAmnt is closer to 0, the result will be closer to the begin value and if it is closer to 1, the
     * result will be closer to the end value. The result is stored back in this vector.
     *
     * @param beginVec
     * @param endVec
     * @param changeAmnt
     *            the scalar as a percent.
     * @return this vector for chaining
     * @throws NullPointerException
     *             if beginVec or endVec are null.
     */
    public Vector3 lerpLocal(final ReadOnlyVector3 beginVec, final ReadOnlyVector3 endVec, final double scalar) {
        // Check for equality and skip operation if possible.
        if (!beginVec.equals(endVec)) {
            setX((1.0 - scalar) * beginVec.getX() + scalar * endVec.getX());
            setY((1.0 - scalar) * beginVec.getY() + scalar * endVec.getY());
            setZ((1.0 - scalar) * beginVec.getZ() + scalar * endVec.getZ());
        } else {
            set(beginVec);
        }
        return this;
    }

    /**
     * @return the magnitude or distance between the origin (0, 0, 0) and the point described by this vector (x, y, z).
     *         Effectively the square root of the value returned by {@link #lengthSquared()}.
     */
    @Override
    public double length() {
        return MathUtils.sqrt(lengthSquared());
    }

    /**
     * @return the squared magnitude or squared distance between the origin (0, 0, 0) and the point described by this
     *         vector (x, y, z)
     */
    @Override
    public double lengthSquared() {
        return getX() * getX() + getY() * getY() + getZ() * getZ();
    }

    /**
     * @param x
     * @param y
     * @param z
     * @return the squared distance between the point described by this vector and the given x, y, z point. When
     *         comparing the relative distance between two points it is usually sufficient to compare the squared
     *         distances, thus avoiding an expensive square root operation.
     */
    @Override
    public double distanceSquared(final double x, final double y, final double z) {
        final double dx = getX() - x;
        final double dy = getY() - y;
        final double dz = getZ() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * @param destination
     * @return the squared distance between the point described by this vector and the given destination point. When
     *         comparing the relative distance between two points it is usually sufficient to compare the squared
     *         distances, thus avoiding an expensive square root operation.
     * @throws NullPointerException
     *             if destination is null.
     */
    @Override
    public double distanceSquared(final ReadOnlyVector3 destination) {
        return distanceSquared(destination.getX(), destination.getY(), destination.getZ());
    }

    /**
     * @param x
     * @param y
     * @param z
     * @return the distance between the point described by this vector and the given x, y, z point.
     */
    @Override
    public double distance(final double x, final double y, final double z) {
        return MathUtils.sqrt(distanceSquared(x, y, z));
    }

    /**
     * @param destination
     * @return the distance between the point described by this vector and the given destination point.
     * @throws NullPointerException
     *             if destination is null.
     */
    @Override
    public double distance(final ReadOnlyVector3 destination) {
        return MathUtils.sqrt(distanceSquared(destination));
    }

    /**
     * @param x
     * @param y
     * @param z
     * @return the dot product of this vector with the given x, y, z values.
     */
    @Override
    public double dot(final double x, final double y, final double z) {
        return getX() * x + getY() * y + getZ() * z;
    }

    /**
     * @param vec
     * @return the dot product of this vector with the x, y, z values of the given vector.
     * @throws NullPointerException
     *             if vec is null.
     */
    @Override
    public double dot(final ReadOnlyVector3 vec) {
        return dot(vec.getX(), vec.getY(), vec.getZ());
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return the cross product of this vector with the given x, y, z values.
     */
    @Override
    public Vector3 cross(final double x, final double y, final double z, final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }
        final double newX = getY() * z - getZ() * y;
        final double newY = getZ() * x - getX() * z;
        final double newZ = getX() * y - getY() * x;
        result.set(newX, newY, newZ);
        return result;
    }

    /**
     * @param vec
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return the cross product of this vector with the given vector's x, y, z values
     * @throws NullPointerException
     *             if vec is null.
     */
    @Override
    public Vector3 cross(final ReadOnlyVector3 vec, final Vector3 store) {
        return cross(vec.getX(), vec.getY(), vec.getZ(), store);
    }

    /**
     * @param x
     * @param y
     * @param z
     * @return this vector, set to the cross product of this vector with the given x, y, z values.
     */
    public Vector3 crossLocal(final double x, final double y, final double z) {
        final double newX = getY() * z - getZ() * y;
        final double newY = getZ() * x - getX() * z;
        final double newZ = getX() * y - getY() * x;
        set(newX, newY, newZ);
        return this;
    }

    /**
     * @param vec
     * @return this vector, set to the cross product of this vector with the given vector's x, y, z values
     * @throws NullPointerException
     *             if vec is null.
     */
    public Vector3 crossLocal(final ReadOnlyVector3 vec) {
        return crossLocal(vec.getX(), vec.getY(), vec.getZ());
    }

    /**
     * @param otherVector
     *            a unit vector to find the angle against
     * @return the minimum angle (in radians) between two vectors. It is assumed that both this vector and the given
     *         vector are unit vectors (normalized).
     * @throws NullPointerException
     *             if otherVector is null.
     */
    @Override
    public double smallestAngleBetween(final ReadOnlyVector3 otherVector) {
        return MathUtils.acos(dot(otherVector));
    }

    /**
     * Check a vector... if it is null or its doubles are NaN or infinite, return false. Else return true.
     *
     * @param vector
     *            the vector to check
     * @return true or false as stated above.
     */
    public static boolean isValid(final ReadOnlyVector3 vector) {
        if (vector == null) {
            return false;
        }
        if (Double.isNaN(vector.getX()) || Double.isNaN(vector.getY()) || Double.isNaN(vector.getZ())) {
            return false;
        }
        if (Double.isInfinite(vector.getX()) || Double.isInfinite(vector.getY()) || Double.isInfinite(vector.getZ())) {
            return false;
        }
        return true;
    }

    /**
     * Check if a vector is non-null and has infinite values.
     *
     * @param vector
     *            the vector to check
     * @return true or false as stated above.
     */
    public static boolean isInfinite(final ReadOnlyVector3 vector) {
        if (vector == null) {
            return false;
        }
        if (Double.isInfinite(vector.getX()) || Double.isInfinite(vector.getY()) || Double.isInfinite(vector.getZ())) {
            return true;
        }
        return false;
    }

    /**
     * @return the string representation of this vector.
     */
    @Override
    public String toString() {
        return "com.ardor3d.math.Vector3 [X=" + getX() + ", Y=" + getY() + ", Z=" + getZ() + "]";
    }

    /**
     * @return returns a unique code for this vector object based on its values. If two vectors are numerically equal,
     *         they will return the same hash code value.
     */
    @Override
    public int hashCode() {
        int result = 17;

        final long x = Double.doubleToLongBits(getX());
        result += 31 * result + (int) (x ^ x >>> 32);

        final long y = Double.doubleToLongBits(getY());
        result += 31 * result + (int) (y ^ y >>> 32);

        final long z = Double.doubleToLongBits(getZ());
        result += 31 * result + (int) (z ^ z >>> 32);

        return result;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this vector and the provided vector have the same x, y and z values.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyVector3)) {
            return false;
        }
        final ReadOnlyVector3 comp = (ReadOnlyVector3) o;
        return getX() == comp.getX() && getY() == comp.getY() && getZ() == comp.getZ();
    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public Vector3 clone() {
        return new Vector3(this);
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends Vector3> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(getX(), "x", 0);
        capsule.write(getY(), "y", 0);
        capsule.write(getZ(), "z", 0);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        setX(capsule.readDouble("x", 0));
        setY(capsule.readDouble("y", 0));
        setZ(capsule.readDouble("z", 0));
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
        setX(in.readDouble());
        setY(in.readDouble());
        setZ(in.readDouble());
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
        out.writeDouble(getX());
        out.writeDouble(getY());
        out.writeDouble(getZ());
    }

    // /////////////////
    // Methods for creating temp variables (pooling)
    // /////////////////

    /**
     * @return An instance of Vector3 that is intended for temporary use in calculations and so forth. Multiple calls to
     *         the method should return instances of this class that are not currently in use.
     */
    public final static Vector3 fetchTempInstance() {
        if (MathConstants.useMathPools) {
            return Vector3.VEC_POOL.fetch();
        } else {
            return new Vector3();
        }
    }

    /**
     * Releases a Vector3 back to be used by a future call to fetchTempInstance. TAKE CARE: this Vector3 object should
     * no longer have other classes referencing it or "Bad Things" will happen.
     *
     * @param vec
     *            the Vector3 to release.
     */
    public final static void releaseTempInstance(final Vector3 vec) {
        if (MathConstants.useMathPools) {
            Vector3.VEC_POOL.release(vec);
        }
    }
}
