/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
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
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.SceneIndexer;

public class DirectionalShadowData extends AbstractShadowData {

  public static final String KEY_DebugSplit = "_debugSplit";

  public static int MAX_SPLITS = 6;

  /**
   * The lambda value used in split distance calculations. Should be in the range [0.0, 1.0] and
   * handles blending between logarithmic and uniform split calculations.
   */
  protected double _lambda = 0.5;

  // Tracked light
  protected final DirectionalLight _light;

  // Represents the view frustum, for use in calculations
  protected final Frustum _frustum = new Frustum();

  // Our split distances and matrices
  protected double[] _splitDistances = new double[0];
  protected transient Matrix4[] _matrices = new Matrix4[0];

  protected final BoundingSphere _frustumBoundingSphere = new BoundingSphere();

  protected double _maxDistance = 100;

  protected int _cascades = 4;

  private double _minimumCameraDistance = 1.0;

  public DirectionalShadowData(final DirectionalLight light) {
    super();

    _light = light;
    _bias = 0.01f;
  }

  /**
   * Gets the lambda.
   *
   * @return the lambda
   */
  public double getLambda() { return _lambda; }

  /**
   * Sets the lambda.
   *
   * @param lambda
   *          the new lambda
   */
  public void setLambda(final double lambda) { _lambda = lambda; }

  public double getMaxDistance() { return _maxDistance; }

  public void setMaxDistance(final double maxDistance) { _maxDistance = maxDistance; }

  public int getCascades() { return _cascades; }

  /**
   * @param cascades
   *          number of layers to use for our CSM. Must be >= 1. Normal range is [1, 4]
   */
  public void setCascades(final int cascades) { _cascades = cascades; }

  public double getMinimumCameraDistance() { return _minimumCameraDistance; }

  public void setMinimumCameraDistance(final double distance) { _minimumCameraDistance = distance; }

  @Override
  protected ProjectionMode getProjectionMode() { return ProjectionMode.Orthographic; }

  @Override
  public void updateShadows(final Renderer renderer, final SceneIndexer indexer) {
    final Camera viewCam = Camera.getCurrentCamera();
    updateFrustum(viewCam, indexer);

    // Render our splits
    final var shadowRenderer = getShadowRenderer(_light, getCascades(), renderer);
    for (int i = getCascades(); i-- > 0;) {
      // set up current split
      applySplitToLightCamera(i, viewCam, shadowRenderer.getCamera());
      _texture.setTexRenderLayer(i);

      // render roots to current split
      shadowRenderer.render(indexer, _texture, Renderer.BUFFER_DEPTH);
    }
  }

  private void updateFrustum(final Camera viewCam, final SceneIndexer indexer) {
    _frustum.copyFrustumValues(viewCam);

    // Figure out good near / far plans for our frustum
    final BoundingVolume worldBounds = indexer.getRootBounds();
    if (worldBounds != null && worldBounds.isValid()) {
      _frustum.pack(worldBounds);
    } else {
      _frustum.setFar( //
          Math.max(viewCam.getFrustumNear() + 1.0, //
              Math.min(getMaxDistance(), viewCam.getFrustumFar()))//
      );
    }

    // Now calculate splits
    calculateSplitDistances(getCascades());
  }

  // XXX: Skip in cases where we know near/far/lambda have not changed
  private void calculateSplitDistances(final int splitCount) {
    // ensure correct size.
    if (_splitDistances.length != splitCount + 1) {
      _splitDistances = new double[splitCount + 1];
      _matrices = new Matrix4[splitCount];
      for (int i = 0; i < _matrices.length; i++) {
        _matrices[i] = new Matrix4();
      }
    }

    final double nearPlane = _frustum.getNear();
    final double farPlane = _frustum.getFar();
    final double ratio = farPlane / nearPlane;
    final double range = farPlane - nearPlane;

    // setup intermediate splits
    for (int i = 1; i < splitCount; i++) {
      final double part = i / (double) splitCount;
      final double logSplit = nearPlane * Math.pow(ratio, part);
      final double uniformSplit = nearPlane + range * part;
      _splitDistances[i] = MathUtils.lerp(_lambda, uniformSplit, logSplit);
    }

    // setup first and last split (near/far planes)
    _splitDistances[0] = nearPlane;
    _splitDistances[splitCount] = farPlane;
  }

  private void applySplitToLightCamera(final int split, final Camera viewCamera, final Camera lightCamera) {
    final Vector3 tmpVec = Vector3.fetchTempInstance();
    final Vector3 lightSpace = Vector3.fetchTempInstance();
    final Quaternion axesQuat = Quaternion.fetchTempInstance();
    final Quaternion axesQuatInvert = Quaternion.fetchTempInstance();
    try {
      final double fNear = _splitDistances[split];
      final double fFar = _splitDistances[split + 1];

      // calculate the frustum corners for the current split
      _frustum.calculateCorners(fNear, fFar, viewCamera);

      // calculate a bounding sphere to encompass the calculated split corners
      _frustum.toBoundingSphere(_frustumBoundingSphere);

      final ReadOnlyVector3 center = _frustumBoundingSphere.getCenter();
      final double radius = _frustumBoundingSphere.getRadius();

      Vector3 direction = new Vector3();
      direction = direction.set(_light.getWorldDirection());
      final double distance = Math.max(radius, _minimumCameraDistance);

      tmpVec.set(direction);
      tmpVec.negateLocal();
      tmpVec.multiplyLocal(distance);
      tmpVec.addLocal(center);

      // temporary location
      // FIXME: has a singularity if light comes directly from Y.
      lightCamera.setLocation(tmpVec);
      lightCamera.lookAt(center, Vector3.UNIT_Y);

      {
        // determine
        final int texSize = getTextureSize();
        final double texelSize = (2.0 * radius) / texSize;

        // build a Quaternion from camera axes to move
        axesQuat.fromAxes(lightCamera.getLeft(), lightCamera.getUp(), lightCamera.getDirection());

        // invert to calculate in light space
        axesQuat.invert(axesQuatInvert).apply(tmpVec, lightSpace);

        // snap to nearest texel
        lightSpace.setX(lightSpace.getX() - (lightSpace.getX() % texelSize));
        lightSpace.setY(lightSpace.getY() - (lightSpace.getY() % texelSize));

        // convert back
        axesQuat.apply(lightSpace, tmpVec);
      }

      // updated location
      final double x = tmpVec.getX();
      final double y = tmpVec.getY();
      final double z = tmpVec.getZ();
      final double farZ = tmpVec.subtractLocal(center).length() + radius;

      // set frustum, then location
      lightCamera.setProjectionMode(ProjectionMode.Orthographic);
      lightCamera.setFrustum(1, farZ, -radius, radius, radius, -radius);
      lightCamera.setLocation(x, y, z);
      _matrices[split].set(lightCamera.getViewProjectionMatrix());
    } finally {
      Vector3.releaseTempInstance(tmpVec);
      Vector3.releaseTempInstance(lightSpace);
      Quaternion.releaseTempInstance(axesQuat);
      Quaternion.releaseTempInstance(axesQuatInvert);
    }
  }

  public ReadOnlyMatrix4 getShadowMatrix(final int split) {
    return split >= 0 && split < _matrices.length ? _matrices[split] : Matrix4.IDENTITY;
  }

  public double getSplit(final int split) {
    return split >= 0 && split < _splitDistances.length ? _splitDistances[split] : Double.MAX_VALUE;
  }
}
