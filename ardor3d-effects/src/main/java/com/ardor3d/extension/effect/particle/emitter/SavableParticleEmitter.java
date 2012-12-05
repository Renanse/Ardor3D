/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.particle.emitter;

import com.ardor3d.util.export.Savable;

public abstract class SavableParticleEmitter implements Savable, ParticleEmitter {

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends SavableParticleEmitter> getClassTag() {
        return this.getClass();
    }

}
