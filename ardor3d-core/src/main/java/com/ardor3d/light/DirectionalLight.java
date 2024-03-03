/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.light;

import java.io.Serial;
import java.util.function.Supplier;

import com.ardor3d.light.shadow.DirectionalShadowData;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;

/**
 * <code>DirectionalLight</code> defines a light that is assumed to be infinitely far away
 * (something similar to the sun). This means the direction of the light rays are all parallel. The
 * direction field of this class identifies the direction in which the light is traveling.
 */
public class DirectionalLight extends Light {
  @Serial
  private static final long serialVersionUID = 1L;

  // Locally cached direction vector for our light.
  private final Vector3 _worldDirection = new Vector3(Vector3.UNIT_Z);

  private DirectionalShadowData _shadowData;

  /**
   * Constructor instantiates a new <code>DirectionalLight</code> object. The initial light colors are
   * white and the direction the light travels is along the positive z axis (0,0,1). The direction
   * alters based on the World Transform's rotation.
   *
   */
  public DirectionalLight() {
    super();
    setShadowData();

    _cachedUniforms.add(new UniformRef("direction", UniformType.Float3, UniformSource.Supplier,
        (Supplier<ReadOnlyVector3>) this::getWorldDirection));

    for (int i = 0; i < DirectionalShadowData.MAX_SPLITS; i++) {
      final int index = i;
      _cachedUniforms.add(new UniformRef("shadowMatrix[" + i + "]", UniformType.Matrix4x4, UniformSource.Supplier,
          (Supplier<ReadOnlyMatrix4>) () -> _shadowData.getShadowMatrix(index)));
    }
  }

  protected void setShadowData() {
    _shadowData = new DirectionalShadowData(this);
  }

  /**
   * @return the calculated world direction the light traveling in. It starts out traveling down the
   *         positive Z Axis, and is then rotated by our world transform.
   */
  public ReadOnlyVector3 getWorldDirection() { return _worldDirection; }

  /**
   * Convenience method for setting this light's rotation such that the forward vector points at the
   * given direction in world coordinate space. Assumes Y axis is up, which in most cases has no
   * impact.
   *
   * @param direction
   */
  public void setWorldDirection(final ReadOnlyVector3 direction) {
    setWorldDirection(direction.getX(), direction.getY(), direction.getZ());
  }

  /**
   * Convenience method for setting this light's rotation such that the forward vector points at the
   * given direction in world coordinate space. Assumes Y axis is up, which in most cases has no
   * impact.
   *
   * @param x
   * @param y
   * @param z
   */
  public void setWorldDirection(final double x, final double y, final double z) {
    updateWorldTransformToRoot();
    _worldDirection.set(_worldTransform.getTranslation()).addLocal(x, y, z);
    lookAt(_worldDirection.getX(), _worldDirection.getY(), _worldDirection.getZ());
  }

  @Override
  public void updateWorldTransform(final boolean recurse) {
    super.updateWorldTransform(recurse);
    _worldDirection.set(Vector3.UNIT_Z);
    getWorldTransform().applyForwardVector(_worldDirection);
  }

  @Override
  public DirectionalShadowData getShadowData() { return _shadowData; }

  @Override
  public void applyDefaultUniformValues() {
    setColor(ColorRGBA.BLACK_NO_ALPHA);
    setIntensity(1f);
    setEnabled(false);
  }

  /**
   * <code>getType</code> returns this light's type (Type.Directional).
   *
   * @see com.ardor3d.light.Light#getType()
   */
  @Override
  public Type getType() { return Type.Directional; }

}
