/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.effect.particle;

import java.io.IOException;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>ParticleInfluence</code> is an abstract class defining an external influence to be used with the ParticleMesh
 * class.
 */
public abstract class ParticleInfluence implements Savable {

    /**
     * Is this influence enabled? ie, should it be used when updating particles.
     */
    private boolean _enabled = true;

    /**
     * Set this influence enabled or not.
     * 
     * @param enabled
     *            boolean
     */
    public void setEnabled(final boolean enabled) {
        _enabled = enabled;
    }

    /**
     * Return whether or not this influence is enabled.
     * 
     * @return boolean
     */
    public boolean isEnabled() {
        return _enabled;
    }

    /**
     * Gives the influence a chance to perform any necessary initialization immediately before {@link #apply} is called
     * on each particle for the current frame.
     * 
     * @param particleGeom
     *            the particle system containing the influence
     */
    public void prepare(final ParticleSystem particleGeom) {}

    /**
     * Apply the influence defined by this class on a given particle. Should probably do this by making a call to
     * <i>particle.getSpeed().addLocal(....);</i> etc.
     * 
     * @param dt
     *            amount of time since last apply call in ms.
     * @param particle
     *            the particle to apply the influence to.
     * @param index
     *            the index of the particle we are working with. This is useful for adding small steady amounts of
     *            variation, or remembering information.
     */
    public abstract void apply(double dt, Particle particle, int index);

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_enabled, "enabled", true);
    }

    public void read(final InputCapsule capsule) throws IOException {
        _enabled = capsule.readBoolean("enabled", true);
    }

    public Class<? extends ParticleInfluence> getClassTag() {
        return this.getClass();
    }
}
