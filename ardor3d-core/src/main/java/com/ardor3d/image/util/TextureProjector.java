/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.util;

import com.ardor3d.math.Matrix4;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Spatial;

public class TextureProjector extends Camera {

  /** Takes clip space [-1, 1] to texture space [0, 1]. Laid out for column vectors, as our camera matrices are. */
  private final static ReadOnlyMatrix4 BIAS = new Matrix4( //
      0.5, 0.0, 0.0, 0.5, //
      0.0, 0.5, 0.0, 0.5, //
      0.0, 0.0, 0.5, 0.5, //
      0.0, 0.0, 0.0, 1.0);

  public TextureProjector() {
    super(1, 1);
  }

  public void updateTextureMatrix(final Spatial store, final String key) {
    Matrix4 texMat = store.getProperty(key, null);
    if (texMat == null) {
      texMat = new Matrix4();
      store.setProperty(key, texMat);
    }
    updateTextureMatrix(texMat);
  }

  /**
   * Computes the matrix taking a world position to this projector's texture space: bias * projection *
   * view, applied as M * v like the camera matrices it is built from. Divide s, t and r (depth) by q.
   *
   * @param matrixStore
   *          the matrix to store the result in.
   */
  public void updateTextureMatrix(final Matrix4 matrixStore) {
    update();
    final ReadOnlyMatrix4 projectorView = getViewMatrix();
    final ReadOnlyMatrix4 projectorProjection = getProjectionMatrix();
    matrixStore.set(BIAS).multiplyLocal(projectorProjection).multiplyLocal(projectorView);
  }
}
