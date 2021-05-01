/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.loader.dds;

import static com.ardor3d.image.loader.dds.DdsUtils.isSet;

import java.io.IOException;
import java.util.logging.Logger;

import com.ardor3d.util.LittleEndianDataInput;

class DdsHeader {
  private static final Logger logger = Logger.getLogger(DdsHeader.class.getName());

  // ---- VALUES USED IN dwFlags ----
  // Required caps flag.
  final static int DDSD_CAPS = 0x1;
  // Required caps flag.
  final static int DDSD_HEIGHT = 0x2;
  // Required caps flag.
  final static int DDSD_WIDTH = 0x4;
  // Required when pitch is provided for an uncompressed texture.
  final static int DDSD_PITCH = 0x8;
  // Required caps flag.
  final static int DDSD_PIXELFORMAT = 0x1000;
  // Required in a mipmapped texture.
  final static int DDSD_MIPMAPCOUNT = 0x20000;
  // Required when pitch is provided for a compressed texture.
  final static int DDSD_LINEARSIZE = 0x80000;
  // Required in a depth texture.
  final static int DDSD_DEPTH = 0x800000;
  // ---- /end VALUES USED IN dwFlags ----

  // ---- VALUES USED IN dwCaps ----
  // Optional; must be used on any file that contains more than one surface (a mipmap, a cubic
  // environment map, or
  // volume texture).
  final static int DDSCAPS_COMPLEX = 0x8;
  // Optional; should be used for a mipmap.
  final static int DDSCAPS_MIPMAP = 0x400000;
  // Required caps flag.
  final static int DDSCAPS_TEXTURE = 0x1000;
  // ---- /end VALUES USED IN dwCaps ----

  // ---- VALUES USED IN dwCaps2 ----
  // Required for a cube map.
  final static int DDSCAPS2_CUBEMAP = 0x200;
  // Required when these surfaces are stored in a cube map.
  final static int DDSCAPS2_CUBEMAP_POSITIVEX = 0x400;
  // Required when these surfaces are stored in a cube map.
  final static int DDSCAPS2_CUBEMAP_NEGATIVEX = 0x800;
  // Required when these surfaces are stored in a cube map.
  final static int DDSCAPS2_CUBEMAP_POSITIVEY = 0x1000;
  // Required when these surfaces are stored in a cube map.
  final static int DDSCAPS2_CUBEMAP_NEGATIVEY = 0x2000;
  // Required when these surfaces are stored in a cube map.
  final static int DDSCAPS2_CUBEMAP_POSITIVEZ = 0x4000;
  // Required when these surfaces are stored in a cube map.
  final static int DDSCAPS2_CUBEMAP_NEGATIVEZ = 0x8000;
  // Required for a volume texture.
  final static int DDSCAPS2_VOLUME = 0x200000;
  // ---- /end VALUES USED IN dwCaps2 ----

  int dwSize;
  int dwFlags;
  int dwHeight;
  int dwWidth;
  int dwLinearSize;
  int dwDepth;
  int dwMipMapCount;
  int dwAlphaBitDepth;
  int[] dwReserved1 = new int[10];
  DdsPixelFormat ddpf;
  int dwCaps;
  int dwCaps2;
  int dwCaps3;
  int dwCaps4;
  int dwTextureStage;

  static DdsHeader read(final LittleEndianDataInput in) throws IOException {
    final DdsHeader header = new DdsHeader();
    header.dwSize = in.readInt();
    if (header.dwSize != 124) {
      throw new Error("invalid dds header size: " + header.dwSize);
    }
    header.dwFlags = in.readInt();
    header.dwHeight = in.readInt();
    header.dwWidth = in.readInt();
    header.dwLinearSize = in.readInt();
    header.dwDepth = in.readInt();
    header.dwMipMapCount = in.readInt();
    header.dwAlphaBitDepth = in.readInt();
    for (int i = 0; i < header.dwReserved1.length; i++) {
      header.dwReserved1[i] = in.readInt();
    }
    header.ddpf = DdsPixelFormat.read(in);
    header.dwCaps = in.readInt();
    header.dwCaps2 = in.readInt();
    header.dwCaps3 = in.readInt();
    header.dwCaps4 = in.readInt();
    header.dwTextureStage = in.readInt();

    final int expectedMipmaps = 1 + (int) Math.ceil(Math.log(Math.max(header.dwHeight, header.dwWidth)) / Math.log(2));

    if (isSet(header.dwCaps, DDSCAPS_MIPMAP)) {
      if (!isSet(header.dwFlags, DDSD_MIPMAPCOUNT)) {
        header.dwMipMapCount = expectedMipmaps;
      } else if (header.dwMipMapCount != expectedMipmaps) {
        logger.fine("Got " + header.dwMipMapCount + " mipmaps, expected " + expectedMipmaps);
      }
    } else {
      header.dwMipMapCount = 1;
    }

    return header;
  }
}
