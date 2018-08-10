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

class DdsPixelFormat {

    // ---- VALUES USED IN dwFlags ----
    // Texture contains alpha data; dwABitMask contains valid data.
    final static int DDPF_ALPHAPIXELS = 0x1;
    // Used in some older DDS files for alpha channel only uncompressed data (dwRGBBitCount contains the alpha channel
    // bitcount; dwABitMask contains valid data)
    final static int DDPF_ALPHA = 0x2;
    // Texture contains compressed RGB data; dwFourCC contains valid data.
    final static int DDPF_FOURCC = 0x4;
    // Texture contains uncompressed RGB data; dwRGBBitCount and the RGB masks (dwRBitMask, dwGBitMask, dwBBitMask)
    // contain valid data.
    final static int DDPF_RGB = 0x40;
    // Used in some older DDS files for YUV uncompressed data (dwRGBBitCount contains the YUV bit count; dwRBitMask
    // contains the Y mask, dwGBitMask contains the U mask, dwBBitMask contains the V mask)
    final static int DDPF_YUV = 0x200;
    // Used in some older DDS files for single channel color uncompressed data (dwRGBBitCount contains the luminance
    // channel bit count; dwRBitMask contains the channel mask). Can be combined with DDPF_ALPHAPIXELS for a two channel
    // DDS file.
    final static int DDPF_LUMINANCE = 0x20000;
    // ---- /end VALUES USED IN dwFlags ----

    int dwSize;
    int dwFlags;
    int dwFourCC;
    int dwRGBBitCount;
    int dwRBitMask;
    int dwGBitMask;
    int dwBBitMask;
    int dwABitMask;

    static DdsPixelFormat read(final LittleEndianDataInput in) throws IOException {
        final DdsPixelFormat format = new DdsPixelFormat();
        format.dwSize = in.readInt();
        if (format.dwSize != 32) {
            throw new Error("invalid pixel format size: " + format.dwSize);
        }
        format.dwFlags = in.readInt();
        format.dwFourCC = in.readInt();
        format.dwRGBBitCount = in.readInt();
        format.dwRBitMask = in.readInt();
        format.dwGBitMask = in.readInt();
        format.dwBBitMask = in.readInt();
        format.dwABitMask = in.readInt();
        return format;
    }
}
