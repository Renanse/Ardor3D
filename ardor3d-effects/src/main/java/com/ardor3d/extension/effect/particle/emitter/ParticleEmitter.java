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

import com.ardor3d.math.Vector3;

public interface ParticleEmitter {

    /**
     * Get the next point from this emitter.
     * 
     * @param store
     *            the vector to store our point in. If null, a new one is created.
     * @return the vector we stored in
     */
    public Vector3 randomEmissionPoint(final Vector3 store);

}
