/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.compound;

import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.providers.compound.function.ICombineFunction;

public class Entry {
  protected final TerrainSource _source;
  protected final ICombineFunction _combine;

  public Entry(final TerrainSource source, final ICombineFunction combine) {
    _source = source;
    _combine = combine;
  }

  public TerrainSource getSource() { return _source; }

  public ICombineFunction getCombine() { return _combine; }

}
