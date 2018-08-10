/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util.dds;

enum D3d10ResourceDimension {
    D3D10_RESOURCE_DIMENSION_UNKNOWN(0), //
    D3D10_RESOURCE_DIMENSION_BUFFER(1), //
    D3D10_RESOURCE_DIMENSION_TEXTURE1D(2), //
    D3D10_RESOURCE_DIMENSION_TEXTURE2D(3), //
    D3D10_RESOURCE_DIMENSION_TEXTURE3D(4); //

    int _value;

    D3d10ResourceDimension(final int value) {
        _value = value;
    }

    static D3d10ResourceDimension forInt(final int value) {
        for (final D3d10ResourceDimension dim : values()) {
            if (dim._value == value) {
                return dim;
            }
        }
        throw new Error("unknown D3D10ResourceDimension: " + value);
    }
}
