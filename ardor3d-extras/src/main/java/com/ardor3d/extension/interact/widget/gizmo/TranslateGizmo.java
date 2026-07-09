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
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.buffer.BufferUtils;
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
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Cylinder;
import com.ardor3d.scenegraph.shape.Disk;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.MaterialUtil;

/**
 * A v2 translation gizmo: three cone-tipped axis arrows, three corner quads for dragging in the
 * planes spanned by axis pairs, and a camera-facing center circle for dragging in the view plane.
 * Shafts and outlines are antialiased screen-space strokes holding a constant pixel width; only
 * the cone tips are solid geometry. See {@link AbstractGizmo} for the shared visual behavior
 * (constant screen size, per-handle highlighting, x-ray rendering, view-angle fades).
 */
public class TranslateGizmo extends AbstractGizmo {

  // All geometry is built to a gizmo of length 1.0, sized on screen by AbstractGizmo.
  // Stroke widths are in screen pixels at 1:1 DPI scale.
  public static final double SHAFT_START = 0.18;
  public static final float SHAFT_WIDTH = 3.5f;
  public static final float OUTLINE_WIDTH = 3f;
  public static final double TIP_RADIUS = 0.085;
  public static final double TIP_LENGTH = 0.22;
  public static final int TIP_SAMPLES = 24;
  public static final double PICK_PROXY_RADIUS = 0.09;
  public static final double PLANE_QUAD_SIZE = 0.32;
  public static final double PLANE_QUAD_CENTER = 0.42;
  public static final double CENTER_RADIUS = 0.1;
  public static final double CENTER_PICK_RADIUS = 0.13;
  public static final int CIRCLE_SAMPLES = 48;
  public static final float PLANE_FILL_ALPHA = 0.45f;

  /** Snap tick crossbars on the axis: half length across the axis, half thickness along it, units. */
  public static final double SNAP_TICK_HALF = 0.05;
  public static final double SNAP_TICK_THICK = 0.01;
  public static final float SNAP_TICK_ALPHA = 0.9f;
  /** Cap on snap ticks per side of the origin, and how far along the axis they run, in local units. */
  public static final int SNAP_TICK_MAX_PER_SIDE = 10;
  public static final double SNAP_TICK_MAX_EXTENT = 6.0;
  /** On-screen tick spacing (px) below which ticks fade fully out, and above which they are full. */
  public static final double SNAP_TICK_MIN_SCREEN_SPACING = 10;
  public static final double SNAP_TICK_FULL_SCREEN_SPACING = 24;

  protected GizmoHandle _centerHandle;

  protected final Quaternion _calcQuat = new Quaternion();

  /** Snap tick crossbars along the active axis, shown while a snapping axis drag is active. */
  protected final Mesh _snapTicks;
  protected final FloatBuffer _snapTickBuffer =
      BufferUtils.createVector3Buffer((2 * TranslateGizmo.SNAP_TICK_MAX_PER_SIDE + 1) * 6);
  /** Last snapped translation, to pulse when it steps; null when not snapping. */
  protected Vector3 _lastSnapTranslation = null;

  /** Formats the translation readout - see {@link TranslateGizmo#setReadoutFormatter}. */
  @FunctionalInterface
  public interface ReadoutFormatter {
    /**
     * @param delta
     *          translation applied since the drag began, in the target's parent frame.
     * @param position
     *          the target's current local translation.
     * @param manager
     *          the interact manager (for the target, etc.).
     * @return the readout text, or null to show nothing.
     */
    String format(ReadOnlyVector3 delta, ReadOnlyVector3 position, InteractManager manager);
  }

  /** Custom formatter for the translation readout; the built-in delta text is used when null. */
  protected ReadoutFormatter _readoutFormatter;

  public TranslateGizmo() {
    super("translateGizmo");

    // Snap tick crossbars along the active axis, shown while a snapping axis drag is active;
    // updateSnapTicks() rewrites them each frame. Non-indexed triangles, two per tick.
    _snapTicks = new Mesh("snapTicks");
    _snapTicks.getMeshData().setVertexBuffer(_snapTickBuffer);
    _snapTicks.getMeshData().setIndexMode(IndexMode.Triangles);
    LightProperties.setLightReceiver(_snapTicks, false);
    _snapTicks.getSceneHints().setAllPickingHints(false);
    _snapTicks.getSceneHints().setCullHint(CullHint.Always);
    GizmoGeometry.disableDepthWrite(_snapTicks);
    _handle.attachChild(_snapTicks);
    MaterialUtil.autoMaterials(_snapTicks);
  }

