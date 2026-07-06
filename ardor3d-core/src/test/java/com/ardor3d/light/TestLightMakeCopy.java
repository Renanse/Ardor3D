/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.light;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ardor3d.math.ColorRGBA;

/**
 * Tests that Spatial.makeCopy on lights preserves light state, not just the base spatial
 * fields. (Before the Light overrides existed, a copied light silently reverted to defaults -
 * white, intensity 1, enabled.)
 */
public class TestLightMakeCopy {

  @Test
  public void pointLightCopiesLightAndAttenuationState() {
    final PointLight light = new PointLight();
    light.setName("point");
    light.setColor(ColorRGBA.RED);
    light.setIntensity(0.3f);
    light.setEnabled(false);
    light.setShadowCaster(true);
    light.setConstant(0.5f);
    light.setLinear(0.25f);
    light.setQuadratic(0.125f);
    light.setRange(42f);

    final PointLight copy = (PointLight) light.makeCopy(true);

    assertEquals("point", copy.getName());
    assertEquals(ColorRGBA.RED, copy.getColor());
    assertEquals(0.3f, copy.getIntensity(), 0f);
    assertFalse(copy.isEnabled());
    assertTrue(copy.isShadowCaster());
    assertEquals(0.5f, copy.getConstant(), 0f);
    assertEquals(0.25f, copy.getLinear(), 0f);
    assertEquals(0.125f, copy.getQuadratic(), 0f);
    assertEquals(42f, copy.getRange(), 0f);
  }

  @Test
  public void spotLightCopiesConeState() {
    final SpotLight light = new SpotLight();
    light.setColor(ColorRGBA.GREEN);
    light.setIntensity(2f);
    light.setAngle(0.75f);
    light.setInnerAngle(0.5f);
    light.setRange(10f);

    final SpotLight copy = (SpotLight) light.makeCopy(true);

    assertEquals(ColorRGBA.GREEN, copy.getColor());
    assertEquals(2f, copy.getIntensity(), 0f);
    assertEquals(0.75f, copy.getAngle(), 0f);
    assertEquals(0.5f, copy.getInnerAngle(), 0f);
    assertEquals(10f, copy.getRange(), 0f);
  }

  @Test
  public void directionalLightCopiesBaseLightState() {
    final DirectionalLight light = new DirectionalLight();
    light.setColor(ColorRGBA.BLUE);
    light.setIntensity(1.5f);
    light.setEnabled(false);

    final DirectionalLight copy = (DirectionalLight) light.makeCopy(true);

    assertEquals(ColorRGBA.BLUE, copy.getColor());
    assertEquals(1.5f, copy.getIntensity(), 0f);
    assertFalse(copy.isEnabled());
  }
}
