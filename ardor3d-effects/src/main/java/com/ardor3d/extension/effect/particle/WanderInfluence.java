/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.effect.particle;

import java.io.IOException;
import java.util.ArrayList;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class WanderInfluence extends ParticleInfluence {

  public static final double DEFAULT_RADIUS = .03f;
  public static final double DEFAULT_DISTANCE = .2f;
  public static final double DEFAULT_JITTER = .005f;

  private double _wanderRadius = DEFAULT_RADIUS;
  private double _wanderDistance = DEFAULT_DISTANCE;
  private double _wanderJitter = DEFAULT_JITTER;

  private ArrayList<Vector3> _wanderTargets = new ArrayList<>(1);
  private final Vector3 _workVect = new Vector3();

  @Override
  public void prepare(final ParticleSystem system) {
    if (_wanderTargets.size() != system.getNumParticles()) {
      _wanderTargets = new ArrayList<>(system.getNumParticles());
      for (int x = system.getNumParticles(); --x >= 0;) {
        _wanderTargets.add(new Vector3(system.getEmissionDirection()).normalizeLocal());
      }
    }
  }

  @Override
  public void apply(final double dt, final Particle particle, final int index) {
    if (_wanderRadius == 0 && _wanderDistance == 0 && _wanderJitter == 0) {
      return;
    }

    final Vector3 wanderTarget = _wanderTargets.get(index);

    wanderTarget.addLocal(calcNewJitter(), calcNewJitter(), calcNewJitter());
    wanderTarget.normalizeLocal();
    wanderTarget.multiplyLocal(_wanderRadius);

    _workVect.set(particle.getVelocity()).normalizeLocal().multiplyLocal(_wanderDistance);
    _workVect.addLocal(wanderTarget).normalizeLocal();
    _workVect.multiplyLocal(particle.getVelocity().length());
    particle.getVelocity().set(_workVect);
  }

  private double calcNewJitter() {
    return ((MathUtils.nextRandomFloat() * 2.0f) - 1.0f) * _wanderJitter;
  }

  public double getWanderDistance() { return _wanderDistance; }

  public void setWanderDistance(final double wanderDistance) { _wanderDistance = wanderDistance; }

  public double getWanderJitter() { return _wanderJitter; }

  public void setWanderJitter(final double wanderJitter) { _wanderJitter = wanderJitter; }

  public double getWanderRadius() { return _wanderRadius; }

  public void setWanderRadius(final double wanderRadius) { _wanderRadius = wanderRadius; }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    final OutputCapsule cap = capsule;
    cap.write(_wanderRadius, "wanderRadius", DEFAULT_RADIUS);
    cap.write(_wanderDistance, "wanderDistance", DEFAULT_DISTANCE);
    cap.write(_wanderJitter, "wanderJitter", DEFAULT_JITTER);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    final InputCapsule cap = capsule;
    _wanderRadius = cap.readDouble("wanderRadius", DEFAULT_RADIUS);
    _wanderDistance = cap.readDouble("wanderDistance", DEFAULT_DISTANCE);
    _wanderJitter = cap.readDouble("wanderJitter", DEFAULT_JITTER);
  }

  @Override
  public Class<? extends WanderInfluence> getClassTag() { return getClass(); }
}
