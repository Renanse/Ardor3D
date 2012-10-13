/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image;

public enum PixelDataType {
    UnsignedByte(1, null, null), //
    Byte(1, null, null), //
    UnsignedShort(2, null, null), //
    Short(2, null, null), //
    UnsignedInt(4, null, null), //
    Int(4, null, null), //
    Float(4, null, null), //
    HalfFloat(2, null, null), //
    UnsignedByte_3_3_2(null, 8, 3), //
    UnsignedByte_2_3_3_Rev(null, 8, 3), //
    UnsignedShort_5_6_5(null, 16, 3), //
    UnsignedShort_5_6_5_Rev(null, 16, 3), //
    UnsignedShort_4_4_4_4(null, 16, 4), //
    UnsignedShort_4_4_4_4_Rev(null, 16, 4), //
    UnsignedShort_5_5_5_1(null, 16, 4), //
    UnsignedShort_1_5_5_5_Rev(null, 16, 4), //
    UnsignedInt_8_8_8_8(null, 32, 4), //
    UnsignedInt_8_8_8_8_Rev(null, 32, 4), //
    UnsignedInt_10_10_10_2(null, 32, 4), //
    UnsignedInt_2_10_10_10_Rev(null, 32, 4);

    final Integer _bytesPerComponent;
    final Integer _bytesPerPixel;
    final Integer _components;

    PixelDataType(final Integer bytesPerComponent, final Integer bytesPerPixel, final Integer components) {
        _bytesPerComponent = bytesPerComponent;
        _bytesPerPixel = bytesPerPixel;
        _components = components;
    }

    public int getBytesPerPixel(final int components) {
        if (_components != null && components != _components.intValue()) {
            throw new IllegalArgumentException("invalid number of components for " + name());
        }

        if (_bytesPerPixel != null) {
            return _bytesPerPixel.intValue();
        } else {
            return components * _bytesPerComponent;
        }
    }
}
