/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.queue;

import java.util.Comparator;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Spatial;

public class OrthoOrderRenderBucket extends AbstractRenderBucket {

    public OrthoOrderRenderBucket() {
        super();

        _comparator = new OrthoOrderComparator();
    }

    private static class OrthoOrderComparator implements Comparator<Spatial> {
        public int compare(final Spatial o1, final Spatial o2) {
            if (o2.getSceneHints().getOrthoOrder() == o1.getSceneHints().getOrthoOrder()) {
                return 0;
            } else if (o2.getSceneHints().getOrthoOrder() < o1.getSceneHints().getOrthoOrder()) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
