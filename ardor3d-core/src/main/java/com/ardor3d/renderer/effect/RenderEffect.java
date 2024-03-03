/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.effect;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.renderer.effect.step.EffectStep;

/**
 * A RenderEffect object represents a complete set of instructions necessary for applying a specific
 * effect to our render output. Each effect is comprised of a set of 1 or more steps (EffectStep).
 */
public abstract class RenderEffect {

  /** A list of logical steps that comprise our effect. */
  protected final List<EffectStep> _steps = new ArrayList<>();

  /** Is this render effect active? */
  protected boolean _enabled = true;

  /**
   * Do any setup necessary for our effect prior. This should be called only once, or on changes to
   * the effect chain.
   *
   * @param manager
   */
  public void prepare(final EffectManager manager) {}

  /**
   * Render this effect.
   *
   * @param manager
   */
  public void render(final EffectManager manager) {
    for (final EffectStep step : _steps) {
      step.apply(manager);
    }
  }

  public boolean isEnabled() { return _enabled; }

  public void setEnabled(final boolean enabled) { _enabled = enabled; }
}
