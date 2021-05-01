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

import com.ardor3d.renderer.effect.EffectManager;

/**
 * A specific instruction in a RenderEffect.
 */
public interface EffectStep {

  /**
   * Apply this step.
   *
   * @param manager
   */
  void apply(final EffectManager manager);

}
