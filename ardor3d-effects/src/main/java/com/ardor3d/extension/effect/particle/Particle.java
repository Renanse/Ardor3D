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
import java.nio.FloatBuffer;

import com.ardor3d.extension.effect.particle.ParticleSystem.ParticleType;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Triangle;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>Particle</code> defines a single Particle of a Particle system. Generally, you would not
 * interact with this class directly.
 */
public class Particle implements Savable {

  public enum Status {
    /** Particle is dead -- not in play. */
    Dead,
    /** Particle is currently active. */
    Alive,
    /** Particle is available for spawning. */
    Available;
  }

  static final int VAL_CURRENT_SIZE = 0;
  static final int VAL_CURRENT_SPIN = 1;
  static final int VAL_CURRENT_MASS = 2;

  private int startIndex;
  private final Vector3 _position = new Vector3();
  private final Vector3 _velocity = new Vector3();
  private final ColorRGBA currColor = new ColorRGBA(ColorRGBA.BLACK);
  private Status status = Status.Available;
  private double lifeSpan;
  private final double[] values = new double[3];
  private int currentAge;
  private int currentTexIndex = -1;
  private ParticleSystem parent;
  private final Vector3 bbX = new Vector3(), bbY = new Vector3();

  // colors
  private ParticleType type = ParticleSystem.ParticleType.Triangle;

  private Triangle triModel;

  /**
   * Empty constructor - mostly for use with Savable interface
   */
  public Particle() {}

  /**
   * Normal use constructor. Sets up the parent and particle type for this particle.
   *
   * @param parent
   *          the particle collection this particle belongs to
   */
  public Particle(final ParticleSystem parent) {
    this.parent = parent;
    type = parent.getParticleType();
  }

  /**
   * Cause this particle to reset it's lifespan, velocity, color, age and size per the parent's
   * settings. status is set to Status.Available and location is set to 0,0,0. Actual geometry data is
   * not affected by this call, only particle params.
   */
  public void init() {
    init(parent.getRandomVelocity(_velocity), Vector3.ZERO, parent.getRandomLifeSpan());
  }

  /**
   * Cause this particle to reset it's color, age and size per the parent's settings. status is set to
   * Status.Available. Location, velocity and lifespan are set as given. Actual geometry data is not
   * affected by this call, only particle params.
   *
   * @param velocity
   *          new initial particle velocity
   * @param position
   *          new initial particle position
   * @param lifeSpan
   *          new particle lifespan in ms
   */
  public void init(final ReadOnlyVector3 velocity, final ReadOnlyVector3 position, final double lifeSpan) {
    this.lifeSpan = lifeSpan;
    _velocity.set(velocity);
    _position.set(position);

    currColor.set(parent.getStartColor());
    currentAge = 0;
    status = Status.Available;
    values[VAL_CURRENT_SIZE] = parent.getStartSize();
  }

  /**
   * Reset particle conditions. Besides the passed lifespan, we also reset color, size, and spin angle
   * to their starting values (as given by parent.) Status is set to Status.Available.
   *
   * @param lifeSpan
   *          the recreated particle's new lifespan
   */
  public void recreateParticle(final double lifeSpan) {
    this.lifeSpan = lifeSpan;

    currColor.set(parent.getStartColor());
    values[VAL_CURRENT_SIZE] = parent.getStartSize();
    currentAge = 0;
    values[VAL_CURRENT_MASS] = 1;
    status = Status.Available;
  }

