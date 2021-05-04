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

import com.ardor3d.light.SpotLight;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.SceneIndexer;

public class SpotShadowData extends AbstractShadowData {

  // Tracked light
  protected final SpotLight _light;

  protected transient Matrix4 _matrix = new Matrix4();

  public SpotShadowData(final SpotLight light) {
    _light = light;
  }

  @Override
  protected ProjectionMode getProjectionMode() { return ProjectionMode.Perspective; }

  @Override
  public void updateShadows(final Renderer renderer, final SceneIndexer indexer) {
    final var shadowRenderer = getShadowRenderer(_light, 1, renderer);
    // update our camera
    updateShadowCamera(shadowRenderer);
    shadowRenderer.render(indexer, _texture, Renderer.BUFFER_DEPTH);
  }

  private void updateShadowCamera(final TextureRenderer shadowRenderer) {
    final var shadowCam = shadowRenderer.getCamera();
    shadowCam.setLocation(_light.getWorldTranslation());
    shadowCam.setAxes(_light.getWorldRotation());
    final double near = 1.0;
    final double far = _light.getRange();
    final double h = Math.tan(_light.getAngle()) * near;
    final double w = h * 1.0;
    final double left = -w;
    final double right = w;
    final double bottom = -h;
    final double top = h;
    shadowCam.setFrustum(near, far, left, right, top, bottom);
    shadowCam.update();
    _matrix.set(shadowCam.getViewProjectionMatrix());
  }

  public ReadOnlyMatrix4 getShadowMatrix() { return _matrix; }
}
