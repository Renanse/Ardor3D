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

  public static boolean DefaultCastsShadows = false;

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
}
