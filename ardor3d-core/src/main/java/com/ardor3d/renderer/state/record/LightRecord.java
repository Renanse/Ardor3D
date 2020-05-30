/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.state.record;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Vector4;

public class LightRecord extends StateRecord {
  public ColorRGBA ambient = new ColorRGBA(-1, -1, -1, -1);
  public ColorRGBA diffuse = new ColorRGBA(-1, -1, -1, -1);
  public ColorRGBA specular = new ColorRGBA(-1, -1, -1, -1);
  private float constant = -1;
  private float linear = -1;
  private float quadratic = -1;
  private float spotExponent = -1;
  private float spotCutoff = -1;
  private boolean enabled = false;

  public Vector4 position = new Vector4();
  public Matrix4 modelViewMatrix = new Matrix4();

  private boolean attenuate;

  public boolean isAttenuate() { return attenuate; }

  public void setAttenuate(final boolean attenuate) { this.attenuate = attenuate; }

  public float getConstant() { return constant; }

  public void setConstant(final float constant) { this.constant = constant; }

  public float getLinear() { return linear; }

  public void setLinear(final float linear) { this.linear = linear; }

  public float getQuadratic() { return quadratic; }

  public void setQuadratic(final float quadratic) { this.quadratic = quadratic; }

  public float getSpotExponent() { return spotExponent; }

  public void setSpotExponent(final float exponent) { spotExponent = exponent; }

  public float getSpotCutoff() { return spotCutoff; }

  public void setSpotCutoff(final float spotCutoff) { this.spotCutoff = spotCutoff; }

  public boolean isEnabled() { return enabled; }

  public void setEnabled(final boolean enabled) { this.enabled = enabled; }

  @Override
  public void invalidate() {
    super.invalidate();

    ambient.set(-1, -1, -1, -1);
    diffuse.set(-1, -1, -1, -1);
    specular.set(-1, -1, -1, -1);
    constant = -1;
    linear = -1;
    quadratic = -1;
    spotExponent = -1;
    spotCutoff = -1;
    enabled = false;

    position.set(-1, -1, -1, -1);
    modelViewMatrix.setIdentity();
  }
}
