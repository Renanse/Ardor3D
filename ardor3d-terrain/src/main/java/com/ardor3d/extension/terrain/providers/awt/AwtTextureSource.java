/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.awt;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ardor3d.extension.terrain.client.TextureConfiguration;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyVector4;
import com.ardor3d.util.geom.BufferUtils;

public class AwtTextureSource implements TextureSource, ElementUpdateListener {
    private static final int tileSize = 128;
    private final int availableClipmapLevels;

    private final AwtElementProvider provider;
    private final TextureStoreFormat format;
    private final boolean hasAlpha;

    private final BufferedImage _image[];
    private final Set<Tile> _updatedTiles[];

    private final ThreadLocal<byte[]> tileDataPool = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[tileSize * tileSize * (hasAlpha ? 4 : 3)];
        }
    };

    @SuppressWarnings("unchecked")
    public AwtTextureSource(final int availableClipmapLevels, final TextureStoreFormat format) {
        if (format != TextureStoreFormat.RGB8 && format != TextureStoreFormat.RGBA8) {
            throw new IllegalArgumentException("Only RGB8 and RGBA8 currently supported.");
        }

        this.availableClipmapLevels = availableClipmapLevels;
        this.format = format;
        hasAlpha = format == TextureStoreFormat.RGBA8;

        _image = new BufferedImage[availableClipmapLevels];
        _updatedTiles = new Set[availableClipmapLevels];

        provider = new AwtElementProvider();
        provider.addElementUpdateListener(this);

        for (int i = 0; i < availableClipmapLevels; i++) {
            _image[i] = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
            _updatedTiles[i] = new HashSet<>();
        }
    }

    public AwtElementProvider getProvider() {
        return provider;
    }

    @Override
    public TextureConfiguration getConfiguration() throws Exception {
        final Map<Integer, TextureStoreFormat> textureStoreFormat = new HashMap<>();
        textureStoreFormat.put(0, format);

        return new TextureConfiguration(availableClipmapLevels, textureStoreFormat, tileSize, 1f, false, true);
    }

    @Override
    public Set<Tile> getValidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
            final int numTilesY) throws Exception {
        return null;
    }

    @Override
    public Set<Tile> getInvalidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
            final int numTilesY) throws Exception {
        final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

        if (_updatedTiles[baseClipmapLevel].isEmpty()) {
            return null;
        }

        final Set<Tile> tiles = new HashSet<>();

        int checkX, checkY;
        for (final Iterator<Tile> it = _updatedTiles[baseClipmapLevel].iterator(); it.hasNext();) {
            final Tile tile = it.next();
            checkX = tile.getX();
            checkY = tile.getY();
            if (checkX >= tileX && checkX < tileX + numTilesX && checkY >= tileY && checkY < tileY + numTilesY) {
                tiles.add(tile);
                it.remove();
            }
        }

        return tiles;
    }

    @Override
    public int getContributorId(final int clipmapLevel, final Tile sourceTile) {
        return 0;
    }

    @Override
    public ByteBuffer getTile(final int clipmapLevel, final Tile tile) throws Exception {
        final int tileX = tile.getX();
        final int tileY = tile.getY();

        final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

        // build a transform that would take us to the local space of the tile.
        final Transform localTransform = new Transform();
        localTransform.setTranslation(-tileX * tileSize, -tileY * tileSize, 0);
        final double scale = 1.0 / MathUtils.pow2(baseClipmapLevel);
        localTransform.setScale(scale);

        final double tileInScale = MathUtils.pow2(baseClipmapLevel) * tileSize;
        final double minX = tileInScale * tileX;
        final double minY = tileInScale * tileY;
        final double maxX = minX + tileInScale - 1;
        final double maxY = minY + tileInScale - 1;

        // Clear image
        final Graphics2D graphics = (Graphics2D) _image[baseClipmapLevel].getGraphics();
        final Composite composite = graphics.getComposite();
        // TODO: Add: do regular clear with black if no alpha channel right?
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        graphics.fillRect(0, 0, tileSize, tileSize);
        graphics.setComposite(composite);

        // get list of elements that intersect the given region
        final List<AbstractAwtElement> elements = new ArrayList<>(provider.getElements());
        for (final Iterator<AbstractAwtElement> it = elements.iterator(); it.hasNext();) {
            final AbstractAwtElement element = it.next();

            // check bounds to toss it or keep it.
            final ReadOnlyVector4 bounds = element.getBounds();

            if (bounds.getX() > maxX || bounds.getX() + bounds.getZ() <= minX || bounds.getY() > maxY
                    || bounds.getY() + bounds.getW() <= minY) {
                // toss it
                it.remove();
            }
        }

        // make our buffer - init to all 0's
        final ByteBuffer data = BufferUtils.createByteBufferOnHeap(tileSize * tileSize * (hasAlpha ? 4 : 3));

        // shortcut - no data, return buffer.
        if (elements.isEmpty()) {
            // graphics.setBackground((tileX + tileY) % 2 == 0 ? Color.GREEN : Color.GRAY);
            // graphics.clearRect(0, 0, tileSize, tileSize);
            return data;
        }

        // otherwise draw... for each element, apply to our image.
        for (final AbstractAwtElement element : elements) {
            element.drawTo(_image[baseClipmapLevel], localTransform, clipmapLevel);
        }

        // if (clipmapLevel == 0) {
        // graphics.setBackground((tileX + tileY) % 2 == 0 ? Color.GREEN : Color.GRAY);
        // graphics.clearRect(0, 0, tileSize, tileSize);
        // }

        // grab image contents to buffer
        final byte[] byteArray = tileDataPool.get();
        final DataBufferInt dataBuffer = (DataBufferInt) _image[baseClipmapLevel].getData().getDataBuffer();
        final int[] tmpData = dataBuffer.getData();
        int index = 0;
        for (int i = 0; i < tileSize * tileSize; i++) {
            final int argb = tmpData[i];
            byteArray[index++] = (byte) (argb >> 16 & 0xFF);
            byteArray[index++] = (byte) (argb >> 8 & 0xFF);
            byteArray[index++] = (byte) (argb & 0xFF);
            if (hasAlpha) {
                byteArray[index++] = (byte) (argb >> 24 & 0xFF);
            }
        }

        data.put(byteArray);
        data.flip();

        // return buffer
        return data;
    }

    @Override
    public void elementUpdated(final ReadOnlyVector4 oldBounds, final ReadOnlyVector4 newBounds) {
        addTiles(oldBounds);
        addTiles(newBounds);
    }

    protected void addTiles(final ReadOnlyVector4 bounds) {
        for (int i = 0; i < availableClipmapLevels; i++) {
            final double scale = 1.0 / (tileSize * MathUtils.pow2(i));
            final int minX = (int) MathUtils.floor(bounds.getX() * scale);
            final int minY = (int) MathUtils.floor(bounds.getY() * scale);
            final int maxX = (int) MathUtils.floor((bounds.getX() + bounds.getZ() - 1) * scale);
            final int maxY = (int) MathUtils.floor((bounds.getY() + bounds.getW() - 1) * scale);

            Tile tile;
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    tile = new Tile(x, y);
                    _updatedTiles[i].add(tile);
                }
            }
        }
    }
}
