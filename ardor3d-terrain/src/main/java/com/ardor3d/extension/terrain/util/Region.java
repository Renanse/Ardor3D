/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.util;

/**
 * Used to calculate clipmap block boundaries etc
 */
public class Region {
  private int x;
  private int y;
  private int width;
  private int height;

  private final int level;

  public Region(final Region source) {
    this(source.level, source.x, source.y, source.width, source.height);
  }

  public Region(final int x, final int y, final int width, final int height) {
    this(0, x, y, width, height);
  }

  public Region(final int level, final int x, final int y, final int width, final int height) {
    this.level = level;

    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  /**
   * @return the x
   */
  public int getX() { return x; }

  /**
   * @return the y
   */
  public int getY() { return y; }

  /**
   * @param x
   *          the x to set
   */
  public void setX(final int x) { this.x = x; }

  /**
   * @param y
   *          the y to set
   */
  public void setY(final int y) { this.y = y; }

  public void setWidth(final int width) { this.width = width; }

  public void setHeight(final int height) { this.height = height; }

  /**
   * @return the left
   */
  public int getLeft() { return x; }

  /**
   * @return the right
   */
  public int getRight() { return x + width; }

  /**
   * @return the top
   */
  public int getTop() { return y; }

  /**
   * @return the bottom
   */
  public int getBottom() { return y + height; }

  /**
   * @return the width
   */
  public int getWidth() { return width; }

  /**
   * @return the height
   */
  public int getHeight() { return height; }

  public boolean intersects(final Region r) {
    int tw = width;
    int th = height;
    int rw = r.width;
    int rh = r.height;
    if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
      return false;
    }
    final int tx = x;
    final int ty = y;
    final int rx = r.x;
    final int ry = r.y;
    rw += rx;
    rh += ry;
    tw += tx;
    th += ty;
    // overflow || intersect
    return (rw < rx || rw > tx) && (rh < ry || rh > ty) && (tw < tx || tw > rx) && (th < ty || th > ry);
  }

  public Region intersection(final Region r) {
    int tx1 = x;
    int ty1 = y;
    final int rx1 = r.x;
    final int ry1 = r.y;
    long tx2 = tx1;
    tx2 += width;
    long ty2 = ty1;
    ty2 += height;
    long rx2 = rx1;
    rx2 += r.width;
    long ry2 = ry1;
    ry2 += r.height;
    if (tx1 < rx1) {
      tx1 = rx1;
    }
    if (ty1 < ry1) {
      ty1 = ry1;
    }
    if (tx2 > rx2) {
      tx2 = rx2;
    }
    if (ty2 > ry2) {
      ty2 = ry2;
    }
    tx2 -= tx1;
    ty2 -= ty1;
    // tx2,ty2 will never overflow (they will never be
    // larger than the smallest of the two source w,h)
    // they might underflow, though...
    if (tx2 < Integer.MIN_VALUE) {
      tx2 = Integer.MIN_VALUE;
    }
    if (ty2 < Integer.MIN_VALUE) {
      ty2 = Integer.MIN_VALUE;
    }

    r.setX(tx1);
    r.setY(ty1);
    r.setWidth((int) tx2);
    r.setHeight((int) ty2);

    return r;
  }

  public int getLevel() { return level; }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + height;
    result = prime * result + level;
    result = prime * result + width;
    result = prime * result + x;
    result = prime * result + y;
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Region other)) {
      return false;
    }
    if (height != other.height) {
      return false;
    }
    if (level != other.level) {
      return false;
    }
    if (width != other.width) {
      return false;
    }
    if (x != other.x) {
      return false;
    }
    if (y != other.y) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Region [level=" + level + ", x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
  }

}
