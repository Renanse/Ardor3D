/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.util.export.CapsuleUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Joins a Skeleton with an array of joint poses. This allows the skeleton to exist and be reused
 * between multiple instances of poses.
 */
@SavableFactory(factoryMethod = "initSavable")
public class SkeletonPose implements Savable {

  /** The skeleton being "posed". */
  private Skeleton _skeleton;

  /** Local transforms for the joints of the associated skeleton. */
  private Transform[] _localTransforms;

  /** Global transforms for the joints of the associated skeleton. Not saved to savable. */
  private transient Transform[] _globalTransforms;

  /**
   * A palette of matrices used in skin deformation - basically the global transform X the inverse
   * bind pose transform. Not saved to savable.
   */
  private transient Matrix4[] _matrixPalette;

  /**
   * The list of elements interested in notification when this SkeletonPose updates. Not saved to
   * savable.
   */
  private transient final List<PoseListener> _poseListeners = new ArrayList<>(1);

  /**
   * Construct a new SkeletonPose using the given Skeleton.
   *
   * @param skeleton
   *          the skeleton to use.
   */
  public SkeletonPose(final Skeleton skeleton) {
    assert skeleton != null : "skeleton must not be null.";

    _skeleton = skeleton;
    final int jointCount = _skeleton.getJoints().length;

    // init local transforms
    _localTransforms = new Transform[jointCount];
    for (int i = 0; i < jointCount; i++) {
      _localTransforms[i] = new Transform();
    }

    // init global transforms
    _globalTransforms = new Transform[jointCount];
    for (int i = 0; i < jointCount; i++) {
      _globalTransforms[i] = new Transform();
    }

    // init palette
    _matrixPalette = new Matrix4[jointCount];
    for (int i = 0; i < jointCount; i++) {
      _matrixPalette[i] = new Matrix4();
    }

    // start off in bind pose.
    setToBindPose();
  }

  /**
   * @return the skeleton posed by this object.
   */
  public Skeleton getSkeleton() { return _skeleton; }

  /**
   * @return an array of local space transforms for each of the skeleton's joints.
   */
  public Transform[] getLocalJointTransforms() { return _localTransforms; }

  /**
   * @return an array of global space transforms for each of the skeleton's joints. This does not take
   *         into account any transformation of the SkeletonMesh using the pose.
   */
  public Transform[] getGlobalJointTransforms() { return _globalTransforms; }

  /**
   * @return an array of global space transforms for each of the skeleton's joints.
   */
  public Matrix4[] getMatrixPalette() { return _matrixPalette; }

  /**
   * Register a PoseListener on this SkeletonPose.
   *
   * @param listener
   *          the PoseListener
   */
  public void addPoseListener(final PoseListener listener) {
    _poseListeners.add(listener);
  }

  /**
   * Remove a PoseListener from this SkeletonPose.
   *
   * @param listener
   *          the PoseListener
   */
  public void removePoseListener(final PoseListener listener) {
    _poseListeners.remove(listener);
  }

  /**
   * Clear all PoseListeners registered on this SkeletonPose.
   */
  public void clearListeners() {
    _poseListeners.clear();
  }

  /**
   * Update the global and palette transforms of our posed joints based on the current local joint
   * transforms.
   */
  public void updateTransforms() {
    final Transform temp = Transform.fetchTempInstance();
    // we go in update array order, which ensures parent global transforms are updated before child.
    // final int[] orders = _skeleton.getJointOrders();
    final int nrJoints = _skeleton.getJoints().length;
    // for (int i = 0; i < orders.length; i++) {
    for (int i = 0; i < nrJoints; i++) {
      // the joint index
      final int index = i;

      // find our parent
      final short parentIndex = _skeleton.getJoints()[index].getParentIndex();
      if (parentIndex != Joint.NO_PARENT) {
        // we have a parent, so take us from local->parent->model space by multiplying by parent's
        // local->model
        // space transform.
        _globalTransforms[parentIndex].multiply(_localTransforms[index], _globalTransforms[index]);
      } else {
        // no parent so just set global to the local transform
        _globalTransforms[index].set(_localTransforms[index]);
      }

      // at this point we have a local->model space transform for this joint, for skinning we multiply
      // this by the
      // joint's inverse bind pose (joint->model space, inverted). This gives us a transform that can take
      // a
      // vertex from bind pose (model space) to current pose (model space).
      _globalTransforms[index].multiply(_skeleton.getJoints()[index].getInverseBindPose(), temp);
      temp.getHomogeneousMatrix(_matrixPalette[index]);
    }
    Transform.releaseTempInstance(temp);
    firePoseUpdated();
  }

  /**
   * Update our local joint transforms so that they reflect the skeleton in bind pose.
   */
  public void setToBindPose() {
    final Transform temp = Transform.fetchTempInstance();
    // go through our local transforms
    for (int i = 0; i < _localTransforms.length; i++) {
      // Set us to the bind pose
      _localTransforms[i].set(_skeleton.getJoints()[i].getInverseBindPose());
      // then invert.
      _localTransforms[i].invert(_localTransforms[i]);

      // At this point we are in model space, so we need to remove our parent's transform (if we have
      // one.)
      final short parentIndex = _skeleton.getJoints()[i].getParentIndex();
      if (parentIndex != Joint.NO_PARENT) {
        // We remove the parent's transform simply by multiplying by its inverse bind pose. Done! :)
        _skeleton.getJoints()[parentIndex].getInverseBindPose().multiply(_localTransforms[i], temp);
        _localTransforms[i].set(temp);
      }
    }
    Transform.releaseTempInstance(temp);
    updateTransforms();
  }

  /**
   * Notify any registered PoseListeners that this pose has been "updated".
   */
  public void firePoseUpdated() {
    for (int i = _poseListeners.size(); --i >= 0;) {
      // Pull out pose
      final PoseListener listener = _poseListeners.get(i);

      // notify
      listener.poseUpdated(this);
    }
  }

  public SkeletonPose makeCopy() {
    final SkeletonPose copy = new SkeletonPose(_skeleton);

    int i = 0;
    for (final Transform t : _localTransforms) {
      copy._localTransforms[i++] = t.clone();
    }
    i = 0;
    for (final Transform t : _globalTransforms) {
      copy._globalTransforms[i++] = t.clone();
    }
    i = 0;
    for (final Matrix4 m : _matrixPalette) {
      copy._matrixPalette[i++] = m.clone();
    }

    return copy;
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends SkeletonPose> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_skeleton, "skeleton", null);
    capsule.write(_localTransforms, "localTransforms", null);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _skeleton = capsule.readSavable("skeleton", null);
    _localTransforms = CapsuleUtils.asArray(capsule.readSavableArray("localTransforms", null), Transform.class);

    final int jointCount = _skeleton.getJoints().length;

    // init global transforms
    _globalTransforms = new Transform[jointCount];
    for (int i = 0; i < jointCount; i++) {
      _globalTransforms[i] = new Transform();
    }

    // init palette
    _matrixPalette = new Matrix4[jointCount];
    for (int i = 0; i < jointCount; i++) {
      _matrixPalette[i] = new Matrix4();
    }

    updateTransforms();
  }

  public static SkeletonPose initSavable() {
    return new SkeletonPose();
  }

  protected SkeletonPose() {
    _skeleton = null;
    _localTransforms = null;
    _globalTransforms = null;
    _matrixPalette = null;
  }
}
