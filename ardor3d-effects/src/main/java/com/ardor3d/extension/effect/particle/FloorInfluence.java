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

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyPlane;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class FloorInfluence extends ParticleInfluence {

  /**
   * Bounciness is the factor of multiplication when bouncing off the floor. A bounciness factor of 1
   * means the particle leaves the floor with the same velocity as it hit the floor.
   */
  private double _bounciness = 1;

  /**
   * Our imaginary floor
   */
  private final Plane _floor = new Plane();

  public FloorInfluence() {}

  /**
   * @param plane
   *          The imaginary floor plane
   * @param bounciness
   *          Bounciness is the factor of multiplication when bouncing off the floor. A bounciness
   *          factor of 1 means the ball leaves the floor with the same velocity as it hit the floor,
   *          much like a rubber ball.
   */
  public FloorInfluence(final ReadOnlyPlane plane, final double bounciness) {
    _bounciness = bounciness;
    _floor.set(plane);
  }

  @Override
  public void apply(final double dt, final Particle particle, final int index) {
    // Is particle alive, AND "under" our floor?
    if (particle.getStatus() == Particle.Status.Alive && _floor.pseudoDistance(particle.getPosition()) <= 0) {
      final Vector3 tempVect1 = Vector3.fetchTempInstance();
      final double scale = particle.getVelocity().length();
      tempVect1.set(particle.getVelocity()).divideLocal(scale); // normalize

      // Is the particle moving further into the floor?
      if (_floor.getNormal().smallestAngleBetween(tempVect1) > MathUtils.HALF_PI) {
        // reflect our velocity vector across the floor plane
        _floor.reflectVector(tempVect1, tempVect1);

        // apply the "bounciness" factor
        tempVect1.multiplyLocal(scale * _bounciness);

        // write back to particle
        particle.setVelocity(tempVect1);
      }
      Vector3.releaseTempInstance(tempVect1);
    }
  }

  public double getBounciness() { return _bounciness; }

  public void setBounciness(final double bounciness) { _bounciness = bounciness; }

  public ReadOnlyPlane getFloor() { return _floor; }

  public void setFloor(final ReadOnlyPlane floor) {

    _floor.set(floor);
  }

  public ReadOnlyVector3 getNormal() { return _floor.getNormal(); }

  public void setNormal(final Vector3 normal) {
    _floor.setNormal(normal.normalize(null));
  }

  public double getConstant() { return _floor.getConstant(); }

  public void setConstant(final double constant) {
    _floor.setConstant(constant);
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends FloorInfluence> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_bounciness, "bounciness", 1.0);
    capsule.write(_floor, "floor", new Plane());
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _bounciness = capsule.readDouble("bounciness", 1.0);
    _floor.set(capsule.readSavable("floor", new Plane()));
  }

}
