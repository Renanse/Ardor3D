/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
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

import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Vector2 represents a point or vector in a two dimensional system. This implementation stores its data in
 * double-precision.
 */
public class Vector2 implements Cloneable, Savable, Externalizable, ReadOnlyVector2, Poolable {

    private static final long serialVersionUID = 1L;

    private static final ObjectPool<Vector2> VEC_POOL = ObjectPool.create(Vector2.class, MathConstants.maxMathPoolSize);

    /**
     * 0, 0
     */
    public final static ReadOnlyVector2 ZERO = new Vector2(0, 0);

    /**
     * 1, 1
     */
    public final static ReadOnlyVector2 ONE = new Vector2(1, 1);

    /**
     * -1, -1
     */
    public final static ReadOnlyVector2 NEG_ONE = new Vector2(-1, -1);

    /**
     * 1, 0
     */
    public final static ReadOnlyVector2 UNIT_X = new Vector2(1, 0);

    /**
     * -1, 0
     */
    public final static ReadOnlyVector2 NEG_UNIT_X = new Vector2(-1, 0);

    /**
     * 0, 1
     */
    public final static ReadOnlyVector2 UNIT_Y = new Vector2(0, 1);

    /**
     * 0, -1
     */
    public final static ReadOnlyVector2 NEG_UNIT_Y = new Vector2(0, -1);

    protected double _x = 0;
    protected double _y = 0;

    /**
     * Constructs a new vector set to (0, 0).
     */
    public Vector2() {
        this(0, 0);
    }

    /**
     * Constructs a new vector set to the (x, y) values of the given source vector.
     * 
     * @param src
     */
    public Vector2(final ReadOnlyVector2 src) {
        this(src.getX(), src.getY());
    }

    /**
     * Constructs a new vector set to (x, y).
     * 
     * @param x
     * @param y
     */
    public Vector2(final double x, final double y) {
        _x = x;
        _y = y;
    }

    @Override
    public double getX() {
        return _x;
    }

