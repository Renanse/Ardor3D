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

import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.DepthTextureCompareFunc;
import com.ardor3d.image.Texture.DepthTextureCompareMode;
import com.ardor3d.image.Texture2DArray;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.light.Light;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.SceneIndexer;

public abstract class AbstractShadowData {

  protected transient TextureRenderer _shadowRenderer;
  protected transient Texture _texture;
  protected transient Matrix4 _matrix = new Matrix4();

  public AbstractShadowData() {
    _texture = new Texture2DArray();
    _texture.setTextureStoreFormat(TextureStoreFormat.Depth);
    _texture.setDepthCompareMode(DepthTextureCompareMode.RtoTexture);
    _texture.setDepthCompareFunc(DepthTextureCompareFunc.LessThanEqual);
  }

  public void cleanUp() {
    // TODO: properly delete from card
    // _shadowTexture.markForDelete();
    _texture = null;
    if (_shadowRenderer != null) {
      _shadowRenderer.cleanup();
      _shadowRenderer = null;
    }
  }

  public Texture getTexture() { return _texture; }

  public abstract void updateShadows(Renderer renderer, SceneIndexer indexer);

  protected TextureRenderer getShadowRenderer(final Light light, final int layers, final Renderer renderer) {
    if (_shadowRenderer == null || (layers != 0 && _shadowRenderer.getLayers() != layers)) {
      final int pixelSize = light.getShadowSize();
      final int depthBits = light.getShadowDepthBits();
      _shadowRenderer = renderer.createTextureRenderer(pixelSize, pixelSize, layers, depthBits, 0);
      _shadowRenderer.getCamera().setProjectionMode(ProjectionMode.Perspective);
      _shadowRenderer.setBackgroundColor(ColorRGBA.BLACK_NO_ALPHA);

      if (_texture.getTextureKey() == null) {
        _shadowRenderer.setupTexture(_texture);
      }
    }
    return _shadowRenderer;
  }

  public ReadOnlyMatrix4 getShadowMatrix() { // TODO Auto-generated method stub
    return _matrix;
  }
}
