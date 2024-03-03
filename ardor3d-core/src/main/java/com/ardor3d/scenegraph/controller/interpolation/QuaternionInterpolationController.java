/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.controller.interpolation;

import com.ardor3d.math.Quaternion;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.scenegraph.Spatial;

import java.io.Serial;

/**
 * QuaternionInterpolationController class interpolates a {@link Spatial}s rotation using
 * {@link Quaternion}s.
 */
public class QuaternionInterpolationController extends InterpolationController<ReadOnlyQuaternion, Spatial> {

  /** Serial UID */
  @Serial
  private static final long serialVersionUID = 1L;

  /** @see #setLocalRotation(boolean) */
  private boolean _localRotation = true;

  /**
   * Interpolates between the given quaternions using the
   * {@link Quaternion#slerpLocal(ReadOnlyQuaternion, ReadOnlyQuaternion, double)} method.
   */
  @Override
  protected void interpolate(final ReadOnlyQuaternion from, final ReadOnlyQuaternion to, final double delta,
      final Spatial caller) {

    assert (null != from) : "parameter 'from' can not be null";
    assert (null != to) : "parameter 'to' can not be null";
    assert (null != caller) : "parameter 'caller' can not be null";

    final Quaternion tempQuat = Quaternion.fetchTempInstance();

    tempQuat.slerpLocal(from, to, delta);

    if (isLocalRotation()) {
      caller.setRotation(tempQuat);
    } else {
      caller.setWorldRotation(tempQuat);
    }

    Quaternion.releaseTempInstance(tempQuat);
  }

  /**
   * @param localRotation
   *          <code>true</code> to update local rotation, <code>false</code> to update world rotation.
   * @see #isLocalRotation()
   */
  public void setLocalRotation(final boolean localRotation) { _localRotation = localRotation; }

  /**
   * @return <code>true</code> if the local rotation is being updated, <code>false</code> if the world
   *         rotation is.
   * @see #setLocalRotation(boolean)
   */
  public boolean isLocalRotation() { return _localRotation; }

}
