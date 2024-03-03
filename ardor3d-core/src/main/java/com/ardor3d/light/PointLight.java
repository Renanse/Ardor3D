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

import java.io.IOException;
import java.io.Serial;
import java.util.function.Supplier;

import com.ardor3d.light.shadow.AbstractShadowData;
import com.ardor3d.light.shadow.PointShadowData;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>PointLight</code> defines a light that emits light in all directions evenly. This would be
 * something similar to a light bulb. Typically this light's values are attenuated based on the
 * distance of the point light and the object it illuminates. The light's position is read from its
 * WorldTransform.
 */
public class PointLight extends Light {

  @Serial
  private static final long serialVersionUID = 1L;

  private float _constant = 1;
  private float _linear;
  private float _quadratic;
  private float _range = 100;

  private PointShadowData _shadowData;

  /**
   * Constructor instantiates a new <code>PointLight</code> object. The initial position of the light
   * is (0,0,0) and it's colors are white.
   *
   */
  public PointLight() {
    super();
    setShadowData();

    _cachedUniforms.add(new UniformRef("position", UniformType.Float3, UniformSource.Supplier,
        (Supplier<ReadOnlyVector3>) this::getWorldTranslation));
    _cachedUniforms.add(
        new UniformRef("constant", UniformType.Float1, UniformSource.Supplier, (Supplier<Float>) this::getConstant));
    _cachedUniforms
        .add(new UniformRef("linear", UniformType.Float1, UniformSource.Supplier, (Supplier<Float>) this::getLinear));
    _cachedUniforms.add(
        new UniformRef("quadratic", UniformType.Float1, UniformSource.Supplier, (Supplier<Float>) this::getQuadratic));
    _cachedUniforms
        .add(new UniformRef("range", UniformType.Float1, UniformSource.Supplier, (Supplier<Float>) this::getRange));
  }

  protected void setShadowData() {
    _shadowData = new PointShadowData(this);

    _cachedUniforms.add(new UniformRef("shadowMatrix[0]", UniformType.Matrix4x4, UniformSource.Supplier,
        (Supplier<ReadOnlyMatrix4>) () -> _shadowData.getShadowMatrix()));
  }

  /**
   * <code>getConstant</code> returns the value for the constant attenuation.
   *
   * @return the value for the constant attenuation.
   */
  public float getConstant() { return _constant; }

  /**
   * <code>setConstant</code> sets the value for the constant attentuation.
   *
   * @param constant
   *          the value for the constant attenuation.
   */
  public void setConstant(final float constant) { _constant = constant; }

  /**
   * <code>getLinear</code> returns the value for the linear attenuation.
   *
   * @return the value for the linear attenuation.
   */
  public float getLinear() { return _linear; }

  /**
   * <code>setLinear</code> sets the value for the linear attentuation.
   *
   * @param linear
   *          the value for the linear attenuation.
   */
  public void setLinear(final float linear) { _linear = linear; }

  /**
   * <code>getQuadratic</code> returns the value for the quadratic attentuation.
   *
   * @return the value for the quadratic attenuation.
   */
  public float getQuadratic() { return _quadratic; }

  /**
   * <code>setQuadratic</code> sets the value for the quadratic attenuation.
   *
   * @param quadratic
   *          the value for the quadratic attenuation.
   */
  public void setQuadratic(final float quadratic) { _quadratic = quadratic; }

  /**
   * @return the maximum world distance at which this light will affect geometry. Default is 100.
   */
  public float getRange() { return _range; }

  /**
   * @param range
   *          the maximum world distance at which this light will affect geometry
   */
  public void setRange(final float range) { _range = range; }

  @Override
  public AbstractShadowData getShadowData() { return _shadowData; }

  @Override
  public void applyDefaultUniformValues() {
    setColor(ColorRGBA.BLACK_NO_ALPHA);
    setIntensity(1f);
    setConstant(1);
    setQuadratic(0);
    setLinear(0);
    setEnabled(false);
  }

  /**
   * <code>getType</code> returns the type of this light (Type.Point).
   *
   * @see com.ardor3d.light.Light#getType()
   */
  @Override
  public Type getType() { return Type.Point; }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_constant, "constant", 1);
    capsule.write(_linear, "linear", 0);
    capsule.write(_quadratic, "quadratic", 0);
    capsule.write(_range, "range", 100);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _constant = capsule.readFloat("constant", 1);
    _linear = capsule.readFloat("linear", 0);
    _quadratic = capsule.readFloat("quadratic", 0);
    _range = capsule.readFloat("range", 100);
  }

}
