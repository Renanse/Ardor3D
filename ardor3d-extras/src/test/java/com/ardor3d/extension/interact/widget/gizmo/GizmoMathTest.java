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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Camera.ProjectionMode;

public class GizmoMathTest {

  private static final double EPS = 1e-9;

  // --- worldSizeForPixels (perspective) ---

  @Test
  public void testWorldSizeForPixelsKnownValue() {
    // 60 degree vertical fov, near=1 -> frustumTop = tan(30 deg). At depth 10 the frustum is
    // 2 * 10 * tan(30) = 11.547 world units tall. 90px of a 900px viewport is a tenth of that.
    final double top = Math.tan(30 * MathUtils.DEG_TO_RAD);
    final double size = GizmoMath.worldSizeForPixels(90, 10, top, 1, 900);
    assertEquals(90.0 / 900.0 * 2.0 * 10.0 * top, size, EPS);
    assertEquals(1.1547005383792515, size, 1e-12);
  }

  @Test
  public void testWorldSizeForPixelsScalesLinearlyWithDepth() {
    final double top = Math.tan(30 * MathUtils.DEG_TO_RAD);
    final double near = GizmoMath.worldSizeForPixels(90, 5, top, 1, 900);
    final double far = GizmoMath.worldSizeForPixels(90, 50, top, 1, 900);
    assertEquals(10.0, far / near, EPS);
  }

  @Test
  public void testWorldSizeForPixelsInverseWithViewportHeight() {
    // Same fov on a taller viewport means more pixels per world unit, so a smaller world size.
    final double top = Math.tan(30 * MathUtils.DEG_TO_RAD);
    final double at900 = GizmoMath.worldSizeForPixels(90, 10, top, 1, 900);
    final double at1800 = GizmoMath.worldSizeForPixels(90, 10, top, 1, 1800);
    assertEquals(at900 / 2.0, at1800, EPS);
  }

  @Test
  public void testWorldSizeForPixelsNearPlaneIndependence() {
    // frustumTop and frustumNear encode the same fov when scaled together; result must not change.
    final double top = Math.tan(30 * MathUtils.DEG_TO_RAD);
    final double nearOne = GizmoMath.worldSizeForPixels(90, 10, top, 1, 900);
    final double nearTen = GizmoMath.worldSizeForPixels(90, 10, 10 * top, 10, 900);
    assertEquals(nearOne, nearTen, EPS);
  }

  // --- worldSizeForPixelsOrtho ---

  @Test
  public void testWorldSizeForPixelsOrtho() {
    // 40 world units of frustum height over 800 pixels = 0.05 units per pixel.
    assertEquals(90 * 0.05, GizmoMath.worldSizeForPixelsOrtho(90, 20, -20, 800), EPS);
  }

  // --- calculateFixedScreenScale ---

  @Test
  public void testFixedScreenScalePerspective() {
    final Camera cam = new Camera(1200, 900);
    cam.setFrustumPerspective(60, 1200.0 / 900.0, 1, 1000);
    cam.setLocation(0, 0, 10);
    cam.lookAt(Vector3.ZERO, Vector3.UNIT_Y);

    final double scale = GizmoMath.calculateFixedScreenScale(cam, Vector3.ZERO, 90);
    assertEquals(1.1547005383792515, scale, 1e-9);
  }

  @Test
  public void testFixedScreenScaleUsesViewDepthNotDistance() {
    final Camera cam = new Camera(1200, 900);
    cam.setFrustumPerspective(60, 1200.0 / 900.0, 1, 1000);
    cam.setLocation(0, 0, 10);
    cam.lookAt(Vector3.ZERO, Vector3.UNIT_Y);

    // A point off to the side at the same view depth (z=0 plane) must get the same scale,
    // even though its euclidean distance from the camera is larger.
    final double centered = GizmoMath.calculateFixedScreenScale(cam, Vector3.ZERO, 90);
    final double offside = GizmoMath.calculateFixedScreenScale(cam, new Vector3(7, 3, 0), 90);
    assertEquals(centered, offside, EPS);
  }

