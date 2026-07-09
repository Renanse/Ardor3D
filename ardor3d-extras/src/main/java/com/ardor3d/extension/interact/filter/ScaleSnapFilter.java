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
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;

/**
 * Filter that snaps the scale factor accumulated over the course of a drag to fixed increments,
 * e.g. quarter steps. The scale at drag start is captured, the raw per-event factors the widget
 * applies are accumulated per axis (the manager resets the state from the target every input
 * event, and the target only ever receives snapped values - the filter must keep the running total
 * itself), and each axis' factor is quantized so the target ends on a clean multiple of the start
 * scale. Only axes the drag has actually changed are snapped, so a single-axis drag leaves the
 * other two alone regardless of their (possibly off-grid) start scale. Works with any widget that
 * scales the state (ScaleWidget, ScaleGizmo). Toggle with {@link #setEnabled(boolean)} to make
 * snapping a modifier-key behavior (e.g. while Ctrl is held); the raw total keeps accumulating
 * while disabled, so mid-drag toggles snap relative to the whole drag.
 */
public class ScaleSnapFilter extends UpdateFilterAdapter implements SnapSource {

  protected double _snapStep;
  protected boolean _enabled = true;

  /** Target scale captured at drag start; null when no drag is active. */
  protected Vector3 _startScale = null;
  /** Raw (unsnapped) factor the drag has applied per axis since it started. */
  protected final Vector3 _accumulated = new Vector3(1, 1, 1);

  /**
   * @param snapStep
   *          the scale-factor increment to snap to (e.g. 0.25 for quarter steps).
   */
  public ScaleSnapFilter(final double snapStep) {
    _snapStep = snapStep;
  }

  @Override
  public void beginDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState state) {
    if (manager.getSpatialTarget() != null) {
      _startScale = new Vector3(manager.getSpatialTarget().getTransform().getScale());
      _accumulated.set(1, 1, 1);
    }
  }

  @Override
  public void endDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState state) {
    _startScale = null;
  }

  @Override
  public void applyFilter(final InteractManager manager, final AbstractInteractWidget widget) {
    if (_startScale == null || manager.getSpatialTarget() == null) {
      return;
    }
    final SpatialState state = manager.getSpatialState();

    // This event's raw widget factor is the state's divergence from the target, which the manager
    // has not yet updated this cycle. Fold it into the running per-axis total.
    final ReadOnlyVector3 target = manager.getSpatialTarget().getTransform().getScale();
    final ReadOnlyVector3 current = state.getTransform().getScale();
    _accumulated.multiplyLocal(current.getX() / target.getX(), current.getY() / target.getY(),
        current.getZ() / target.getZ());

    if (!_enabled || _snapStep <= 0) {
      return;
    }

    // Quantize each axis the drag has touched to a clean multiple of its start scale, leaving the
    // untouched axes (factor still exactly 1) alone so a single-axis drag doesn't nudge the others.
    state.getTransform().setScale(snap(_accumulated.getX(), _startScale.getX(), current.getX()),
        snap(_accumulated.getY(), _startScale.getY(), current.getY()),
        snap(_accumulated.getZ(), _startScale.getZ(), current.getZ()));
  }

  /**
   * Snap one axis: if the drag has changed this axis' factor, round it to the nearest step (floored
   * at one step so it never collapses to zero) and apply it to the start scale; otherwise leave the
   * axis at the value the widget already left in the state.
   */
  protected double snap(final double factor, final double startScale, final double unsnapped) {
    if (Math.abs(factor - 1.0) <= MathUtils.ZERO_TOLERANCE) {
      return unsnapped;
    }
    final double snapped = Math.max(_snapStep, Math.round(factor / _snapStep) * _snapStep);
    return startScale * snapped;
  }

  public double getSnapStep() { return _snapStep; }

  /** Set the scale-factor increment to snap to (e.g. 0.25 for quarter steps). */
  public void setSnapStep(final double snapStep) { _snapStep = snapStep; }

  public boolean isEnabled() { return _enabled; }

  public void setEnabled(final boolean enabled) { _enabled = enabled; }

  @Override
  public boolean isSnapping() { return _enabled && _snapStep > 0; }

  @Override
  public double getSnapIncrement() { return _snapStep; }
}
