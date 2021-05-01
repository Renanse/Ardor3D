/** * Copyright (c) 2008-2021 Bird Dog Games, Inc. * * This file is part of Ardor3D. * * Ardor3D is free software: you can redistribute it and/or modify it * under the terms of its license which may be found in the accompanying * LICENSE file or at <https://git.io/fjRmv>. */package com.ardor3d.extension.animation.skeletal.clip;import java.io.IOException;import com.ardor3d.util.export.InputCapsule;import com.ardor3d.util.export.OutputCapsule;/** * Describes transform of a joint. */public class JointData extends TransformData {  private int _jointIndex;  /**   * Construct a new, identity joint data object.   */  public JointData(final int index) {    _jointIndex = index;  }  /**   * Construct a new joint data object, copying the value of the given source.   *    * @param source   *          our source to copy.   * @throws NullPointerException   *           if source is null.   */  public JointData(final JointData source) {    set(source);  }  /**   * Construct a new, identity joint data object.   */  public JointData() {}  /**   * Copy the source's values into this transform data object.   *    * @param source   *          our source to copy.   * @throws NullPointerException   *           if source is null.   */  public void set(final JointData source) {    super.set(source);    _jointIndex = source._jointIndex;  }  public int getJointIndex() { return _jointIndex; }  public void setJointIndex(final int jointIndex) { _jointIndex = jointIndex; }  /**   * Blend this transform with the given transform.   *    * @param blendTo   *          The transform to blend to   * @param blendWeight   *          The blend weight   * @param store   *          The transform store.   * @return The blended transform.   */  @Override  public TransformData blend(final TransformData blendTo, final double blendWeight, final TransformData store) {    TransformData rVal = store;    if (rVal == null) {      rVal = new JointData(_jointIndex);    } else if (rVal instanceof JointData) {      ((JointData) rVal).setJointIndex(_jointIndex);    }    return super.blend(blendTo, blendWeight, rVal);  }  // /////////////////  // Methods for Savable  // /////////////////  @Override  public void write(final OutputCapsule capsule) throws IOException {    super.write(capsule);    capsule.write(_jointIndex, "jointIndex", 0);  }  @Override  public void read(final InputCapsule capsule) throws IOException {    super.read(capsule);    _jointIndex = capsule.readInt("jointIndex", 0);  }}