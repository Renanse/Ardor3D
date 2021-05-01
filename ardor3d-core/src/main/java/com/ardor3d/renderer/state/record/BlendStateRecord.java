/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.state.record;

import com.ardor3d.math.ColorRGBA;

public class BlendStateRecord extends StateRecord {
  public boolean blendEnabled = false;

  // RGB or primary
  public int srcFactorRGB = -1;
  public int dstFactorRGB = -1;
  public int blendEqRGB = -1;

  // Alpha
  public int srcFactorAlpha = -1;
  public int dstFactorAlpha = -1;
  public int blendEqAlpha = -1;

  public ColorRGBA blendColor = new ColorRGBA(-1, -1, -1, -1);

  // sample coverage
  public boolean sampleAlphaToCoverageEnabled = false;
  public boolean sampleAlphaToOneEnabled = false;
  public boolean sampleCoverageEnabled = false;
  public boolean sampleCoverageInverted = false;
  public float sampleCoverage = 1f;

  @Override
  public void invalidate() {
    super.invalidate();

    blendEnabled = false;

    srcFactorRGB = -1;
    dstFactorRGB = -1;
    blendEqRGB = -1;

    srcFactorAlpha = -1;
    dstFactorAlpha = -1;
    blendEqAlpha = -1;

    blendColor.set(-1, -1, -1, -1);

    sampleAlphaToCoverageEnabled = false;
    sampleAlphaToOneEnabled = false;
    sampleCoverageEnabled = false;
    sampleCoverageInverted = false;
    sampleCoverage = -1;
  }
}
