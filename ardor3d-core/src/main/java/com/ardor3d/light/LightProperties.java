/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.light;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.scenegraph.Spatial;

public final class LightProperties {

  /**
   * Spatial property indicating if a Spatial wants to be lit or not. Should be mapped to a boolean.
   */
  public static final String KEY_ReceiveLight = "_receiveLight";

  public static boolean DefaultReceiveLight = true;

  /**
   * Spatial property indicating if a Spatial should cast a shadow. Should be mapped to a boolean.
   */
  public static final String KEY_CastsShadows = "_castsShadows";

  public static boolean DefaultCastsShadows = true;

  /**
   * Spatial property indicating the ambient light color. Should be mapped to a ReadOnlyColorRGBA.
   * Generally, this would be set at the root node and inherited by children.
   */
  public static final String KEY_AmbientLightColor = "_ambientLightColor";

  public static ReadOnlyColorRGBA DefaultAmbientColor = new ColorRGBA(0.25f, 0.25f, 0.25f, 1.0f);

  private LightProperties() {}

  public static boolean isLightReceiver(final Spatial spat) {
    return spat.getProperty(LightProperties.KEY_ReceiveLight, LightProperties.DefaultReceiveLight);
  }

  public static void setLightReceiver(final Spatial spat, final boolean receivesLight) {
    spat.setProperty(LightProperties.KEY_ReceiveLight, receivesLight);
  }

  public static boolean isShadowCaster(final Spatial spat) {
    return spat.getProperty(LightProperties.KEY_CastsShadows, LightProperties.DefaultCastsShadows);
  }

  public static void setShadowCaster(final Spatial spat, final boolean castsShadow) {
    spat.setProperty(LightProperties.KEY_CastsShadows, castsShadow);
  }

  public static ReadOnlyColorRGBA getAmbientLightColor(final Spatial spat) {
    return spat.getProperty(LightProperties.KEY_AmbientLightColor, LightProperties.DefaultAmbientColor);
  }

  public static void setAmbientLightColor(final Spatial spat, final ReadOnlyColorRGBA color) {
    spat.setProperty(LightProperties.KEY_AmbientLightColor, color);
  }
}
