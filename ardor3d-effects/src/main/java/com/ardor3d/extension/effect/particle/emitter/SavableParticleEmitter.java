/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.effect.particle.emitter;

import com.ardor3d.util.export.Savable;

public abstract class SavableParticleEmitter implements Savable, ParticleEmitter {

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends SavableParticleEmitter> getClassTag() { return this.getClass(); }

}