  @Test
  public void testFixedScreenScaleClampsBehindCamera() {
    final Camera cam = new Camera(1200, 900);
    cam.setFrustumPerspective(60, 1200.0 / 900.0, 1, 1000);
    cam.setLocation(0, 0, 10);
    cam.lookAt(Vector3.ZERO, Vector3.UNIT_Y);

    // Behind the camera: clamped to the near plane, still a small positive scale.
    final double scale = GizmoMath.calculateFixedScreenScale(cam, new Vector3(0, 0, 50), 90);
    assertTrue("expected positive scale, got " + scale, scale > 0);
    assertEquals(GizmoMath.worldSizeForPixels(90, cam.getFrustumNear(), cam.getFrustumTop(), cam.getFrustumNear(),
        cam.getHeight()), scale, EPS);
  }

  @Test
  public void testFixedScreenScaleOrthographic() {
    final Camera cam = new Camera(800, 800);
    cam.setProjectionMode(ProjectionMode.Orthographic);
    cam.setFrustum(1, 1000, -20, 20, 20, -20);
    cam.setLocation(0, 0, 10);
    cam.lookAt(Vector3.ZERO, Vector3.UNIT_Y);

    // Ortho ignores depth entirely.
    final double near = GizmoMath.calculateFixedScreenScale(cam, Vector3.ZERO, 90);
    final double far = GizmoMath.calculateFixedScreenScale(cam, new Vector3(0, 0, -500), 90);
    assertEquals(near, far, EPS);
    assertEquals(90 * 40.0 / 800.0, near, EPS);
  }

  // --- axisViewAngle / planeViewAngle ---

  @Test
  public void testAxisViewAngle() {
    // Perpendicular to the view: fully well-conditioned.
    assertEquals(MathUtils.HALF_PI, GizmoMath.axisViewAngle(Vector3.UNIT_X, Vector3.NEG_UNIT_Z), EPS);
    // Pointing straight along the view: degenerate, folded the same both ways.
    assertEquals(0.0, GizmoMath.axisViewAngle(Vector3.UNIT_Z, Vector3.NEG_UNIT_Z), EPS);
    assertEquals(0.0, GizmoMath.axisViewAngle(Vector3.NEG_UNIT_Z, Vector3.NEG_UNIT_Z), EPS);
    // 45 degrees.
    final Vector3 diag = new Vector3(1, 0, 1).normalizeLocal();
    assertEquals(MathUtils.QUARTER_PI, GizmoMath.axisViewAngle(diag, Vector3.NEG_UNIT_Z), EPS);
  }

  @Test
  public void testPlaneViewAngle() {
    // Plane faces the camera (normal along view): fully visible.
    assertEquals(MathUtils.HALF_PI, GizmoMath.planeViewAngle(Vector3.UNIT_Z, Vector3.NEG_UNIT_Z), EPS);
    // Plane edge-on to the camera (normal perpendicular to view): degenerate.
    assertEquals(0.0, GizmoMath.planeViewAngle(Vector3.UNIT_X, Vector3.NEG_UNIT_Z), EPS);
    // 45 degrees.
    final Vector3 diag = new Vector3(1, 0, 1).normalizeLocal();
    assertEquals(MathUtils.QUARTER_PI, GizmoMath.planeViewAngle(diag, Vector3.NEG_UNIT_Z), EPS);
  }

  @Test
  public void testViewAnglesTolerateUnnormalizedRoundoff() {
    // dot may drift a hair over 1.0 for normalized inputs; acos/asin must not return NaN.
    final Vector3 axis = new Vector3(1 + 1e-12, 0, 0);
    final double angle = GizmoMath.axisViewAngle(axis, Vector3.UNIT_X);
    assertEquals(0.0, angle, EPS);
    final double plane = GizmoMath.planeViewAngle(axis, Vector3.UNIT_X);
    assertEquals(MathUtils.HALF_PI, plane, EPS);
  }

  // --- signedAngle ---

