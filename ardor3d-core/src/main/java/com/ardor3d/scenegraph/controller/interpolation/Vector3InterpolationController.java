/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.controller.interpolation;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Spatial;

/**
 * Vector3InterpolationController class is a base class for controllers that can interpolate on
 * vectors.
 */
public abstract class Vector3InterpolationController extends InterpolationController<ReadOnlyVector3, Spatial> {

  /** Serial UID */
  private static final long serialVersionUID = 1L;

  /** @see #setConstantSpeed(boolean) */
  private boolean _constantSpeed;

  /** @see #setUpdateField(UpdateField) */
  private UpdateField _updateField = UpdateField.LOCAL_TRANSLATION;

  /**
   * Implemented by sub classes to perform the actual interpolation.
   * 
   * @param from
   *          The vector to interpolate from.
   * @param to
   *          The vector to interpolate to.
   * @param delta
   *          The distance between <code>from</code> and <code>to</code>, will be between
   *          <code>0.0</code> and <code>1.0</code> (inclusive).
   * @param target
   *          The vector to actually interpolate.
   * @return The interpolated vector, should not be <code>null</code>.
   */
  protected abstract Vector3 interpolateVectors(ReadOnlyVector3 from, ReadOnlyVector3 to, double delta, Vector3 target);

  /**
   * Interpolates between the given vectors using the
   * {@link #interpolateVectors(ReadOnlyVector3, ReadOnlyVector3, double, Vector3)} to perform the
   * actual interpolation.
   */
  @Override
  protected void interpolate(final ReadOnlyVector3 from, final ReadOnlyVector3 to, final double delta,
      final Spatial caller) {

    assert (null != from) : "parameter 'from' can not be null";
    assert (null != to) : "parameter 'to' can not be null";
    assert (null != caller) : "parameter 'caller' can not be null";

    final Vector3 target = Vector3.fetchTempInstance();

    final ReadOnlyVector3 interpolated = interpolateVectors(from, to, delta, target);

    switch (getUpdateField()) {
      case LOCAL_SCALE:
        caller.setScale(interpolated);
        break;
      case LOCAL_TRANSLATION:
        caller.setTranslation(interpolated);
        break;
      case WORLD_SCALE:
        caller.setWorldScale(interpolated);
        break;
      case WORLD_TRANSLATION:
        caller.setWorldTranslation(interpolated);
        break;
      default:
        caller.setTranslation(interpolated);
    }

    Vector3.releaseTempInstance(target);

  }

  /**
   * @param constantSpeed
   *          <code>true</code> to interpolate between vectors at a constant speed, <code>false</code>
   *          to interpolate at a constant time.
   * @see #isConstantSpeed()
   */
  public void setConstantSpeed(final boolean constantSpeed) { _constantSpeed = constantSpeed; }

  /**
   * See the setters Javadoc for more information.
   * 
   * @return <code>true</code> if interpolating at a constant speed, <code>false</code> otherwise.
   * @see #setConstantSpeed(boolean)
   */
  public boolean isConstantSpeed() { return _constantSpeed; }

  /**
   * @param updateField
   *          The new field to update.
   * @see #getUpdateField()
   */
  public void setUpdateField(final UpdateField updateField) { _updateField = updateField; }

  /**
   * @return The field being updated.
   * @see #setUpdateField(UpdateField)
   */
  public UpdateField getUpdateField() { return _updateField; }

  /**
   * Specifies which field on the spatial to update.
   */
  public enum UpdateField {
    /** @see Spatial#getTranslation() */
    LOCAL_TRANSLATION,
    /** @see Spatial#getWorldTranslation() */
    WORLD_TRANSLATION,
    /** @see Spatial#getScale() */
    LOCAL_SCALE,
    /** @see Spatial#getWorldScale() */
    WORLD_SCALE;
  }

}
