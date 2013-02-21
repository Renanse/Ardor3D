/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
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
final class NvStripStartInfo {
    NvFaceInfo _startFace;
    NvEdgeInfo _startEdge;
    boolean _toV1;

    NvStripStartInfo(final NvFaceInfo startFace, final NvEdgeInfo startEdge, final boolean toV1) {
        _startFace = startFace;
        _startEdge = startEdge;
        _toV1 = toV1;
    }
}
