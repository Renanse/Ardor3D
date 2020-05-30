/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.atlas;

import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture.WrapMode;

public class AtlasTextureParameter {
  public MinificationFilter minificationFilter = MinificationFilter.Trilinear;
  public MagnificationFilter magnificationFilter = MagnificationFilter.Bilinear;
  public WrapMode wrapMode = WrapMode.EdgeClamp;
  public boolean compress = false;
}
