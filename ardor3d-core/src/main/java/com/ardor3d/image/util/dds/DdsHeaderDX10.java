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

import java.io.IOException;

import com.ardor3d.util.LittleEndianDataInput;

class DdsHeaderDX10 {
    final static int D3D10_RESOURCE_MISC_GENERATE_MIPS = 0x1;
    final static int D3D10_RESOURCE_MISC_SHARED = 0x2;
    final static int D3D10_RESOURCE_MISC_TEXTURECUBE = 0x4;
    final static int D3D10_RESOURCE_MISC_SHARED_KEYEDMUTEX = 0x10;
    final static int D3D10_RESOURCE_MISC_GDI_COMPATIBLE = 0x20;

    DxgiFormat dxgiFormat;
    D3d10ResourceDimension resourceDimension;
    int miscFlag;
    int arraySize;
    int reserved;

    static DdsHeaderDX10 read(final LittleEndianDataInput in) throws IOException {
        final DdsHeaderDX10 header = new DdsHeaderDX10();
        header.dxgiFormat = DxgiFormat.forInt(in.readInt());
        header.resourceDimension = D3d10ResourceDimension.forInt(in.readInt());
        header.miscFlag = in.readInt();
        header.arraySize = in.readInt();
        header.reserved = in.readInt();
        return header;
    }
}
