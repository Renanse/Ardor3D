/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image;

public enum ImageDataFormat {
    RG(2, false, false), //
    RGB(3, false, false), //
    RGBA(4, false, true), //
    BGR(3, false, false), //
    BGRA(4, false, true), //
    Alpha(1, false, true), //
    Red(1, false, false), //
    Green(1, false, false), //
    Blue(1, false, false), //
    StencilIndex(1, false, false), //
    Depth(1, false, false), //
    PrecompressedDXT1(1, true, false), //
    PrecompressedDXT1A(1, true, true), //
    PrecompressedDXT3(2, true, true), //
    PrecompressedDXT5(2, true, true), //
    PrecompressedLATC_L(1, true, true), //
    PrecompressedLATC_LA(2, true, true);

    private final int _components;
    private final boolean _compressed;
    private final boolean _hasAlpha;

    ImageDataFormat(final int components, final boolean isCompressed, final boolean hasAlpha) {
        _components = components;
        _compressed = isCompressed;
        _hasAlpha = hasAlpha;
    }

    public int getComponents() {
        return _components;
    }

    public boolean isCompressed() {
        return _compressed;
    }

    public boolean hasAlpha() {
        return _hasAlpha;
    }
}