  /**
   * Update the vertices for this particle, taking size, spin and viewer into consideration. In the
   * case of particle type ParticleType.GeomMesh, the original triangle normal is maintained rather
   * than rotating it to face the camera or parent vectors.
   *
   * @param cam
   *          Camera to use in determining viewer aspect. If null, or if parent is not set to camera
   *          facing, parent's left and up vectors are used.
   */
  public void updateVerts(final Camera cam) {
    final double orient = parent.getParticleOrientation() + values[VAL_CURRENT_SPIN];
    final double currSize = values[VAL_CURRENT_SIZE];

    if (type == ParticleSystem.ParticleType.GeomMesh || type == ParticleSystem.ParticleType.Point) {
      // nothing to do
    } else if (cam != null && parent.isCameraFacing()) {
      final ReadOnlyVector3 camUp = cam.getUp();
      final ReadOnlyVector3 camLeft = cam.getLeft();
      final ReadOnlyVector3 camDir = cam.getDirection();
      if (parent.isVelocityAligned()) {
        bbX.set(_velocity).normalizeLocal().multiplyLocal(currSize);
        camDir.cross(bbX, bbY).normalizeLocal().multiplyLocal(currSize);
      } else if (orient == 0) {
        bbX.set(camLeft).multiplyLocal(currSize);
        bbY.set(camUp).multiplyLocal(currSize);
      } else {
        final double cA = MathUtils.cos(orient) * currSize;
        final double sA = MathUtils.sin(orient) * currSize;
        bbX.set(camLeft).multiplyLocal(cA).addLocal(camUp.getX() * sA, camUp.getY() * sA, camUp.getZ() * sA);
        bbY.set(camLeft).multiplyLocal(-sA).addLocal(camUp.getX() * cA, camUp.getY() * cA, camUp.getZ() * cA);
      }
    } else {
      final ReadOnlyVector3 left = parent.getFacingLeftVector();
      final ReadOnlyVector3 up = parent.getFacingUpVector();

      if (parent.isVelocityAligned()) {
        bbX.set(_velocity).normalizeLocal().multiplyLocal(currSize);
        up.cross(bbX, bbY).normalizeLocal().multiplyLocal(currSize);
      } else if (orient == 0) {
        bbX.set(left).multiplyLocal(currSize);
        bbY.set(up).multiplyLocal(currSize);
      } else {
        final double cA = MathUtils.cos(orient) * currSize;
        final double sA = MathUtils.sin(orient) * currSize;
        bbX.set(left).multiplyLocal(cA).addLocal(up.getX() * sA, up.getY() * sA, up.getZ() * sA);
        bbY.set(left).multiplyLocal(-sA).addLocal(up.getX() * cA, up.getY() * cA, up.getZ() * cA);
      }
    }

    final Vector3 tempVec3 = Vector3.fetchTempInstance();
    final FloatBuffer vertexBuffer = parent.getParticleGeometry().getMeshData().getVertexBuffer();
    switch (type) {
      case GeomMesh: {
        final Quaternion tempQuat = Quaternion.fetchTempInstance();
        final ReadOnlyVector3 norm = triModel.getNormal();
        if (orient != 0) {
          tempQuat.fromAngleNormalAxis(orient, norm);
        }

        for (int x = 0; x < 3; x++) {
          if (orient != 0) {
            tempQuat.apply(triModel.get(x), tempVec3);
          } else {
            tempVec3.set(triModel.get(x));
          }
          tempVec3.multiplyLocal(currSize).addLocal(_position);
          BufferUtils.setInBuffer(tempVec3, vertexBuffer, startIndex + x);
        }
        Quaternion.releaseTempInstance(tempQuat);
        break;
      }
      case Triangle: {
        _position.subtract(3 * bbX.getX(), 3 * bbX.getY(), 3 * bbX.getZ(), tempVec3).subtractLocal(bbY);
        BufferUtils.setInBuffer(tempVec3, vertexBuffer, startIndex + 0);

        _position.add(bbX, tempVec3).addLocal(3 * bbY.getX(), 3 * bbY.getY(), 3 * bbY.getZ());
        BufferUtils.setInBuffer(tempVec3, vertexBuffer, startIndex + 1);

        _position.add(bbX, tempVec3).subtractLocal(bbY);
        BufferUtils.setInBuffer(tempVec3, vertexBuffer, startIndex + 2);
        break;
      }
      case Line: {
        _position.subtract(bbX, tempVec3);
        BufferUtils.setInBuffer(tempVec3, vertexBuffer, startIndex);

        _position.add(bbX, tempVec3);
        BufferUtils.setInBuffer(tempVec3, vertexBuffer, startIndex + 1);
        break;
      }
      case Point: {
        BufferUtils.setInBuffer(_position, vertexBuffer, startIndex);
        break;
      }
    }
    Vector3.releaseTempInstance(tempVec3);
  }

