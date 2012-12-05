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

import java.io.IOException;

import com.ardor3d.math.Rectangle3;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class RectangleEmitter extends SavableParticleEmitter {

    private Rectangle3 _source;

    public RectangleEmitter() {
        _source = new Rectangle3();
    }

    /**
     * @param source
     *            the rectangle to use as our source
     */
    public RectangleEmitter(final Rectangle3 source) {
        _source = source;
    }

    public void setSource(final Rectangle3 source) {
        _source = source;
    }

    public Rectangle3 getSource() {
        return _source;
    }

    public Vector3 randomEmissionPoint(final Vector3 store) {
        Vector3 rVal = store;
        if (rVal == null) {
            rVal = new Vector3();
        }

        getSource().random(rVal);
        return rVal;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public void read(final InputCapsule capsule) throws IOException {
        _source = (Rectangle3) capsule.readSavable("source", null);
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_source, "source", null);
    }
}
