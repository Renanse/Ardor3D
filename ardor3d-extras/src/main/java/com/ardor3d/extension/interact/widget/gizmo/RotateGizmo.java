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
import com.ardor3d.math.ColorRGBA;
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
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.geom.GeometryTool;

/**
 * A v2 rotation gizmo: three axis rings drawn as half-circles kept facing the camera (the back
 * half of a rotation circle is not usefully draggable), plus an outer ring for rolling about the
 * view direction. Rings are antialiased screen-space strokes holding a constant pixel width.
 * While dragging, a translucent pie wedge with stroked edges sweeps out the accumulated rotation
 * and a text readout shows the angle in degrees. See {@link AbstractGizmo} for the shared visual
 * behavior.
 */
public class RotateGizmo extends AbstractGizmo {

  // All geometry is built to a gizmo of radius ~1.0, sized on screen by AbstractGizmo.
  // Stroke widths are in screen pixels at 1:1 DPI scale.
  public static final double RING_RADIUS = 1.0;
  public static final double VIEW_RING_RADIUS = 1.18;
  public static final float RING_WIDTH = 3f;
  public static final double PICK_PROXY_TUBE_RADIUS = 0.08;
  public static final double PIE_RADIUS = 0.95;
  public static final float PIE_ALPHA = 0.3f;
  public static final float PIE_EDGE_WIDTH = 2.5f;
  public static final float PIE_EDGE_ALPHA = 0.9f;
  /** Cap on pie wedge tessellation; beyond this, segments span more than 5 degrees each. */
  public static final int PIE_MAX_SEGMENTS = 256;
  public static final int ARC_SAMPLES = 48;

  /** Snap tick notches straddle the ring: half radial extent and half tangential width, in units. */
  public static final double SNAP_TICK_RADIAL = 0.06;
  public static final double SNAP_TICK_TANGENT = 0.012;
  public static final float SNAP_TICK_ALPHA = 0.9f;
  /** Cap on snap ticks drawn to each side of the grab direction. */
  public static final int SNAP_TICK_MAX_PER_SIDE = 18;

  /** The roll ring's white, a touch brighter than the shared center-handle gray. */
  public static final ReadOnlyColorRGBA VIEW_RING_COLOR = new ColorRGBA(0.95f, 0.95f, 0.95f, 0.9f);

  /** Formats the drag angle readout - see {@link RotateGizmo#setReadoutFormatter}. */
  @FunctionalInterface
  public interface ReadoutFormatter {
    /**
     * @param angleRadians
     *          the applied drag angle, in radians.
     * @param manager
     *          the interact manager (for the target, etc.).
     * @return the readout text, or null to show nothing.
     */
    String format(double angleRadians, InteractManager manager);
  }

  /** Custom formatter for the angle readout; the built-in degrees text is used when null. */
  protected ReadoutFormatter _readoutFormatter;

  protected GizmoHandle _viewRingHandle;

  // _calcQuat is the shared scratch quaternion from AbstractGizmo.
  protected final Matrix3 _calcMat3 = new Matrix3();
  protected final Matrix3 _calcMat3B = new Matrix3();

  /** Accumulated raw angle of the active drag, in radians, before any filters. */
  protected double _dragAngle;
  /**
   * The drag angle actually applied to the state after filters ran (e.g. angle snapping), used
   * for the pie wedge and readout. Matches _dragAngle when no filter modifies the rotation.
   */
  protected double _displayAngle;
  /** World-space direction from the gizmo center to the initial drag grab point. */
  protected Vector3 _dragStartDir = null;
  /** State rotation captured when the drag started, for measuring the applied delta. */
  protected Matrix3 _dragStartRotation = null;
  /** World-space rotation axis of the active drag. */
  protected final Vector3 _dragAxis = new Vector3();

  protected final Mesh _pieMesh;
  /** Reused vertex storage for the pie wedge, so dragging stays allocation-free. */
  protected final FloatBuffer _pieBuffer =
      BufferUtils.createVector3Buffer(RotateGizmo.PIE_MAX_SEGMENTS + 2);
  /** Stroke along the pie wedge's two radial edges: grab direction and current direction. */
  protected final Line _pieEdgeLine;
  protected final FloatBuffer _pieEdgeBuffer = BufferUtils.createVector3Buffer(3);

