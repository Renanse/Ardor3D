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

import com.ardor3d.math.type.ReadOnlyVector4;
import com.ardor3d.math.util.EqualsUtil;
import com.ardor3d.math.util.HashUtil;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Vector4 represents a point or vector in a four dimensional system. This implementation stores its
 * data in double-precision.
 */
public class Vector4 implements Cloneable, Savable, Externalizable, ReadOnlyVector4, Poolable {

  private static final long serialVersionUID = 1L;

  private static final ObjectPool<Vector4> VEC_POOL = ObjectPool.create(Vector4.class, MathConstants.maxMathPoolSize);

  /**
   * 0, 0, 0, 0
   */
  public final static ReadOnlyVector4 ZERO = new Vector4(0, 0, 0, 0);

  /**
   * 1, 1, 1, 1
   */
  public final static ReadOnlyVector4 ONE = new Vector4(1, 1, 1, 1);

  /**
   * -1, -1, -1, -1
   */
  public final static ReadOnlyVector4 NEG_ONE = new Vector4(-1, -1, -1, -1);

  /**
   * 1, 0, 0, 0
   */
  public final static ReadOnlyVector4 UNIT_X = new Vector4(1, 0, 0, 0);

  /**
   * -1, 0, 0, 0
   */
  public final static ReadOnlyVector4 NEG_UNIT_X = new Vector4(-1, 0, 0, 0);

  /**
   * 0, 1, 0, 0
   */
  public final static ReadOnlyVector4 UNIT_Y = new Vector4(0, 1, 0, 0);

  /**
   * 0, -1, 0, 0
   */
  public final static ReadOnlyVector4 NEG_UNIT_Y = new Vector4(0, -1, 0, 0);

  /**
   * 0, 0, 1, 0
   */
  public final static ReadOnlyVector4 UNIT_Z = new Vector4(0, 0, 1, 0);

  /**
   * 0, 0, -1, 0
   */
  public final static ReadOnlyVector4 NEG_UNIT_Z = new Vector4(0, 0, -1, 0);

  /**
   * 0, 0, 0, 1
   */
  public final static ReadOnlyVector4 UNIT_W = new Vector4(0, 0, 0, 1);

  /**
   * 0, 0, 0, -1
   */
  public final static ReadOnlyVector4 NEG_UNIT_W = new Vector4(0, 0, 0, -1);

  protected double _x = 0;
  protected double _y = 0;
  protected double _z = 0;
  protected double _w = 0;

  /**
   * Constructs a new vector set to (0, 0, 0, 0).
   */
  public Vector4() {
    this(0, 0, 0, 0);
  }

  /**
   * Constructs a new vector set to the (x, y, z, w) values of the given source vector.
   *
   * @param src
   */
  public Vector4(final ReadOnlyVector4 src) {
    this(src.getX(), src.getY(), src.getZ(), src.getW());
  }

  /**
   * Constructs a new vector set to (x, y, z, w).
   *
   * @param x
   * @param y
   * @param z
   * @param w
   */
  public Vector4(final double x, final double y, final double z, final double w) {
    _x = x;
    _y = y;
    _z = z;
    _w = w;
  }

  @Override
  public double getX() { return _x; }

  @Override
  public double getY() { return _y; }

  @Override
  public double getZ() { return _z; }

  @Override
  public double getW() { return _w; }

  /**
   * @return x as a float, to decrease need for explicit casts.
   */
  @Override
  public float getXf() { return (float) _x; }

  /**
   * @return y as a float, to decrease need for explicit casts.
   */
  @Override
  public float getYf() { return (float) _y; }

  /**
   * @return z as a float, to decrease need for explicit casts.
   */
  @Override
  public float getZf() { return (float) _z; }

  /**
   * @return w as a float, to decrease need for explicit casts.
   */
  @Override
  public float getWf() { return (float) _w; }

