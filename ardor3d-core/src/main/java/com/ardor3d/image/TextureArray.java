/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.util.Ardor3dException;

public abstract class TextureArray extends Texture {

  protected static void copyTextureDataInto(final TextureArray rVal, final Texture[] textures) {
    if (textures == null || textures.length == 0) {
      return;
    }

    // grab our first texture out and copy params
    // we'll also use this texture to verify our width/height are all identical.
    final var first = textures[0];
    final var firstImg = first.getImage();
    first.createSimpleClone(rVal);

    final var srcData = new ArrayList<ByteBuffer>();
    for (int i = 0; i < textures.length; i++) {
      final var tex = textures[i];
      if (tex == null || tex.getImage() == null) {
        continue;
      }
      final var texImg = tex.getImage();
      if (texImg.getWidth() != firstImg.getWidth() //
          || texImg.getHeight() != firstImg.getHeight() //
          || texImg.getDepth() != firstImg.getDepth() //
          || texImg.getDataFormat() != firstImg.getDataFormat() //
          || texImg.getDataType() != firstImg.getDataType() //
      ) {
        throw new Ardor3dException("All textures must have the same dimensions, format and pixel type.");
      }
      srcData.addAll(tex.getImage().getData());
    }

    final var img = new Image();
    img.setData(BufferUtils.concat(srcData));
    rVal.setImage(img);
  }
}
