/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture.WrapMode;

/**
 * Various enum types to be used when parsing <sampler> Collada tags.
 */
public abstract class SamplerTypes {

    /**
     * Enum matching Collada's texture wrapping modes to Ardor3D's.
     */
    public enum WrapModeType {
        WRAP(WrapMode.Repeat), MIRROR(WrapMode.MirroredRepeat), CLAMP(WrapMode.EdgeClamp), BORDER(WrapMode.BorderClamp), NONE(
                WrapMode.BorderClamp);

        final WrapMode _wm;

        private WrapModeType(final WrapMode ardorWrapMode) {
            _wm = ardorWrapMode;
        }

        public WrapMode getArdor3dWrapMode() {
            return _wm;
        }
    }

    /**
     * Enum matching Collada's texture minification modes to Ardor3D's.
     */
    public enum MinFilterType {
        NONE(MinificationFilter.NearestNeighborNoMipMaps), NEAREST(MinificationFilter.NearestNeighborNoMipMaps), LINEAR(
                MinificationFilter.BilinearNoMipMaps), NEAREST_MIPMAP_NEAREST(
                MinificationFilter.NearestNeighborNearestMipMap), LINEAR_MIPMAP_NEAREST(
                MinificationFilter.BilinearNearestMipMap), NEAREST_MIPMAP_LINEAR(
                MinificationFilter.NearestNeighborLinearMipMap), LINEAR_MIPMAP_LINEAR(MinificationFilter.Trilinear);

        final MinificationFilter _mf;

        private MinFilterType(final MinificationFilter ardorFilter) {
            _mf = ardorFilter;
        }

        public MinificationFilter getArdor3dFilter() {
            return _mf;
        }
    }

    /**
     * Enum matching Collada's texture magnification modes to Ardor3D's.
     */
    public enum MagFilterType {
        NONE(MagnificationFilter.NearestNeighbor), NEAREST(MagnificationFilter.NearestNeighbor), LINEAR(
                MagnificationFilter.Bilinear), NEAREST_MIPMAP_NEAREST(MagnificationFilter.NearestNeighbor), LINEAR_MIPMAP_NEAREST(
                MagnificationFilter.Bilinear), NEAREST_MIPMAP_LINEAR(MagnificationFilter.NearestNeighbor), LINEAR_MIPMAP_LINEAR(
                MagnificationFilter.Bilinear);

        final MagnificationFilter _mf;

        private MagFilterType(final MagnificationFilter ardorFilter) {
            _mf = ardorFilter;
        }

        public MagnificationFilter getArdor3dFilter() {
            return _mf;
        }
    }
}