  /**
   * <p>
   * update position (using current position and velocity), color (interpolating between start and end
   * color), size (interpolating between start and end size), spin (using parent's spin speed) and
   * current age of particle. If this particle's age is greater than its lifespan, it is set to status
   * DEAD.
   * </p>
   *
   * @param secondsPassed
   *          number of seconds passed since last update.
   * @return true if this particle is not ALIVE (in other words, if it is ready to be reused.)
   */
  public boolean updateAndCheck(final double secondsPassed) {
    if (status != Status.Alive) {
      return true;
    }
    currentAge += secondsPassed * 1000; // add ms time to age
    if (currentAge > lifeSpan) {
      killParticle();
      return true;
    }

    final Vector3 temp = Vector3.fetchTempInstance();
    _position.addLocal(_velocity.multiply(secondsPassed * 1000f, temp));
    Vector3.releaseTempInstance(temp);

    // get interpolated values from appearance ramp:
    parent.getRamp().getValuesAtAge(currentAge, lifeSpan, currColor, values, parent);

    // interpolate colors
    final int verts = ParticleSystem.getVertsForParticleType(type);
    for (int x = 0; x < verts; x++) {
      BufferUtils.setInBuffer(currColor, parent.getParticleGeometry().getMeshData().getColorBuffer(), startIndex + x);
    }

    // check for tex animation
    final int newTexIndex = parent.getTexAnimation().getTexIndexAtAge(currentAge, lifeSpan, parent);
    // Update tex coords if applicable
    if (currentTexIndex != newTexIndex) {
      // // Only supported in Quad type for now.
      // if (ParticleType.Quad.equals(parent.getParticleType())) {
      // // determine side
      // final float side = (float) Math.sqrt(parent.getTexQuantity());
      // int index = newTexIndex;
      // if (index >= parent.getTexQuantity()) {
      // index %= parent.getTexQuantity();
      // }
      // // figure row / col
      // final float row = side - (int) (index / side) - 1;
      // final float col = index % side;
      // // set texcoords
      // final float sU = col / side, eU = (col + 1) / side;
      // final float sV = row / side, eV = (row + 1) / side;
      // final FloatBuffer texs =
      // parent.getParticleGeometry().getMeshData().getTextureCoords(0).getBuffer();
      // texs.position(startIndex * 2);
      // texs.put(eU).put(sV);
      // texs.put(eU).put(eV);
      // texs.put(sU).put(eV);
      // texs.put(sU).put(sV);
      // texs.clear();
      // }
      currentTexIndex = newTexIndex;
    }

    return false;
  }

  public void killParticle() {
    setStatus(Status.Dead);

    final Vector3 tempVec3 = Vector3.fetchTempInstance();
    final FloatBuffer vertexBuffer = parent.getParticleGeometry().getMeshData().getVertexBuffer();
    BufferUtils.populateFromBuffer(tempVec3, vertexBuffer, startIndex);
    final int verts = ParticleSystem.getVertsForParticleType(type);
    for (int x = 1; x < verts; x++) {
      BufferUtils.setInBuffer(tempVec3, vertexBuffer, startIndex + x);
    }
    Vector3.releaseTempInstance(tempVec3);

  }

  /**
   * Resets current age to 0
   */
  public void resetAge() {
    currentAge = 0;
  }

  /**
   * @return the current age of the particle in ms
   */
  public int getCurrentAge() { return currentAge; }

  /**
   * @return the current position of the particle in space
   */
  public Vector3 getPosition() { return _position; }

