/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.widget.gizmo;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.widget.gizmo.GizmoHandle.FadeMode;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Cylinder;
import com.ardor3d.util.MaterialUtil;

/**
 * A v2 scale gizmo: three cube-tipped axis handles for non-uniform, per-axis scaling and a center
 * cube for uniform scaling. Shafts are antialiased screen-space strokes holding a constant pixel
 * width; the cubes are solid geometry. See {@link AbstractGizmo} for the shared visual behavior.
 *
 * Because a non-uniform scale is only meaningful along the target's own axes, this gizmo always
 * operates in the target's local frame - the interact matrix setting is ignored.
 *
 * Axis drags scale by the ratio of the grab point's distance from the gizmo center, so pulling
 * the X cube out to twice its distance doubles the X scale. The center cube scales uniformly with
 * mouse movement: up/right grows, down/left shrinks.
 */
public class ScaleGizmo extends AbstractGizmo {

  // All geometry is built to a gizmo of length 1.0, sized on screen by AbstractGizmo. Slightly
  // shorter than the translate arrows so the two read differently at a glance. Stroke widths are
  // in screen pixels at 1:1 DPI scale.
  public static final double SHAFT_START = 0.18;
  public static final float SHAFT_WIDTH = 3.5f;
  public static final double TIP_HALF_EXTENT = 0.075;
  public static final double TIP_CENTER = 0.87;
  public static final double PICK_PROXY_RADIUS = 0.09;
  public static final double CENTER_HALF_EXTENT = 0.085;

  /** Uniform scale factor applied per pixel of center-cube mouse movement. */
  public static final double UNIFORM_SCALE_RATE = 0.005;

  /** Bounds on the scale factor applied by a single input event, guarding ratio blow-ups. */
  public static final double MIN_EVENT_FACTOR = 0.01;
  public static final double MAX_EVENT_FACTOR = 100.0;

  public ScaleGizmo() {
    super("scaleGizmo");
  }

  /** Add the full set of handles: three axis cubes and the uniform center cube. */
  public ScaleGizmo withAllHandles() {
    return withAxes().withUniformHandle();
  }

