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

import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.EqualsUtil;
import com.ardor3d.math.util.HashUtil;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

public abstract class Line3Base implements Savable, Externalizable, Cloneable {

  protected final Vector3 _origin = new Vector3();
  protected final Vector3 _direction = new Vector3();

  public Line3Base(final ReadOnlyVector3 origin, final ReadOnlyVector3 direction) {
    _origin.set(origin);
    _direction.set(direction);
  }

  /**
   * @return this line's origin point as a readable vector
   */
  public ReadOnlyVector3 getOrigin() { return _origin; }

  /**
   * @return this line's direction as a readable vector
   */
  public ReadOnlyVector3 getDirection() { return _direction; }

  /**
   * Sets the line's origin point to the values of the given vector.
   *
   * @param origin
   * @throws NullPointerException
   *           if normal is null.
   */
  public void setOrigin(final ReadOnlyVector3 origin) {
    _origin.set(origin);
  }

  /**
   * Sets the line's direction to the values of the given vector.
   *
   * @param direction
   * @throws NullPointerException
   *           if direction is null.
   */
  public void setDirection(final ReadOnlyVector3 direction) {
    _direction.set(direction);
  }

  /**
   * @return returns a unique code for this line3base object based on its values. If two line3base
   *         objects are numerically equal, they will return the same hash code value.
   */
  @Override
  public int hashCode() {
    int result = 17;

    result = HashUtil.hash(result, _origin);
    result = HashUtil.hash(result, _direction);

    return result;
  }

  /**
   * @param o
   *          the object to compare for equality
   * @return true if this line3base and the provided line3base have the same origin and direction
   *         values.
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Line3Base)) {
      return false;
    }
    final Line3Base comp = (Line3Base) o;
    return EqualsUtil.areEqual(getOrigin(), comp.getOrigin()) //
        && EqualsUtil.areEqual(getDirection(), comp.getDirection());
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends Line3Base> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_origin, "origin", (Vector3) Vector3.ZERO);
    capsule.write(_direction, "direction", (Vector3) Vector3.UNIT_Z);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _origin.set(capsule.readSavable("origin", (Vector3) Vector3.ZERO));
    _direction.set(capsule.readSavable("direction", (Vector3) Vector3.UNIT_Z));
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
    setOrigin((Vector3) in.readObject());
    setDirection((Vector3) in.readObject());
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
    out.writeObject(_origin);
    out.writeObject(_direction);
  }

}