  /** Radial snap tick notches around the active ring, shown while a snapping ring drag is active. */
  protected final Mesh _snapTicks;
  protected final FloatBuffer _snapTickBuffer =
      BufferUtils.createVector3Buffer((2 * RotateGizmo.SNAP_TICK_MAX_PER_SIDE + 1) * 6);
  /** Last snapped step index, to pulse when it changes; Long.MIN_VALUE when not snapping. */
  protected long _lastSnapStep = Long.MIN_VALUE;

  public RotateGizmo() {
    super("rotateGizmo");

    // Pie wedge showing the swept angle during a drag. Not a handle: not pickable, recolored
    // to match the dragged ring.
    _pieMesh = new Mesh("dragPie");
    _pieMesh.getMeshData().setIndexMode(IndexMode.TriangleFan);
    // seed with a degenerate wedge; updatePie() rewrites the buffer while dragging
    _pieBuffer.limit(3 * 3);
    _pieMesh.getMeshData().setVertexBuffer(_pieBuffer);
    LightProperties.setLightReceiver(_pieMesh, false);
    _pieMesh.getSceneHints().setAllPickingHints(false);
    _pieMesh.getSceneHints().setCullHint(CullHint.Always);
    GizmoGeometry.disableDepthWrite(_pieMesh);
    _handle.attachChild(_pieMesh);
    MaterialUtil.autoMaterials(_pieMesh);

    // Crisp edges on the wedge's radii: rim at the grab direction, center, rim at the current
    // direction. updatePie() rewrites the three points while dragging.
    _pieEdgeLine = new Line("dragPieEdges");
    _pieEdgeLine.getMeshData().setVertexBuffer(_pieEdgeBuffer);
    _pieEdgeLine.getMeshData().setIndexMode(IndexMode.LineStripAdjacency);
    _pieEdgeLine.getMeshData()
        .setIndices(GeometryTool.generateAdjacencyIndices(IndexMode.LineStripAdjacency, 3));
    _pieEdgeLine.setAntialiased(true);
    _pieEdgeLine.setLineWidth(RotateGizmo.PIE_EDGE_WIDTH);
    LightProperties.setLightReceiver(_pieEdgeLine, false);
    _pieEdgeLine.getSceneHints().setAllPickingHints(false);
    _pieEdgeLine.getSceneHints().setCullHint(CullHint.Always);
    GizmoGeometry.disableDepthWrite(_pieEdgeLine);
    _handle.attachChild(_pieEdgeLine);
    MaterialUtil.autoMaterials(_pieEdgeLine);

    // Snap tick notches (small solid quads straddling the ring), shown while a snapping ring drag
    // is active; updateSnapTicks() rewrites them each frame. Non-indexed triangles, two per notch.
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

  /** Add the full set of handles: the three axis rings and the view roll ring. */
  public RotateGizmo withAllHandles() {
    return withAxisRings().withViewRing();
  }

  /** Add the three camera-facing half rings. */
  public RotateGizmo withAxisRings() {
    addRingHandle(GizmoPart.RingX, Vector3.UNIT_X, DEFAULT_X_COLOR);
    addRingHandle(GizmoPart.RingY, Vector3.UNIT_Y, DEFAULT_Y_COLOR);
    addRingHandle(GizmoPart.RingZ, Vector3.UNIT_Z, DEFAULT_Z_COLOR);
    return this;
  }

  /** Add the outer, screen-space roll ring. */
  public RotateGizmo withViewRing() {
    final Node root = new Node(GizmoPart.RingView.name());
    LightProperties.setLightReceiver(root, false);

    final Line ring = GizmoGeometry.arcStroke("ring", RotateGizmo.VIEW_RING_RADIUS, 0, MathUtils.TWO_PI,
        RotateGizmo.ARC_SAMPLES * 2, RotateGizmo.RING_WIDTH);
    root.attachChild(ring);

    final Mesh proxy = GizmoGeometry.arcTube("pickProxy", RotateGizmo.VIEW_RING_RADIUS,
        RotateGizmo.PICK_PROXY_TUBE_RADIUS, 0, MathUtils.TWO_PI, RotateGizmo.ARC_SAMPLES, 6);
    proxy.updateModelBound();
    proxy.getSceneHints().setCullHint(CullHint.Always);
    root.attachChild(proxy);

    _viewRingHandle = new GizmoHandle(GizmoPart.RingView, root, RotateGizmo.VIEW_RING_COLOR, Vector3.ZERO,
        FadeMode.None);
    addGizmoHandle(_viewRingHandle);
    MaterialUtil.autoMaterials(root);
    return this;
  }

  protected void addRingHandle(final GizmoPart part, final ReadOnlyVector3 axis, final ReadOnlyColorRGBA color) {
    final Node root = new Node(part.name());
    LightProperties.setLightReceiver(root, false);

    // A half circle in the XY plane centered on +X; updateCameraFacingHandles turns it so +Z is
    // the rotation axis and +X faces the camera.
    final Line arc = GizmoGeometry.arcStroke("arc", RotateGizmo.RING_RADIUS, -MathUtils.HALF_PI, MathUtils.HALF_PI,
        RotateGizmo.ARC_SAMPLES, RotateGizmo.RING_WIDTH);
    root.attachChild(arc);

    final Mesh proxy = GizmoGeometry.arcTube("pickProxy", RotateGizmo.RING_RADIUS,
        RotateGizmo.PICK_PROXY_TUBE_RADIUS, -MathUtils.HALF_PI, MathUtils.HALF_PI, RotateGizmo.ARC_SAMPLES / 2, 6);
    proxy.updateModelBound();
    proxy.getSceneHints().setCullHint(CullHint.Always);
    root.attachChild(proxy);

    addGizmoHandle(new GizmoHandle(part, root, color, axis, FadeMode.None));
    MaterialUtil.autoMaterials(root);
  }

  @Override
  protected void updateCameraFacingHandles(final Camera camera) {
    // Camera direction in the gizmo's local frame.
    final Vector3 toCamera = _calcVec3A.set(camera.getDirection()).negateLocal();
    _handle.getRotation().applyPre(toCamera, toCamera);

    for (int i = _gizmoHandles.size(); --i >= 0;) {
      final GizmoHandle handle = _gizmoHandles.get(i);
      switch (handle.getPart()) {
        case RingX, RingY, RingZ -> {
          // Face the half ring's center (+X in arc space) at the camera: local +Z maps to the
          // ring axis and +X to the component of the camera direction perpendicular to it.
          final ReadOnlyVector3 axis = handle.getAxis();
          final Vector3 facing = _calcVec3B.set(toCamera)
              .subtractLocal(_calcVec3C.set(axis).multiplyLocal(toCamera.dot(axis)));
          if (facing.lengthSquared() > 1e-8) {
            facing.normalizeLocal();
            handle.getRoot().setRotation(_calcMat3.fromAxes(facing, _calcVec3C.set(axis).crossLocal(facing), axis));
          }
          // Otherwise the ring is face-on and every half is equally good; keep the last one.
        }
        case RingView -> {
          _calcQuat.fromVectorToVector(Vector3.UNIT_Z, toCamera);
          handle.getRoot().setRotation(_calcQuat);
        }
        default -> {
        }
      }
    }

    updatePie();
    updateSnapTicks();
  }

  /**
   * Rebuild the radial snap tick notches around the active ring at the snap increment, or hide them
   * when no snapping ring drag is active. Also pulses when the applied angle crosses to a new step.
   */
  protected void updateSnapTicks() {
    final GizmoHandle active = _dragState != DragState.NONE ? findGizmoHandle(_lastDragSpatial) : null;
    final double inc = activeSnapIncrement();
    if (active == null || _dragStartDir == null || inc <= 0) {
      if (_snapTicks.getSceneHints().getCullHint() != CullHint.Always) {
        _snapTicks.getSceneHints().setCullHint(CullHint.Always);
      }
      _lastSnapStep = Long.MIN_VALUE;
      return;
    }

    // Pulse when the applied (snapped) angle crosses to a new increment.
    final long step = Math.round(_displayAngle / inc);
    if (_lastSnapStep != Long.MIN_VALUE && step != _lastSnapStep) {
      triggerSnapPulse();
    }
    _lastSnapStep = step;

    // In-plane basis from the grab direction and drag axis, in the gizmo's local frame (as updatePie).
    final Vector3 axisLocal = _calcVec3A.set(_dragAxis);
    _handle.getRotation().applyPre(axisLocal, axisLocal);
    final Vector3 e1 = _calcVec3B.set(_dragStartDir);
    _handle.getRotation().applyPre(e1, e1);
    final Vector3 e2 = axisLocal.cross(e1, _calcVec3C);

    final int perSide = Math.min(RotateGizmo.SNAP_TICK_MAX_PER_SIDE, (int) Math.floor(MathUtils.HALF_PI / inc));
    final double rIn = RotateGizmo.RING_RADIUS - RotateGizmo.SNAP_TICK_RADIAL;
    final double rOut = RotateGizmo.RING_RADIUS + RotateGizmo.SNAP_TICK_RADIAL;
    final double w = RotateGizmo.SNAP_TICK_TANGENT;
    _snapTickBuffer.clear();
    for (int k = -perSide; k <= perSide; k++) {
      final double theta = k * inc;
      final double c = Math.cos(theta), s = Math.sin(theta);
      // radial unit dir and tangent, in local space
      final double dx = e1.getX() * c + e2.getX() * s, dy = e1.getY() * c + e2.getY() * s,
          dz = e1.getZ() * c + e2.getZ() * s;
      final double tx = -e1.getX() * s + e2.getX() * c, ty = -e1.getY() * s + e2.getY() * c,
          tz = -e1.getZ() * s + e2.getZ() * c;
      putTick(dx, dy, dz, tx, ty, tz, rIn, rOut, w);
    }
    _snapTickBuffer.flip();
    _snapTicks.getMeshData().updateVertexCount();
    _snapTicks.getMeshData().markBufferDirty(MeshData.KEY_VertexCoords);
    _snapTicks.updateModelBound();

    // Color: the active ring's color, brightened toward white by the snap pulse.
    final ReadOnlyColorRGBA base = active.getBaseColor();
    final float p = (float) getSnapPulse();
    _snapTicks.setDefaultColor(base.getRed() + (1f - base.getRed()) * p, base.getGreen() + (1f - base.getGreen()) * p,
        base.getBlue() + (1f - base.getBlue()) * p, RotateGizmo.SNAP_TICK_ALPHA);
    _snapTicks.getSceneHints().setCullHint(CullHint.Never);
  }

  /** Append one tick notch (two triangles) spanning the radial range at the given in-plane dir/tangent. */
  private void putTick(final double dx, final double dy, final double dz, final double tx, final double ty,
      final double tz, final double rIn, final double rOut, final double w) {
    final float ix = (float) (dx * rIn - tx * w), iy = (float) (dy * rIn - ty * w), iz = (float) (dz * rIn - tz * w);
    final float ox = (float) (dx * rOut - tx * w), oy = (float) (dy * rOut - ty * w), oz = (float) (dz * rOut - tz * w);
    final float ox2 = (float) (dx * rOut + tx * w), oy2 = (float) (dy * rOut + ty * w), oz2 = (float) (dz * rOut + tz * w);
    final float ix2 = (float) (dx * rIn + tx * w), iy2 = (float) (dy * rIn + ty * w), iz2 = (float) (dz * rIn + tz * w);
    _snapTickBuffer.put(ix).put(iy).put(iz).put(ox).put(oy).put(oz).put(ox2).put(oy2).put(oz2);
    _snapTickBuffer.put(ix).put(iy).put(iz).put(ox2).put(oy2).put(oz2).put(ix2).put(iy2).put(iz2);
  }

  @Override
  public void render(final Renderer renderer, final InteractManager manager) {
    // Resolve the applied angle before super.render(), which fills the shared readout via
    // getReadoutText().
    _displayAngle = calculateAppliedAngle(manager);
    super.render(renderer, manager);
  }

  @Override
  protected String getReadoutText(final InteractManager manager) {
    if (_dragStartDir == null) {
      return null;
    }
    if (_readoutFormatter != null) {
      return _readoutFormatter.format(_displayAngle, manager);
    }
    // ASCII only - the outlined readout font carries just printable ASCII, no degree glyph.
    return String.format("%.1f deg", _displayAngle * MathUtils.RAD_TO_DEG);
  }

  public ReadoutFormatter getReadoutFormatter() { return _readoutFormatter; }

  /**
   * Set a custom formatter for the drag angle readout (e.g. radians, or a localized string). Pass
   * null to restore the built-in degrees text.
   */
  public void setReadoutFormatter(final ReadoutFormatter formatter) { _readoutFormatter = formatter; }

  /**
   * The drag angle actually applied to the spatial state, read back after filters ran. A snap
   * filter, for example, leaves the state at quantized steps while the raw drag angle moves
   * continuously - the pie wedge and readout must show the former. The result is unwrapped
   * against the raw angle so drags past a half revolution keep counting.
   */
  protected double calculateAppliedAngle(final InteractManager manager) {
    if (_dragState == DragState.NONE || _dragStartDir == null || _dragStartRotation == null
        || manager.getSpatialTarget() == null) {
      return _dragAngle;
    }

    // Applied delta since drag start, in the target's parent frame.
    _calcMat3.set(_dragStartRotation).transposeLocal();
    manager.getSpatialState().getTransform().getMatrix().multiply(_calcMat3, _calcMat3B);
    final double angle = _calcQuat.fromRotationMatrix(_calcMat3B).toAngleAxis(_calcVec3A);

    // Sign the angle against the drag axis, expressed in the same frame.
    final Vector3 axis = _calcVec3B.set(_dragAxis);
    final Node parent = manager.getSpatialTarget().getParent();
    if (parent != null) {
      parent.getWorldTransform().applyInverseVector(axis);
    }
    if (axis.lengthSquared() < 1e-12) {
      return _dragAngle;
    }
    final double signed = _calcVec3A.dot(axis) < 0 ? -angle : angle;

    // Unwrap: the applied angle is only known modulo a full turn; anchor it to the raw angle.
    double diff = (signed - _dragAngle) % MathUtils.TWO_PI;
    if (diff > Math.PI) {
      diff -= MathUtils.TWO_PI;
    } else if (diff < -Math.PI) {
      diff += MathUtils.TWO_PI;
    }
    return _dragAngle + diff;
  }

  /**
   * Rebuild the pie wedge fan for the current drag, or hide it when no ring drag is active.
   */
  protected void updatePie() {
    final GizmoHandle active = _dragState != DragState.NONE ? findGizmoHandle(_lastDragSpatial) : null;
    if (active == null || _dragStartDir == null) {
      _pieMesh.getSceneHints().setCullHint(CullHint.Always);
      _pieEdgeLine.getSceneHints().setCullHint(CullHint.Always);
      return;
    }
    _pieMesh.getSceneHints().setCullHint(CullHint.Never);
    _pieMesh.setDefaultColor(active.getBaseColor().getRed(), active.getBaseColor().getGreen(),
        active.getBaseColor().getBlue(), RotateGizmo.PIE_ALPHA);
    _pieEdgeLine.getSceneHints().setCullHint(CullHint.Never);
    _pieEdgeLine.setDefaultColor(active.getBaseColor().getRed(), active.getBaseColor().getGreen(),
        active.getBaseColor().getBlue(), RotateGizmo.PIE_EDGE_ALPHA);
    _pieEdgeLine.setLineWidth(RotateGizmo.PIE_EDGE_WIDTH * (float) _appliedDpiScale);

    // Work in the gizmo's local frame: in-plane basis from the grab direction and drag axis.
    final Vector3 axisLocal = _calcVec3A.set(_dragAxis);
    _handle.getRotation().applyPre(axisLocal, axisLocal);
    final Vector3 e1 = _calcVec3B.set(_dragStartDir);
    _handle.getRotation().applyPre(e1, e1);
    final Vector3 e2 = axisLocal.cross(e1, _calcVec3C);

    final int segments = MathUtils.clamp(
        (int) Math.ceil(Math.abs(_displayAngle) / (5 * MathUtils.DEG_TO_RAD)), 1, RotateGizmo.PIE_MAX_SEGMENTS);
    _pieBuffer.clear();
    _pieBuffer.put(0).put(0).put(0);
    for (int i = 0; i <= segments; i++) {
      final double angle = _displayAngle * i / segments;
      final double c = Math.cos(angle) * RotateGizmo.PIE_RADIUS;
      final double s = Math.sin(angle) * RotateGizmo.PIE_RADIUS;
      _pieBuffer.put((float) (e1.getX() * c + e2.getX() * s)) //
          .put((float) (e1.getY() * c + e2.getY() * s)) //
          .put((float) (e1.getZ() * c + e2.getZ() * s));
    }
    _pieBuffer.flip();
    _pieMesh.getMeshData().updateVertexCount();
    _pieMesh.getMeshData().markBufferDirty(MeshData.KEY_VertexCoords);

    // Edge stroke: rim at the grab direction, through the center, to the rim at the current
    // direction.
    final double cosEnd = Math.cos(_displayAngle) * RotateGizmo.PIE_RADIUS;
    final double sinEnd = Math.sin(_displayAngle) * RotateGizmo.PIE_RADIUS;
    _pieEdgeBuffer.clear();
    _pieEdgeBuffer.put((float) (e1.getX() * RotateGizmo.PIE_RADIUS)) //
        .put((float) (e1.getY() * RotateGizmo.PIE_RADIUS)) //
        .put((float) (e1.getZ() * RotateGizmo.PIE_RADIUS));
    _pieEdgeBuffer.put(0).put(0).put(0);
    _pieEdgeBuffer.put((float) (e1.getX() * cosEnd + e2.getX() * sinEnd)) //
        .put((float) (e1.getY() * cosEnd + e2.getY() * sinEnd)) //
        .put((float) (e1.getZ() * cosEnd + e2.getZ() * sinEnd));
    _pieEdgeBuffer.rewind();
    _pieEdgeLine.getMeshData().markBufferDirty(MeshData.KEY_VertexCoords);
  }

  @Override
  public void endDrag(final InteractManager manager, final MouseState current) {
    super.endDrag(manager, current);
    _dragStartDir = null;
    _dragStartRotation = null;
    _dragAngle = 0;
    _displayAngle = 0;
    _lastSnapStep = Long.MIN_VALUE;
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
    final ReadOnlyQuaternion rot = getNewRotation(handle, _calcVec2A, current, camera, manager);
    final Transform transform = manager.getSpatialState().getTransform();
    rot.toRotationMatrix(_calcMat3).multiply(transform.getMatrix(), _calcMat3);
    transform.setRotation(_calcMat3);

    // apply our filters, if any, now that we've made updates.
    applyFilters(manager);
  }

  /**
   * Work out the rotation described by the mouse moving from oldMouse to the current position
   * while dragging the given ring, expressed in the target's parent coordinate space. Also tracks
   * the drag's accumulated angle for the pie wedge and readout.
   */
  protected ReadOnlyQuaternion getNewRotation(final GizmoHandle handle, final Vector2 oldMouse,
      final MouseState current, final Camera camera, final InteractManager manager) {

    final ReadOnlyVector3 origin = _handle.getWorldTranslation();

    // The rotation axis, in world space.
    if (handle.getPart() == GizmoPart.RingView) {
      _dragAxis.set(camera.getDirection()).negateLocal();
    } else {
      _handle.getRotation().applyPost(handle.getAxis(), _dragAxis).normalizeLocal();
    }

    // Drag against the ring's plane.
    _calcPlane.setNormal(_dragAxis);
    _calcPlane.setConstant(_dragAxis.dot(origin));

    getPickRay(oldMouse, camera);
    if (!_calcRay.intersectsPlane(_calcPlane, _calcVec3A)) {
      return Quaternion.IDENTITY;
    }
    getPickRay(_calcVec2B.set(current.getX(), current.getY()), camera);
    if (!_calcRay.intersectsPlane(_calcPlane, _calcVec3B)) {
      return Quaternion.IDENTITY;
    }

    final Vector3 oldDir = _calcVec3A.subtractLocal(origin);
    final Vector3 newDir = _calcVec3B.subtractLocal(origin);
    if (oldDir.lengthSquared() < 1e-12 || newDir.lengthSquared() < 1e-12) {
      return Quaternion.IDENTITY;
    }
    oldDir.normalizeLocal();
    newDir.normalizeLocal();

    // Track the accumulated angle for the pie wedge and readout, anchoring the wedge at the
    // first grab direction. The state still holds the pristine pre-drag rotation here - the
    // widget's delta is applied after this method returns.
    if (_dragStartDir == null) {
      _dragStartDir = new Vector3(oldDir);
      _dragStartRotation = new Matrix3(manager.getSpatialState().getTransform().getMatrix());
      _dragAngle = 0;
    }
    _dragAngle += GizmoMath.signedAngle(oldDir, newDir, _dragAxis);

    // convert to target coord space
    final Node parent = manager.getSpatialTarget().getParent();
    if (parent != null) {
      parent.getWorldTransform().applyInverseVector(oldDir);
      parent.getWorldTransform().applyInverseVector(newDir);
    }

    // return a rotation to take us to the new rotation
    return _calcQuat.fromVectorToVector(oldDir, newDir);
  }

  public double getDragAngle() { return _dragAngle; }
}
