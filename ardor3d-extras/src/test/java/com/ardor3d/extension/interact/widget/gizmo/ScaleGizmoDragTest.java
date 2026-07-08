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
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Node;

/**
 * Headless geometric tests of ScaleGizmo's drag math, using the same synthetic camera as
 * TranslateGizmoDragTest: 90 degree fov, square 100px viewport, distance 10 - so mouse x=60 maps
 * to 2 world units from center on a plane through the origin, x=70 to 4, and so on.
 */
public class ScaleGizmoDragTest {

  private static final double EPS = 1e-9;

  private Node _target;
  private InteractManager _manager;
  private TestGizmo _gizmo;
  private Camera _camera;

  /** Exposes the protected drag math for testing. */
  private static class TestGizmo extends ScaleGizmo {
    double factor(final GizmoHandle handle, final Vector2 oldMouse, final MouseState current, final Camera camera) {
      return getScaleFactor(handle, oldMouse, current, camera);
    }
  }

  @Before
  public void setup() {
    _target = new Node("target");
    _target.updateGeometricState(0);

    _manager = new InteractManager();
    _gizmo = new TestGizmo();
    _gizmo.withAllHandles();
    _manager.addWidget(_gizmo);
    _manager.setSpatialTarget(_target);

    _camera = new Camera(100, 100);
    _camera.setFrustumPerspective(90, 1, 1, 1000);
    _camera.setLocation(0, 0, 10);
    _camera.lookAt(Vector3.ZERO, Vector3.UNIT_Y);

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

  private double drag(final GizmoPart part, final int fromX, final int fromY, final int toX, final int toY) {
    final GizmoHandle handle = handle(part);
    assertNotNull("gizmo should have a handle for " + part, handle);
    final MouseState current = new MouseState(toX, toY, toX - fromX, toY - fromY, 0, null, null);
    return _gizmo.factor(handle, new Vector2(fromX, fromY), current, _camera);
  }

  @Test
  public void testAxisScaleIsDistanceRatio() {
    // Grab at 2 world units from center, pull to 4: double.
    assertEquals(2.0, drag(GizmoPart.AxisX, 60, 50, 70, 50), EPS);
    // ...and push back in to 1 world unit: quarter.
    assertEquals(0.25, drag(GizmoPart.AxisX, 70, 50, 55, 50), EPS);
  }

  @Test
  public void testAxisScaleIgnoresOffAxisMovement() {
    // Vertical mouse movement is perpendicular to the X axis here; only the X component counts.
    assertEquals(1.0, drag(GizmoPart.AxisX, 60, 50, 60, 70), EPS);
  }

  @Test
  public void testAxisScaleClampsAtCenterCrossing() {
    // Dragging across the gizmo center would flip the sign; the per-event factor is clamped
    // to a small positive value instead of mirroring or exploding.
    assertEquals(ScaleGizmo.MIN_EVENT_FACTOR, drag(GizmoPart.AxisX, 60, 50, 40, 50), EPS);
  }

  @Test
  public void testAxisAlignedWithViewIsRejected() {
    // The Z axis points straight at this camera; the drag must be a no-op.
    assertEquals(1.0, drag(GizmoPart.AxisZ, 60, 50, 70, 50), EPS);
  }

  @Test
  public void testUniformScaleFollowsMouseDelta() {
    // 20px right at UNIFORM_SCALE_RATE 0.005 -> 1.1; up adds the same way, down-left shrinks.
    assertEquals(1.1, drag(GizmoPart.Center, 50, 50, 70, 50), EPS);
    assertEquals(1.1, drag(GizmoPart.Center, 50, 50, 50, 70), EPS);
    assertEquals(0.9, drag(GizmoPart.Center, 50, 50, 40, 40), EPS);
  }

  @Test
  public void testAxisStretchMovesShaftEndpointAndTip() {
    _gizmo.setAxisStretch(GizmoPart.AxisX, 2.0);

    final var shaft = _gizmo._axisShafts.get(GizmoPart.AxisX);
    final var tip = _gizmo._axisTips.get(GizmoPart.AxisX);
    // The shaft's far endpoint (vertex 1) tracks the near face of the stretched tip cube...
    assertEquals(2.0 * ScaleGizmo.TIP_CENTER - ScaleGizmo.TIP_HALF_EXTENT,
        shaft.getMeshData().getVertexBuffer().get(5), 1e-6);
    // ...and the tip translates to the stretched distance without deforming.
    assertEquals(ScaleGizmo.TIP_CENTER * (2.0 - 1.0), tip.getTranslation().getZ(), EPS);

    // Back to rest.
    _gizmo.setAxisStretch(GizmoPart.AxisX, 1.0);
    assertEquals(ScaleGizmo.TIP_CENTER - ScaleGizmo.TIP_HALF_EXTENT,
        shaft.getMeshData().getVertexBuffer().get(5), 1e-6);
    assertEquals(0.0, tip.getTranslation().getZ(), EPS);
  }

  @Test
  public void testAxisStretchIsClampedForDisplay() {
    final var shaft = _gizmo._axisShafts.get(GizmoPart.AxisX);
    final var tip = _gizmo._axisTips.get(GizmoPart.AxisX);

    // Extreme scale-ups display at the cap...
    _gizmo.setAxisStretch(GizmoPart.AxisX, 1000.0);
    assertEquals(ScaleGizmo.TIP_CENTER * (ScaleGizmo.MAX_DISPLAY_STRETCH - 1.0), tip.getTranslation().getZ(), EPS);

    // ...and extreme scale-downs never invert the shaft through the gizmo center.
    _gizmo.setAxisStretch(GizmoPart.AxisX, 1e-6);
    assertTrue(shaft.getMeshData().getVertexBuffer().get(5) > (float) ScaleGizmo.SHAFT_START);
    assertEquals(ScaleGizmo.TIP_CENTER * (ScaleGizmo.MIN_DISPLAY_STRETCH - 1.0), tip.getTranslation().getZ(), EPS);
  }

  @Test
  public void testAxisScaleTracksLocalFrame() {
    // Rotate the target 90 degrees about Z: its local X axis is world +Y. The X handle must
    // project against world Y - the same 2:1 pull, made vertically, still doubles.
    _target.setRotation(new Matrix3().fromAngleNormalAxis(MathUtils.HALF_PI, Vector3.UNIT_Z));
    _target.updateGeometricState(0);
    _gizmo.targetDataUpdated(_manager);
    _gizmo.getHandle().updateGeometricState(0);

    assertEquals(2.0, drag(GizmoPart.AxisX, 50, 60, 50, 70), EPS);
    // ...and horizontal movement no longer affects it.
    assertEquals(1.0, drag(GizmoPart.AxisX, 50, 60, 70, 60), EPS);
  }
}
