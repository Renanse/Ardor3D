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

import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Camera.ProjectionMode;

/**
 * Pure math used by the v2 interact gizmos: constant screen-size scaling and view-angle based
 * handle fades. Kept free of scenegraph and GL types (save read-only Camera access) so it can be
 * unit tested directly.
 */
public final class GizmoMath {

  private GizmoMath() {}

  /**
   * Calculate the world-space size that covers a desired number of vertical pixels at a given view
   * depth, under a perspective frustum.
   *
   * @param pixels
   *          desired on-screen size, in pixels.
   * @param camDepth
   *          distance from the camera along the view direction (camera-space z depth). Must be
   *          positive for a sensible result.
   * @param frustumTop
   *          the camera's frustum top value (half the frustum height at the near plane).
   * @param frustumNear
   *          the camera's near plane distance.
   * @param viewportHeightPixels
   *          the viewport height, in pixels.
   * @return the world-space size at the given depth that projects to the requested pixel size.
   */
  public static double worldSizeForPixels(final double pixels, final double camDepth, final double frustumTop,
      final double frustumNear, final double viewportHeightPixels) {
    // The frustum spans 2*frustumTop world units of height at the near plane, growing linearly
    // with depth. Map that against the viewport height to get world units per pixel.
    final double worldHeightAtDepth = 2.0 * camDepth * frustumTop / frustumNear;
    return pixels * worldHeightAtDepth / viewportHeightPixels;
  }

  /**
   * Calculate the world-space size that covers a desired number of vertical pixels under an
   * orthographic frustum. Depth does not matter in this projection.
   *
   * @param pixels
   *          desired on-screen size, in pixels.
   * @param frustumTop
   *          the camera's frustum top value.
   * @param frustumBottom
   *          the camera's frustum bottom value.
   * @param viewportHeightPixels
   *          the viewport height, in pixels.
   * @return the world-space size that projects to the requested pixel size.
   */
  public static double worldSizeForPixelsOrtho(final double pixels, final double frustumTop,
      final double frustumBottom, final double viewportHeightPixels) {
    return pixels * (frustumTop - frustumBottom) / viewportHeightPixels;
  }

  /**
   * Calculate the scale to apply to a unit-sized handle so it covers a desired number of vertical
   * pixels on screen, regardless of camera distance. Dispatches on the camera's projection mode.
   * Positions at or behind the near plane are clamped to the near plane, so the returned scale is
   * always positive.
   *
   * @param camera
   *          the camera viewing the handle.
   * @param worldPos
   *          the world position of the handle.
   * @param pixels
   *          desired on-screen size, in pixels.
   * @return scale factor for a handle of world-size 1.0.
   */
  public static double calculateFixedScreenScale(final Camera camera, final ReadOnlyVector3 worldPos,
      final double pixels) {
    if (camera.getProjectionMode() == ProjectionMode.Perspective) {
      // Depth = (worldPos - cameraLocation) . cameraDirection, computed inline: this runs every
      // frame per gizmo, so avoid allocating a scratch vector for the subtraction.
      final ReadOnlyVector3 loc = camera.getLocation();
      final ReadOnlyVector3 dir = camera.getDirection();
      final double along = (worldPos.getX() - loc.getX()) * dir.getX() + (worldPos.getY() - loc.getY()) * dir.getY()
          + (worldPos.getZ() - loc.getZ()) * dir.getZ();
      final double depth = Math.max(camera.getFrustumNear(), along);
      return worldSizeForPixels(pixels, depth, camera.getFrustumTop(), camera.getFrustumNear(), camera.getHeight());
    }

    return worldSizeForPixelsOrtho(pixels, camera.getFrustumTop(), camera.getFrustumBottom(), camera.getHeight());
  }

