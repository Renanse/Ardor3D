/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.light;

import java.util.function.Supplier;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;

/**
 * <code>DirectionalLight</code> defines a light that is assumed to be infinitely far away
 * (something similar to the sun). This means the direction of the light rays are all parallel. The
 * direction field of this class identifies the direction in which the light is traveling, which is
 * opposite how jME works.
 */
public class DirectionalLight extends Light {
  private static final long serialVersionUID = 1L;

  // Locally cached direction vector for our light.
  private final Vector3 _worldDirection = new Vector3(Vector3.UNIT_Z);

  /**
   * Constructor instantiates a new <code>DirectionalLight</code> object. The initial light colors are
   * white and the direction the light travels is along the positive z axis (0,0,1). The direction
   * alters based on the World Transform of the light.
   *
   */
  public DirectionalLight() {
    super();

    cachedUniforms.add(new UniformRef("direction", UniformType.Float3, UniformSource.Supplier,
        (Supplier<ReadOnlyVector3>) this::getWorldDirection));
  }

  /**
   * @return the calculated world direction the light traveling in. It starts out traveling down the
   *         positive Z Axis, and is then rotated by our world transform.
   */
  public ReadOnlyVector3 getWorldDirection() { return _worldDirection; }

  /**
   * @param direction
   *          the world direction we want the light to be traveling in.
   */
  public void setWorldDirection(final ReadOnlyVector3 direction) {
    setWorldDirection(direction.getX(), direction.getY(), direction.getZ());
  }

  /**
   * @param x
   *          the direction the light is traveling in on the x axis.
   * @param y
   *          the direction the light is traveling in on the y axis.
   * @param z
   *          the direction the light is traveling in on the z axis.
   */
  public void setWorldDirection(final double x, final double y, final double z) {
    _worldDirection.set(x, y, z);
    final Vector3 from = Vector3.fetchTempInstance();
    final Quaternion q = Quaternion.fetchTempInstance();
    try {
      from.set(Vector3.UNIT_Z);

      if (_parent != null) {
        final ReadOnlyTransform parentTransform = _parent.getWorldTransform();
        parentTransform.applyForwardVector(from);
      }

      q.fromVectorToVector(from, _worldDirection);
      setRotation(q);
    } finally {
      Quaternion.releaseTempInstance(q);
      Vector3.releaseTempInstance(from);
    }
  }

  @Override
  public void applyDefaultUniformValues() {
    setColor(ColorRGBA.BLACK_NO_ALPHA);
    setIntensity(1f);
  }

  /**
   * <code>getType</code> returns this light's type (Type.Directional).
   *
   * @see com.ardor3d.light.Light#getType()
   */
  @Override
  public Type getType() { return Type.Directional; }

  @Override
  public void updateWorldTransform(final boolean recurse) {
    super.updateWorldTransform(recurse);
    _worldDirection.set(0, 0, 1);
    getWorldTransform().applyForwardVector(_worldDirection);
  }
}
