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

import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.texture.CubeMapRenderUtil;
import com.ardor3d.scenegraph.SceneIndexer;

public class PointShadowData extends AbstractShadowData {

  // Tracked light
  protected final PointLight _light;

  protected transient Matrix4[] _matrices = new Matrix4[TextureCubeMap.Face.values().length];

  public PointShadowData(final PointLight pointLight) {
    super();
    _light = pointLight;
    _bias = 0.001f;
    for (final var face : TextureCubeMap.Face.values()) {
      _matrices[face.ordinal()] = new Matrix4();
    }
  }

  @Override
  protected ProjectionMode getProjectionMode() { return ProjectionMode.Perspective; }

  @Override
  public void updateShadows(final Renderer renderer, final SceneIndexer indexer) {
    final Camera viewCam = Camera.getCurrentCamera();
    final var shadowRenderer = getShadowRenderer(_light, 6, renderer);
    final Camera shadowCam = shadowRenderer.getCamera();
    // TODO: Fix near/far using light attenuation properties.
    shadowCam.setFrustumPerspective(90.0, 1.0, viewCam.getFrustumNear(), viewCam.getFrustumFar());

    for (final var face : TextureCubeMap.Face.values()) {
      // set up our texture camera to look out of the cube in the correct direction
      updateCameraForFace(shadowRenderer.getCamera(), face, _light.getWorldTranslation());
      // TODO: probably could be cached?
      _matrices[face.ordinal()].set(shadowCam.getViewProjectionMatrix());

      // render to the corresponding layer
      _texture.setTexRenderLayer(face.ordinal());
      shadowRenderer.render(indexer, _texture, Renderer.BUFFER_DEPTH);
    }
  }

  private void updateCameraForFace(final Camera cam, final Face face, final ReadOnlyVector3 location) {
    cam.setLocation(location);
    CubeMapRenderUtil.pointAtFace(face, cam);
  }

  public ReadOnlyMatrix4 getShadowMatrix(final Face face) {
    return _matrices[face.ordinal()];
  }
}