  @Test
  public void testSignedAngle() {
    // Counter-clockwise about +Z (right-hand rule): X to Y is +90 degrees.
    assertEquals(MathUtils.HALF_PI, GizmoMath.signedAngle(Vector3.UNIT_X, Vector3.UNIT_Y, Vector3.UNIT_Z), EPS);
    // ...and clockwise is negative.
    assertEquals(-MathUtils.HALF_PI, GizmoMath.signedAngle(Vector3.UNIT_Y, Vector3.UNIT_X, Vector3.UNIT_Z), EPS);
    // Flipping the axis flips the sign.
    assertEquals(-MathUtils.HALF_PI, GizmoMath.signedAngle(Vector3.UNIT_X, Vector3.UNIT_Y, Vector3.NEG_UNIT_Z), EPS);
    // Identity and half-turn.
    assertEquals(0.0, GizmoMath.signedAngle(Vector3.UNIT_X, Vector3.UNIT_X, Vector3.UNIT_Z), EPS);
    assertEquals(MathUtils.PI, Math.abs(GizmoMath.signedAngle(Vector3.UNIT_X, Vector3.NEG_UNIT_X, Vector3.UNIT_Z)),
        EPS);
    // 45 degrees.
    final Vector3 diag = new Vector3(1, 1, 0).normalizeLocal();
    assertEquals(MathUtils.QUARTER_PI, GizmoMath.signedAngle(Vector3.UNIT_X, diag, Vector3.UNIT_Z), EPS);
  }

  // --- fadeAlpha ---

  @Test
  public void testFadeAlpha() {
    final double hide = 10 * MathUtils.DEG_TO_RAD;
    final double full = 20 * MathUtils.DEG_TO_RAD;

    assertEquals(0.0, GizmoMath.fadeAlpha(0, hide, full), EPS);
    assertEquals(0.0, GizmoMath.fadeAlpha(hide, hide, full), EPS);
    assertEquals(0.5, GizmoMath.fadeAlpha(15 * MathUtils.DEG_TO_RAD, hide, full), EPS);
    assertEquals(1.0, GizmoMath.fadeAlpha(full, hide, full), EPS);
    assertEquals(1.0, GizmoMath.fadeAlpha(MathUtils.HALF_PI, hide, full), EPS);
  }

  // --- approach ---

  @Test
  public void testApproachSnapsWhenTauNonPositive() {
    assertEquals(5.0, GizmoMath.approach(2.0, 5.0, 0.016, 0.0), EPS);
    assertEquals(5.0, GizmoMath.approach(2.0, 5.0, 0.016, -1.0), EPS);
  }

  @Test
  public void testApproachUnchangedWhenNoTimePasses() {
    assertEquals(2.0, GizmoMath.approach(2.0, 5.0, 0.0, 0.05), EPS);
    assertEquals(2.0, GizmoMath.approach(2.0, 5.0, -0.01, 0.05), EPS);
  }

  @Test
  public void testApproachCoversOneMinusInverseEAfterOneTau() {
    // After dt == tau, exactly (1 - 1/e) of the gap toward the target is covered.
    assertEquals(1.0 - 1.0 / Math.E, GizmoMath.approach(0.0, 1.0, 0.05, 0.05), 1e-12);
  }

  @Test
  public void testApproachConvergesMonotonicallyWithoutOvershoot() {
    double v = 0.0;
    for (int i = 0; i < 100; i++) {
      final double next = GizmoMath.approach(v, 1.0, 0.016, 0.05);
      assertTrue("must not overshoot the target", next <= 1.0 + EPS);
      assertTrue("must move toward the target", next >= v);
      v = next;
    }
    assertEquals("should be nearly converged after ~1.6s at tau=0.05", 1.0, v, 1e-6);
  }

  @Test
  public void testApproachIsSymmetricDecreasing() {
    // Falling toward a target mirrors rising toward it.
    final double up = GizmoMath.approach(0.0, 1.0, 0.05, 0.05);
    final double down = GizmoMath.approach(1.0, 0.0, 0.05, 0.05);
    assertEquals(up, 1.0 - down, 1e-12);
  }

  @Test
  public void testApproachIsFrameRateIndependent() {
    // One step of 0.1s lands where two steps of 0.05s do - exponential smoothing composes.
    final double oneBig = GizmoMath.approach(0.0, 1.0, 0.1, 0.05);
    double two = GizmoMath.approach(0.0, 1.0, 0.05, 0.05);
    two = GizmoMath.approach(two, 1.0, 0.05, 0.05);
    assertEquals(oneBig, two, 1e-12);
  }
}
