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
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.SceneIndexer;

public abstract class AbstractShadowData {

  public final static int FILTER_MODE_PCF = 0;
  public final static int FILTER_MODE_PCF_3x3 = 1;

  public static final int DEFAULT_SHADOW_SIZE = 1024;
  public static final int DEFAULT_SHADOW_DEPTH_BITS = 16;

  protected transient TextureRenderer _shadowRenderer;

  protected transient Texture _texture;
  protected int _textureSize = AbstractShadowData.DEFAULT_SHADOW_SIZE;
  protected int _textureDepthBits = AbstractShadowData.DEFAULT_SHADOW_DEPTH_BITS;

  protected float _bias;
  protected int _filterMode = 0;

  public AbstractShadowData() {
    _texture = new Texture2DArray();
    _texture.setTextureStoreFormat(TextureStoreFormat.Depth);
    _texture.setDepthCompareMode(DepthTextureCompareMode.RtoTexture);
    _texture.setDepthCompareFunc(DepthTextureCompareFunc.LessThanEqual);
  }

  public void cleanUp() {
    // TODO: properly delete from card
    // _texture.markForDelete();
    _texture = null;
    if (_shadowRenderer != null) {
      _shadowRenderer.cleanup();
      _shadowRenderer = null;
    }
  }

  public Texture getTexture() { return _texture; }

  public float getBias() { return _bias; }

  public void setBias(final float bias) { _bias = bias; }

  public int getFilterMode() { return _filterMode; }

  public void setFilterMode(final int filterMode) { _filterMode = filterMode; }

  public int getTextureSize() { return _textureSize; }

  public void setTextureSize(final int pixels) {
    if (pixels != _textureSize) {
      cleanUp();
    }
    _textureSize = pixels;
  }

  public int getTextureDepthBits() { return _textureDepthBits; }

  public void setTextureDepthBits(final int bits) { _textureDepthBits = bits; }

  public abstract void updateShadows(Renderer renderer, SceneIndexer indexer);

  protected TextureRenderer getShadowRenderer(final Light light, final int layers, final Renderer renderer) {
    if (_shadowRenderer == null || (layers != 0 && _shadowRenderer.getLayers() != layers)) {
      final int pixelSize = getTextureSize();
      final int depthBits = getTextureDepthBits();
      _shadowRenderer = renderer.createTextureRenderer(pixelSize, pixelSize, layers, depthBits, 0);
      _shadowRenderer.getCamera().setProjectionMode(getProjectionMode());
      _shadowRenderer.setBackgroundColor(ColorRGBA.BLACK_NO_ALPHA);

      if (_texture.getTextureKey() == null) {
        _shadowRenderer.setupTexture(_texture);
      }
    }
    return _shadowRenderer;
  }

  protected abstract ProjectionMode getProjectionMode();
}
