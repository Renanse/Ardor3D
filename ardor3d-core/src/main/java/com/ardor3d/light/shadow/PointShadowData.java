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

import com.ardor3d.image.Texture.DepthTextureCompareFunc;
import com.ardor3d.image.Texture.DepthTextureCompareMode;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.image.TextureStoreFormat;
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

  public static final String KEY_DebugFace = "_debugFace";

  // Tracked light
  protected final PointLight _light;

  protected transient Matrix4 _matrix = new Matrix4();

  public PointShadowData(final PointLight pointLight) {
    _texture = new TextureCubeMap();
    _texture.setTextureStoreFormat(TextureStoreFormat.Depth32);
    _texture.setDepthCompareMode(DepthTextureCompareMode.RtoTexture);
    _texture.setDepthCompareFunc(DepthTextureCompareFunc.LessThanEqual);

    _light = pointLight;
    _bias = 0.001f;
  }

  @Override
  protected ProjectionMode getProjectionMode() { return ProjectionMode.Perspective; }

  @Override
  public void updateShadows(final Renderer renderer, final SceneIndexer indexer) {
    final Camera viewCam = Camera.getCurrentCamera();
    final var shadowRenderer = getShadowRenderer(_light, 0, renderer);

    final Camera shadowCam = shadowRenderer.getCamera();
    shadowCam.setFrustumPerspective(90.0, 1.0, viewCam.getFrustumNear(),
        Math.min(viewCam.getFrustumFar(), _light.getRange()));
    _matrix.set(shadowCam.getProjectionMatrix());

    for (final var face : TextureCubeMap.Face.values()) {
      // set up our texture camera to look out of the cube in the correct direction
      updateCameraForFace(shadowRenderer.getCamera(), face, _light.getWorldTranslation());

      // render to the corresponding layer
      ((TextureCubeMap) _texture).setCurrentRTTFace(face);
      shadowRenderer.render(indexer, _texture, Renderer.BUFFER_DEPTH);
    }
  }

  private void updateCameraForFace(final Camera cam, final Face face, final ReadOnlyVector3 location) {
    cam.setLocation(location);
    CubeMapRenderUtil.pointAtFace(face, cam);
  }

  public ReadOnlyMatrix4 getShadowMatrix() { return _matrix; }
}
