/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;

/**
 * Camera with additional pssm related functionality.
 */
public class ExtendedCamera extends Camera {
  /** The corners of the camera frustum. */
  protected final Vector3[] _corners = new Vector3[8];

  /** Temporary vector used for storing extents during corner calculations. */
  protected final Vector3 _extents = new Vector3();

  /**
   * Instantiates a new PSSM camera.
   */
  public ExtendedCamera() {
    this(0, 0); // copy later
  }

  /**
   * Instantiates a new PSSM camera.
   * 
   * @param width
   *          the width
   * @param height
   *          the height
   */
  public ExtendedCamera(final int width, final int height) {
    super(width, height);
    init();
  }

  /**
   * Instantiates a new PSSM camera.
   * 
   * @param source
   *          the source
   */
  public ExtendedCamera(final Camera source) {
    super(source);
    init();
  }

  /**
   * Initialize structures.
   */
  private void init() {
    for (int i = 0; i < _corners.length; i++) {
      _corners[i] = new Vector3();
    }
  }

  /**
   * Compress this camera's near and far frustum planes to be smaller if possible, using the given
   * bounds as a measure.
   * 
   * @param sceneBounds
   *          the scene bounds
   */
  public void pack(final BoundingVolume sceneBounds) {
    final ReadOnlyVector3 center = sceneBounds.getCenter();
    for (int i = 0; i < _corners.length; i++) {
      _corners[i].set(center);
    }

    if (sceneBounds instanceof BoundingBox) {
      final BoundingBox bbox = (BoundingBox) sceneBounds;
      bbox.getExtent(_extents);
    } else if (sceneBounds instanceof BoundingSphere) {
      final BoundingSphere bsphere = (BoundingSphere) sceneBounds;
      _extents.set(bsphere.getRadius(), bsphere.getRadius(), bsphere.getRadius());
    }

    _corners[0].addLocal(_extents.getX(), _extents.getY(), _extents.getZ());
    _corners[1].addLocal(_extents.getX(), -_extents.getY(), _extents.getZ());
    _corners[2].addLocal(_extents.getX(), _extents.getY(), -_extents.getZ());
    _corners[3].addLocal(_extents.getX(), -_extents.getY(), -_extents.getZ());
    _corners[4].addLocal(-_extents.getX(), _extents.getY(), _extents.getZ());
    _corners[5].addLocal(-_extents.getX(), -_extents.getY(), _extents.getZ());
    _corners[6].addLocal(-_extents.getX(), _extents.getY(), -_extents.getZ());
    _corners[7].addLocal(-_extents.getX(), -_extents.getY(), -_extents.getZ());

    final ReadOnlyMatrix4 mvMatrix = getViewMatrix();
    double optimalCameraNear = Double.MAX_VALUE;
    double optimalCameraFar = -Double.MAX_VALUE;
    final Vector4 position = Vector4.fetchTempInstance();
    for (int i = 0; i < _corners.length; i++) {
      position.set(_corners[i].getX(), _corners[i].getY(), _corners[i].getZ(), 1);
      mvMatrix.applyPre(position, position);

      optimalCameraNear = Math.min(-position.getZ(), optimalCameraNear);
      optimalCameraFar = Math.max(-position.getZ(), optimalCameraFar);
    }
    Vector4.releaseTempInstance(position);

    // XXX: use of getFrustumNear and getFrustumFar seems suspicious...
    // XXX: It depends on the frustum being reset each update
    optimalCameraNear = Math.min(Math.max(getFrustumNear(), optimalCameraNear), getFrustumFar());
    optimalCameraFar = Math.max(optimalCameraNear, Math.min(getFrustumFar(), optimalCameraFar));

    final double change = optimalCameraNear / _frustumNear;
    setFrustumLeft(getFrustumLeft() * change);
    setFrustumRight(getFrustumRight() * change);
    setFrustumTop(getFrustumTop() * change);
    setFrustumBottom(getFrustumBottom() * change);

    setFrustumNear(optimalCameraNear);
    setFrustumFar(optimalCameraFar);
  }

  public void calculateFrustum() {
    calculateFrustum(_frustumNear, _frustumFar);
  }

  /**
   * Calculate frustum corners and center.
   * 
   * @param fNear
   *          the near distance
   * @param fFar
   *          the far distance
   */
  public void calculateFrustum(final double fNear, final double fFar) {
    double fNearPlaneHeight = (_frustumTop - _frustumBottom) * fNear * 0.5 / _frustumNear;
    double fNearPlaneWidth = (_frustumRight - _frustumLeft) * fNear * 0.5 / _frustumNear;

    double fFarPlaneHeight = (_frustumTop - _frustumBottom) * fFar * 0.5 / _frustumNear;
    double fFarPlaneWidth = (_frustumRight - _frustumLeft) * fFar * 0.5 / _frustumNear;

    if (getProjectionMode() == ProjectionMode.Orthographic) {
      fNearPlaneHeight = (_frustumTop - _frustumBottom) * 0.5;
      fNearPlaneWidth = (_frustumRight - _frustumLeft) * 0.5;

      fFarPlaneHeight = (_frustumTop - _frustumBottom) * 0.5;
      fFarPlaneWidth = (_frustumRight - _frustumLeft) * 0.5;
    }

    final Vector3 vNearPlaneCenter = Vector3.fetchTempInstance();
    final Vector3 vFarPlaneCenter = Vector3.fetchTempInstance();
    final Vector3 direction = Vector3.fetchTempInstance();
    final Vector3 left = Vector3.fetchTempInstance();
    final Vector3 up = Vector3.fetchTempInstance();

    direction.set(getDirection()).multiplyLocal(fNear);
    vNearPlaneCenter.set(getLocation()).addLocal(direction);
    direction.set(getDirection()).multiplyLocal(fFar);
    vFarPlaneCenter.set(getLocation()).addLocal(direction);

    left.set(getLeft()).multiplyLocal(fNearPlaneWidth);
    up.set(getUp()).multiplyLocal(fNearPlaneHeight);
    _corners[0].set(vNearPlaneCenter).subtractLocal(left).subtractLocal(up);
    _corners[1].set(vNearPlaneCenter).subtractLocal(left).addLocal(up);
    _corners[2].set(vNearPlaneCenter).addLocal(left).addLocal(up);
    _corners[3].set(vNearPlaneCenter).addLocal(left).subtractLocal(up);

    left.set(getLeft()).multiplyLocal(fFarPlaneWidth);
    up.set(getUp()).multiplyLocal(fFarPlaneHeight);
    _corners[4].set(vFarPlaneCenter).subtractLocal(left).subtractLocal(up);
    _corners[5].set(vFarPlaneCenter).subtractLocal(left).addLocal(up);
    _corners[6].set(vFarPlaneCenter).addLocal(left).addLocal(up);
    _corners[7].set(vFarPlaneCenter).addLocal(left).subtractLocal(up);

    Vector3.releaseTempInstance(vNearPlaneCenter);
    Vector3.releaseTempInstance(vFarPlaneCenter);
    Vector3.releaseTempInstance(direction);
    Vector3.releaseTempInstance(left);
    Vector3.releaseTempInstance(up);
  }

  public Vector3[] getCorners() { return _corners; }
}
