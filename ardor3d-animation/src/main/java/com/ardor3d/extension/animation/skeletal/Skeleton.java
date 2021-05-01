/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
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
import com.ardor3d.util.export.CapsuleUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Describes a collection of Joints. This class represents the hierarchy of a Skeleton and its
 * original aspect (via the Joint class). This does not support posing the joints in any way... Use
 * with a SkeletonPose to describe a skeleton in a specific pose.
 */
@SavableFactory(factoryMethod = "initSavable")
public class Skeleton implements Savable {

  /**
   * An array of Joints associated with this Skeleton.
   */
  private final Joint[] _joints;

  /** A name, for display or debugging purposes. */
  private final String _name;

  /**
   * 
   * @param name
   *          A name, for display or debugging purposes
   * @param joints
   *          An array of Joints associated with this Skeleton.
   */
  public Skeleton(final String name, final Joint[] joints) {
    _name = name;
    _joints = joints;
  }

  /**
   * @return the human-readable name of this skeleton.
   */
  public String getName() { return _name; }

  /**
   * @return the array of Joints that make up this skeleton.
   */
  public Joint[] getJoints() { return _joints; }

  /**
   * 
   * @param jointName
   *          name of the joint to locate. Case sensitive.
   * @return the index of the joint, if found, or -1 if not.
   */
  public int findJointByName(final String jointName) {
    for (int i = 0; i < _joints.length; i++) {
      if (jointName.equals(_joints[i].getName())) {
        return i;
      }
    }
    return -1;
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends Skeleton> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_name, "name", null);
    capsule.write(_joints, "joints", null);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    final String name = capsule.readString("name", null);
    final Joint[] joints = CapsuleUtils.asArray(capsule.readSavableArray("joints", null), Joint.class);
    try {
      final Field field1 = Skeleton.class.getDeclaredField("_name");
      field1.setAccessible(true);
      field1.set(this, name);

      final Field field2 = Skeleton.class.getDeclaredField("_joints");
      field2.setAccessible(true);
      field2.set(this, joints);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  public static Skeleton initSavable() {
    return new Skeleton();
  }

  protected Skeleton() {
    _name = null;
    _joints = null;
  }
}
