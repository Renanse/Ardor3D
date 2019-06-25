/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.effect.step;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.effect.EffectManager;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Spatial;

public class EffectStep_RenderSpatials implements EffectStep {
    private final EnumMap<StateType, RenderState> _states = new EnumMap<>(StateType.class);
    private RenderMaterial _enforcedMaterial;
    private final List<Spatial> _spatials = new ArrayList<>();
    private final Camera _trackedCamera;

    public EffectStep_RenderSpatials(final Camera trackedCamera) {
        _trackedCamera = trackedCamera;
    }

    @Override
    public void apply(final EffectManager manager) {
        manager.getCurrentRenderTarget().render(manager,
                _trackedCamera != null ? _trackedCamera : manager.getSceneCamera(), _spatials, _enforcedMaterial,
                _states);
    }

    public void addSpatial(final Spatial spat) {
        _spatials.add(spat);
    }

    public List<Spatial> getSpatials() {
        return _spatials;
    }

    public void setEnforcedState(final RenderState state) {
        _states.put(state.getType(), state);
    }

    public void clearEnforcedState(final StateType type) {
        _states.remove(type);
    }

    public EnumMap<StateType, RenderState> getEnforcedStates() {
        return _states;
    }

    public void setEnforcedMaterial(final RenderMaterial enforcedMaterial) {
        _enforcedMaterial = enforcedMaterial;
    }

    public RenderMaterial getEnforcedMaterial() {
        return _enforcedMaterial;
    }
}
