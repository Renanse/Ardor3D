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

import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.scenegraph.Spatial;

/**
 * A pose listener whose purpose is to update the transform of a Spatial to align with a specific
 * joint of the SkeletonPose we are listening to. Note that this is in the coordinate space of the
 * pose, so the managed spatial should be a sibling to the skin mesh we are attaching to, or
 * likewise similarly transformed.
 */
public class AttachmentPoint implements PoseListener {

  /** The index of the joint we are listening to. */
  private short _jointIndex;

  /** The spatial we are moving around. */
  private Spatial _attachment;

  /** An offset from the joint's position to use when placing the spatial attachment. */
  private final Transform _offset = new Transform();

  /** A scratch-paper transform object for calculations. */
  private final Transform _store = new Transform();

  /** A name used to identify this attachment point. */
  private String _name;

  /**
   * Create a new attachment point.
   * 
   * @param name
   *          used to identify this attachment point.
   */
  public AttachmentPoint(final String name) {
    setName(name);
  }

  /**
   * Create a new attachment point.
   * 
   * @param name
   *          used to identify this attachment point.
   * @param jointIndex
   *          the joint index to listen to.
   * @param attachment
   *          the spatial to manage transformation of.
   * @param offset
   *          an offset to add to the joint's transform before updating the attachment spatial.
   */
  public AttachmentPoint(final String name, final short jointIndex, final Spatial attachment,
    final ReadOnlyTransform offset) {
    this(name);
    setJointIndex(jointIndex);
    setAttachment(attachment);
    setOffset(offset);
  }

  public String getName() { return _name; }

  public void setName(final String name) { _name = name; }

  public Spatial getAttachment() { return _attachment; }

  public void setAttachment(final Spatial attachment) { _attachment = attachment; }

  public int getJointIndex() { return _jointIndex; }

  public void setJointIndex(final short jointIndex) { _jointIndex = jointIndex; }

  public ReadOnlyTransform getOffset() { return _offset; }

  public void setOffset(final ReadOnlyTransform offset) {
    _offset.set(offset);
  }

  /**
   * Move our managed spatial to align with the referenced joint's position in the given pose,
   * modified by our offset. See class javadoc for more information.
   */
  @Override
  public void poseUpdated(final SkeletonPose pose) {
    // only update if we have something attached.
    if (_attachment != null) {
      final Transform t = pose.getGlobalJointTransforms()[_jointIndex];
      t.multiply(_offset, _store);
      _attachment.setTransform(_store);
    }
  }
}
