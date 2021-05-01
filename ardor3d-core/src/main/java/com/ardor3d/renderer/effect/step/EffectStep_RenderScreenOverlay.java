/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.effect.step;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.ardor3d.renderer.effect.EffectManager;
import com.ardor3d.renderer.effect.rendertarget.RenderTarget;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;

public class EffectStep_RenderScreenOverlay implements EffectStep {

  private final EnumMap<StateType, RenderState> _states = new EnumMap<>(StateType.class);
  private RenderMaterial _enforcedMaterial;
  private final TextureState _texState = new TextureState();
  private final Map<String, Integer> _targetMap = new HashMap<>();

  public EffectStep_RenderScreenOverlay() {
    _states.put(StateType.Texture, _texState);
  }

  @Override
  public void apply(final EffectManager manager) {
    // prepare our texture state
    for (final String key : _targetMap.keySet()) {
      final RenderTarget target = manager.getRenderTarget(key);
      final Integer unit = _targetMap.get(key);
      _texState.setTexture(target.getTexture(), unit.intValue());
    }

    // render a quad to the screen using our states.
    manager.renderFullScreenQuad(_enforcedMaterial, _states);
  }

  public TextureState getTextureState() { return _texState; }

  public EnumMap<StateType, RenderState> getEnforcedStates() { return _states; }

  public void setEnforcedState(final RenderState state) {
    _states.put(state.getType(), state);
  }

  public void clearEnforcedState(final StateType type) {
    _states.remove(type);
  }

  public void setEnforcedMaterial(final RenderMaterial enforcedMaterial) { _enforcedMaterial = enforcedMaterial; }

  public RenderMaterial getEnforcedMaterial() { return _enforcedMaterial; }

  public Map<String, Integer> getTargetMap() { return _targetMap; }
}
