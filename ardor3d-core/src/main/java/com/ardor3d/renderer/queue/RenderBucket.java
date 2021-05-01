/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.queue;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Spatial;

public interface RenderBucket {
  void add(Spatial spatial);

  void remove(Spatial spatial);

  void clear();

  void sort();

  void render(Renderer renderer);

  void pushBucket();

  void popBucket();

}
