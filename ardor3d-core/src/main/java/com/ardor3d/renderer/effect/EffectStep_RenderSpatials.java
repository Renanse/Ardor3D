/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.effect;

import java.util.EnumMap;
import java.util.List;

import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Spatial;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class EffectStep_RenderSpatials implements EffectStep {
    private final EnumMap<StateType, RenderState> _states = Maps.newEnumMap(StateType.class);
    private final List<Spatial> _spatials = Lists.newArrayList();
    private final Camera _trackedCamera;

    public EffectStep_RenderSpatials(final Camera trackedCamera) {
        _trackedCamera = trackedCamera;
    }

    @Override
    public void apply(final EffectManager manager) {
        manager.getCurrentRenderTarget().render(manager,
                _trackedCamera != null ? _trackedCamera : manager.getSceneCamera(), _spatials, _states);
    }

    public List<Spatial> getSpatials() {
        return _spatials;
    }

    public EnumMap<StateType, RenderState> getEnforcedStates() {
        return _states;
    }
}
