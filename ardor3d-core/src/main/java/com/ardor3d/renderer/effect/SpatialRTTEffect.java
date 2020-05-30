/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.effect;

import java.util.Arrays;

import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.effect.step.EffectStep_RenderSpatials;
import com.ardor3d.renderer.effect.step.EffectStep_SetRenderTarget;
import com.ardor3d.scenegraph.Spatial;

public class SpatialRTTEffect extends RenderEffect {

  private final EffectStep_SetRenderTarget _targetStep;
  private final EffectStep_RenderSpatials _drawStep;

  public SpatialRTTEffect(final String targetName, final Camera trackedCamera, final Spatial... spatials) {
    _targetStep = new EffectStep_SetRenderTarget(targetName);
    _drawStep = new EffectStep_RenderSpatials(trackedCamera);
    _drawStep.getSpatials().addAll(Arrays.asList(spatials));
  }

  @Override
  public void prepare(final EffectManager manager) {
    _steps.clear();
    _steps.add(_targetStep);
    _steps.add(_drawStep);

    super.prepare(manager);
  }

}
