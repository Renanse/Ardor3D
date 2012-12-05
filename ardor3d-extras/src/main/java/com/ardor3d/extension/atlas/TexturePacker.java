/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.atlas;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapAxis;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A tool that uses the AtlasNode/AtlasPacker algorithm to pack textures into texture atlases. It modifies the uv
 * coordinates of the meshes, and tries to pack the atlases in a way that works with the wrap modes of the textures
 * involved.
 * <p>
 * Simple use case:
 * <p>
 * <blockquote>
 * 
 * <pre>
 * // Create a texture atlas packer with maximum atlas size of 256x256
 * final TexturePacker packer = new TexturePacker(256, 256);
 * 
 * // Add meshes into atlas (lots of different ways of doing this if you have other source/target texture indices)
 * packer.insert(mesh1);
 * packer.insert(mesh2);
 * 
 * // Create all the atlases (also possible to set filters etc here through the AtlasTextureParameter)
 * packer.createAtlases();
 * </pre>
 * 
 * </blockquote>
 */
public class TexturePacker {
    private static final Logger logger = Logger.getLogger(TexturePacker.class.getName());

    private final int atlasWidth;
    private final int atlasHeight;
    private int nrTextures = 0;
    private final boolean useAlpha = false;

    private final Map<TextureParameter, List<TextureParameter>> cachedAtlases;
    private final List<AtlasPacker> packers;
    private final List<ByteBuffer> dataBuffers;

    private final List<Texture> textures = Lists.newArrayList();

    public TexturePacker(final int atlasWidth, final int atlasHeight) {
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;

        cachedAtlases = Maps.newHashMap();
        packers = Lists.newArrayList();
        dataBuffers = Lists.newArrayList();

        addPacker();
    }

    public void insert(final Mesh mesh) {
        insert(mesh, 0);
    }

    public void insert(final Mesh mesh, final int textureIndex) {
        insert(mesh, textureIndex, 0);
    }

    public void insert(final Mesh mesh, final int textureIndex, final int targetTextureIndex) {
        final TextureParameter param = new TextureParameter(mesh, textureIndex, targetTextureIndex);
        insert(param);
    }