  /**
   * @param index
   * @return x value if index == 0, y value if index == 1, z value if index == 2 or w value if index
   *         == 3
   * @throws IllegalArgumentException
   *           if index is not one of 0, 1, 2, 3.
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
      case 3:
        return getW();
    }
    throw new IllegalArgumentException("index must be either 0, 1, 2 or 3");
  }

  /**
   * @param index
   *          which field index in this vector to set.
   * @param value
   *          to set to one of x, y, z or w.
   * @throws IllegalArgumentException
   *           if index is not one of 0, 1, 2, 3.
   *
   *           if this vector is read only
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
      case 3:
        setW(value);
        return;
    }
    throw new IllegalArgumentException("index must be either 0, 1, 2 or 3");
  }

  /**
   * Stores the double values of this vector in the given double array.
   *
   * @param store
   *          if null, a new double[4] array is created.
   * @return the double array
   * @throws ArrayIndexOutOfBoundsException
   *           if store is not at least length 4.
   */
  @Override
  public double[] toArray(double[] store) {
    if (store == null) {
      store = new double[4];
    }
    // do last first to ensure size is correct before any edits occur.
    store[3] = getW();
    store[2] = getZ();
    store[1] = getY();
    store[0] = getX();
    return store;
  }

  /**
   * Sets the first component of this vector to the given double value.
   *
   * @param x
   */
  public void setX(final double x) { _x = x; }

  /**
   * Sets the second component of this vector to the given double value.
   *
   * @param y
   */
  public void setY(final double y) { _y = y; }

  /**
   * Sets the third component of this vector to the given double value.
   *
   * @param z
   */
  public void setZ(final double z) { _z = z; }

  /**
   * Sets the fourth component of this vector to the given double value.
   *
   * @param w
   */
  public void setW(final double w) { _w = w; }

  /**
   * Sets the value of this vector to (x, y, z, w)
   *
   * @param x
   * @param y
   * @param z
   * @param w
   * @return this vector for chaining
   */
  public Vector4 set(final double x, final double y, final double z, final double w) {
    setX(x);
    setY(y);
    setZ(z);
    setW(w);
    return this;
  }

  /**
   * Sets the value of this vector to the (x, y, z, w) values of the provided source vector.
   *
   * @param source
   * @return this vector for chaining
   * @throws NullPointerException
   *           if source is null.
   */
  public Vector4 set(final ReadOnlyVector4 source) {
    setX(source.getX());
    setY(source.getY());
    setZ(source.getZ());
    setW(source.getW());
    return this;
  }

  /**
   * Sets the value of this vector to (0, 0, 0, 0)
   *
   * @return this vector for chaining
   */
  public Vector4 zero() {
    return set(0, 0, 0, 0);
  }

  /**
   * Adds the given values to those of this vector and returns them in store.
   *
   * @param x
   * @param y
   * @param z
   * @param w
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return (this.x + x, this.y + y, this.z + z, this.w + w)
   */
  @Override
  public Vector4 add(final double x, final double y, final double z, final double w, final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }

