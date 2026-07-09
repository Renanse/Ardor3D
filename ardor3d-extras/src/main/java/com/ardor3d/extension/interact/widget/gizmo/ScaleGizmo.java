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

import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.widget.DragState;
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
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
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

  /**
   * Bounds on the visual shaft stretch while dragging an axis handle. Display only - the applied
   * scale is not clamped by these.
   */
  public static final double MIN_DISPLAY_STRETCH = 0.32;
  public static final double MAX_DISPLAY_STRETCH = 8.0;

  /** The dragged axis' shaft and tip, stretched during drags to track the applied scale. */
  protected final EnumMap<GizmoPart, Line> _axisShafts = new EnumMap<>(GizmoPart.class);
  protected final EnumMap<GizmoPart, Mesh> _axisTips = new EnumMap<>(GizmoPart.class);
  protected final EnumMap<GizmoPart, Double> _appliedStretch = new EnumMap<>(GizmoPart.class);

  /** State scale captured when a drag started, for measuring the applied ratio. */
  protected Vector3 _dragStartScale = null;

  /** Formats the scale factor readout - see {@link ScaleGizmo#setReadoutFormatter}. */
  @FunctionalInterface
  public interface ReadoutFormatter {
    /**
     * @param factor
     *          the per-axis scale factor applied since the drag began.
     * @param manager
     *          the interact manager (for the target, etc.).
     * @return the readout text, or null to show nothing.
     */
    String format(ReadOnlyVector3 factor, InteractManager manager);
  }

  /** Custom formatter for the scale readout; the built-in factor text is used when null. */
  protected ReadoutFormatter _readoutFormatter;
  /** Scratch for the per-axis factor handed to the formatter. */
  protected final Vector3 _calcFactor = new Vector3();

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
    _axisShafts.put(part, shaft);

    final Box tip = new Box("tip", Vector3.ZERO, ScaleGizmo.TIP_HALF_EXTENT, ScaleGizmo.TIP_HALF_EXTENT,
        ScaleGizmo.TIP_HALF_EXTENT);
    tip.getMeshData().translatePoints(0, 0, ScaleGizmo.TIP_CENTER);
    tip.updateModelBound();
    root.attachChild(tip);
    _axisTips.put(part, tip);

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

    // Escape aborts a drag in progress, restoring the target to its pre-drag transform.
    if (cancelDragIfRequested(inputStates.getCurrent().getKeyboardState(), current, inputConsumed, manager)) {
      return;
    }

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
    if (_dragStartScale == null) {
      // First event of the drag - the state still holds the pristine pre-drag scale here.
      _dragStartScale = new Vector3(transform.getScale());
    }
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

  @Override
  public void render(final Renderer renderer, final InteractManager manager) {
    updateShaftStretch(manager);
    super.render(renderer, manager);
  }

  @Override
  protected String getReadoutText(final InteractManager manager) {
    if (_dragStartScale == null) {
      return null;
    }
    final GizmoHandle handle = findGizmoHandle(_lastDragSpatial);
    if (handle == null) {
      return null;
    }
    // Per-axis factor applied since the drag began, read back from the state so any snap shows true.
    final ReadOnlyVector3 scale = manager.getSpatialState().getTransform().getScale();
    final double fx = scale.getX() / _dragStartScale.getX();
    final double fy = scale.getY() / _dragStartScale.getY();
    final double fz = scale.getZ() / _dragStartScale.getZ();
    if (_readoutFormatter != null) {
      return _readoutFormatter.format(_calcFactor.set(fx, fy, fz), manager);
    }
    // Built-in: the dragged axis' factor (uniform cube uses X). ASCII 'x', not the multiply glyph.
    final double ratio = switch (handle.getPart()) {
      case AxisY -> fy;
      case AxisZ -> fz;
      default -> fx;
    };
    return String.format("%.2fx", ratio);
  }

  public ReadoutFormatter getReadoutFormatter() { return _readoutFormatter; }

  /**
   * Set a custom formatter for the scale readout (e.g. per-axis, or percentages). Pass null to
   * restore the built-in factor text.
   */
  public void setReadoutFormatter(final ReadoutFormatter formatter) { _readoutFormatter = formatter; }

  /**
   * While an axis drag is active, stretch that axis' shaft and tip to track the scale actually
   * applied to the state - read back after filters ran, like the rotate gizmo's readout, so any
   * snapping shows true. Everything else sits at rest length.
   */
  protected void updateShaftStretch(final InteractManager manager) {
    GizmoPart activePart = null;
    double stretch = 1.0;
    if (_dragState != DragState.NONE && _dragStartScale != null && manager.getSpatialTarget() != null) {
      final GizmoHandle handle = findGizmoHandle(_lastDragSpatial);
      if (handle != null) {
        final ReadOnlyVector3 scale = manager.getSpatialState().getTransform().getScale();
        switch (handle.getPart()) {
          case AxisX -> stretch = scale.getX() / _dragStartScale.getX();
          case AxisY -> stretch = scale.getY() / _dragStartScale.getY();
          case AxisZ -> stretch = scale.getZ() / _dragStartScale.getZ();
          default -> {
          }
        }
        if (stretch != 1.0) {
          activePart = handle.getPart();
        }
      }
    }
    for (final GizmoPart part : _axisShafts.keySet()) {
      setAxisStretch(part, part == activePart ? stretch : 1.0);
    }
  }

  /**
   * Stretch an axis handle to the given factor: the tip cube slides to factor times its rest
   * distance and the shaft's far endpoint follows it. The factor is clamped to the display
   * bounds; the tip keeps its shape and the shaft never inverts through the gizmo center.
   */
  protected void setAxisStretch(final GizmoPart part, final double stretch) {
    final double clamped = MathUtils.clamp(stretch, ScaleGizmo.MIN_DISPLAY_STRETCH, ScaleGizmo.MAX_DISPLAY_STRETCH);
    final Double previous = _appliedStretch.get(part);
    if (previous != null && previous.doubleValue() == clamped) {
      return;
    }
    _appliedStretch.put(part, clamped);

    final double tipCenter = ScaleGizmo.TIP_CENTER * clamped;

    final Line shaft = _axisShafts.get(part);
    if (shaft != null) {
      final FloatBuffer verts = shaft.getMeshData().getVertexBuffer();
      // vertex 1's z: the shaft runs from SHAFT_START to the near face of the tip cube.
      verts.put(5, (float) Math.max(ScaleGizmo.SHAFT_START + 0.01, tipCenter - ScaleGizmo.TIP_HALF_EXTENT));
      shaft.getMeshData().markBufferDirty(MeshData.KEY_VertexCoords);
      shaft.updateModelBound();
    }

    final Mesh tip = _axisTips.get(part);
    if (tip != null) {
      // The tip's points are baked at TIP_CENTER; translate the mesh to put it at the stretched
      // distance without deforming it.
      tip.setTranslation(0, 0, ScaleGizmo.TIP_CENTER * (clamped - 1.0));
    }
  }

  @Override
  public void endDrag(final InteractManager manager, final MouseState current) {
    super.endDrag(manager, current);
    // Shafts return to rest length on the next render, where no drag is active anymore.
    _dragStartScale = null;
  }
}
