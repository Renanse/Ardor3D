/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.light.shadow;

import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Camera.ProjectionMode;

public class Frustum {

  private double _left, _right, _top, _bottom, _near, _far;

  /** The corners of the frustum. */
  private final Vector3[] _corners = new Vector3[8];

  public Frustum() {
    for (int i = 0; i < _corners.length; i++) {
      _corners[i] = new Vector3();
    }
  }

  public double getLeft() { return _left; }

  public void setLeft(final double left) { _left = left; }

  public double getRight() { return _right; }

  public void setRight(final double right) { _right = right; }

  public double getTop() { return _top; }

  public void setTop(final double top) { _top = top; }

  public double getBottom() { return _bottom; }

  public void setBottom(final double bottom) { _bottom = bottom; }

  public double getNear() { return _near; }

  public void setNear(final double near) { _near = near; }

  public double getFar() { return _far; }

  public void setFar(final double far) { _far = far; }

  public void copyFrustumValues(final Camera source) {
    _near = source.getFrustumNear();
    _far = source.getFrustumFar();
    _left = source.getFrustumLeft();
    _right = source.getFrustumRight();
    _top = source.getFrustumTop();
    _bottom = source.getFrustumBottom();
  }

  /**
   * Compress this camera's near and far frustum planes to be smaller if possible, using the given
   * bounds as a measure.
   *
   * @param sceneBounds
   *          the scene bounds
   */
  public void pack(final BoundingVolume sceneBounds) {
    // final ReadOnlyVector3 center = sceneBounds.getCenter();
    // for (int i = 0; i < _corners.length; i++) {
    // _corners[i].set(center);
    // }
    //
    // if (sceneBounds instanceof BoundingBox) {
    // final BoundingBox bbox = (BoundingBox) sceneBounds;
    // bbox.getExtent(_extents);
    // } else if (sceneBounds instanceof BoundingSphere) {
    // final BoundingSphere bsphere = (BoundingSphere) sceneBounds;
    // _extents.set(bsphere.getRadius(), bsphere.getRadius(), bsphere.getRadius());
    // } else {
    // throw new Ardor3dException("Unsupported bounding volume type: " +
    // sceneBounds.getClass().getName());
    // }
    //
    // _corners[0].addLocal(_extents.getX(), _extents.getY(), _extents.getZ());
    // _corners[1].addLocal(_extents.getX(), -_extents.getY(), _extents.getZ());
    // _corners[2].addLocal(_extents.getX(), _extents.getY(), -_extents.getZ());
    // _corners[3].addLocal(_extents.getX(), -_extents.getY(), -_extents.getZ());
    // _corners[4].addLocal(-_extents.getX(), _extents.getY(), _extents.getZ());
    // _corners[5].addLocal(-_extents.getX(), -_extents.getY(), _extents.getZ());
    // _corners[6].addLocal(-_extents.getX(), _extents.getY(), -_extents.getZ());
    // _corners[7].addLocal(-_extents.getX(), -_extents.getY(), -_extents.getZ());
    //
    // final ReadOnlyMatrix4 mvMatrix = getViewMatrix();
    // double optimalCameraNear = Double.MAX_VALUE;
    // double optimalCameraFar = -Double.MAX_VALUE;
    // final Vector4 position = Vector4.fetchTempInstance();
    // for (int i = 0; i < _corners.length; i++) {
    // position.set(_corners[i].getX(), _corners[i].getY(), _corners[i].getZ(), 1);
    // mvMatrix.applyPre(position, position);
    //
    // optimalCameraNear = Math.min(-position.getZ(), optimalCameraNear);
    // optimalCameraFar = Math.max(-position.getZ(), optimalCameraFar);
    // }
    // Vector4.releaseTempInstance(position);
    //
    // optimalCameraNear = Math.min(Math.max(eyeCam.getFrustumNear(), optimalCameraNear),
    // eyeCam.getFrustumFar());
    // optimalCameraFar = Math.max(optimalCameraNear, Math.min(_maxFarPlaneDistance, optimalCameraFar));
    //
    // final double change = optimalCameraNear / eyeCam.getFrustumNear();
    // setFrustumLeft(eyeCam.getFrustumLeft() * change);
    // setFrustumRight(eyeCam.getFrustumRight() * change);
    // setFrustumTop(eyeCam.getFrustumTop() * change);
    // setFrustumBottom(eyeCam.getFrustumBottom() * change);
    //
    // setFrustumNear(optimalCameraNear);
    // setFrustumFar(optimalCameraFar);
  }

