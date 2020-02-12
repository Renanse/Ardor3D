/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.effect.particle.emitter;

import java.io.IOException;

import com.ardor3d.math.Vector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class PointEmitter extends SavableParticleEmitter {

    public PointEmitter() {}

    public Vector3 randomEmissionPoint(final Vector3 store) {
        Vector3 rVal = store;
        if (rVal == null) {
            rVal = new Vector3();
        }

        rVal.set(0, 0, 0);
        return rVal;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public void read(final InputCapsule capsule) throws IOException {}

    public void write(final OutputCapsule capsule) throws IOException {}
}
