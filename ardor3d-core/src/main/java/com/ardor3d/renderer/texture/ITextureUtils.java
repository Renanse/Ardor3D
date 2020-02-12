/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.texture;

import java.nio.ByteBuffer;
import java.util.Collection;

import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture1D;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureCubeMap;

public interface ITextureUtils {

    /**
     * Loads a texture onto the card for the current OpenGL context.
     *
     * @param texture
     *            the texture obejct to load.
     * @param unit
     *            the texture unit to load into
     */
    void loadTexture(Texture texture, int unit);

    /**
     * Explicitly remove this Texture from the graphics card. Note, the texture is only removed for the current context.
     * If the texture is used in other contexts, those uses are not touched. If the texture is not used in this context,
     * this is a no-op.
     *
     * @param texture
     *            the Texture object to remove.
     */
    void deleteTexture(Texture texture);

    /**
     * Removes the given texture ids from the current OpenGL context.
     *
     * @param ids
     *            a list/set of ids to remove.
     */
    void deleteTextureIds(Collection<Integer> ids);

    /**
     * Update all or a portion of an existing one dimensional texture object.
     *
     * @param destination
     *            the texture to update. Should already have been sent to the card (have a valid texture id.)
     * @param dstOffsetX
     *            the offset into the destination to start our update.
     * @param dstWidth
     *            the width of the region to update.
     * @param source
     *            the data to update from.
     * @param srcOffsetX
     *            the optional offset into our source data.
     */
    void updateTexture1DSubImage(Texture1D destination, int dstOffsetX, int dstWidth, ByteBuffer source,
            int srcOffsetX);

    /**
     * Update all or a portion of an existing two dimensional texture object.
     *
     * @param destination
     *            the texture to update. Should already have been sent to the card (have a valid texture id.)
     * @param dstOffsetX
     *            the x offset into the destination to start our update.
     * @param dstOffsetY
     *            the y offset into the destination to start our update.
     * @param dstWidth
     *            the width of the region to update.
     * @param dstHeight
     *            the height of the region to update.
     * @param source
     *            the data to update from.
     * @param srcOffsetX
     *            the optional X offset into our source data.
     * @param srcOffsetY
     *            the optional Y offset into our source data.
     * @param srcTotalWidth
     *            the total width of our source data, so we can properly walk through it.
     */
    void updateTexture2DSubImage(Texture2D destination, int dstOffsetX, int dstOffsetY, int dstWidth, int dstHeight,
            ByteBuffer source, int srcOffsetX, int srcOffsetY, int srcTotalWidth);

    /**
     * Update all or a portion of an existing one dimensional texture object.
     *
     * @param destination
     *            the texture to update. Should already have been sent to the card (have a valid texture id.)
     * @param dstOffsetX
     *            the x offset into the destination to start our update.
     * @param dstOffsetY
     *            the y offset into the destination to start our update.
     * @param dstOffsetZ
     *            the z offset into the destination to start our update.
     * @param dstWidth
     *            the width of the region to update.
     * @param dstHeight
     *            the height of the region to update.
     * @param dstDepth
     *            the depth of the region to update. eg. 1 == one slice
     * @param source
     *            the data to update from.
     * @param srcOffsetX
     *            the optional X offset into our source data.
     * @param srcOffsetY
     *            the optional Y offset into our source data.
     * @param srcOffsetZ
     *            the optional Z offset into our source data.
     * @param srcTotalWidth
     *            the total width of our source data, so we can properly walk through it.
     * @param srcTotalHeight
     *            the total height of our source data, so we can properly walk through it.
     */
    void updateTexture3DSubImage(Texture3D destination, int dstOffsetX, int dstOffsetY, int dstOffsetZ, int dstWidth,
            int dstHeight, int dstDepth, ByteBuffer source, int srcOffsetX, int srcOffsetY, int srcOffsetZ,
            int srcTotalWidth, int srcTotalHeight);

    /**
     * Update all or a portion of a single two dimensional face on an existing cubemap texture object.
     *
     * @param destination
     *            the texture to update. Should already have been sent to the card (have a valid texture id.)
     * @param dstFace
     *            the face to update.
     * @param dstOffsetX
     *            the x offset into the destination to start our update.
     * @param dstOffsetY
     *            the y offset into the destination to start our update.
     * @param dstWidth
     *            the width of the region to update.
     * @param dstHeight
     *            the height of the region to update.
     * @param source
     *            the data to update from.
     * @param srcOffsetX
     *            the optional X offset into our source data.
     * @param srcOffsetY
     *            the optional Y offset into our source data.
     * @param srcTotalWidth
     *            the total width of our source data, so we can properly walk through it.
     */
    void updateTextureCubeMapSubImage(TextureCubeMap destination, TextureCubeMap.Face dstFace, int dstOffsetX,
            int dstOffsetY, int dstWidth, int dstHeight, ByteBuffer source, int srcOffsetX, int srcOffsetY,
            int srcTotalWidth);

}
