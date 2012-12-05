/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
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
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Spatial;

public interface RenderTarget {

    void render(final EffectManager effectManager, final Camera camera, final Spatial spatial,
            EnumMap<StateType, RenderState> enforcedStates);

    void render(final EffectManager effectManager, final Camera camera, final List<Spatial> spatials,
            EnumMap<StateType, RenderState> enforcedStates);

    Texture getTexture();

}
