/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.obj;

import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.material.uniform.AlphaTestConsts;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.surface.ColorSurface;

public class ObjMaterial {
  private final String name;

  double[] Ka = null;
  double[] Kd = null;
  double[] Ks = null;
  double Ns = -1;

  String textureName;
  Texture map_Kd = null;

  int illumType = 2;

  boolean forceBlend = false;
  double d = -1;

  public ObjMaterial(final String name) {
    this.name = name;
  }

  public void applyBlendAndAlpha(final Spatial target) {
    if (forceBlend || d != -1 && d < 1.0f) {
      final BlendState blend = new BlendState();
      blend.setBlendEnabled(true);
      blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
      blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
      target.setRenderState(blend);

      // set alpha testing
      target.setProperty(AlphaTestConsts.KEY_AlphaTestType, AlphaTestConsts.TestFunction.GreaterThan);
      target.setProperty(AlphaTestConsts.KEY_AlphaReference, 0f);

      target.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
    }
  }

  public TextureState getTextureState() {
    if (map_Kd != null) {
      final TextureState tState = new TextureState();
      tState.setTexture(map_Kd, 0);
      return tState;
    }
    return null;
  }

  public void applyMaterialProperties(final Spatial spat) {
    if (Ka != null || Kd != null || Ks != null || d != -1 || Ns != -1) {
      final ColorSurface surface = new ColorSurface();
      spat.setProperty(ColorSurface.DefaultPropertyKey, surface);
      final float alpha = (float) (d != -1 ? MathUtils.clamp(d, 0.0, 1.0) : 1.0);
      if (Ka != null) {
        surface.setAmbient(new ColorRGBA((float) Ka[0], (float) Ka[1], (float) Ka[2], alpha));
      }
      if (Kd != null) {
        surface.setDiffuse(new ColorRGBA((float) Kd[0], (float) Kd[1], (float) Kd[2], alpha));
      }
      if (Ks != null) {
        surface.setSpecular(new ColorRGBA((float) Ks[0], (float) Ks[1], (float) Ks[2], alpha));
      }

      if (Ns != -1) {
        surface.setShininess((float) Ns);
      }
    }
  }

  public String getName() { return name; }

  public String getTextureName() { return textureName; }

  public Texture getMap_Kd() { return map_Kd; }
}
