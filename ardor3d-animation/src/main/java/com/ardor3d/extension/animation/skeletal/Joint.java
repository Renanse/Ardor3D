/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal;

import java.io.IOException;
import java.lang.reflect.Field;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Representation of a Joint in a Skeleton. Meant to be used within a specific Skeleton object.
 */
@SavableFactory(factoryMethod = "initSavable")
public class Joint implements Savable {
  /** Root node ID */
  public static final short NO_PARENT = Short.MIN_VALUE;

  /** The inverse transform of this Joint in its bind position. */
  private final Transform _inverseBindPose = new Transform(Transform.IDENTITY);

  /** A name, for display or debugging purposes. */
  private final String _name;

  protected short _index;

  /** Index of our parent Joint, or NO_PARENT if we are the root. */
  protected short _parentIndex;

  /**
   * Construct a new Joint object using the given name.
   *
   * @param name
   *          the name
   */
  public Joint(final String name) {
    _name = name;
  }

  /**
   * @return the inverse of the joint space -> model space transformation.
   */
  public ReadOnlyTransform getInverseBindPose() { return _inverseBindPose; }

  public void setInverseBindPose(final ReadOnlyTransform inverseBindPose) {
    _inverseBindPose.set(inverseBindPose);
  }

  /**
   * @return the human-readable name of this joint.
   */
  public String getName() { return _name; }

  /**
   * Set the index of this joint's parent within the containing Skeleton's joint array.
   *
   * @param parentIndex
   *          the index, or NO_PARENT if this Joint is root (has no parent)
   */
  public void setParentIndex(final short parentIndex) { _parentIndex = parentIndex; }

  public short getParentIndex() { return _parentIndex; }

  public void setIndex(final short index) { _index = index; }

  public short getIndex() { return _index; }

  @Override
  public String toString() {
    return "Joint: '" + getName() + "'";
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends Joint> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_name, "name", null);
    capsule.write(_index, "index", (short) 0);
    capsule.write(_parentIndex, "parentIndex", (short) 0);
    capsule.write(_inverseBindPose, "inverseBindPose", (Transform) Transform.IDENTITY);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    final String name = capsule.readString("name", null);
    try {
      final Field field1 = Joint.class.getDeclaredField("_name");
      field1.setAccessible(true);
      field1.set(this, name);
    } catch (final Exception e) {
      e.printStackTrace();
    }

    _index = capsule.readShort("index", (short) 0);
    _parentIndex = capsule.readShort("parentIndex", (short) 0);

    setInverseBindPose(capsule.readSavable("inverseBindPose", (Transform) Transform.IDENTITY));
  }

  public static Joint initSavable() {
    return new Joint();
  }

  protected Joint() {
    _name = null;
  }
}