  /**
   * The angle between an axis line and the view direction, folded into [0, pi/2]. An axis pointing
   * directly at (or away from) the camera returns 0; an axis perpendicular to the view direction
   * returns pi/2.
   *
   * @param axisDir
   *          unit direction of the axis.
   * @param viewDir
   *          unit view direction of the camera.
   * @return the folded angle, in radians.
   */
  public static double axisViewAngle(final ReadOnlyVector3 axisDir, final ReadOnlyVector3 viewDir) {
    return Math.acos(Math.min(1.0, Math.abs(axisDir.dot(viewDir))));
  }

  /**
   * The angle between a plane and the view direction, folded into [0, pi/2]. A plane viewed
   * edge-on returns 0; a plane viewed face-on returns pi/2.
   *
   * @param planeNormal
   *          unit normal of the plane.
   * @param viewDir
   *          unit view direction of the camera.
   * @return the folded angle, in radians.
   */
  public static double planeViewAngle(final ReadOnlyVector3 planeNormal, final ReadOnlyVector3 viewDir) {
    return Math.asin(Math.min(1.0, Math.abs(planeNormal.dot(viewDir))));
  }

  /**
   * The signed angle that rotates one direction onto another about an axis, in radians. Positive
   * angles are counter-clockwise looking down the axis (right-hand rule). The inputs are expected
   * to be unit length and reasonably close to the plane perpendicular to the axis.
   *
   * @param from
   *          unit start direction.
   * @param to
   *          unit end direction.
   * @param axis
   *          unit rotation axis.
   * @return the signed angle, in radians, in (-pi, pi].
   */
  public static double signedAngle(final ReadOnlyVector3 from, final ReadOnlyVector3 to,
      final ReadOnlyVector3 axis) {
    final double crossDot = axis.getX() * (from.getY() * to.getZ() - from.getZ() * to.getY())
        + axis.getY() * (from.getZ() * to.getX() - from.getX() * to.getZ())
        + axis.getZ() * (from.getX() * to.getY() - from.getY() * to.getX());
    return Math.atan2(crossDot, from.dot(to));
  }

  /**
   * Map a view angle to a fade alpha: 0 at or below hideBelow, 1 at or above fullAbove, linear in
   * between. Used to fade out handles whose drag direction is ill-conditioned at the current view
   * angle.
   *
   * @param angle
   *          the view angle, in radians (see {@link #axisViewAngle(ReadOnlyVector3, ReadOnlyVector3)}
   *          and {@link #planeViewAngle(ReadOnlyVector3, ReadOnlyVector3)}).
   * @param hideBelow
   *          angle at or below which the handle is fully hidden, in radians.
   * @param fullAbove
   *          angle at or above which the handle is fully visible, in radians. Must be greater than
   *          hideBelow.
   * @return alpha in [0, 1].
   */
  public static double fadeAlpha(final double angle, final double hideBelow, final double fullAbove) {
    return MathUtils.clamp((angle - hideBelow) / (fullAbove - hideBelow), 0.0, 1.0);
  }

  /**
   * Advance a value toward a target by frame-rate independent exponential smoothing. Over an
   * elapsed time equal to {@code tau} the value covers {@code 1 - 1/e} (~63%) of the remaining gap,
   * ~95% over {@code 3*tau}; it eases in - fast then slow - and never overshoots the target.
   * Composing two half-steps lands exactly where one full step of the summed time would, so the
   * animation looks the same regardless of frame rate.
   *
   * @param current
   *          the current value.
   * @param target
   *          the value to move toward.
   * @param dt
   *          elapsed time, in the same units as tau. Values &lt;= 0 leave current unchanged.
   * @param tau
   *          the smoothing time constant. Values &lt;= 0 snap straight to the target.
   * @return the value moved toward the target.
   */
  public static double approach(final double current, final double target, final double dt, final double tau) {
    if (tau <= 0.0) {
      return target;
    }
    if (dt <= 0.0) {
      return current;
    }
    return current + (target - current) * (1.0 - Math.exp(-dt / tau));
  }
}