    public void insert(final TextureParameter parameterObject) {
        if (parameterObject.getTextureCoords() == null) {
            TexturePacker.logger.warning("Skipping mesh! - No texture coords found at index "
                    + parameterObject.getTextureIndex() + " for mesh: " + parameterObject);
            return;
        }
        if (parameterObject.getTexture() == null) {
            TexturePacker.logger.warning("Skipping mesh! - No texture found at index "
                    + parameterObject.getTextureIndex() + " for mesh: " + parameterObject);
            return;
        }
        final ImageDataFormat format = parameterObject.getTexture().getImage().getDataFormat();
        if (format != ImageDataFormat.RGB && format != ImageDataFormat.RGBA) {
            TexturePacker.logger.warning("Skipping mesh! - Only RGB and RGBA texture formats supported currently: "
                    + parameterObject);
            return;
        }

        List<TextureParameter> list = cachedAtlases.get(parameterObject);
        if (list != null) {
            final TextureParameter cachedParameter = list.get(0);

            final float diffX = cachedParameter.getDiffX();
            final float diffY = cachedParameter.getDiffY();
            final float offsetX = cachedParameter.getOffsetX();
            final float offsetY = cachedParameter.getOffsetY();

            final FloatBuffer destination = parameterObject.getTextureCoords();
            for (int i = 0; i < destination.limit(); i += 2) {
                destination.put(i, destination.get(i) * diffX + offsetX);
                destination.put(i + 1, destination.get(i + 1) * diffY + offsetY);
            }

            parameterObject.setAtlasIndex(cachedParameter.getAtlasIndex());

            list.add(parameterObject);

            return;
        }

        final int textureWidth = parameterObject.getWidth();
        final int textureHeight = parameterObject.getHeight();

        final int totalWidth = textureWidth + 2;
        final int totalHeight = textureHeight + 2;

        if (totalWidth > atlasWidth || totalHeight > atlasHeight) {
            System.err.println("Could not fit texture into atlas!");
            return;
        }

        AtlasNode node = null;
        int index = 0;
        for (int i = 0; i < packers.size(); i++) {
            final AtlasPacker packer = packers.get(i);

            node = packer.insert(totalWidth, totalHeight);
            if (node != null) {
                index = i;
                break;
            }
        }

        if (node == null) {
            index = addPacker();
            node = packers.get(index).insert(totalWidth, totalHeight);
        }

        if (node == null) {
            System.err.println("Could not fit texture into any atlas!");
            return;
        }

        list = Lists.newArrayList();
        cachedAtlases.put(parameterObject, list);
        list.add(parameterObject);

        nrTextures++;

        parameterObject.setAtlasIndex(index);

        final Rectangle2 rectangle = node.getRectangle();
        final ByteBuffer data = dataBuffers.get(index);
        final ByteBuffer lightData = parameterObject.getTexture().getImage().getData(0);

        boolean hasAlpha = false;
        if (format == ImageDataFormat.RGBA) {
            hasAlpha = true;
        }

        for (int y = 0; y < textureHeight; y++) {
            for (int x = 0; x < textureWidth; x++) {
                setDataPixel(rectangle, textureWidth, textureHeight, lightData, data, y, x, hasAlpha);
            }
        }

        final WrapMode mode = parameterObject.getTexture().getWrap(WrapAxis.S);
        switch (mode) {
            case BorderClamp:
            case MirrorBorderClamp:
                final ReadOnlyColorRGBA col = parameterObject.getTexture().getBorderColor();
                borderClamp(data, rectangle, textureWidth, textureHeight, parameterObject, col);
                break;

            case Clamp:
            case MirrorClamp:
                borderClamp(data, rectangle, textureWidth, textureHeight, parameterObject, ColorRGBA.BLACK);
                break;

            case EdgeClamp:
            case MirrorEdgeClamp:
                edgeClamp(data, rectangle, textureWidth, textureHeight, parameterObject);
                break;

            case MirroredRepeat:
            case Repeat:
                repeat(data, rectangle, textureWidth, textureHeight, parameterObject);
                break;
            default:
        }

        final float invAtlasWidth = 1f / atlasWidth;
        final float invAtlasHeight = 1f / atlasHeight;

        final float diffX = textureWidth * invAtlasWidth;
        final float diffY = textureHeight * invAtlasHeight;

        final float offsetX = (rectangle.getX() + 1) * invAtlasWidth;
        final float offsetY = (rectangle.getY() + 1) * invAtlasHeight;

        parameterObject.setDiffX(diffX);
        parameterObject.setDiffY(diffY);
        parameterObject.setOffsetX(offsetX);
        parameterObject.setOffsetY(offsetY);

        final FloatBuffer destination = parameterObject.getTextureCoords();
        for (int i = 0; i < destination.limit(); i += 2) {
            destination.put(i, destination.get(i) * diffX + offsetX);
            destination.put(i + 1, destination.get(i + 1) * diffY + offsetY);
        }
    }

    private void repeat(final ByteBuffer data, final Rectangle2 rectangle, final int textureWidth,
            final int textureHeight, final TextureParameter parameterObject) {
        for (int y = 0; y < textureHeight; y++) {
            localCopyBuffer(data, rectangle, textureWidth, y + 1, 0, y + 1);
            localCopyBuffer(data, rectangle, 1, y + 1, textureWidth + 1, y + 1);
        }
        for (int x = 0; x < textureWidth; x++) {
            localCopyBuffer(data, rectangle, x + 1, textureHeight, x + 1, 0);
            localCopyBuffer(data, rectangle, x + 1, 1, x + 1, textureHeight + 1);
        }
        localCopyBuffer(data, rectangle, textureWidth, textureHeight, 0, 0);
        localCopyBuffer(data, rectangle, 1, textureHeight, textureWidth + 1, 0);
        localCopyBuffer(data, rectangle, textureWidth, 1, 0, textureHeight + 1);
        localCopyBuffer(data, rectangle, 1, 1, textureWidth + 1, textureHeight + 1);
    }

