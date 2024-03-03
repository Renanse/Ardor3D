/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
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
import com.ardor3d.renderer.effect.EffectManager;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Spatial;

public interface RenderTarget {

  void render(final EffectManager effectManager, final Camera camera, final Spatial spatial,
      RenderMaterial enforcedMaterial, EnumMap<StateType, RenderState> enforcedStates);

  void render(final EffectManager effectManager, final Camera camera, final List<Spatial> spatials,
      RenderMaterial enforcedMaterial, EnumMap<StateType, RenderState> enforcedStates);

  Texture getTexture();

}
