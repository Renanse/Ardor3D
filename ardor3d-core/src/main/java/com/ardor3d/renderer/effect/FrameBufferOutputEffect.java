/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.effect;

import com.ardor3d.renderer.effect.step.EffectStep_RenderScreenOverlay;
import com.ardor3d.renderer.effect.step.EffectStep_SetRenderTarget;
import com.ardor3d.renderer.material.MaterialManager;
import com.ardor3d.renderer.state.BlendState;

public class FrameBufferOutputEffect extends RenderEffect {

    private BlendState _blend = null;

    @Override
    public void prepare(final EffectManager manager) {
        _steps.clear();
        _steps.add(new EffectStep_SetRenderTarget(EffectManager.RT_FRAMEBUFFER));

        final EffectStep_RenderScreenOverlay drawStep = new EffectStep_RenderScreenOverlay();
        drawStep.getTargetMap().put(EffectManager.RT_PREVIOUS, 0);
        if (_blend != null) {
            drawStep.setEnforcedState(_blend);
        }
        drawStep.setEnforcedMaterial(MaterialManager.INSTANCE.findMaterial("unlit/textured/fsq.yaml"));
        _steps.add(drawStep);

        super.prepare(manager);
    }

    public BlendState getBlend() {
        return _blend;
    }

    public void setBlend(final BlendState blend) {
        _blend = blend;
    }
}
