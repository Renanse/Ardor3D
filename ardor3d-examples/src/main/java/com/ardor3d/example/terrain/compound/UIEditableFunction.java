/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
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

public interface UIEditableFunction {
  void setupFunctionEditPanel(Container parent, ProceduralTerrainSource terrainSource);
}