    private void edgeClamp(final ByteBuffer data, final Rectangle2 rectangle, final int textureWidth,
            final int textureHeight, final TextureParameter parameterObject) {
        for (int y = 0; y < textureHeight; y++) {
            localCopyBuffer(data, rectangle, 1, y + 1, 0, y + 1);
            localCopyBuffer(data, rectangle, textureWidth, y + 1, textureWidth + 1, y + 1);
        }
        for (int x = 0; x < textureWidth; x++) {
            localCopyBuffer(data, rectangle, x + 1, 1, x + 1, 0);
            localCopyBuffer(data, rectangle, x + 1, textureHeight, x + 1, textureHeight + 1);
        }
        localCopyBuffer(data, rectangle, 1, 1, 0, 0);
        localCopyBuffer(data, rectangle, textureWidth, 1, textureWidth + 1, 0);
        localCopyBuffer(data, rectangle, 1, textureHeight, 0, textureHeight + 1);
        localCopyBuffer(data, rectangle, textureWidth, textureHeight, textureWidth + 1, textureHeight + 1);
    }

    private void borderClamp(final ByteBuffer data, final Rectangle2 rectangle, final int textureWidth,
            final int textureHeight, final TextureParameter parameterObject, final ReadOnlyColorRGBA col) {
        for (int y = 0; y < textureHeight; y++) {
            setColor(data, rectangle, 0, y + 1, col);
            setColor(data, rectangle, textureWidth + 1, y + 1, col);
        }
        for (int x = 0; x < textureWidth; x++) {
            setColor(data, rectangle, x + 1, 0, col);
            setColor(data, rectangle, x + 1, textureHeight + 1, col);
        }
        setColor(data, rectangle, 0, 0, col);
        setColor(data, rectangle, textureWidth + 1, 0, col);
        setColor(data, rectangle, 0, textureHeight + 1, col);
        setColor(data, rectangle, textureWidth + 1, textureHeight + 1, col);
    }

    public void createAtlases() {
        createAtlases(new AtlasTextureParameter());
    }

    public void createAtlases(final AtlasTextureParameter atlasTextureParameter) {
        for (final ByteBuffer data : dataBuffers) {
            data.rewind();

            final ImageDataFormat fmt = useAlpha ? ImageDataFormat.RGBA : ImageDataFormat.RGB;
            final Image image = new Image(fmt, PixelDataType.UnsignedByte, atlasWidth, atlasHeight, data, null);

            final TextureStoreFormat format = atlasTextureParameter.compress ? TextureStoreFormat.GuessCompressedFormat
                    : TextureStoreFormat.GuessNoCompressedFormat;
            final Texture texture = TextureManager.loadFromImage(image, atlasTextureParameter.minificationFilter,
                    format);
            texture.setMagnificationFilter(atlasTextureParameter.magnificationFilter);

            texture.setWrap(atlasTextureParameter.wrapMode);
            texture.setApply(atlasTextureParameter.applyMode);

            textures.add(texture);
        }

        for (final List<TextureParameter> paramList : cachedAtlases.values()) {
            for (final TextureParameter param : paramList) {
                final Texture texture = textures.get(param.getAtlasIndex());
                final TextureState ts = (TextureState) param.getMesh().getLocalRenderState(StateType.Texture);
                ts.setTexture(texture, param.getTargetTextureIndex());
                ts.setNeedsRefresh(true);
            }
        }

        TexturePacker.logger.info(nrTextures + " textures packed into " + packers.size() + " atlases.");
    }

