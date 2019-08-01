/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.util.awt;

import java.awt.image.BufferedImage;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.TextureManager;

public abstract class AWTTextureUtil {

    /**
     * Convenience Utility for loading a BufferedImage as an Ardor3D Texture.
     * 
     * @param image
     *            our buffered image
     * @param minFilter
     *            the filter for the near values. Used to determine if we should generate mipmaps.
     * @param flipVertically
     *            If true, the image is flipped vertically during image conversion.
     * @param storeFormat
     *            the specific format to use when storing this texture on the card.
     * @return our new Texture.
     */
    public static final Texture loadTexture(final BufferedImage image, final Texture.MinificationFilter minFilter,
            final TextureStoreFormat storeFormat, final boolean flipVertically) {
        final Image imageData = AWTImageLoader.makeArdor3dImage(image, flipVertically);
        final String fileType = (image != null) ? "" + image.hashCode() : null;
        final TextureKey tkey = TextureKey.getKey(null, flipVertically, storeFormat, fileType, minFilter);
        return TextureManager.loadFromKey(tkey, imageData, null);
    }
}
