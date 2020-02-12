/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.effect.step;

import com.ardor3d.renderer.effect.EffectManager;

public class EffectStep_SetRenderTarget implements EffectStep {

    private final String _target;

    public EffectStep_SetRenderTarget(final String target) {
        _target = target;
    }

    @Override
    public void apply(final EffectManager manager) {
        manager.setCurrentRenderTarget(_target);
    }
}
