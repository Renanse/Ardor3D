/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.effect.rendertarget;

import java.util.EnumMap;
import java.util.List;

import com.ardor3d.image.Texture;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.effect.EffectManager;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Spatial;

public class RenderTarget_Framebuffer implements RenderTarget {

  @Override
  public void render(final EffectManager effectManager, final Camera camera, final List<Spatial> spatials,
      final RenderMaterial enforcedMaterial, final EnumMap<StateType, RenderState> enforcedStates) {
    render(effectManager.getCurrentRenderer(), camera, spatials, null, enforcedMaterial, enforcedStates);
  }

  @Override
  public void render(final EffectManager effectManager, final Camera camera, final Spatial spatial,
      final RenderMaterial enforcedMaterial, final EnumMap<StateType, RenderState> enforcedStates) {
    render(effectManager.getCurrentRenderer(), camera, null, spatial, enforcedMaterial, enforcedStates);
  }

  public void render(final Renderer renderer, final Camera camera, final List<Spatial> spatials, final Spatial spatial,
      final RenderMaterial enforcedMaterial, final EnumMap<StateType, RenderState> enforcedStates) {
    camera.apply(renderer);

    final RenderContext context = ContextManager.getCurrentContext();

    context.enforceMaterial(enforcedMaterial);
    context.enforceStates(enforcedStates);

    if (spatial != null) {
      spatial.onDraw(renderer);
    } else {
      for (final Spatial spat : spatials) {
        spat.onDraw(renderer);
      }
    }

    renderer.renderBuckets();
    context.clearEnforcedStates();
    context.enforceMaterial(null);
  }

  @Override
  public Texture getTexture() {
    throw new UnsupportedOperationException();
  }
}
