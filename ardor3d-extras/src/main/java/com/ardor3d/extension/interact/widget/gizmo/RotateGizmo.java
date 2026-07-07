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
import com.ardor3d.math.Plane;
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
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.MaterialUtil;

/**
 * A v2 rotation gizmo: three axis rings drawn as half-circles kept facing the camera (the back
 * half of a rotation circle is not usefully draggable), plus an outer ring for rolling about the
 * view direction. While dragging, a translucent pie wedge sweeps out the accumulated rotation and
 * a text readout shows the angle in degrees. See {@link AbstractGizmo} for the shared visual
 * behavior.
 */
public class RotateGizmo extends AbstractGizmo {

  // All geometry is built to a gizmo of radius ~1.0, sized on screen by AbstractGizmo.
  public static final double RING_RADIUS = 1.0;
  public static final double VIEW_RING_RADIUS = 1.18;
  public static final double TUBE_RADIUS = 0.011;
  public static final double PICK_PROXY_TUBE_RADIUS = 0.06;
  public static final double PIE_RADIUS = 0.95;
  public static final float PIE_ALPHA = 0.3f;
  public static final int ARC_SAMPLES = 48;
  public static final int TUBE_SAMPLES = 8;

  protected GizmoHandle _viewRingHandle;

  protected final Quaternion _calcQuat = new Quaternion();
  protected final Matrix3 _calcMat3 = new Matrix3();

  /** Accumulated angle of the active drag, in radians. */
  protected double _dragAngle;
  /** World-space direction from the gizmo center to the initial drag grab point. */
  protected Vector3 _dragStartDir = null;
  /** World-space rotation axis of the active drag. */
  protected final Vector3 _dragAxis = new Vector3();

  protected final Mesh _pieMesh;
  protected final BasicText _angleText;

  public RotateGizmo() {
    super("rotateGizmo");

    // Pie wedge showing the swept angle during a drag. Not a handle: not pickable, recolored
    // to match the dragged ring.
    _pieMesh = new Mesh("dragPie");
    _pieMesh.getMeshData().setIndexMode(IndexMode.TriangleFan);
    // seed with a degenerate wedge; updatePie() replaces it while dragging
    _pieMesh.getMeshData().setVertexBuffer(BufferUtils.createVector3Buffer(3));
    LightProperties.setLightReceiver(_pieMesh, false);
    final CullState pieCull = new CullState();
    pieCull.setCullFace(CullState.Face.None);
    _pieMesh.setRenderState(pieCull);
    _pieMesh.getSceneHints().setAllPickingHints(false);
    _pieMesh.getSceneHints().setCullHint(CullHint.Always);
    _handle.attachChild(_pieMesh);
    MaterialUtil.autoMaterials(_pieMesh);

    // Screen-space angle readout, living in the ortho render queue. The gizmo shows, hides,
    // positions and updates it during ring drags; the application decides where it renders by
    // attaching it to its UI/ortho root (see getAngleReadout()).
    _angleText = BasicText.createDefaultTextLabel("angleReadout", "", 16);
    _angleText.setTextColor(ColorRGBA.WHITE);
    _angleText.getSceneHints().setCullHint(CullHint.Always);
  }

  /**
   * @return the screen-space angle readout shown while dragging a ring. Attach it to an
   *         application node rendered in ortho/screen coordinates (with the ortho origin at the
   *         bottom left) to enable it; the gizmo takes care of visibility, position and content.
   */
  public BasicText getAngleReadout() { return _angleText; }

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

    final Mesh ring = GizmoGeometry.arcTube("ring", RotateGizmo.VIEW_RING_RADIUS, RotateGizmo.TUBE_RADIUS, 0,
        MathUtils.TWO_PI, RotateGizmo.ARC_SAMPLES * 2, RotateGizmo.TUBE_SAMPLES);
    ring.updateModelBound();
    root.attachChild(ring);

    final Mesh proxy = GizmoGeometry.arcTube("pickProxy", RotateGizmo.VIEW_RING_RADIUS,
        RotateGizmo.PICK_PROXY_TUBE_RADIUS, 0, MathUtils.TWO_PI, RotateGizmo.ARC_SAMPLES, 6);
    proxy.updateModelBound();
    proxy.getSceneHints().setCullHint(CullHint.Always);
    root.attachChild(proxy);