  /**
   * Calculate frustum corners .
   *
   * @param nearDistance
   *          the near distance
   * @param farDistance
   *          the far distance
   */
  public void calculateCorners(final double nearDistance, final double farDistance, final Camera viewCam) {
    // determine the dimensions of the near and far frustums at the given distances
    final double nearPlaneHeight, nearPlaneWidth, farPlaneHeight, farPlaneWidth;
    if (viewCam.getProjectionMode() == ProjectionMode.Orthographic) {
      nearPlaneHeight = (_top - _bottom) * 0.5;
      nearPlaneWidth = (_right - _left) * 0.5;

      farPlaneHeight = (_top - _bottom) * 0.5;
      farPlaneWidth = (_right - _left) * 0.5;
    } else {
      nearPlaneHeight = (_top - _bottom) * nearDistance * 0.5 / _near;
      nearPlaneWidth = (_right - _left) * nearDistance * 0.5 / _near;

      farPlaneHeight = (_top - _bottom) * farDistance * 0.5 / _near;
      farPlaneWidth = (_right - _left) * farDistance * 0.5 / _near;
    }

    final var viewDirection = viewCam.getDirection();
    final var viewLocation = viewCam.getLocation();
    final var viewLeft = viewCam.getLeft();
    final var viewUp = viewCam.getUp();

    final Vector3 nearPlaneCenter = Vector3.fetchTempInstance();
    final Vector3 farPlaneCenter = Vector3.fetchTempInstance();
    final Vector3 direction = Vector3.fetchTempInstance();
    final Vector3 left = Vector3.fetchTempInstance();
    final Vector3 up = Vector3.fetchTempInstance();

    try {
      // determine the center of our near and far planes
      direction.set(viewDirection).multiplyLocal(nearDistance);
      nearPlaneCenter.set(viewLocation).addLocal(direction);
      direction.set(viewDirection).multiplyLocal(farDistance);
      farPlaneCenter.set(viewLocation).addLocal(direction);

      // calculate the corners of the near plane
      left.set(viewLeft).multiplyLocal(nearPlaneWidth);
      up.set(viewUp).multiplyLocal(nearPlaneHeight);
      _corners[0].set(nearPlaneCenter).subtractLocal(left).subtractLocal(up);
      _corners[1].set(nearPlaneCenter).subtractLocal(left).addLocal(up);
      _corners[2].set(nearPlaneCenter).addLocal(left).addLocal(up);
      _corners[3].set(nearPlaneCenter).addLocal(left).subtractLocal(up);

      // calculate the corners of the far plane
      left.set(viewLeft).multiplyLocal(farPlaneWidth);
      up.set(viewUp).multiplyLocal(farPlaneHeight);
      _corners[4].set(farPlaneCenter).subtractLocal(left).subtractLocal(up);
      _corners[5].set(farPlaneCenter).subtractLocal(left).addLocal(up);
      _corners[6].set(farPlaneCenter).addLocal(left).addLocal(up);
      _corners[7].set(farPlaneCenter).addLocal(left).subtractLocal(up);

    } finally {
      Vector3.releaseTempInstance(nearPlaneCenter);
      Vector3.releaseTempInstance(farPlaneCenter);
      Vector3.releaseTempInstance(direction);
      Vector3.releaseTempInstance(left);
      Vector3.releaseTempInstance(up);
    }
  }

  public void toBoundingSphere(final BoundingSphere store) {
    store.averagePoints(_corners);
  }

}
