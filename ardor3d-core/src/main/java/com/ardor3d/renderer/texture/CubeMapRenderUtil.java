/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.texture;

import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderable;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Spatial;

public class CubeMapRenderUtil {
  protected TextureRenderer _textureRenderer = null;
  protected final Renderer _renderer;
  protected final int _samples;

  public CubeMapRenderUtil(final Renderer renderer) {
    this(renderer, 0);
  }

  public CubeMapRenderUtil(final Renderer renderer, final int samples) {
    _renderer = renderer;
    _samples = samples;
  }

  public TextureRenderer getTextureRenderer() { return _textureRenderer; }

  public void updateSettings(final int width, final int height, final int depthBits, final double near,
      final double far) {
    if (_textureRenderer == null) {
      _textureRenderer = _renderer.createTextureRenderer(width, height, depthBits, _samples);
      _textureRenderer.getCamera().setFrustumPerspective(90.0, 1.0, near, far);
    } else {
      _textureRenderer.resize(width, height, depthBits);

      final Camera cam = _textureRenderer.getCamera();
      if (near != cam.getDepthRangeNear() || far != cam.getDepthRangeFar()) {
        _textureRenderer.getCamera().setFrustumPerspective(90.0, 1.0, near, far);
      }
    }
  }

  public void renderToCubeMap(final Renderable renderable, final TextureCubeMap cubemap,
      final ReadOnlyVector3 cameraPosition, final int clear) {
    if (cubemap.getTextureKey() == null) {
      _textureRenderer.setupTexture(cubemap);
    }

    final Camera cam = _textureRenderer.getCamera();
    cam.setLocation(cameraPosition);

    for (final Face face : Face.values()) {
      pointAtFace(face, cam);
      cubemap.setCurrentRTTFace(face);
      _textureRenderer.render(renderable, cubemap, clear);
    }
  }

  public void renderToCubeMap(final Spatial spatial, final TextureCubeMap cubemap, final ReadOnlyVector3 cameraPosition,
      final int clear) {
    if (cubemap.getTextureKey() == null) {
      _textureRenderer.setupTexture(cubemap);
    }

    final Camera cam = _textureRenderer.getCamera();
    cam.setLocation(cameraPosition);

    for (final Face face : Face.values()) {
      pointAtFace(face, cam);
      cubemap.setCurrentRTTFace(face);
      _textureRenderer.renderSpatial(spatial, cubemap, clear);
    }
  }

  public void cleanup() {
    if (_textureRenderer != null) {
      _textureRenderer.cleanup();
      _textureRenderer = null;
    }
  }

  private void pointAtFace(final Face face, final Camera cam) {
    switch (face) {
      case NegativeX:
        cam.setAxes(Vector3.NEG_UNIT_Z, Vector3.NEG_UNIT_Y, Vector3.NEG_UNIT_X);
        break;

      case PositiveX:
        cam.setAxes(Vector3.UNIT_Z, Vector3.NEG_UNIT_Y, Vector3.UNIT_X);
        break;

      case NegativeY:
        cam.setAxes(Vector3.NEG_UNIT_X, Vector3.NEG_UNIT_Z, Vector3.NEG_UNIT_Y);
        break;

      case PositiveY:
        cam.setAxes(Vector3.NEG_UNIT_X, Vector3.UNIT_Z, Vector3.UNIT_Y);
        break;

      case NegativeZ:
        cam.setAxes(Vector3.UNIT_X, Vector3.NEG_UNIT_Y, Vector3.NEG_UNIT_Z);
        break;

      case PositiveZ:
        cam.setAxes(Vector3.NEG_UNIT_X, Vector3.NEG_UNIT_Y, Vector3.UNIT_Z);
        break;
    }
  }
}
