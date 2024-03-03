/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.geom;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

@Deprecated
public abstract class SceneCopier {

  public static Spatial makeCopy(final Spatial source, final CopyLogic logic) {
    return makeCopy(source, null, logic);
  }

  private static Spatial makeCopy(final Spatial source, final Spatial parent, final CopyLogic logic) {
    final AtomicBoolean recurse = new AtomicBoolean();
    final Spatial result = logic.copy(source, recurse);
    if (recurse.get() && source instanceof Node && result instanceof Node
        && ((Node) source).getNumberOfChildren() > 0) {
      for (final Spatial child : ((Node) source).getChildren()) {
        final Spatial copy = makeCopy(child, result, logic);
        if (copy != null) {
          ((Node) result).attachChild(copy);
        }
      }
    }
    return result;
  }

}