  /**
   * Set the position of the particle in space.
   *
   * @param position
   *          the new position in world coordinates
   */
  public void setPosition(final Vector3 position) {
    _position.set(position);
  }

  /**
   * @return the current status of this particle.
   * @see Status
   */
  public Status getStatus() { return status; }

  /**
   * Set the status of this particle.
   *
   * @param status
   *          new status of this particle
   * @see Status
   */
  public void setStatus(final Status status) { this.status = status; }

  /**
   * @return the current velocity of this particle
   */
  public Vector3 getVelocity() { return _velocity; }

  /**
   * Set the current velocity of this particle
   *
   * @param velocity
   *          the new velocity
   */
  public void setVelocity(final Vector3 velocity) {
    _velocity.set(velocity);
  }

  /**
   * @return the current color applied to this particle
   */
  public ColorRGBA getCurrentColor() { return currColor; }

  /**
   * @return the start index of this particle in relation to where it exists in its parent's geometry
   *         data.
   */
  public int getStartIndex() { return startIndex; }

  /**
   * Set the starting index where this particle is represented in its parent's geometry data
   *
   * @param index
   */
  public void setStartIndex(final int index) { startIndex = index; }

  /**
   * @return the mass of this particle. Only used by ParticleInfluences such as drag.
   */
  public double getMass() { return values[VAL_CURRENT_MASS]; }

  /**
   * @return the inverse mass of this particle. Often useful for skipping constant division by mass
   *         calculations. If the mass is 0, the inverse mass is considered to be positive infinity.
   *         Conversely, if the mass is positive infinity, the inverse is 0. The inverse of negative
   *         infinity is considered to be -0.
   */
  public double getInvMass() {
    final double mass = values[VAL_CURRENT_MASS];
    if (mass == 0) {
      return Float.POSITIVE_INFINITY;
    } else if (mass == Float.POSITIVE_INFINITY) {
      return 0;
    } else if (mass == Float.NEGATIVE_INFINITY) {
      return -0;
    } else {
      return 1f / mass;
    }
  }

  /**
   * Sets a triangle model to use for particle calculations when using particle type
   * ParticleType.GeomMesh. The particle will maintain the triangle's ratio and plane of orientation.
   * It will spin (if applicable) around the triangle's normal axis. The triangle should already have
   * its center and normal fields calculated before calling this method.
   *
   * @param t
   *          the triangle to model this particle after.
   */
  public void setTriangleModel(final Triangle t) { triModel = t; }

  /**
   * @return the triangle model used by this particle
   * @see #setTriangleModel(Triangle)
   */
  public Triangle getTriangleModel() { return triModel; }

  // /////
  // Savable interface methods
  // /////

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(startIndex, "startIndex", 0);
    capsule.write(_position, "position", (Vector3) Vector3.ZERO);
    capsule.write(status, "status", Status.Available);
    capsule.write(lifeSpan, "lifeSpan", 0);
    capsule.write(currentAge, "currentAge", 0);
    capsule.write(parent, "parent", null);
    capsule.write(_velocity, "velocity", (Vector3) Vector3.ZERO);
    capsule.write(type, "type", ParticleSystem.ParticleType.Triangle);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    startIndex = capsule.readInt("startIndex", 0);
    _position.set(capsule.readSavable("position", (Vector3) Vector3.ZERO));
    status = capsule.readEnum("status", Status.class, Status.Available);
    lifeSpan = capsule.readDouble("lifeSpan", 0);
    currentAge = capsule.readInt("currentAge", 0);
    parent = capsule.readSavable("parent", null);
    _velocity.set(capsule.readSavable("velocity", (Vector3) Vector3.ZERO));
    type = capsule.readEnum("type", ParticleSystem.ParticleType.class, ParticleSystem.ParticleType.Triangle);
  }

  @Override
  public Class<? extends Particle> getClassTag() { return this.getClass(); }
}
