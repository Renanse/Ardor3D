/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.atlas;

import com.ardor3d.math.Rectangle2;

public class AtlasPacker {
  private final AtlasNode rootNode;

  public AtlasPacker(final int width, final int height) {
    rootNode = new AtlasNode(width, height);
  }

  public AtlasNode insert(final int width, final int height) {
    return rootNode.insert(new Rectangle2(0, 0, width, height));
  }

  public AtlasNode insert(final Rectangle2 image) {
    return rootNode.insert(image);
  }

  public AtlasNode getRootNode() { return rootNode; }
}
