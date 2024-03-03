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

import java.util.Comparator;

import com.ardor3d.scenegraph.Spatial;

public class OrthoOrderRenderBucket extends AbstractRenderBucket {

  public OrthoOrderRenderBucket() {
    super();

    _comparator = new OrthoOrderComparator();
  }

  private static class OrthoOrderComparator implements Comparator<Spatial> {
    @Override
    public int compare(final Spatial o1, final Spatial o2) {
      int order1 = o1.getSceneHints().getOrthoOrder();
      int order2 = o2.getSceneHints().getOrthoOrder();
      return Integer.compare(order2, order1);
    }
  }
}