    return result.set(getX() + x, getY() + y, getZ() + z, getW() + w);
  }

  /**
   * Increments the values of this vector with the given x, y, z and w values.
   *
   * @param x
   * @param y
   * @param z
   * @param w
   * @return this vector for chaining
   */
  public Vector4 addLocal(final double x, final double y, final double z, final double w) {
    return set(getX() + x, getY() + y, getZ() + z, getW() + w);
  }

  /**
   * Adds the values of the given source vector to those of this vector and returns them in store.
   *
   * @param source
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return (this.x + source.x, this.y + source.y, this.z + source.z, this.w + source.w)
   * @throws NullPointerException
   *           if source is null.
   */
  @Override
  public Vector4 add(final ReadOnlyVector4 source, final Vector4 store) {
    return add(source.getX(), source.getY(), source.getZ(), source.getW(), store);
  }

  /**
   * Increments the values of this vector with the x, y, z and w values of the given vector.
   *
   * @param source
   * @return this vector for chaining
   * @throws NullPointerException
   *           if source is null.
   */
  public Vector4 addLocal(final ReadOnlyVector4 source) {
    return addLocal(source.getX(), source.getY(), source.getZ(), source.getW());
  }

  /**
   * Subtracts the given values from those of this vector and returns them in store.
   *
   * @param x
   * @param y
   * @param z
   * @param w
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return (this.x - x, this.y - y, this.z - z, this.w - w)
   */
  @Override
  public Vector4 subtract(final double x, final double y, final double z, final double w, final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }

    return result.set(getX() - x, getY() - y, getZ() - z, getW() - w);
  }

  /**
   * Decrements the values of this vector by the given x, y, z and w values.
   *
   * @param x
   * @param y
   * @param z
   * @param w
   * @return this vector for chaining
   */
  public Vector4 subtractLocal(final double x, final double y, final double z, final double w) {
    return set(getX() - x, getY() - y, getZ() - z, getW() - w);
  }

  /**
   * Subtracts the values of the given source vector from those of this vector and returns them in
   * store.
   *
   * @param source
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return (this.x - source.x, this.y - source.y, this.z - source.z, this.w - source.w)
   * @throws NullPointerException
   *           if source is null.
   */
  @Override
  public Vector4 subtract(final ReadOnlyVector4 source, final Vector4 store) {
    return subtract(source.getX(), source.getY(), source.getZ(), source.getW(), store);
  }

  /**
   * Decrements the values of this vector by the x, y, z and w values from the given source vector.
   *
   * @param source
   * @return this vector for chaining
   * @throws NullPointerException
   *           if source is null.
   */
  public Vector4 subtractLocal(final ReadOnlyVector4 source) {
    return subtractLocal(source.getX(), source.getY(), source.getZ(), source.getW());
  }

  /**
   * Multiplies the values of this vector by the given scalar value and returns the result in store.
   *
   * @param scalar
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return a new vector (this.x * scalar, this.y * scalar, this.z * scalar, this.w * scalar)
   */
  @Override
  public Vector4 multiply(final double scalar, final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }

    return result.set(getX() * scalar, getY() * scalar, getZ() * scalar, getW() * scalar);
  }

  /**
   * Internally modifies the values of this vector by multiplying them each by the given scalar value.
   *
   * @param scalar
   * @return this vector for chaining
   */
  public Vector4 multiplyLocal(final double scalar) {
    return set(getX() * scalar, getY() * scalar, getZ() * scalar, getW() * scalar);
  }

  /**
   * Multiplies the values of this vector by the given scalar value and returns the result in store.
   *
   * @param scale
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return a new vector (this.x * scale.x, this.y * scale.y, this.z * scale.z, this.w * scale.w)
   */
  @Override
  public Vector4 multiply(final ReadOnlyVector4 scale, final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }

    return result.set(getX() * scale.getX(), getY() * scale.getY(), getZ() * scale.getZ(), getW() * scale.getW());
  }

  /**
   * Internally modifies the values of this vector by multiplying them each by the given scale values.
   *
   * @param scale
   * @return this vector for chaining
   */
  public Vector4 multiplyLocal(final ReadOnlyVector4 scale) {
    return set(getX() * scale.getX(), getY() * scale.getY(), getZ() * scale.getZ(), getW() * scale.getW());
  }

  /**
   * Multiplies the values of this vector by the given scalar value and returns the result in store.
   *
   * @param x
   * @param y
   * @param z
   * @param w
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return a new vector (this.x * scale.x, this.y * scale.y, this.z * scale.z, this.w * scale.w)
   */
  @Override
  public Vector4 multiply(final double x, final double y, final double z, final double w, final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }

    return result.set(getX() * x, getY() * y, getZ() * z, getW() * w);
  }

  /**
   * Internally modifies the values of this vector by multiplying them each by the given scale values.
   *
   * @param x
   * @param y
   * @param z
   * @param w
   * @return this vector for chaining
   */
  public Vector4 multiplyLocal(final double x, final double y, final double z, final double w) {
    return set(getX() * x, getY() * y, getZ() * z, getW() * w);
  }

  /**
   * Divides the values of this vector by the given scalar value and returns the result in store.
   *
   * @param scalar
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return a new vector (this.x / scalar, this.y / scalar, this.z / scalar, this.w / scalar)
   */
  @Override
  public Vector4 divide(final double scalar, final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }

    return result.set(getX() / scalar, getY() / scalar, getZ() / scalar, getW() / scalar);
  }

  /**
   * Internally modifies the values of this vector by dividing them each by the given scalar value.
   *
   * @param scalar
   * @return this vector for chaining
   *
   *
   * @throws ArithmeticException
   *           if scalar is 0
   */
  public Vector4 divideLocal(final double scalar) {
    final double invScalar = 1.0 / scalar;

    return set(getX() * invScalar, getY() * invScalar, getZ() * invScalar, getW() * invScalar);
  }

  /**
   * Divides the values of this vector by the given scale values and returns the result in store.
   *
   * @param scale
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return a new vector (this.x / scale.x, this.y / scale.y, this.z / scale.z, this.w / scale.w)
   */
  @Override
  public Vector4 divide(final ReadOnlyVector4 scale, final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }

    return result.set(getX() / scale.getX(), getY() / scale.getY(), getZ() / scale.getZ(), getW() / scale.getW());
  }

  /**
   * Internally modifies the values of this vector by dividing them each by the given scale values.
   *
   * @param scale
   * @return this vector for chaining
   */
  public Vector4 divideLocal(final ReadOnlyVector4 scale) {
    return set(getX() / scale.getX(), getY() / scale.getY(), getZ() / scale.getZ(), getW() / scale.getW());
  }

  /**
   * Divides the values of this vector by the given scale values and returns the result in store.
   *
   * @param x
   * @param y
   * @param z
   * @param w
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return a new vector (this.x / scale.x, this.y / scale.y, this.z / scale.z, this.w / scale.w)
   */
  @Override
  public Vector4 divide(final double x, final double y, final double z, final double w, final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }

    return result.set(getX() / x, getY() / y, getZ() / z, getW() / w);
  }

  /**
   * Internally modifies the values of this vector by dividing them each by the given scale values.
   *
   * @param x
   * @param y
   * @param z
   * @param w
   * @return this vector for chaining
   */
  public Vector4 divideLocal(final double x, final double y, final double z, final double w) {
    return set(getX() / x, getY() / y, getZ() / z, getW() / w);
  }

  /**
   *
   * Internally modifies this vector by multiplying its values with a given scale value, then adding a
   * given "add" value.
   *
   * @param scale
   *          the value to multiply this vector by.
   * @param add
   *          the value to add to the result
   * @return this vector for chaining
   */
  public Vector4 scaleAddLocal(final double scale, final Vector4 add) {
    _x = _x * scale + add.getX();
    _y = _y * scale + add.getY();
    _z = _z * scale + add.getZ();
    _w = _w * scale + add.getW();
    return this;
  }

  /**
   * Scales this vector by multiplying its values with a given scale value, then adding a given "add"
   * value. The result is store in the given store parameter.
   *
   * @param scale
   *          the value to multiply by.
   * @param add
   *          the value to add
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return the store variable
   */
  @Override
  public Vector4 scaleAdd(final double scale, final ReadOnlyVector4 add, final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }

    result.setX(_x * scale + add.getX());
    result.setY(_y * scale + add.getY());
    result.setZ(_z * scale + add.getZ());
    result.setW(_w * scale + add.getW());
    return result;
  }

  /**
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return same as multiply(-1, store)
   */
  @Override
  public Vector4 negate(final Vector4 store) {
    return multiply(-1, store);
  }

  /**
   * @return same as multiplyLocal(-1)
   */
  public Vector4 negateLocal() {
    return multiplyLocal(-1);
  }

  /**
   * Creates a new unit length vector from this one by dividing by length. If the length is 0, (ie, if
   * the vector is 0, 0, 0, 0) then a new vector (0, 0, 0, 0) is returned.
   *
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return a new unit vector (or 0, 0, 0, 0 if this unit is 0 length)
   */
  @Override
  public Vector4 normalize(final Vector4 store) {
    final double lengthSq = lengthSquared();
    if (Math.abs(lengthSq) > MathUtils.EPSILON) {
      return multiply(MathUtils.inverseSqrt(lengthSq), store);
    }

    return store != null ? store.set(Vector4.ZERO) : new Vector4(Vector4.ZERO);
  }

  /**
   * Converts this vector into a unit vector by dividing it internally by its length. If the length is
   * 0, (ie, if the vector is 0, 0, 0, 0) then no action is taken.
   *
   * @return this vector for chaining
   */
  public Vector4 normalizeLocal() {
    final double lengthSq = lengthSquared();
    if (Math.abs(lengthSq) > MathUtils.EPSILON) {
      return multiplyLocal(MathUtils.inverseSqrt(lengthSq));
    }

    return this;
  }

  /**
   * Performs a linear interpolation between this vector and the given end vector, using the given
   * scalar as a percent. iow, if changeAmnt is closer to 0, the result will be closer to the current
   * value of this vector and if it is closer to 1, the result will be closer to the end value. The
   * result is returned as a new vector object.
   *
   * @param endVec
   * @param scalar
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return a new vector as described above.
   * @throws NullPointerException
   *           if endVec is null.
   */
  @Override
  public Vector4 lerp(final ReadOnlyVector4 endVec, final double scalar, final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }

    final double x = (1.0 - scalar) * getX() + scalar * endVec.getX();
    final double y = (1.0 - scalar) * getY() + scalar * endVec.getY();
    final double z = (1.0 - scalar) * getZ() + scalar * endVec.getZ();
    final double w = (1.0 - scalar) * getW() + scalar * endVec.getW();
    return result.set(x, y, z, w);
  }

  /**
   * Performs a linear interpolation between this vector and the given end vector, using the given
   * scalar as a percent. iow, if changeAmnt is closer to 0, the result will be closer to the current
   * value of this vector and if it is closer to 1, the result will be closer to the end value. The
   * result is stored back in this vector.
   *
   * @param endVec
   * @param scalar
   * @return this vector for chaining
   *
   *
   * @throws NullPointerException
   *           if endVec is null.
   */
  public Vector4 lerpLocal(final ReadOnlyVector4 endVec, final double scalar) {
    setX((1.0 - scalar) * getX() + scalar * endVec.getX());
    setY((1.0 - scalar) * getY() + scalar * endVec.getY());
    setZ((1.0 - scalar) * getZ() + scalar * endVec.getZ());
    setW((1.0 - scalar) * getW() + scalar * endVec.getW());
    return this;
  }

  /**
   * Performs a linear interpolation between the given begin and end vectors, using the given scalar
   * as a percent. iow, if changeAmnt is closer to 0, the result will be closer to the begin value and
   * if it is closer to 1, the result will be closer to the end value. The result is returned as a new
   * vector object.
   *
   * @param beginVec
   * @param endVec
   * @param scalar
   *          the scalar as a percent.
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   * @return a new vector as described above.
   * @throws NullPointerException
   *           if beginVec or endVec are null.
   */
  public static Vector4 lerp(final ReadOnlyVector4 beginVec, final ReadOnlyVector4 endVec, final double scalar,
      final Vector4 store) {
    Vector4 result = store;
    if (result == null) {
      result = new Vector4();
    }

    final double x = (1.0 - scalar) * beginVec.getX() + scalar * endVec.getX();
    final double y = (1.0 - scalar) * beginVec.getY() + scalar * endVec.getY();
    final double z = (1.0 - scalar) * beginVec.getZ() + scalar * endVec.getZ();
    final double w = (1.0 - scalar) * beginVec.getW() + scalar * endVec.getW();
    return result.set(x, y, z, w);
  }

  /**
   * Performs a linear interpolation between the given begin and end vectors, using the given scalar
   * as a percent. iow, if changeAmnt is closer to 0, the result will be closer to the begin value and
   * if it is closer to 1, the result will be closer to the end value. The result is stored back in
   * this vector.
   *
   * @param beginVec
   * @param endVec
   * @param changeAmnt
   *          the scalar as a percent.
   * @return this vector for chaining
   *
   *
   * @throws NullPointerException
   *           if beginVec or endVec are null.
   */
  public Vector4 lerpLocal(final ReadOnlyVector4 beginVec, final ReadOnlyVector4 endVec, final double scalar) {
    setX((1.0 - scalar) * beginVec.getX() + scalar * endVec.getX());
    setY((1.0 - scalar) * beginVec.getY() + scalar * endVec.getY());
    setZ((1.0 - scalar) * beginVec.getZ() + scalar * endVec.getZ());
    setW((1.0 - scalar) * beginVec.getW() + scalar * endVec.getW());
    return this;
  }

  /**
   * @return the magnitude or distance between the origin (0, 0, 0, 0) and the point described by this
   *         vector (x, y, z, w). Effectively the square root of the value returned by
   *         {@link #lengthSquared()}.
   */
  @Override
  public double length() {
    return Math.sqrt(lengthSquared());
  }

  /**
   * @return the squared magnitude or squared distance between the origin (0, 0, 0, 0) and the point
   *         described by this vector (x, y, z, w)
   */
  @Override
  public double lengthSquared() {
    return getX() * getX() + getY() * getY() + getZ() * getZ() + getW() * getW();
  }

  /**
   * @param x
   * @param y
   * @param z
   * @param w
   * @return the squared distance between the point described by this vector and the given x, y, z, w
   *         point. When comparing the relative distance between two points it is usually sufficient
   *         to compare the squared distances, thus avoiding an expensive square root operation.
   */
  @Override
  public double distanceSquared(final double x, final double y, final double z, final double w) {
    final double dx = getX() - x;
    final double dy = getY() - y;
    final double dz = getZ() - z;
    final double dw = getW() - w;
    return dx * dx + dy * dy + dz * dz + dw * dw;
  }

  /**
   * @param destination
   * @return the squared distance between the point described by this vector and the given destination
   *         point. When comparing the relative distance between two points it is usually sufficient
   *         to compare the squared distances, thus avoiding an expensive square root operation.
   * @throws NullPointerException
   *           if destination is null.
   */
  @Override
  public double distanceSquared(final ReadOnlyVector4 destination) {
    return distanceSquared(destination.getX(), destination.getY(), destination.getZ(), destination.getW());
  }

  /**
   * @param x
   * @param y
   * @param z
   * @param w
   * @return the distance between the point described by this vector and the given x, y, z, w point.
   */
  @Override
  public double distance(final double x, final double y, final double z, final double w) {
    return Math.sqrt(distanceSquared(x, y, z, w));
  }

  /**
   * @param destination
   * @return the distance between the point described by this vector and the given destination point.
   * @throws NullPointerException
   *           if destination is null.
   */
  @Override
  public double distance(final ReadOnlyVector4 destination) {
    return Math.sqrt(distanceSquared(destination));
  }

  /**
   * @param x
   * @param y
   * @param z
   * @param w
   * @return the dot product of this vector with the given x, y, z, w values.
   */
  @Override
  public double dot(final double x, final double y, final double z, final double w) {
    return getX() * x + getY() * y + getZ() * z + getW() * w;
  }

  /**
   * @param vec
   * @return the dot product of this vector with the x, y, z, w values of the given vector.
   * @throws NullPointerException
   *           if vec is null.
   */
  @Override
  public double dot(final ReadOnlyVector4 vec) {
    return dot(vec.getX(), vec.getY(), vec.getZ(), vec.getW());
  }

  /**
   * Check a vector... if it is null or its doubles are NaN or infinite, return false. Else return
   * true.
   *
   * @param vector
   *          the vector to check
   * @return true or false as stated above.
   * @see java.lang.Double#isFinite(double)
   */
  public static boolean isFinite(final ReadOnlyVector4 vector) {
    if (vector == null) {
      return false;
    }
    return Double.isFinite(vector.getX()) //
        && Double.isFinite(vector.getY()) //
        && Double.isFinite(vector.getZ()) //
        && Double.isFinite(vector.getW());
  }

  /**
   * @return the string representation of this vector.
   */
  @Override
  public String toString() {
    return "com.ardor3d.math.Vector4 [X=" + getX() + ", Y=" + getY() + ", Z=" + getZ() + ", W=" + getW() + "]";
  }

  /**
   * @return returns a unique code for this vector object based on its values. If two vectors are
   *         numerically equal, they will return the same hash code value.
   */
  @Override
  public int hashCode() {
    int result = 17;

    result = HashUtil.hash(result, getX());
    result = HashUtil.hash(result, getY());
    result = HashUtil.hash(result, getZ());
    result = HashUtil.hash(result, getW());

    return result;
  }

  /**
   * @param o
   *          the object to compare for equality
   * @return true if this vector and the provided vector have the same x, y, z and w values.
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReadOnlyVector4)) {
      return false;
    }
    final ReadOnlyVector4 comp = (ReadOnlyVector4) o;
    return EqualsUtil.areEqual(getX(), comp.getX()) //
        && EqualsUtil.areEqual(getY(), comp.getY()) //
        && EqualsUtil.areEqual(getZ(), comp.getZ()) //
        && EqualsUtil.areEqual(getW(), comp.getW());
  }

  // /////////////////
  // Method for Cloneable
  // /////////////////

  @Override
  public Vector4 clone() {
    return new Vector4(this);
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends Vector4> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(getX(), "x", 0);
    capsule.write(getY(), "y", 0);
    capsule.write(getZ(), "z", 0);
    capsule.write(getW(), "w", 0);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    setX(capsule.readDouble("x", 0));
    setY(capsule.readDouble("y", 0));
    setZ(capsule.readDouble("z", 0));
    setW(capsule.readDouble("w", 0));
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
    setX(in.readDouble());
    setY(in.readDouble());
    setZ(in.readDouble());
    setW(in.readDouble());
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
    out.writeDouble(getX());
    out.writeDouble(getY());
    out.writeDouble(getZ());
    out.writeDouble(getW());
  }

  // /////////////////
  // Methods for creating temp variables (pooling)
  // /////////////////

  /**
   * @return An instance of Vector4 that is intended for temporary use in calculations and so forth.
   *         Multiple calls to the method should return instances of this class that are not currently
   *         in use.
   */
  public final static Vector4 fetchTempInstance() {
    if (MathConstants.useMathPools) {
      return Vector4.VEC_POOL.fetch().zero();
    } else {
      return new Vector4();
    }
  }

  /**
   * Releases a Vector4 back to be used by a future call to fetchTempInstance. TAKE CARE: this Vector4
   * object should no longer have other classes referencing it or "Bad Things" will happen.
   *
   * @param vec
   *          the Vector4 to release.
   */
  public final static void releaseTempInstance(final Vector4 vec) {
    if (MathConstants.useMathPools) {
      Vector4.VEC_POOL.release(vec);
    }
  }
}