  /** Add the full set of handles: axis arrows, plane quads and the view-plane center disk. */
  public TranslateGizmo withAllHandles() {
    return withAxes().withPlanes().withViewPlaneHandle();
  }

  /** Add the three cone-tipped axis arrows. */
  public TranslateGizmo withAxes() {
    addAxisHandle(GizmoPart.AxisX, Vector3.UNIT_X, DEFAULT_X_COLOR,
        new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_Y));
    addAxisHandle(GizmoPart.AxisY, Vector3.UNIT_Y, DEFAULT_Y_COLOR,
        new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.NEG_UNIT_X));
    addAxisHandle(GizmoPart.AxisZ, Vector3.UNIT_Z, DEFAULT_Z_COLOR, Quaternion.IDENTITY);
    return this;
  }

  /** Add the three corner quads for two-axis drags. Each is colored by its plane's normal axis. */
  public TranslateGizmo withPlanes() {
    // columns are the images of local X, Y, Z: quads are built in XY facing +Z, then rotated so
    // the quad spans the named plane and +Z becomes the plane normal.
    addPlaneHandle(GizmoPart.PlaneXY, Vector3.UNIT_Z, DEFAULT_Z_COLOR, Matrix3.IDENTITY);
    addPlaneHandle(GizmoPart.PlaneXZ, Vector3.UNIT_Y, DEFAULT_Y_COLOR,
        new Matrix3().fromAxes(Vector3.UNIT_Z, Vector3.UNIT_X, Vector3.UNIT_Y));
    addPlaneHandle(GizmoPart.PlaneYZ, Vector3.UNIT_X, DEFAULT_X_COLOR,
        new Matrix3().fromAxes(Vector3.UNIT_Y, Vector3.UNIT_Z, Vector3.UNIT_X));
    return this;
  }

  /** Add the camera-facing center circle for view-plane drags. */
  public TranslateGizmo withViewPlaneHandle() {
    final Node root = new Node(GizmoPart.Center.name());
    LightProperties.setLightReceiver(root, false);

    final Line circle = GizmoGeometry.arcStroke("center", TranslateGizmo.CENTER_RADIUS, 0, MathUtils.TWO_PI,
        TranslateGizmo.CIRCLE_SAMPLES, TranslateGizmo.OUTLINE_WIDTH);
    root.attachChild(circle);

    // The circle is an outline a couple of pixels wide - pick against an invisible filled disk.
    final Disk proxy = new Disk("pickProxy", 2, 24, TranslateGizmo.CENTER_PICK_RADIUS);
    proxy.updateModelBound();
    proxy.getSceneHints().setCullHint(CullHint.Always);
    root.attachChild(proxy);

    _centerHandle = new GizmoHandle(GizmoPart.Center, root, DEFAULT_CENTER_COLOR, Vector3.ZERO, FadeMode.None);
    addGizmoHandle(_centerHandle);
    MaterialUtil.autoMaterials(root);
    return this;
  }

  protected void addAxisHandle(final GizmoPart part, final ReadOnlyVector3 axis, final ReadOnlyColorRGBA color,
      final ReadOnlyQuaternion rotation) {
    final Node root = new Node(part.name());
    LightProperties.setLightReceiver(root, false);

    // Geometry is built pointing down +Z, then the whole part is rotated onto its axis.
    final Line shaft = GizmoGeometry.segmentStroke("shaft", new Vector3(0, 0, TranslateGizmo.SHAFT_START),
        new Vector3(0, 0, 1.0 - TranslateGizmo.TIP_LENGTH), TranslateGizmo.SHAFT_WIDTH);
    root.attachChild(shaft);

    final Cylinder tip = new Cylinder("tip", 2, TranslateGizmo.TIP_SAMPLES, TranslateGizmo.TIP_RADIUS,
        TranslateGizmo.TIP_LENGTH, true);
    // Taper the +Z end to a point, leaving the base radius at -Z: a cone pointing along the axis.
    tip.setRadius1(0);
    tip.getMeshData().translatePoints(0, 0, 1.0 - TranslateGizmo.TIP_LENGTH * 0.5);
    tip.updateModelBound();
    root.attachChild(tip);

    // The visible geometry is only a few pixels wide - pick against an invisible, fatter proxy.
    final double proxyLength = 1.0 - TranslateGizmo.SHAFT_START;
    final Cylinder proxy = new Cylinder("pickProxy", 2, 8, TranslateGizmo.PICK_PROXY_RADIUS, proxyLength, true);
    proxy.getMeshData().translatePoints(0, 0, TranslateGizmo.SHAFT_START + proxyLength * 0.5);
    proxy.updateModelBound();
    proxy.getSceneHints().setCullHint(CullHint.Always);
    root.attachChild(proxy);

    root.setRotation(rotation);

    addGizmoHandle(new GizmoHandle(part, root, color, axis, FadeMode.Axis));
    MaterialUtil.autoMaterials(root);
  }

  protected void addPlaneHandle(final GizmoPart part, final ReadOnlyVector3 normal, final ReadOnlyColorRGBA color,
      final ReadOnlyMatrix3 rotation) {
    final Node root = new Node(part.name());
    LightProperties.setLightReceiver(root, false);

    final Quad fill = new Quad("fill", TranslateGizmo.PLANE_QUAD_SIZE, TranslateGizmo.PLANE_QUAD_SIZE);
    fill.getMeshData().translatePoints(TranslateGizmo.PLANE_QUAD_CENTER, TranslateGizmo.PLANE_QUAD_CENTER, 0);
    fill.updateModelBound();
    GizmoGeometry.disableDepthWrite(fill);
    root.attachChild(fill);

    final double near = TranslateGizmo.PLANE_QUAD_CENTER - TranslateGizmo.PLANE_QUAD_SIZE * 0.5;
    final double far = TranslateGizmo.PLANE_QUAD_CENTER + TranslateGizmo.PLANE_QUAD_SIZE * 0.5;
    final Line border = GizmoGeometry.polylineStroke("border", new ReadOnlyVector3[] {new Vector3(near, near, 0),
        new Vector3(far, near, 0), new Vector3(far, far, 0), new Vector3(near, far, 0)}, true,
        TranslateGizmo.OUTLINE_WIDTH);
    root.attachChild(border);

    root.setRotation(rotation);

    final GizmoHandle handle = new GizmoHandle(part, root, color, normal, FadeMode.Plane);
    handle.setAlphaScale(fill, TranslateGizmo.PLANE_FILL_ALPHA);
    addGizmoHandle(handle);
    MaterialUtil.autoMaterials(root);
  }

  @Override
  protected void updateCameraFacingHandles(final Camera camera) {
    updateSnapTicks(camera);
    if (_centerHandle == null) {
      return;
    }
    // Orient the center disk's +Z at the camera, expressed in the gizmo's local space.
    _calcVec3A.set(camera.getDirection()).negateLocal();
    _handle.getRotation().applyPre(_calcVec3A, _calcVec3A);
    _calcQuat.fromVectorToVector(Vector3.UNIT_Z, _calcVec3A);
    _centerHandle.getRoot().setRotation(_calcQuat);
  }

  /**
   * Rebuild the snap tick crossbars along the active axis at the grid increment, or hide them when
   * no snapping axis drag is active. Pulses when the snapped position steps to a new grid cell. The
   * grid is in world units while the gizmo holds a constant screen size, so the increment is divided
   * by the gizmo's scale to get its length in the local frame the ticks live in.
   */
  protected void updateSnapTicks(final Camera camera) {
    final GizmoHandle active = _dragState != DragState.NONE ? findGizmoHandle(_lastDragSpatial) : null;
    final boolean axisDrag = active != null && switch (active.getPart()) {
      case AxisX, AxisY, AxisZ -> true;
      default -> false;
    };
    final double inc = activeSnapIncrement();
    if (!axisDrag || inc <= 0) {
      hideSnapTicks();
      return;
    }

    // The grid is world-spaced but the gizmo holds a constant screen size, so a coarse grid seen
    // from far away crowds many ticks into the gizmo. Convert the increment to the local frame the
    // ticks live in (1 local unit is ~the gizmo's pixel footprint on screen) and fade the ticks out
    // as that on-screen spacing gets too small to read.
    final double spacing = inc / _handle.getScale().getX();
    final double density = GizmoMath.fadeAlpha(spacing * _pixelSize * _appliedDpiScale,
        TranslateGizmo.SNAP_TICK_MIN_SCREEN_SPACING, TranslateGizmo.SNAP_TICK_FULL_SCREEN_SPACING);
    if (density <= 0) {
      hideSnapTicks();
      return;
    }

    // Pulse when the snapped gizmo position steps to a new grid cell.
    final ReadOnlyVector3 pos = _handle.getTranslation();
    if (_lastSnapTranslation == null) {
      _lastSnapTranslation = new Vector3(pos);
    } else if (!_lastSnapTranslation.equals(pos)) {
      triggerSnapPulse();
      _lastSnapTranslation.set(pos);
    }

    final ReadOnlyVector3 axis = active.getAxis();
    // Crossbar direction: perpendicular to the axis, facing the camera. The axis is faded out when
    // near edge-on, so it is well-conditioned during a real drag; guard the degenerate case anyway.
    final Vector3 camDir = _handle.getRotation().applyPre(camera.getDirection(), _calcVec3A);
    final Vector3 perp = axis.cross(camDir, _calcVec3B);
    if (perp.lengthSquared() < 1e-10) {
      axis.cross(Vector3.UNIT_Y, perp);
      if (perp.lengthSquared() < 1e-10) {
        axis.cross(Vector3.UNIT_X, perp);
      }
    }
    perp.normalizeLocal();

    final int perSide = Math.min(TranslateGizmo.SNAP_TICK_MAX_PER_SIDE,
        (int) Math.floor(TranslateGizmo.SNAP_TICK_MAX_EXTENT / spacing));
    _snapTickBuffer.clear();
    for (int k = -perSide; k <= perSide; k++) {
      putTick(axis, perp, k * spacing);
    }
    _snapTickBuffer.flip();
    _snapTicks.getMeshData().updateVertexCount();
    _snapTicks.getMeshData().markBufferDirty(MeshData.KEY_VertexCoords);
    _snapTicks.updateModelBound();

    final ReadOnlyColorRGBA base = active.getBaseColor();
    final float p = (float) getSnapPulse();
    _snapTicks.setDefaultColor(base.getRed() + (1f - base.getRed()) * p, base.getGreen() + (1f - base.getGreen()) * p,
        base.getBlue() + (1f - base.getBlue()) * p, TranslateGizmo.SNAP_TICK_ALPHA * (float) density);
    _snapTicks.getSceneHints().setCullHint(CullHint.Never);
  }

  private void hideSnapTicks() {
    if (_snapTicks.getSceneHints().getCullHint() != CullHint.Always) {
      _snapTicks.getSceneHints().setCullHint(CullHint.Always);
    }
    _lastSnapTranslation = null;
  }

  /** Append one tick crossbar (two triangles) centered at axis*along, spanning perp. */
  private void putTick(final ReadOnlyVector3 axis, final ReadOnlyVector3 perp, final double along) {
    final double t = TranslateGizmo.SNAP_TICK_THICK, h = TranslateGizmo.SNAP_TICK_HALF;
    final double ax = axis.getX(), ay = axis.getY(), az = axis.getZ();
    final double px = perp.getX(), py = perp.getY(), pz = perp.getZ();
    final float c0x = (float) (ax * (along - t) - px * h), c0y = (float) (ay * (along - t) - py * h),
        c0z = (float) (az * (along - t) - pz * h);
    final float c1x = (float) (ax * (along + t) - px * h), c1y = (float) (ay * (along + t) - py * h),
        c1z = (float) (az * (along + t) - pz * h);
    final float c2x = (float) (ax * (along + t) + px * h), c2y = (float) (ay * (along + t) + py * h),
        c2z = (float) (az * (along + t) + pz * h);
    final float c3x = (float) (ax * (along - t) + px * h), c3y = (float) (ay * (along - t) + py * h),
        c3z = (float) (az * (along - t) + pz * h);
    _snapTickBuffer.put(c0x).put(c0y).put(c0z).put(c1x).put(c1y).put(c1z).put(c2x).put(c2y).put(c2z);
    _snapTickBuffer.put(c0x).put(c0y).put(c0z).put(c2x).put(c2y).put(c2z).put(c3x).put(c3y).put(c3z);
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
    final Vector3 offset = getNewOffset(handle, _calcVec2A, current, camera, manager);
    final Transform transform = manager.getSpatialState().getTransform();
    transform.setTranslation(offset.addLocal(transform.getTranslation()));

    // apply our filters, if any, now that we've made updates.
    applyFilters(manager);
  }

  @Override
  protected String getReadoutText(final InteractManager manager) {
    if (!_hasDragStartTransform || manager.getSpatialTarget() == null) {
      return null;
    }
    final ReadOnlyVector3 now = manager.getSpatialTarget().getTranslation();
    final ReadOnlyVector3 start = _dragStartTransform.getTranslation();
    final double dx = now.getX() - start.getX();
    final double dy = now.getY() - start.getY();
    final double dz = now.getZ() - start.getZ();
    if (_readoutFormatter != null) {
      return _readoutFormatter.format(new Vector3(dx, dy, dz), now, manager);
    }
    // Built-in: signed delta from where the drag began (ASCII; the signs read as a delta).
    return String.format("%+.2f, %+.2f, %+.2f", dx, dy, dz);
  }

  public ReadoutFormatter getReadoutFormatter() { return _readoutFormatter; }

  /**
   * Set a custom formatter for the translation readout (e.g. converted to cm/m, or absolute
   * position instead of delta). Pass null to restore the built-in delta text.
   */
  public void setReadoutFormatter(final ReadoutFormatter formatter) { _readoutFormatter = formatter; }

  /**
   * Work out the translation delta, in the target's parent coordinate space, described by the
   * mouse moving from oldMouse to the current position while dragging the given handle.
   */
  protected Vector3 getNewOffset(final GizmoHandle handle, final Vector2 oldMouse, final MouseState current,
      final Camera camera, final InteractManager manager) {

    final ReadOnlyVector3 origin = _handle.getWorldTranslation();

    switch (handle.getPart()) {
      case AxisX, AxisY, AxisZ -> {
        // Constrain to the axis: project the two mouse hits onto the axis line.
        final Vector3 dragAxis = _handle.getRotation().applyPost(handle.getAxis(), _calcVec3D).normalizeLocal();
        _calcVec2B.set(current.getX(), current.getY());
        if (!projectOnAxis(dragAxis, oldMouse, _calcVec2B, camera, _calcVec2C)) {
          return _calcVec3A.zero();
        }
        _calcVec3A.set(dragAxis).multiplyLocal(_calcVec2C.getX()).addLocal(origin);
        _calcVec3B.set(dragAxis).multiplyLocal(_calcVec2C.getY()).addLocal(origin);
      }
      case PlaneXY, PlaneXZ, PlaneYZ, Center -> {
        // Free drag against a plane through the gizmo origin.
        final Vector3 normal = _calcVec3C;
        if (handle.getPart() == GizmoPart.Center) {
          normal.set(camera.getDirection());
        } else {
          _handle.getRotation().applyPost(handle.getAxis(), normal).normalizeLocal();
        }
        _calcPlane.setNormal(normal);
        _calcPlane.setConstant(normal.dot(origin));

        getPickRay(oldMouse, camera);
        if (!_calcRay.intersectsPlane(_calcPlane, _calcVec3A)) {
          return _calcVec3A.zero();
        }
        getPickRay(_calcVec2B.set(current.getX(), current.getY()), camera);
        if (!_calcRay.intersectsPlane(_calcPlane, _calcVec3B)) {
          return _calcVec3A.zero();
        }
      }
      default -> {
        return _calcVec3A.zero();
      }
    }

    // convert to target coord space
    final Node parent = manager.getSpatialTarget().getParent();
    if (parent != null) {
      parent.getWorldTransform().applyInverse(_calcVec3A);
      parent.getWorldTransform().applyInverse(_calcVec3B);
    }

    return _calcVec3B.subtractLocal(_calcVec3A);
  }
}
