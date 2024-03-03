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

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * Simple swarming influence for use with particles.
 */
public class SwarmInfluence extends ParticleInfluence {

  private double _swarmRangeSQ;
  private final Vector3 _swarmOffset = new Vector3();
  private final Vector3 _swarmPoint = new Vector3();

  public static final double DEFAULT_SWARM_RANGE_SQ = 0.01;
  public static final double DEFAULT_DEVIANCE = MathUtils.DEG_TO_RAD * 15;
  public static final double DEFAULT_TURN_SPEED = MathUtils.DEG_TO_RAD * 180;
  public static final double DEFAULT_SPEED_BUMP = .1;
  public static final double DEFAULT_MAX_SPEED = .2;

  private double _deviance = DEFAULT_DEVIANCE;
  private double _turnSpeed = DEFAULT_TURN_SPEED;
  private double _speedBump = DEFAULT_SPEED_BUMP;
  private double _maxSpeed = DEFAULT_MAX_SPEED;

  private transient double _maxSpeedSQ = DEFAULT_MAX_SPEED * DEFAULT_MAX_SPEED;

  public SwarmInfluence() {
    _swarmRangeSQ = DEFAULT_SWARM_RANGE_SQ;
  }

  public SwarmInfluence(final ReadOnlyVector3 offset, final double swarmRange) {
    super();
    _swarmRangeSQ = swarmRange * swarmRange;
    _swarmOffset.set(offset);
  }

  @Override
  public void prepare(final ParticleSystem system) {
    super.prepare(system);
    _swarmPoint.set(system.getOriginCenter()).addLocal(_swarmOffset);
  }

  @Override
  public void apply(final double dt, final Particle particle, final int index) {
    final Vector3 pVelocity = particle.getVelocity();
    // determine if the particle is in the inner or outer zone
    final double pDist = particle.getPosition().distanceSquared(_swarmPoint);
    final Vector3 workVect = Vector3.fetchTempInstance();
    final Vector3 workVect2 = Vector3.fetchTempInstance();
    final Matrix3 workMat = Matrix3.fetchTempInstance();
    workVect.set(_swarmPoint).subtractLocal(particle.getPosition()).normalizeLocal();
    workVect2.set(pVelocity).normalizeLocal();
    if (pDist > _swarmRangeSQ) {
      // IN THE OUTER ZONE...
      // Determine if the angle between particle velocity and a vector to
      // the swarmPoint is less than the accepted deviance
      final double angle = workVect.smallestAngleBetween(workVect2);
      if (angle < _deviance) {
        // if it is, increase the speed speedBump over time
        if (pVelocity.lengthSquared() < _maxSpeedSQ) {
          final double change = _speedBump * dt;
          workVect2.multiplyLocal(change); // where workVector2 = pVelocity.normalizeLocal()
          pVelocity.addLocal(workVect2);
        }
      } else {
        final Vector3 axis = workVect2.crossLocal(workVect);
        // if it is not, shift the velocity to bring it back in line
        if ((Double.doubleToLongBits(pVelocity.lengthSquared()) & 0x1d) != 0) {
          workMat.fromAngleAxis(_turnSpeed * dt, axis);
        } else {
          workMat.fromAngleAxis(-_turnSpeed * dt, axis);
        }
        workMat.applyPost(pVelocity, pVelocity);
      }
    } else {
      final Vector3 axis = workVect2.crossLocal(workVect);
      // IN THE INNER ZONE...
      // Alter the heading based on how fast we are going
      if ((index & 0x1f) != 0) {
        workMat.fromAngleAxis(_turnSpeed * dt, axis);
      } else {
        workMat.fromAngleAxis(-_turnSpeed * dt, axis);
      }
      workMat.applyPost(pVelocity, pVelocity);
    }
    Vector3.releaseTempInstance(workVect);
    Vector3.releaseTempInstance(workVect2);
    Matrix3.releaseTempInstance(workMat);
  }

  public double getSwarmRange() { return Math.sqrt(_swarmRangeSQ); }

  public void setSwarmRange(final double swarmRange) { _swarmRangeSQ = swarmRange * swarmRange; }

  public ReadOnlyVector3 getSwarmOffset() { return _swarmOffset; }

  public void setSwarmOffset(final ReadOnlyVector3 offset) {
    _swarmOffset.set(offset);
  }

  public double getDeviance() { return _deviance; }

  public void setDeviance(final double deviance) { _deviance = deviance; }

  public double getSpeedBump() { return _speedBump; }

  public void setSpeedBump(final double speedVariance) { _speedBump = speedVariance; }

  public double getTurnSpeed() { return _turnSpeed; }

  public void setTurnSpeed(final double turnSpeed) { _turnSpeed = turnSpeed; }

  public double getMaxSpeed() { return _maxSpeed; }

  public void setMaxSpeed(final double maxSpeed) {
    _maxSpeed = maxSpeed;
    _maxSpeedSQ = maxSpeed * maxSpeed;
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    final OutputCapsule cap = capsule;
    cap.write(_swarmRangeSQ, "swarmRangeSQ", DEFAULT_SWARM_RANGE_SQ);
    cap.write(_deviance, "deviance", DEFAULT_DEVIANCE);
    cap.write(_turnSpeed, "turnSpeed", DEFAULT_TURN_SPEED);
    cap.write(_speedBump, "speedBump", DEFAULT_SPEED_BUMP);
    cap.write(_maxSpeed, "maxSpeed", DEFAULT_MAX_SPEED);
    cap.write(_swarmOffset, "swarmOffset", (Vector3) Vector3.ZERO);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    final InputCapsule cap = capsule;
    _swarmRangeSQ = cap.readDouble("swarmRangeSQ", DEFAULT_SWARM_RANGE_SQ);
    _deviance = cap.readDouble("deviance", DEFAULT_DEVIANCE);
    _turnSpeed = cap.readDouble("turnSpeed", DEFAULT_TURN_SPEED);
    _speedBump = cap.readDouble("speedBump", DEFAULT_SPEED_BUMP);
    _maxSpeed = cap.readDouble("maxSpeed", DEFAULT_MAX_SPEED);
    _swarmOffset.set(cap.readSavable("swarmOffset", (Vector3) Vector3.ZERO));
  }

  @Override
  public Class<? extends SwarmInfluence> getClassTag() { return getClass(); }
}