    _viewRingHandle = new GizmoHandle(GizmoPart.RingView, root, DEFAULT_CENTER_COLOR, Vector3.ZERO, FadeMode.None);
    addGizmoHandle(_viewRingHandle);
    MaterialUtil.autoMaterials(root);
    return this;
  }

  protected void addRingHandle(final GizmoPart part, final ReadOnlyVector3 axis, final ReadOnlyColorRGBA color) {
    final Node root = new Node(part.name());
    LightProperties.setLightReceiver(root, false);

    // A half circle in the XY plane centered on +X; updateCameraFacingHandles turns it so +Z is
    // the rotation axis and +X faces the camera.
    final Mesh arc = GizmoGeometry.arcTube("arc", RotateGizmo.RING_RADIUS, RotateGizmo.TUBE_RADIUS, -MathUtils.HALF_PI,
        MathUtils.HALF_PI, RotateGizmo.ARC_SAMPLES, RotateGizmo.TUBE_SAMPLES);
    arc.updateModelBound();
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
  }

  @Override
  public void render(final Renderer renderer, final InteractManager manager) {
    super.render(renderer, manager);
    updateAngleReadout(manager);
  }

  protected void updateAngleReadout(final InteractManager manager) {
    if (manager.getSpatialTarget() == null || _dragState == DragState.NONE || _dragStartDir == null) {
      hideAngleReadout();
      return;
    }

    // Show the readout just above the gizmo center, in screen coordinates (ortho space).
    final Camera camera = Camera.getCurrentCamera();
    final Vector3 screen = camera.getScreenCoordinates(_handle.getWorldTranslation(), _calcVec3A);
    _angleText.setText(String.format("%.1f°", _dragAngle * MathUtils.RAD_TO_DEG));
    _angleText.setTranslation((int) (screen.getX() + 12), (int) (screen.getY() + 12), 0);
    _angleText.getSceneHints().setCullHint(CullHint.Never);
  }

  protected void hideAngleReadout() {
    if (_angleText.getSceneHints().getCullHint() != CullHint.Always) {
      _angleText.getSceneHints().setCullHint(CullHint.Always);
    }
  }

  /**
   * Rebuild the pie wedge fan for the current drag, or hide it when no ring drag is active.
   */
  protected void updatePie() {
    final GizmoHandle active = _dragState != DragState.NONE ? findGizmoHandle(_lastDragSpatial) : null;
    if (active == null || _dragStartDir == null) {
      _pieMesh.getSceneHints().setCullHint(CullHint.Always);
      return;
    }
    _pieMesh.getSceneHints().setCullHint(CullHint.Never);
    _pieMesh.setDefaultColor(active.getBaseColor().getRed(), active.getBaseColor().getGreen(),
        active.getBaseColor().getBlue(), RotateGizmo.PIE_ALPHA);

    // Work in the gizmo's local frame: in-plane basis from the grab direction and drag axis.
    final Vector3 axisLocal = _calcVec3A.set(_dragAxis);
    _handle.getRotation().applyPre(axisLocal, axisLocal);
    final Vector3 e1 = _calcVec3B.set(_dragStartDir);
    _handle.getRotation().applyPre(e1, e1);
    final Vector3 e2 = axisLocal.cross(e1, _calcVec3C);

    final int segments = Math.max(1, (int) Math.ceil(Math.abs(_dragAngle) / (5 * MathUtils.DEG_TO_RAD)));
    final FloatBuffer vertexBuffer = BufferUtils.createVector3Buffer(segments + 2);
    vertexBuffer.put(0).put(0).put(0);
    for (int i = 0; i <= segments; i++) {
      final double angle = _dragAngle * i / segments;
      final double c = Math.cos(angle) * RotateGizmo.PIE_RADIUS;
      final double s = Math.sin(angle) * RotateGizmo.PIE_RADIUS;
      vertexBuffer.put((float) (e1.getX() * c + e2.getX() * s)) //
          .put((float) (e1.getY() * c + e2.getY() * s)) //
          .put((float) (e1.getZ() * c + e2.getZ() * s));
    }
    _pieMesh.getMeshData().setVertexBuffer(vertexBuffer);
  }

  @Override
  public void endDrag(final InteractManager manager, final MouseState current) {
    super.endDrag(manager, current);
    _dragStartDir = null;
    _dragAngle = 0;
    hideAngleReadout();
  }

  @Override
  public void processInput(final Canvas source, final TwoInputStates inputStates, final AtomicBoolean inputConsumed,
      final InteractManager manager) {

    final Camera camera = source.getCanvasRenderer().getCamera();
    final MouseState current = inputStates.getCurrent().getMouseState();
    final MouseState previous = inputStates.getPrevious().getMouseState();

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

    final Vector2 oldMouse = new Vector2(previous.getX(), previous.getY());
    final ReadOnlyQuaternion rot = getNewRotation(handle, oldMouse, current, camera, manager);
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
    final Plane pickPlane = new Plane(_dragAxis, _dragAxis.dot(origin));

    getPickRay(oldMouse, camera);
    if (!_calcRay.intersectsPlane(pickPlane, _calcVec3A)) {
      return Quaternion.IDENTITY;
    }
    getPickRay(new Vector2(current.getX(), current.getY()), camera);
    if (!_calcRay.intersectsPlane(pickPlane, _calcVec3B)) {
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
    // first grab direction.
    if (_dragStartDir == null) {
      _dragStartDir = new Vector3(oldDir);
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
