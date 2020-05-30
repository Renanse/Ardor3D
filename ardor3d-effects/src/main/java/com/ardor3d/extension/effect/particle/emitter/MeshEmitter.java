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
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class MeshEmitter extends SavableParticleEmitter {

  private Mesh _source;
  private boolean _onlyVertices;

  public MeshEmitter() {}

  /**
   * @param source
   *          the mesh to use as our source
   * @param onlyVertices
   *          if true, only the vertices of the emitter should be used for spawning particles.
   *          Otherwise, the mesh's face surfaces should be used.
   */
  public MeshEmitter(final Mesh source, final boolean onlyVertices) {
    _source = source;
    _onlyVertices = onlyVertices;
  }

  public void setSource(final Mesh source) { _source = source; }

  public Mesh getSource() { return _source; }

  public void setOnlyVertices(final boolean onlyVertices) { _onlyVertices = onlyVertices; }

  public boolean isOnlyVertices() { return _onlyVertices; }

  @Override
  public Vector3 randomEmissionPoint(final Vector3 store) {
    Vector3 rVal = store;
    if (rVal == null) {
      rVal = new Vector3();
    }

    if (_onlyVertices) {
      getSource().getMeshData().randomVertex(rVal);
    } else {
      getSource().getMeshData().randomPointOnPrimitives(rVal);
    }
    return rVal;
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _source = capsule.readSavable("source", null);
    _onlyVertices = capsule.readBoolean("onlyVertices", false);
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_source, "source", null);
    capsule.write(_onlyVertices, "onlyVertices", false);
  }
}
