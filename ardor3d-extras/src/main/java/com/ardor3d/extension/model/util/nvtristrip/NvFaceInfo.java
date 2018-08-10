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
final class NvFaceInfo {
    int _v0, _v1, _v2;
    int _stripId; // real strip Id
    int _testStripId; // strip Id in an experiment
    int _experimentId; // in what experiment was it given an experiment Id?
    boolean _isFake; // if true, will be deleted when the strip it's in is deleted

    NvFaceInfo(final int v0, final int v1, final int v2) {
        this(v0, v1, v2, false);
    }

    NvFaceInfo(final int v0, final int v1, final int v2, final boolean bIsFake) {
        _v0 = v0;
        _v1 = v1;
        _v2 = v2;
        _stripId = -1;
        _testStripId = -1;
        _experimentId = -1;
        _isFake = bIsFake;
    }

    /**
     * Copies only v0, v1 and v2
     * 
     * @param source
     */
    public NvFaceInfo(final NvFaceInfo source) {
        this(source._v0, source._v1, source._v2);
    }
}
