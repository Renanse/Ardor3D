/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.filter;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.data.SpatialState;
import com.ardor3d.extension.interact.widget.AbstractInteractWidget;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;

/**
 * Filter that snaps the rotation accumulated over the course of a drag to fixed angle increments,
 * e.g. 15 degree steps. The rotation at drag start is captured, the raw per-event rotations the
 * widget applies are accumulated (the manager resets the state from the target every input event,
 * and the target only ever receives snapped values - the filter must keep the running total
 * itself), and the accumulated delta is quantized as a single axis-angle rotation. Works with any
 * widget that rotates the state (RotateWidget, RotateGizmo). Toggle with
 * {@link #setEnabled(boolean)} to make snapping a modifier-key behavior (e.g. while Ctrl is
 * held); the raw total keeps accumulating while disabled, so mid-drag toggles snap relative to
 * the whole drag.
 */
public class AngleSnapFilter extends UpdateFilterAdapter implements SnapSource {

  protected final Matrix3 _calcMat3A = new Matrix3();
  protected final Matrix3 _calcMat3B = new Matrix3();
  protected final Quaternion _calcQuat = new Quaternion();
  protected final Vector3 _calcVec = new Vector3();

  protected double _snapAngle;
  protected boolean _enabled = true;

  /** Target rotation captured at drag start; null when no drag is active. */
  protected Matrix3 _startRotation = null;
  /** Raw (unsnapped) rotation the drag has applied since it started. */
  protected final Matrix3 _accumulated = new Matrix3();

  /**
   * @param snapAngle
   *          the rotation increment to snap to, in radians.
   */
  public AngleSnapFilter(final double snapAngle) {
    _snapAngle = snapAngle;
  }

  @Override
  public void beginDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState state) {
    if (manager.getSpatialTarget() != null) {
      _startRotation = new Matrix3(manager.getSpatialTarget().getTransform().getMatrix());
      _accumulated.setIdentity();
    }
  }

  @Override
  public void endDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState state) {
    _startRotation = null;
  }

  @Override
  public void applyFilter(final InteractManager manager, final AbstractInteractWidget widget) {
    if (_startRotation == null || manager.getSpatialTarget() == null) {
      return;
    }
    final SpatialState state = manager.getSpatialState();

    // This event's raw widget rotation is the state's divergence from the target, which the
    // manager has not yet updated this cycle. Fold it into the running total.
    _calcMat3A.set(manager.getSpatialTarget().getTransform().getMatrix()).transposeLocal();
    state.getTransform().getMatrix().multiply(_calcMat3A, _calcMat3B);
    _calcMat3B.multiplyLocal(_accumulated);
    _accumulated.set(_calcMat3B);

    if (!_enabled || _snapAngle <= 0) {
      return;
    }

    // Quantize the accumulated delta as a single axis-angle.
    final double angle = _calcQuat.fromRotationMatrix(_accumulated).toAngleAxis(_calcVec);
    final double snapped = Math.round(angle / _snapAngle) * _snapAngle;
    if (snapped == 0 || _calcVec.lengthSquared() < 1e-12) {
      state.getTransform().setRotation(_startRotation);
      return;
    }

    // Rebuild: quantized delta on top of the start rotation.
    _calcMat3A.fromAngleNormalAxis(snapped, _calcVec.normalizeLocal());
    state.getTransform().setRotation(_calcMat3A.multiply(_startRotation, _calcMat3B));
  }

  public double getSnapAngle() { return _snapAngle; }

  /** Set the rotation increment to snap to, in radians. */
  public void setSnapAngle(final double snapAngle) { _snapAngle = snapAngle; }

  public boolean isEnabled() { return _enabled; }

  public void setEnabled(final boolean enabled) { _enabled = enabled; }

  @Override
  public boolean isSnapping() { return _enabled && _snapAngle > 0; }

  @Override
  public double getSnapIncrement() { return _snapAngle; }
}