  /** Add the three cube-tipped axis handles. */
  public ScaleGizmo withAxes() {
    addAxisHandle(GizmoPart.AxisX, Vector3.UNIT_X, DEFAULT_X_COLOR,
        new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_Y));
    addAxisHandle(GizmoPart.AxisY, Vector3.UNIT_Y, DEFAULT_Y_COLOR,
        new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.NEG_UNIT_X));
    addAxisHandle(GizmoPart.AxisZ, Vector3.UNIT_Z, DEFAULT_Z_COLOR, Quaternion.IDENTITY);
    return this;
  }

  /** Add the center cube for uniform scaling. */
  public ScaleGizmo withUniformHandle() {
    final Node root = new Node(GizmoPart.Center.name());
    LightProperties.setLightReceiver(root, false);

    final Box cube = new Box("center", Vector3.ZERO, ScaleGizmo.CENTER_HALF_EXTENT, ScaleGizmo.CENTER_HALF_EXTENT,
        ScaleGizmo.CENTER_HALF_EXTENT);
    cube.updateModelBound();
    root.attachChild(cube);

    addGizmoHandle(new GizmoHandle(GizmoPart.Center, root, DEFAULT_CENTER_COLOR, Vector3.ZERO, FadeMode.None));
    MaterialUtil.autoMaterials(root);
    return this;
  }

  protected void addAxisHandle(final GizmoPart part, final ReadOnlyVector3 axis, final ReadOnlyColorRGBA color,
      final ReadOnlyQuaternion rotation) {
    final Node root = new Node(part.name());
    LightProperties.setLightReceiver(root, false);

    // Geometry is built pointing down +Z, then the whole part is rotated onto its axis.
    final Line shaft = GizmoGeometry.segmentStroke("shaft", new Vector3(0, 0, ScaleGizmo.SHAFT_START),
        new Vector3(0, 0, ScaleGizmo.TIP_CENTER - ScaleGizmo.TIP_HALF_EXTENT), ScaleGizmo.SHAFT_WIDTH);
    root.attachChild(shaft);

    final Box tip = new Box("tip", Vector3.ZERO, ScaleGizmo.TIP_HALF_EXTENT, ScaleGizmo.TIP_HALF_EXTENT,
        ScaleGizmo.TIP_HALF_EXTENT);
    tip.getMeshData().translatePoints(0, 0, ScaleGizmo.TIP_CENTER);
    tip.updateModelBound();
    root.attachChild(tip);

    // The visible geometry is only a few pixels wide - pick against an invisible, fatter proxy.
    final double proxyLength = ScaleGizmo.TIP_CENTER + ScaleGizmo.TIP_HALF_EXTENT - ScaleGizmo.SHAFT_START;
    final Cylinder proxy = new Cylinder("pickProxy", 2, 8, ScaleGizmo.PICK_PROXY_RADIUS, proxyLength, true);
    proxy.getMeshData().translatePoints(0, 0, ScaleGizmo.SHAFT_START + proxyLength * 0.5);
    proxy.updateModelBound();
    proxy.getSceneHints().setCullHint(CullHint.Always);
    root.attachChild(proxy);

    root.setRotation(rotation);

    addGizmoHandle(new GizmoHandle(part, root, color, axis, FadeMode.Axis));
    MaterialUtil.autoMaterials(root);
  }

  /**
   * Non-uniform scaling is only meaningful in the target's local frame, so this gizmo always
   * tracks the target's rotation regardless of the interact matrix setting.
   */
  @Override
  public void targetDataUpdated(final InteractManager manager) {
    final Spatial target = manager.getSpatialTarget();
    if (target == null) {
      _handle.setRotation(Matrix3.IDENTITY);
    } else {
      target.updateGeometricState(0);
      _handle.setRotation(target.getWorldRotation());
    }
  }

  @Override
  public void processInput(final Canvas source, final TwoInputStates inputStates, final AtomicBoolean inputConsumed,
      final InteractManager manager) {

    final Camera camera = source.getCanvasRenderer().getCamera();
    final MouseState current = inputStates.getCurrent().getMouseState();
    final MouseState previous = inputStates.getPrevious().getMouseState();

    captureDpiScaleProvider(source);

    // first process mouse over state
    checkMouseOver(source, current, manager);

    // Now check drag status
    if (!checkShouldDrag(camera, current, previous, inputConsumed, manager)) {
      return;
    }

    // act on drag
    final GizmoHandle handle = findGizmoHandle(_lastDragSpatial);
    if (handle == null) {
      return;
    }

    _calcVec2A.set(previous.getX(), previous.getY());
    final double factor = getScaleFactor(handle, _calcVec2A, current, camera);

    final Transform transform = manager.getSpatialState().getTransform();
    final Vector3 scale = new Vector3(transform.getScale());
    switch (handle.getPart()) {
      case AxisX -> scale.setX(scale.getX() * factor);
      case AxisY -> scale.setY(scale.getY() * factor);
      case AxisZ -> scale.setZ(scale.getZ() * factor);
      case Center -> scale.multiplyLocal(factor);
      default -> {
        return;
      }
    }
    scale.set(Math.max(MIN_SCALE, scale.getX()), Math.max(MIN_SCALE, scale.getY()),
        Math.max(MIN_SCALE, scale.getZ()));
    transform.setScale(scale);

    // apply our filters, if any, now that we've made updates.
    applyFilters(manager);
  }

  /**
   * Work out the scale factor described by the mouse moving from oldMouse to the current position
   * while dragging the given handle.
   */
  protected double getScaleFactor(final GizmoHandle handle, final Vector2 oldMouse, final MouseState current,
      final Camera camera) {

    double factor = 1.0;
    switch (handle.getPart()) {
      case AxisX, AxisY, AxisZ -> {
        // Scale by the ratio of the grab point's distance from the gizmo center along the axis.
        final Vector3 dragAxis = _handle.getRotation().applyPost(handle.getAxis(), _calcVec3D).normalizeLocal();
        _calcVec2B.set(current.getX(), current.getY());
        if (!projectOnAxis(dragAxis, oldMouse, _calcVec2B, camera, _calcVec2C)
            || Math.abs(_calcVec2C.getX()) < 1e-8) {
          return 1.0;
        }
        factor = _calcVec2C.getY() / _calcVec2C.getX();
      }
      case Center ->
        // Uniform: grow dragging up/right, shrink dragging down/left.
        factor = 1.0 + (current.getDx() + current.getDy()) * ScaleGizmo.UNIFORM_SCALE_RATE;
      default -> {
        return 1.0;
      }
    }
    return MathUtils.clamp(factor, ScaleGizmo.MIN_EVENT_FACTOR, ScaleGizmo.MAX_EVENT_FACTOR);
  }
}
