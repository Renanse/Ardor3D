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

import com.ardor3d.image.Texture;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Spatial;

public class RenderTarget_Framebuffer implements RenderTarget {

    @Override
    public void render(final EffectManager effectManager, final Camera camera, final List<Spatial> spatials,
            final EnumMap<StateType, RenderState> enforcedStates) {
        render(effectManager.getCurrentRenderer(), camera, spatials, null, enforcedStates);
    }

    @Override
    public void render(final EffectManager effectManager, final Camera camera, final Spatial spatial,
            final EnumMap<StateType, RenderState> enforcedStates) {
        render(effectManager.getCurrentRenderer(), camera, null, spatial, enforcedStates);
    }

    public void render(final Renderer renderer, final Camera camera, final List<Spatial> spatials,
            final Spatial spatial, final EnumMap<StateType, RenderState> enforcedStates) {
        camera.apply(renderer);

        final RenderContext context = ContextManager.getCurrentContext();

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
    }

    @Override
    public Texture getTexture() {
        throw new UnsupportedOperationException();
    }
}
