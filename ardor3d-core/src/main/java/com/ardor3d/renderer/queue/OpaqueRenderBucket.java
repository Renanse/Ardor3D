/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.queue;

import java.util.Comparator;

import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.TextureKey;

public class OpaqueRenderBucket extends AbstractRenderBucket {

    public OpaqueRenderBucket() {
        super();

        _comparator = new OpaqueComparator();
    }

    private class OpaqueComparator implements Comparator<Spatial> {
        public int compare(final Spatial o1, final Spatial o2) {
            if (o1 instanceof Mesh && o2 instanceof Mesh) {
                return compareByStates((Mesh) o1, (Mesh) o2);
            }

            final double d1 = distanceToCam(o1);
            final double d2 = distanceToCam(o2);
            if (d1 > d2) {
                return 1;
            } else if (d1 < d2) {
                return -1;
            } else {
                return 0;
            }
        }

        /**
         * Compare opaque items by their texture states - generally the most expensive switch. Later this might expand
         * to comparisons by other states as well, such as lighting or material.
         */
        private int compareByStates(final Mesh mesh1, final Mesh mesh2) {
            final TextureState ts1 = (TextureState) mesh1.getWorldRenderState(RenderState.StateType.Texture);
            final TextureState ts2 = (TextureState) mesh2.getWorldRenderState(RenderState.StateType.Texture);
            if (ts1 == ts2) {
                return 0;
            } else if (ts1 == null && ts2 != null) {
                return -1;
            } else if (ts2 == null && ts1 != null) {
                return 1;
            }

            for (int x = 0, maxIndex = Math.min(ts1.getMaxTextureIndexUsed(), ts2.getMaxTextureIndexUsed()); x <= maxIndex; x++) {

                final TextureKey key1 = ts1.getTextureKey(x);
                final TextureKey key2 = ts2.getTextureKey(x);

                if (key1 == null) {
                    if (key2 == null) {
                        continue;
                    } else {
                        return -1;
                    }
                } else if (key2 == null) {
                    return 1;
                }

                final int tid1 = key1.hashCode();
                final int tid2 = key2.hashCode();

                if (tid1 == tid2) {
                    continue;
                } else if (tid1 < tid2) {
                    return -1;
                } else {
                    return 1;
                }
            }

            if (ts1.getMaxTextureIndexUsed() != ts2.getMaxTextureIndexUsed()) {
                return ts2.getMaxTextureIndexUsed() - ts1.getMaxTextureIndexUsed();
            }

            return 0;
        }
    }

}
