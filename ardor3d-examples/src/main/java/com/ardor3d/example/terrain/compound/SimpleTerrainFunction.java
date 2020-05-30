/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.terrain.compound;

import java.awt.Container;

import com.ardor3d.extension.terrain.providers.procedural.ProceduralTerrainSource;
import com.ardor3d.math.functions.FbmFunction3D;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.functions.Functions;

public class SimpleTerrainFunction implements Function3D, UIEditableFunction {

  private Function3D _function;
  private double _scale;

  public SimpleTerrainFunction(final double scale) {
    _scale = scale;
    updateFunction();
  }

  private void updateFunction() {
    _function = Functions.scaleInput( //
        Functions.clamp( //
            new FbmFunction3D(Functions.simplexNoise(), 9, 0.5, 0.5, 3.14), //
            -1.2, 1.2), //
        _scale, _scale, 1);
  }

  @Override
  public double eval(final double x, final double y, final double z) {
    return _function.eval(x, y, z);
  }

  public double getScale() { return _scale; }

  public void setScale(final double scale) {
    _scale = scale;
    updateFunction();
  }

  @Override
  public void setupFunctionEditPanel(final Container parent, final ProceduralTerrainSource terrainSource) {
    // TODO Auto-generated method stub

  }
}
