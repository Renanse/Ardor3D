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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.widget.InteractMatrix;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Node;

/**
 * Headless geometric tests of TranslateGizmo's drag math: synthetic camera, hand-computed pick
 * rays, no GL. The camera uses a 90 degree fov on a square 100px viewport at distance 10, so a
 * 10px mouse move through the view center maps to exactly 2 world units on a plane through the
 * origin.
 */
public class TranslateGizmoDragTest {

  private static final double EPS = MathUtils.ZERO_TOLERANCE;

  private Node _parent;
  private Node _target;
  private InteractManager _manager;
  private TestGizmo _gizmo;
  private Camera _camera;

  /** Exposes the protected drag math for testing. */
  private static class TestGizmo extends TranslateGizmo {
    Vector3 offset(final GizmoHandle handle, final Vector2 oldMouse, final MouseState current, final Camera camera,
        final InteractManager manager) {
      return getNewOffset(handle, oldMouse, current, camera, manager);
    }
  }

  @Before
  public void setup() {
    _parent = new Node("parent");
    _target = new Node("target");
    _parent.attachChild(_target);
    _parent.updateGeometricState(0);

    _manager = new InteractManager();
    _gizmo = new TestGizmo();
    _gizmo.withAllHandles();
    _manager.addWidget(_gizmo);
    _manager.setSpatialTarget(_target);

    _camera = new Camera(100, 100);
    _camera.setFrustumPerspective(90, 1, 1, 1000);
    _camera.setLocation(0, 0, 10);
    _camera.lookAt(Vector3.ZERO, Vector3.UNIT_Y);

    syncHandleToTarget();
  }

  /** Mirror what AbstractGizmo.render does before drag math runs: position the handle node. */
  private void syncHandleToTarget() {
    _gizmo.getHandle().setTranslation(_target.getWorldTranslation());
    _gizmo.getHandle().updateGeometricState(0);
  }

  private GizmoHandle handle(final GizmoPart part) {
    for (final GizmoHandle handle : _gizmo.getGizmoHandles()) {
      if (handle.getPart() == part) {
        return handle;
      }
    }
    return null;
  }

  private Vector3 drag(final GizmoPart part, final int fromX, final int fromY, final int toX, final int toY) {
    final GizmoHandle handle = handle(part);
    assertNotNull("gizmo should have a handle for " + part, handle);
    final MouseState current = new MouseState(toX, toY, toX - fromX, toY - fromY, 0, null, null);
    return _gizmo.offset(handle, new Vector2(fromX, fromY), current, _camera, _manager);
  }

  @Test
  public void testAxisDragMapsPixelsToWorldUnits() {
    // 10px right from view center -> ndc 0.2 -> 0.2 * depth 10 = 2 world units along +X.
    final Vector3 offset = drag(GizmoPart.AxisX, 50, 50, 60, 50);
    assertEquals(2.0, offset.getX(), EPS);
    assertEquals(0.0, offset.getY(), EPS);
    assertEquals(0.0, offset.getZ(), EPS);
  }

  @Test
  public void testAxisDragConstrainsToAxis() {
    // A diagonal mouse move only translates by its component along the dragged axis.
    final Vector3 offset = drag(GizmoPart.AxisX, 50, 50, 60, 60);
    assertEquals(2.0, offset.getX(), EPS);
    assertEquals(0.0, offset.getY(), EPS);
    assertEquals(0.0, offset.getZ(), EPS);

    final Vector3 offsetY = drag(GizmoPart.AxisY, 50, 50, 60, 60);
    assertEquals(0.0, offsetY.getX(), EPS);
    assertEquals(2.0, offsetY.getY(), EPS);
    assertEquals(0.0, offsetY.getZ(), EPS);
  }

  @Test
  public void testAxisAlignedWithViewIsRejected() {
    // The Z axis points straight at this camera; the drag is ill-conditioned and must be a no-op.
    final Vector3 offset = drag(GizmoPart.AxisZ, 50, 50, 60, 50);
    assertEquals(0.0, offset.length(), EPS);
  }

  @Test
  public void testCenterDragMovesInViewPlane() {
    // The view plane here is the XY plane: mouse up maps straight to +Y.
    final Vector3 offset = drag(GizmoPart.Center, 50, 50, 50, 60);
    assertEquals(0.0, offset.getX(), EPS);
    assertEquals(2.0, offset.getY(), EPS);
    assertEquals(0.0, offset.getZ(), EPS);
  }

  @Test
  public void testPlaneDragStaysInPlane() {
    // Look down at 45 degrees so the XZ plane is visible.
    _camera.setLocation(0, 10, 10);
    _camera.lookAt(Vector3.ZERO, Vector3.UNIT_Y);

    // Mouse right: ndc 0.2 of the half-frustum, the ray hits y=0 at depth |(0,10,10)| along the
    // view ray's center line -> x = 0.2 * sqrt(200) = 2*sqrt(2). Must have no Y component.
    final Vector3 offset = drag(GizmoPart.PlaneXZ, 50, 50, 60, 50);
    assertEquals(2.0 * Math.sqrt(2.0), offset.getX(), 1e-6);
    assertEquals(0.0, offset.getY(), EPS);
    assertEquals(0.0, offset.getZ(), 1e-6);

    // Mouse up from the view center slides the hit point away from the camera across the plane:
    // still y=0, moving in -Z.
    final Vector3 offsetUp = drag(GizmoPart.PlaneXZ, 50, 50, 50, 60);
    assertEquals(0.0, offsetUp.getX(), 1e-6);
    assertEquals(0.0, offsetUp.getY(), EPS);
    assertEquals(-5.0, offsetUp.getZ(), 1e-6);
  }

  @Test
  public void testOffsetIsConvertedToParentSpace() {
    // With the parent scaled 2x, the same world-space drag is half as large in parent units.
    _parent.setScale(2);
    _parent.updateGeometricState(0);
    syncHandleToTarget();

    final Vector3 offset = drag(GizmoPart.AxisX, 50, 50, 60, 50);
    assertEquals(1.0, offset.getX(), EPS);
    assertEquals(0.0, offset.getY(), EPS);
    assertEquals(0.0, offset.getZ(), EPS);
  }

  @Test
  public void testLocalInteractMatrixDragsAlongRotatedAxis() {
    // Rotate the target 90 degrees about Z: its local X axis becomes world +Y. In Local mode the
    // X handle must drag along world +Y.
    _target.setRotation(new Matrix3().fromAngleNormalAxis(MathUtils.HALF_PI, Vector3.UNIT_Z));
    _target.updateGeometricState(0);

    _gizmo.setInteractMatrix(InteractMatrix.Local);
    _gizmo.targetDataUpdated(_manager);
    syncHandleToTarget();

    final Vector3 offset = drag(GizmoPart.AxisX, 50, 50, 50, 60);
    assertEquals(0.0, offset.getX(), EPS);
    assertEquals(2.0, offset.getY(), EPS);
    assertEquals(0.0, offset.getZ(), EPS);
  }
}
