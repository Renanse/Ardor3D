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

import com.ardor3d.math.type.ReadOnlyRectangle2;
import com.ardor3d.math.util.EqualsUtil;
import com.ardor3d.math.util.HashUtil;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <p>
 * Defines a finite plane within two dimensional space that is specified via an origin (x,y -
 * considered bottom left usually) and a width and height.
 * </p>
 * This class is at least partially patterned after awt's Rectangle class.
 */

public class Rectangle2 implements Cloneable, Savable, Externalizable, ReadOnlyRectangle2, Poolable {
  private static final long serialVersionUID = 1L;

  private static final ObjectPool<Rectangle2> RECTANGLE_POOL =
      ObjectPool.create(Rectangle2.class, MathConstants.maxMathPoolSize);

  private int _x;
  private int _y;
  private int _width;
  private int _height;

  /**
   * Constructor creates a new <code>Rectangle2</code> with its origin at 0,0 and width/height of 0.
   */
  public Rectangle2() {}

  /**
   * Constructor creates a new <code>Rectangle2</code> with using the given x,y,width and height
   * values.
   *
   */
  public Rectangle2(final int x, final int y, final int width, final int height) {
    setX(x);
    setY(y);
    setWidth(width);
    setHeight(height);
  }

  /**
   * Constructor creates a new <code>Rectangle2</code> using the values of the provided source
   * rectangle.
   *
   * @param source
   *          the rectangle to copy from
   */
  public Rectangle2(final ReadOnlyRectangle2 source) {
    set(source);
  }

  @Override
  public int getX() { return _x; }

  /**
   * @param x
   *          the new x coordinate of the origin of this rectangle
   */
  public void setX(final int x) { _x = x; }

  @Override
  public int getY() { return _y; }

  /**
   * @param y
   *          the new y coordinate of the origin of this rectangle
   */
  public void setY(final int y) { _y = y; }

  @Override
  public int getWidth() { return _width; }

  /**
   * @param width
   *          the new width of this rectangle
   */
  public void setWidth(final int width) { _width = width; }

  @Override
  public int getHeight() { return _height; }

  /**
   * @param height
   *          the new height of this rectangle
   */
  public void setHeight(final int height) { _height = height; }

  public Rectangle2 set(final int x, final int y, final int width, final int height) {
    _x = x;
    _y = y;
    _width = width;
    _height = height;
    return this;
  }

  public Rectangle2 set(final ReadOnlyRectangle2 rect) {
    return set(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
  }

  public Rectangle2 intersect(final ReadOnlyRectangle2 other, final Rectangle2 store) {
    Rectangle2 rVal = store;
    if (rVal == null) {
      rVal = new Rectangle2();
    }
    final int x1 = Math.max(getX(), other.getX());
    final int y1 = Math.max(getY(), other.getY());
    final int x2 = Math.min(getX() + getWidth(), other.getX() + other.getWidth());
    final int y2 = Math.min(getY() + getHeight(), other.getY() + other.getHeight());
    rVal.set(x1, y1, x2 - x1, y2 - y1);
    return rVal;
  }

  public static Rectangle2 intersect(final ReadOnlyRectangle2 src1, final ReadOnlyRectangle2 src2,
      final Rectangle2 store) {
    Rectangle2 rVal = store;
    if (rVal == null) {
      rVal = new Rectangle2();
    }
    rVal.set(src1);
    return rVal.intersect(src2, rVal);
  }

  /**
   * @return the string representation of this rectangle.
   */
  @Override
  public String toString() {
    return "com.ardor3d.math.Rectangle2 [origin: " + _x + ", " + _y + "  width: " + _width + " height: " + _height
        + "]";
  }

  /**
   * @return returns a unique code for this rectangle object based on its values. If two rectangles
   *         are numerically equal, they will return the same hash code value.
   */
  @Override
  public int hashCode() {
    int result = 17;

    result = HashUtil.hash(result, _x);
    result = HashUtil.hash(result, _y);
    result = HashUtil.hash(result, _width);
    result = HashUtil.hash(result, _height);

    return result;
  }

  /**
   * @param o
   *          the object to compare for equality
   * @return true if this rectangle and the provided rectangle have the same origin and dimensions
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReadOnlyRectangle2)) {
      return false;
    }
    final ReadOnlyRectangle2 comp = (ReadOnlyRectangle2) o;
    return EqualsUtil.areEqual(getX(), comp.getX()) //
        && EqualsUtil.areEqual(getY(), comp.getY()) //
        && EqualsUtil.areEqual(getWidth(), comp.getWidth()) //
        && EqualsUtil.areEqual(getHeight(), comp.getHeight());
  }

  // /////////////////
  // Method for Cloneable
  // /////////////////

  @Override
  public Rectangle2 clone() {
    return new Rectangle2(this);
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_x, "x", 0);
    capsule.write(_y, "y", 0);
    capsule.write(_width, "width", 0);
    capsule.write(_height, "height", 0);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _x = capsule.readInt("x", 0);
    _y = capsule.readInt("y", 0);
    _width = capsule.readInt("width", 0);
    _height = capsule.readInt("height", 0);
  }

  @Override
  public Class<? extends Rectangle2> getClassTag() { return this.getClass(); }

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
    setX(in.readInt());
    setY(in.readInt());
    setWidth(in.readInt());
    setHeight(in.readInt());
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
    out.writeInt(_x);
    out.writeInt(_y);
    out.writeInt(_width);
    out.writeInt(_height);
  }

  // /////////////////
  // Methods for creating temp variables (pooling)
  // /////////////////

  /**
   * @return An instance of Rectangle2 that is intended for temporary use in calculations and so
   *         forth. Multiple calls to the method should return instances of this class that are not
   *         currently in use.
   */
  public final static Rectangle2 fetchTempInstance() {
    if (MathConstants.useMathPools) {
      return Rectangle2.RECTANGLE_POOL.fetch();
    } else {
      return new Rectangle2();
    }
  }

  /**
   * Releases a Rectangle2 back to be used by a future call to fetchTempInstance. TAKE CARE: this
   * object should no longer have other classes referencing it or "Bad Things" will happen.
   *
   * @param rectangle
   *          the Rectangle2 to release.
   */
  public final static void releaseTempInstance(final Rectangle2 rectangle) {
    if (MathConstants.useMathPools) {
      Rectangle2.RECTANGLE_POOL.release(rectangle);
    }
  }
}
