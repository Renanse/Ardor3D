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

import com.ardor3d.math.Line3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyLine3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public final class SimpleParticleInfluenceFactory {

  public static class BasicWind extends ParticleInfluence {
    private double _strength;
    private final Vector3 _windDirection = new Vector3();
    private boolean _random, _rotateWithScene;
    private final Vector3 _vector = new Vector3();

    public BasicWind() {}

    public BasicWind(final double windStr, final ReadOnlyVector3 windDir, final boolean addRandom,
      final boolean rotateWithScene) {
      _strength = windStr;
      _windDirection.set(windDir);
      _random = addRandom;
      _rotateWithScene = rotateWithScene;
    }

    public double getStrength() { return _strength; }

    public void setStrength(final double windStr) { _strength = windStr; }

    public ReadOnlyVector3 getWindDirection() { return _windDirection; }

    public void setWindDirection(final ReadOnlyVector3 windDir) {
      _windDirection.set(windDir);
    }

    public boolean isRandom() { return _random; }

    public void setRandom(final boolean addRandom) { _random = addRandom; }

    public boolean isRotateWithScene() { return _rotateWithScene; }

    public void setRotateWithScene(final boolean rotateWithScene) { _rotateWithScene = rotateWithScene; }

    @Override
    public void prepare(final ParticleSystem system) {
      _vector.set(_windDirection);
      final ReadOnlyMatrix3 mat = system.getEmitterTransform().getMatrix();
      if (_rotateWithScene && !mat.isIdentity()) {
        mat.applyPost(_vector, _vector);
      }
    }

    @Override
    public void apply(final double dt, final Particle p, final int index) {
      final double tStr = (_random ? MathUtils.nextRandomFloat() * _strength : _strength);
      _vector.scaleAdd(tStr * dt, p.getVelocity(), p.getVelocity());
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
      super.write(capsule);
      capsule.write(_strength, "strength", 1f);
      capsule.write(_windDirection, "windDirection", (Vector3) Vector3.UNIT_X);
      capsule.write(_random, "random", false);
      capsule.write(_rotateWithScene, "rotateWithScene", true);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
      super.read(capsule);
      _strength = capsule.readDouble("strength", 1.0);
      _windDirection.set(capsule.readSavable("windDirection", (Vector3) Vector3.UNIT_X));
      _random = capsule.readBoolean("random", false);
      _rotateWithScene = capsule.readBoolean("rotateWithScene", true);
    }

    @Override
    public Class<? extends BasicWind> getClassTag() { return this.getClass(); }
  }

  public static class BasicGravity extends ParticleInfluence {
    private final Vector3 gravity = new Vector3();
    private boolean rotateWithScene;
    private final Vector3 vector = new Vector3();

    public BasicGravity() {}

    public BasicGravity(final ReadOnlyVector3 gravForce, final boolean rotateWithScene) {
      gravity.set(gravForce);
      this.rotateWithScene = rotateWithScene;
    }

    public ReadOnlyVector3 getGravityForce() { return gravity; }

    public void setGravityForce(final ReadOnlyVector3 gravForce) {
      gravity.set(gravForce);
    }

    public boolean isRotateWithScene() { return rotateWithScene; }

    public void setRotateWithScene(final boolean rotateWithScene) { this.rotateWithScene = rotateWithScene; }

    @Override
    public void prepare(final ParticleSystem system) {
      vector.set(gravity);
      final ReadOnlyMatrix3 mat = system.getEmitterTransform().getMatrix();
      if (rotateWithScene && !mat.isIdentity()) {
        mat.applyPost(vector, vector);
      }
    }

    @Override
    public void apply(final double dt, final Particle p, final int index) {
      vector.scaleAdd(dt, p.getVelocity(), p.getVelocity());
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
      super.write(capsule);
      capsule.write(gravity, "gravity", (Vector3) Vector3.ZERO);
      capsule.write(rotateWithScene, "rotateWithScene", true);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
      super.read(capsule);
      gravity.set(capsule.readSavable("gravity", (Vector3) Vector3.ZERO));
      rotateWithScene = capsule.readBoolean("rotateWithScene", true);
    }

    @Override
    public Class<? extends BasicGravity> getClassTag() { return this.getClass(); }
  }

  public static class BasicDrag extends ParticleInfluence {
    private final Vector3 velocity = new Vector3();
    private double dragCoefficient;

    public BasicDrag() {}

    public BasicDrag(final double dragCoef) {
      dragCoefficient = dragCoef;
    }

    public double getDragCoefficient() { return dragCoefficient; }

    public void setDragCoefficient(final double dragCoef) { dragCoefficient = dragCoef; }

    @Override
    public void apply(final double dt, final Particle p, final int index) {
      // viscous drag
      velocity.set(p.getVelocity());
      p.getVelocity().addLocal(velocity.multiplyLocal(-dragCoefficient * dt * p.getInvMass()));
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
      super.write(capsule);
      capsule.write(dragCoefficient, "dragCoefficient", 1.0);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
      super.read(capsule);
      dragCoefficient = capsule.readDouble("dragCoefficient", 1.0);
    }

    @Override
    public Class<? extends BasicDrag> getClassTag() { return this.getClass(); }
  }

  public static class BasicVortex extends ParticleInfluence {

    public static final int VT_CYLINDER = 0;
    public static final int VT_TORUS = 1;

    private int _type = VT_CYLINDER;
    private double _strength, _divergence, _height, _radius;
    private final Line3 _axis = new Line3();
    private boolean _random, _transformWithScene;
    private final Vector3 _v1 = new Vector3(), v2 = new Vector3(), v3 = new Vector3();
    private final Quaternion _rot = new Quaternion();
    private final Line3 _line = new Line3();

    public BasicVortex() {}

    public BasicVortex(final double strength, final double divergence, final ReadOnlyLine3 axis, final boolean random,
      final boolean transformWithScene) {
      _strength = strength;
      _divergence = divergence;
      _axis.set(axis);
      _height = 0f;
      _radius = 1f;
      _random = random;
      _transformWithScene = transformWithScene;
    }

    public int getType() { return _type; }

    public void setType(final int type) { _type = type; }

    public double getStrength() { return _strength; }

    public void setStrength(final double strength) { _strength = strength; }

    public double getDivergence() { return _divergence; }

    public void setDivergence(final double divergence) { _divergence = divergence; }

    public ReadOnlyLine3 getAxis() { return _axis; }

    public void setAxis(final ReadOnlyLine3 axis) {
      _axis.set(axis);
    }

    public double getHeight() { return _height; }

    public void setHeight(final double height) { _height = height; }

    public double getRadius() { return _radius; }

    public void setRadius(final double radius) { _radius = radius; }

    public boolean isRandom() { return _random; }

    public void setRandom(final boolean random) { _random = random; }

    public boolean isTransformWithScene() { return _transformWithScene; }

    public void setTransformWithScene(final boolean transformWithScene) { _transformWithScene = transformWithScene; }

    @Override
    public void prepare(final ParticleSystem system) {
      _line.setOrigin(_axis.getOrigin());
      _line.setDirection(_axis.getDirection());
      final ReadOnlyMatrix3 mat = system.getEmitterTransform().getMatrix();
      if (_transformWithScene && !mat.isIdentity()) {
        final Vector3 temp = Vector3.fetchTempInstance();
        mat.applyPost(_line.getOrigin(), temp);
        _line.setOrigin(temp);
        mat.applyPost(_line.getDirection(), temp);
        _line.setDirection(temp);
        Vector3.releaseTempInstance(temp);
      }
      if (_type == VT_CYLINDER) {
        _rot.fromAngleAxis(-_divergence, _line.getDirection());
      }
    }

    @Override
    public void apply(final double dt, final Particle p, final int index) {
      final double dtStr = dt * _strength * (_random ? MathUtils.nextRandomFloat() : 1f);
      p.getPosition().subtract(_line.getOrigin(), _v1);
      _line.getDirection().cross(_v1, v2);
      if (v2.length() == 0) { // particle is on the axis
        return;
      }
      v2.normalizeLocal();
      if (_type == VT_CYLINDER) {
        _rot.apply(v2, v2);
        v2.scaleAdd(dtStr, p.getVelocity(), p.getVelocity());
        return;
      }
      v2.cross(_line.getDirection(), _v1);
      _v1.multiplyLocal(_radius);
      _line.getDirection().scaleAdd(_height, _v1, _v1);
      _v1.addLocal(_line.getOrigin());
      _v1.subtractLocal(p.getPosition());
      if (_v1.length() == 0) { // particle is on the ring
        return;
      }
      _v1.normalizeLocal();
      _v1.cross(v2, v3);
      _rot.fromAngleAxis(-_divergence, v2);
      _rot.apply(v3, v3);
      v3.scaleAdd(dtStr, p.getVelocity(), p.getVelocity());
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
      super.write(capsule);
      capsule.write(_type, "type", VT_CYLINDER);
      capsule.write(_strength, "strength", 1.0);
      capsule.write(_divergence, "divergence", 0.0);
      capsule.write(_axis, "axis", new Line3(Vector3.ZERO, Vector3.UNIT_Y));
      capsule.write(_height, "height", 0.0);
      capsule.write(_radius, "radius", 1.0);
      capsule.write(_random, "random", false);
      capsule.write(_transformWithScene, "transformWithScene", true);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
      super.read(capsule);
      _type = capsule.readInt("type", VT_CYLINDER);
      _strength = capsule.readDouble("strength", 1.0);
      _divergence = capsule.readDouble("divergence", 0.0);
      _axis.set(capsule.readSavable("axis", new Line3(Vector3.ZERO, Vector3.UNIT_Y)));
      _height = capsule.readDouble("height", 0.0);
      _radius = capsule.readDouble("radius", 1.0);
      _random = capsule.readBoolean("random", false);
      _transformWithScene = capsule.readBoolean("transformWithScene", true);
    }

    @Override
    public Class<? extends BasicVortex> getClassTag() { return this.getClass(); }
  }

  /**
   * Not used.
   */
  private SimpleParticleInfluenceFactory() {}

  /**
   * Creates a basic wind that always blows in a single direction.
   *
   * @param windStr
   *          Max strength of wind.
   * @param windDir
   *          Direction wind should blow.
   * @param addRandom
   *          randomly alter the strength of the wind by 0-100%
   * @param rotateWithScene
   *          rotate the wind direction with the particle system
   * @return ParticleInfluence
   */
  public static ParticleInfluence createBasicWind(final double windStr, final ReadOnlyVector3 windDir,
      final boolean addRandom, final boolean rotateWithScene) {
    return new BasicWind(windStr, windDir, addRandom, rotateWithScene);
  }

  /**
   * Create a basic gravitational force.
   *
   * @param rotateWithScene
   *          rotate the gravity vector with the particle system
   * @return ParticleInfluence
   */
  public static ParticleInfluence createBasicGravity(final ReadOnlyVector3 gravForce, final boolean rotateWithScene) {
    return new BasicGravity(gravForce, rotateWithScene);
  }

  /**
   * Create a basic drag force that will use the given drag coefficient. Drag is determined by
   * figuring the current velocity and reversing it, then multiplying by the drag coefficient and
   * dividing by the particle mass.
   *
   * @param dragCoef
   *          Should be positive. Larger values mean more drag but possibly more instability.
   * @return ParticleInfluence
   */
  public static ParticleInfluence createBasicDrag(final double dragCoef) {
    return new BasicDrag(dragCoef);
  }

  /**
   * Creates a basic vortex.
   *
   * @param strength
   *          Max strength of vortex.
   * @param divergence
   *          The divergence in radians from the tangent vector
   * @param axis
   *          The center of the vortex.
   * @param random
   *          randomly alter the strength of the vortex by 0-100%
   * @param transformWithScene
   *          transform the axis with the particle system
   * @return ParticleInfluence
   */
  public static ParticleInfluence createBasicVortex(final double strength, final double divergence,
      final ReadOnlyLine3 axis, final boolean random, final boolean transformWithScene) {
    return new BasicVortex(strength, divergence, axis, random, transformWithScene);
  }
}
