/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.util.nvtristrip;

/**
 * Ported from <a href="http://developer.nvidia.com/object/nvtristrip_library.html">NVIDIA's NvTriStrip Library</a>
 */
final class NvEdgeInfo {
    long _refCount;
    NvFaceInfo _face0, _face1;
    int _v0, _v1;
    NvEdgeInfo _nextV0, _nextV1;

    // constructor puts 1 ref on us
    NvEdgeInfo(final int v0, final int v1) {
        _v0 = v0;
        _v1 = v1;
        _face0 = null;
        _face1 = null;
        _nextV0 = null;
        _nextV1 = null;

        // we will appear in 2 lists. this is a good
        // way to make sure we delete it the second time
        // we hit it in the edge infos
        _refCount = 2;
    }
}
