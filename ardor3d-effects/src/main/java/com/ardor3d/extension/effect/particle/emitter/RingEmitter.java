/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.effect.particle.emitter;

import java.io.IOException;

import com.ardor3d.math.Ring;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class RingEmitter extends SavableParticleEmitter {

  private Ring _source;

  public RingEmitter() {
    _source = new Ring();
  }

  /**
   * @param source
   *          the ring to use as our source
   */
  public RingEmitter(final Ring source) {
    _source = source;
  }

  public void setSource(final Ring source) { _source = source; }

  public Ring getSource() { return _source; }

  @Override
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

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _source = capsule.readSavable("source", null);
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_source, "source", null);
  }
}