    private int addPacker() {
        final AtlasPacker packer = new AtlasPacker(atlasWidth, atlasHeight);
        packers.add(packer);

        final int size = atlasWidth * atlasHeight * (useAlpha ? 4 : 3); // dimensions * 4 bytes per pixel
        final ByteBuffer data = BufferUtils.createByteBuffer(size);

        dataBuffers.add(data);

        return packers.size() - 1;
    }

    private void localCopyBuffer(final ByteBuffer dataAsFloatBuffer, final Rectangle2 rectangle, final int xFrom,
            final int yFrom, final int xTo, final int yTo) {
        final int componentsTarget = useAlpha ? 4 : 3;

        final int sourceIndex = ((yFrom + rectangle.getY()) * atlasWidth + xFrom + rectangle.getX()) * componentsTarget;
        final int targetIndex = ((yTo + rectangle.getY()) * atlasWidth + xTo + rectangle.getX()) * componentsTarget;

        fillDataBuffer(dataAsFloatBuffer, dataAsFloatBuffer, sourceIndex, targetIndex, useAlpha);
    }

    private void setDataPixel(final Rectangle2 rectangle, final int width, final int height,
            final ByteBuffer lightData, final ByteBuffer dataAsFloatBuffer, final int y, final int x,
            final boolean sourceAlpha) {
        final int componentsSource = sourceAlpha ? 4 : 3;
        final int componentsTarget = useAlpha ? 4 : 3;

        final int sourceIndex = (y * width + x) * componentsSource;
        final int targetIndex = ((y + rectangle.getY() + 1) * atlasWidth + x + rectangle.getX() + 1) * componentsTarget;

        fillDataBuffer(lightData, dataAsFloatBuffer, sourceIndex, targetIndex, sourceAlpha);
    }

    private void fillDataBuffer(final ByteBuffer lightData, final ByteBuffer dataAsFloatBuffer, final int sourceIndex,
            final int targetIndex, final boolean sourceAlpha) {
        dataAsFloatBuffer.put(targetIndex, lightData.get(sourceIndex));
        dataAsFloatBuffer.put(targetIndex + 1, lightData.get(sourceIndex + 1));
        dataAsFloatBuffer.put(targetIndex + 2, lightData.get(sourceIndex + 2));
        if (useAlpha) {
            if (sourceAlpha) {
                dataAsFloatBuffer.put(targetIndex + 3, lightData.get(sourceIndex + 3));
            } else {
                dataAsFloatBuffer.put(targetIndex + 3, (byte) (255 & 0xFF));
            }
        }
    }

    private void setColor(final ByteBuffer dataAsFloatBuffer, final Rectangle2 rectangle, final int x, final int y,
            final ReadOnlyColorRGBA color) {
        final int componentsTarget = useAlpha ? 4 : 3;

        final int targetIndex = ((y + rectangle.getY()) * atlasWidth + x + rectangle.getX()) * componentsTarget;

        setColor(dataAsFloatBuffer, targetIndex, color, useAlpha);
    }

    private void setColor(final ByteBuffer dataAsFloatBuffer, final int targetIndex, final ReadOnlyColorRGBA color,
            final boolean sourceAlpha) {
        dataAsFloatBuffer.put(targetIndex, (byte) (color.getRed() * 255));
        dataAsFloatBuffer.put(targetIndex + 1, (byte) (color.getGreen() * 255));
        dataAsFloatBuffer.put(targetIndex + 2, (byte) (color.getBlue() * 255));
        if (sourceAlpha) {
            dataAsFloatBuffer.put(targetIndex + 3, (byte) (color.getAlpha() * 255));
        }
    }

    public List<Texture> getTextures() {
        return textures;
    }

    public int getAtlasWidth() {
        return atlasWidth;
    }

    public int getAtlasHeight() {
        return atlasHeight;
    }

}