    @Override
    public double getY() {
        return _y;
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
     * @param index
     * @return x value if index == 0 or y value if index == 1
     * @throws IllegalArgumentException
     *             if index is not one of 0, 1.
     */
    @Override
    public double getValue(final int index) {
        switch (index) {
            case 0:
                return getX();
            case 1:
                return getY();
        }
        throw new IllegalArgumentException("index must be either 0 or 1");
    }

    /**
     * @param index
     *            which field index in this vector to set.
     * @param value
     *            to set to one of x or y.
     * @throws IllegalArgumentException
     *             if index is not one of 0, 1.
     */
    public void setValue(final int index, final double value) {
        switch (index) {
            case 0:
                setX(value);
                return;
            case 1:
                setY(value);
                return;
        }
        throw new IllegalArgumentException("index must be either 0 or 1");
    }

    /**
     * Stores the double values of this vector in the given double array.
     * 
     * @param store
     *            if null, a new double[2] array is created.
     * @return the double array
     * @throws ArrayIndexOutOfBoundsException
     *             if store is not at least length 2.
     */
    @Override
    public double[] toArray(double[] store) {
        if (store == null) {
            store = new double[2];
        }
        // do last first to ensure size is correct before any edits occur.
        store[1] = getY();
        store[0] = getX();
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
     * Sets the value of this vector to (x, y)
     * 
     * @param x
     * @param y
     * @return this vector for chaining
     */
    public Vector2 set(final double x, final double y) {
        setX(x);
        setY(y);
        return this;
    }

    /**
     * Sets the value of this vector to the (x, y) values of the provided source vector.
     * 
     * @param source
     * @return this vector for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Vector2 set(final ReadOnlyVector2 source) {
        setX(source.getX());
        setY(source.getY());
        return this;
    }

    /**
     * Sets the value of this vector to (0, 0)
     * 
     * @return this vector for chaining
     */
    public Vector2 zero() {
        return set(0, 0);
    }

    /**
     * Adds the given values to those of this vector and returns them in store * @param store the vector to store the
     * result in for return. If null, a new vector object is created and returned. .
     * 
     * @param x
     * @param y
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return (this.x + x, this.y + y)
     */
    @Override
    public Vector2 add(final double x, final double y, final Vector2 store) {
        Vector2 result = store;
        if (result == null) {
            result = new Vector2();
        }

        return result.set(getX() + x, getY() + y);
    }

    /**
     * Increments the values of this vector with the given x and y values.
     * 
     * @param x
     * @param y
     * @return this vector for chaining
     */
    public Vector2 addLocal(final double x, final double y) {
        return set(getX() + x, getY() + y);
    }

    /**
     * Adds the values of the given source vector to those of this vector and returns them in store.
     * 
     * @param source
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return (this.x + source.x, this.y + source.y)
     * @throws NullPointerException
     *             if source is null.
     */
    @Override
    public Vector2 add(final ReadOnlyVector2 source, final Vector2 store) {
        return add(source.getX(), source.getY(), store);
    }

    /**
     * Increments the values of this vector with the x and y values of the given vector.
     * 
     * @param source
     * @return this vector for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Vector2 addLocal(final ReadOnlyVector2 source) {
        return addLocal(source.getX(), source.getY());
    }

    /**
     * Subtracts the given values from those of this vector and returns them in store.
     * 
     * @param x
     * @param y
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return (this.x - x, this.y - y)
     */
    @Override
    public Vector2 subtract(final double x, final double y, final Vector2 store) {
        Vector2 result = store;
        if (result == null) {
            result = new Vector2();
        }

        return result.set(getX() - x, getY() - y);
    }

    /**
     * Decrements the values of this vector by the given x and y values.
     * 
     * @param x
     * @param y
     * @return this vector for chaining
     */
    public Vector2 subtractLocal(final double x, final double y) {
        return set(getX() - x, getY() - y);
    }

    /**
     * Subtracts the values of the given source vector from those of this vector and returns them in store.
     * 
     * @param source
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return (this.x - source.x, this.y - source.y)
     * @throws NullPointerException
     *             if source is null.
     */
    @Override
    public Vector2 subtract(final ReadOnlyVector2 source, final Vector2 store) {
        return subtract(source.getX(), source.getY(), store);
    }

    /**
     * Decrements the values of this vector by the x and y values from the given source vector.
     * 
     * @param source
     * @return this vector for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Vector2 subtractLocal(final ReadOnlyVector2 source) {
        return subtractLocal(source.getX(), source.getY());
    }

    /**
     * Multiplies the values of this vector by the given scalar value and returns the result in store.
     * 
     * @param scalar
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x * scalar, this.y * scalar)
     */
    @Override
    public Vector2 multiply(final double scalar, final Vector2 store) {
        Vector2 result = store;
        if (result == null) {
            result = new Vector2();
        }

        return result.set(getX() * scalar, getY() * scalar);
    }

    /**
     * Internally modifies the values of this vector by multiplying them each by the given scalar value.
     * 
     * @param scalar
     * @return this vector for chaining
     */
    public Vector2 multiplyLocal(final double scalar) {
        return set(getX() * scalar, getY() * scalar);
    }

    /**
     * Multiplies the values of this vector by the given scale values and returns the result in store.
     * 
     * @param scale
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x * scale.x, this.y * scale.y)
     */
    @Override
    public Vector2 multiply(final ReadOnlyVector2 scale, final Vector2 store) {
        Vector2 result = store;
        if (result == null) {
            result = new Vector2();
        }

        return result.set(getX() * scale.getX(), getY() * scale.getY());
    }

    /**
     * Internally modifies the values of this vector by multiplying them each by the values of the given scale.
     * 
     * @param scale
     * @return this vector for chaining
     */
    public Vector2 multiplyLocal(final ReadOnlyVector2 scale) {
        return set(getX() * scale.getX(), getY() * scale.getY());
    }

    /**
     * Multiplies the values of this vector by the given scale values and returns the result in store.
     * 
     * @param x
     * @param y
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x * scale.x, this.y * scale.y)
     */
    @Override
    public Vector2 multiply(final double x, final double y, final Vector2 store) {
        Vector2 result = store;
        if (result == null) {
            result = new Vector2();
        }

        return result.set(getX() * x, getY() * y);
    }

    /**
     * Internally modifies the values of this vector by multiplying them each by the values of the given scale.
     * 
     * @param x
     * @param y
     * @return this vector for chaining
     */
    public Vector2 multiplyLocal(final double x, final double y) {
        return set(getX() * x, getY() * y);
    }

    /**
     * Divides the values of this vector by the given scalar value and returns the result in store.
     * 
     * @param scalar
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x / scalar, this.y / scalar)
     */
    @Override
    public Vector2 divide(final double scalar, final Vector2 store) {
        Vector2 result = store;
        if (result == null) {
            result = new Vector2();
        }

        return result.set(getX() / scalar, getY() / scalar);
    }

    /**
     * Internally modifies the values of this vector by dividing them each by the given scalar value.
     * 
     * @param scalar
     * @return this vector for chaining
     * @throws ArithmeticException
     *             if scalar is 0
     */
    public Vector2 divideLocal(final double scalar) {
        final double invScalar = 1.0 / scalar;

        return set(getX() * invScalar, getY() * invScalar);
    }

    /**
     * Divides the values of this vector by the given scale values and returns the result in store.
     * 
     * @param scale
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x / scale.x, this.y / scale.y)
     */
    @Override
    public Vector2 divide(final ReadOnlyVector2 scale, final Vector2 store) {
        Vector2 result = store;
        if (result == null) {
            result = new Vector2();
        }

        return result.set(getX() / scale.getX(), getY() / scale.getY());
    }

    /**
     * Internally modifies the values of this vector by dividing them each by the values of the given scale.
     * 
     * @param scale
     * @return this vector for chaining
     */
    public Vector2 divideLocal(final ReadOnlyVector2 scale) {
        return set(getX() / scale.getX(), getY() / scale.getY());
    }

    /**
     * Divides the values of this vector by the given scale values and returns the result in store.
     * 
     * @param x
     * @param y
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector (this.x / scale.x, this.y / scale.y)
     */
    @Override
    public Vector2 divide(final double x, final double y, final Vector2 store) {
        Vector2 result = store;
        if (result == null) {
            result = new Vector2();
        }

        return result.set(getX() / x, getY() / y);
    }

    /**
     * Internally modifies the values of this vector by dividing them each by the values of the given scale.
     * 
     * @param x
     * @param y
     * @return this vector for chaining
     */
    public Vector2 divideLocal(final double x, final double y) {
        return set(getX() / x, getY() / y);
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
    public Vector2 scaleAddLocal(final double scale, final ReadOnlyVector2 add) {
        _x = _x * scale + add.getX();
        _y = _y * scale + add.getY();
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
    public Vector2 scaleAdd(final double scale, final ReadOnlyVector2 add, final Vector2 store) {
        Vector2 result = store;
        if (result == null) {
            result = new Vector2();
        }

        result.setX(_x * scale + add.getX());
        result.setY(_y * scale + add.getY());
        return result;
    }

    /**
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return same as multiply(-1, store)
     */
    @Override
    public Vector2 negate(final Vector2 store) {
        return multiply(-1, store);
    }

    /**
     * @return same as multiplyLocal(-1)
     */
    public Vector2 negateLocal() {
        return multiplyLocal(-1);
    }

    /**
     * Creates a new unit length vector from this one by dividing by length. If the length is 0, (ie, if the vector is
     * 0, 0) then a new vector (0, 0) is returned.
     * 
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new unit vector (or 0, 0 if this unit is 0 length)
     */
    @Override
    public Vector2 normalize(final Vector2 store) {
        final double lengthSq = lengthSquared();
        if (Math.abs(lengthSq) > MathUtils.EPSILON) {
            return multiply(MathUtils.inverseSqrt(lengthSq), store);
        }

        return store != null ? store.set(Vector2.ZERO) : new Vector2(Vector2.ZERO);
    }

    /**
     * Converts this vector into a unit vector by dividing it internally by its length. If the length is 0, (ie, if the
     * vector is 0, 0) then no action is taken.
     * 
     * @return this vector for chaining
     */
    public Vector2 normalizeLocal() {
        final double lengthSq = lengthSquared();
        if (Math.abs(lengthSq) > MathUtils.EPSILON) {
            return multiplyLocal(MathUtils.inverseSqrt(lengthSq));
        }

        return this;
    }

    /**
     * Creates a new vector representing this vector rotated around 0,0 by a specified angle in a given direction.
     * 
     * @param angle
     *            in radians
     * @param clockwise
     *            true to rotate in a clockwise direction
     * @param store
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return the new rotated vector
     */
    @Override
    public Vector2 rotateAroundOrigin(double angle, final boolean clockwise, final Vector2 store) {
        Vector2 result = store;
        if (result == null) {
            result = new Vector2();
        }

        if (clockwise) {
            angle = -angle;
        }
        final double newX = MathUtils.cos(angle) * getX() - MathUtils.sin(angle) * getY();
        final double newY = MathUtils.sin(angle) * getX() + MathUtils.cos(angle) * getY();
        return result.set(newX, newY);
    }

    /**
     * Internally rotates this vector around 0,0 by a specified angle in a given direction.
     * 
     * @param angle
     *            in radians
     * @param clockwise
     *            true to rotate in a clockwise direction
     * @return this vector for chaining
     */
    public Vector2 rotateAroundOriginLocal(double angle, final boolean clockwise) {
        if (clockwise) {
            angle = -angle;
        }
        final double newX = MathUtils.cos(angle) * getX() - MathUtils.sin(angle) * getY();
        final double newY = MathUtils.sin(angle) * getX() + MathUtils.cos(angle) * getY();
        return set(newX, newY);
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
    public Vector2 lerp(final ReadOnlyVector2 endVec, final double scalar, final Vector2 store) {
        Vector2 result = store;
        if (result == null) {
            result = new Vector2();
        }

        final double x = (1.0 - scalar) * getX() + scalar * endVec.getX();
        final double y = (1.0 - scalar) * getY() + scalar * endVec.getY();
        return result.set(x, y);
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
    public Vector2 lerpLocal(final ReadOnlyVector2 endVec, final double scalar) {
        setX((1.0 - scalar) * getX() + scalar * endVec.getX());
        setY((1.0 - scalar) * getY() + scalar * endVec.getY());
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
     *            the vector to store the result in for return. If null, a new vector object is created and returned.
     * @return a new vector as described above.
     * @throws NullPointerException
     *             if beginVec or endVec are null.
     */
    public static Vector2 lerp(final ReadOnlyVector2 beginVec, final ReadOnlyVector2 endVec, final double scalar,
            final Vector2 store) {
        Vector2 result = store;
        if (result == null) {
            result = new Vector2();
        }

        final double x = (1.0 - scalar) * beginVec.getX() + scalar * endVec.getX();
        final double y = (1.0 - scalar) * beginVec.getY() + scalar * endVec.getY();
        return result.set(x, y);
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
    public Vector2 lerpLocal(final ReadOnlyVector2 beginVec, final ReadOnlyVector2 endVec, final double scalar) {
        setX((1.0 - scalar) * beginVec.getX() + scalar * endVec.getX());
        setY((1.0 - scalar) * beginVec.getY() + scalar * endVec.getY());
        return this;
    }

    /**
     * @return the magnitude of this vector, or the distance between the origin (0, 0) and the point described by (x,
     *         y). Basically sqrt(x^2 + y^2)
     */
    @Override
    public double length() {
        return MathUtils.sqrt(lengthSquared());
    }

    /**
     * @return the squared magnitude of this vector. (x^2 + y^2)
     */
    @Override
    public double lengthSquared() {
        return getX() * getX() + getY() * getY();
    }

    /**
     * @param x
     * @param y
     * @return the squared distance between the point described by this vector and the given x, y point. When comparing
     *         the relative distance between two points it is usually sufficient to compare the squared distances, thus
     *         avoiding an expensive square root operation.
     */
    @Override
    public double distanceSquared(final double x, final double y) {
        final double dx = getX() - x;
        final double dy = getY() - y;
        return dx * dx + dy * dy;
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
    public double distanceSquared(final ReadOnlyVector2 destination) {
        return distanceSquared(destination.getX(), destination.getY());
    }

    /**
     * @param x
     * @param y
     * @return the distance between the point described by this vector and the given x, y point.
     */
    @Override
    public double distance(final double x, final double y) {
        return MathUtils.sqrt(distanceSquared(x, y));
    }

    /**
     * @param destination
     * @return the distance between the point described by this vector and the given destination point.
     * @throws NullPointerException
     *             if destination is null.
     */
    @Override
    public double distance(final ReadOnlyVector2 destination) {
        return MathUtils.sqrt(distanceSquared(destination));
    }

    /**
     * @param x
     * @param y
     * @return the dot product of this vector with the given x, y values.
     */
    @Override
    public double dot(final double x, final double y) {
        return getX() * x + getY() * y;
    }

    /**
     * @param vec
     * @return the dot product of this vector with the x, y values of the given vector.
     * @throws NullPointerException
     *             if vec is null.
     */
    @Override
    public double dot(final ReadOnlyVector2 vec) {
        return dot(vec.getX(), vec.getY());
    }

    /**
     * @return the angle - in radians [-pi, pi) - represented by this Vector2 as expressed by a conversion from
     *         rectangular coordinates (<code>x</code>,&nbsp;<code>y</code>) to polar coordinates
     *         (r,&nbsp;<i>theta</i>).
     */
    @Override
    public double getPolarAngle() {
        return -Math.atan2(getY(), getX());
    }

    /**
     * @param otherVector
     *            the "destination" unit vector
     * @return the angle (in radians) required to rotate a ray represented by this vector to lie co-linear to a ray
     *         described by the given vector. It is assumed that both this vector and the given vector are unit vectors
     *         (normalized).
     * @throws NullPointerException
     *             if otherVector is null.
     */
    @Override
    public double angleBetween(final ReadOnlyVector2 otherVector) {
        return Math.atan2(otherVector.getY(), otherVector.getX()) - Math.atan2(getY(), getX());
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
    public double smallestAngleBetween(final ReadOnlyVector2 otherVector) {
        final double dotProduct = dot(otherVector);
        return MathUtils.acos(dotProduct);
    }

    /**
     * Check a vector... if it is null or its doubles are NaN or infinite, return false. Else return true.
     * 
     * @param vector
     *            the vector to check
     * @return true or false as stated above.
     */
    public static boolean isValid(final ReadOnlyVector2 vector) {
        if (vector == null) {
            return false;
        }
        if (Double.isNaN(vector.getX()) || Double.isNaN(vector.getY())) {
            return false;
        }
        if (Double.isInfinite(vector.getX()) || Double.isInfinite(vector.getY())) {
            return false;
        }
        return true;
    }

    /**
     * @return the string representation of this vector.
     */
    @Override
    public String toString() {
        return "com.ardor3d.math.Vector2 [X=" + getX() + ", Y=" + getY() + "]";
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

        return result;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this vector and the provided vector have the same x and y values.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyVector2)) {
            return false;
        }
        final ReadOnlyVector2 comp = (ReadOnlyVector2) o;
        return getX() == comp.getX() && getY() == comp.getY();
    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public Vector2 clone() {
        return new Vector2(this);
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends Vector2> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(getX(), "x", 0);
        capsule.write(getY(), "y", 0);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        setX(capsule.readDouble("x", 0));
        setY(capsule.readDouble("y", 0));
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
    }

    // /////////////////
    // Methods for creating temp variables (pooling)
    // /////////////////

    /**
     * @return An instance of Vector2 that is intended for temporary use in calculations and so forth. Multiple calls to
     *         the method should return instances of this class that are not currently in use.
     */
    public final static Vector2 fetchTempInstance() {
        if (MathConstants.useMathPools) {
            return Vector2.VEC_POOL.fetch();
        } else {
            return new Vector2();
        }
    }

    /**
     * Releases a Vector2 back to be used by a future call to fetchTempInstance. TAKE CARE: this Vector2 object should
     * no longer have other classes referencing it or "Bad Things" will happen.
     * 
     * @param vec
     *            the Vector2 to release.
     */
    public final static void releaseTempInstance(final Vector2 vec) {
        if (MathConstants.useMathPools) {
            Vector2.VEC_POOL.release(vec);
        }
    }
}
