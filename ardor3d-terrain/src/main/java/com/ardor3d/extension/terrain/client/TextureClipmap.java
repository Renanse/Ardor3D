/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.terrain.util.DoubleBufferedList;
import com.ardor3d.extension.terrain.util.LevelData;
import com.ardor3d.extension.terrain.util.Region;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture3D;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.ShaderType;
import com.ardor3d.renderer.state.ShaderState;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * An implementation of texture clipmapping
 */
public class TextureClipmap {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(TextureClipmap.class.getName());

    private final TextureSource source;
    private final int textureSize;
    private final int textureLevels;
    private final int validLevels;
    private int currentShownLevels;
    private int minVisibleLevel = 0;

    private float scale = 1f;

    private Texture3D textureClipmap;
    private ShaderState textureClipmapShader;

    private final List<LevelData> levelDataList = Lists.newArrayList();

    private final FloatBuffer sliceDataBuffer;

    private final Vector3 eyePosition = new Vector3();

    private boolean showDebug = false;
    private boolean enabled = true;

    private final List<TextureCache> cacheList;

    private final DoubleBufferedList<Region> mailBox = new DoubleBufferedList<Region>();

    private final boolean useAlpha;
    private final int colorBits;

    /** Timers for mailbox updates */
    private long oldTime = 0;
    private long updateTimer = 0;
    private final long updateThreshold = 300;

    private final Comparator<Region> regionSorter = new Comparator<Region>() {
        @Override
        public int compare(final Region r1, final Region r2) {
            return r1.getLevel() - r2.getLevel();
        }
    };

    /**
     * Construct a new TextureClipmap with the given values.
     *
     * @param cacheList
     *            List of caches used to provide the clipmap with texture data.
     * @param textureSize
     *            our
     * @param textureConfiguration
     * @param source
     *            the TextureSource this clipmap represents. This can conceivably be null if the cacheList was built in
     *            some method that did not involve a texturesource (or involved multiple.)
     */
    public TextureClipmap(final List<TextureCache> cacheList, final int textureSize,
            final TextureConfiguration textureConfiguration, final TextureSource source) {
        this.cacheList = cacheList;
        this.textureSize = textureSize;
        this.source = source;
        validLevels = cacheList.size();
        useAlpha = textureConfiguration.isUseAlpha();
        colorBits = useAlpha ? 4 : 3;

        for (final TextureCache cache : cacheList) {
            cache.setMailBox(mailBox);
        }

        textureLevels = roundUpPowerTwo(validLevels);
        scale = textureConfiguration.getTextureDensity() * textureSize / 128;

        TextureClipmap.logger.info("Texture size: " + textureSize);
        TextureClipmap.logger.info("ValidLevels: " + validLevels);
        TextureClipmap.logger.info("3D Texture depth: " + textureLevels);

        sliceDataBuffer = BufferUtils.createFloatBuffer(textureLevels * 2);

        for (int i = 0; i < validLevels; i++) {
            levelDataList.add(new LevelData(i, textureSize));
        }

        createTexture();
    }

    private final List<Long> timers = Lists.newArrayList();

    public void update(final Renderer renderer, final ReadOnlyVector3 position) {
        if (!isEnabled()) {
            return;
        }

        eyePosition.set(position);
        textureClipmapShader.setUniform("eyePosition", eyePosition);
        eyePosition.multiplyLocal(textureSize / (scale * 4f * 32f));

        for (int unit = minVisibleLevel; unit < validLevels; unit++) {
            if (cacheList.get(unit).isValid()) {
                currentShownLevels = unit;
                break;
            }
        }

        // TODO: improve calcs for removing levels based on height above terrain
        // if (eyePosition.getYf() > heightRangeMax) {
        // final float diff = eyePosition.getYf() - heightRangeMax;
        // final float x = (float) (diff * Math.tan(Math.toRadians(80)));
        // for (int unit = currentShownLevels; unit < validLevels; unit++) {
        // final float heightTest = scale * textureSize * MathUtils.pow2(unit) / x;
        // if (heightTest > 1) {
        // currentShownLevels = unit;
        // break;
        // }
        // }
        // }

        textureClipmapShader.setUniform("minLevel", (float) currentShownLevels);

        if (timers.size() < currentShownLevels) {
            for (int unit = 0; unit < currentShownLevels; unit++) {
                timers.add(System.currentTimeMillis());
            }
        }
        for (int unit = 0; unit < currentShownLevels; unit++) {
            final long t = System.currentTimeMillis() - timers.get(unit);
            if (t > 500) {
                timers.set(unit, System.currentTimeMillis());

                float x = eyePosition.getXf();
                float y = eyePosition.getZf();

                final int exp2 = MathUtils.pow2(unit);
                x /= exp2;
                y /= exp2;

                final int offX = MathUtils.floor(x);
                final int offY = MathUtils.floor(y);

                final LevelData levelData = levelDataList.get(unit);

                if (levelData.x != offX || levelData.y != offY) {
                    final TextureCache cache = cacheList.get(unit);
                    cache.setCurrentPosition(offX, offY);
                }
            }
        }

        // Walk through each level of the clipmap
        for (int unit = validLevels - 1; unit >= currentShownLevels; unit--) {
            // calculate the anchor of the clipmap using our eye pos
            float x = eyePosition.getXf();
            float y = eyePosition.getZf();

            final int exp2 = MathUtils.pow2(unit);
            x /= exp2;
            y /= exp2;

            final int offX = MathUtils.floor(x);
            final int offY = MathUtils.floor(y);

            // Get our level data for this level
            final LevelData levelData = levelDataList.get(unit);

            // If our anchor has shifted, update our level data info.
            final TextureCache cache = cacheList.get(unit);
            if (levelData.x != offX || levelData.y != offY) {
                cache.setCurrentPosition(offX, offY);
                // this also calls updateQuick on level
                updateLevel(renderer, levelData, offX, offY);
            }

            // Check for individually updated tiles from our source
            final Set<Tile> updatedTiles = cache.handleUpdateRequests();
            if (updatedTiles != null) {
                final int sX = offX - textureSize / 2;
                final int sY = offY - textureSize / 2;

                // XXX: Perhaps this could find out a subregion of the tile that had changed and update just that, but
                // for now we will update the full level.
                updateQuick(renderer, levelData, textureSize + 1, textureSize + 1, sX, sY, levelData.offsetX,
                        levelData.offsetY, textureSize, textureSize);
            }

            // calculate values used to shift texcoords in shader
            int shiftX = levelData.x;
            int shiftY = levelData.y;
            shiftX = MathUtils.moduloPositive(shiftX, 2);
            shiftY = MathUtils.moduloPositive(shiftY, 2);

            x = (MathUtils.moduloPositive(x, 2) - shiftX + levelData.offsetX) / textureSize;
            y = (MathUtils.moduloPositive(y, 2) - shiftY + levelData.offsetY) / textureSize;

            sliceDataBuffer.put(unit * 2, x);
            sliceDataBuffer.put(unit * 2 + 1, y);
        }

        sliceDataBuffer.rewind();
        textureClipmapShader.setUniform("sliceOffset", sliceDataBuffer, 2);

        updateFromMailbox(renderer);
    }

    private void updateFromMailbox(final Renderer renderer) {

        if (updateTimer < updateThreshold) {
            updateThrottleTimer();
            return;
        }

        final List<Region> regionList = mailBox.switchAndGet();
        if (!regionList.isEmpty()) {
            for (int unit = validLevels - 1; unit >= 0; unit--) {
                final LevelData levelData = levelDataList.get(unit);
                // final int pow = (int) Math.pow(2, unit);
                final int sX = levelData.x - textureSize / 2;
                final int sY = levelData.y - textureSize / 2;
                levelData.clipRegion.setX(sX);
                levelData.clipRegion.setY(sY);
            }

            for (int i = regionList.size() - 1; i >= 0; i--) {
                final Region region = regionList.get(i);
                final Region clipRegion = levelDataList.get(region.getLevel()).clipRegion;

                if (clipRegion.intersects(region)) {
                    clipRegion.intersection(region);
                } else {
                    regionList.remove(i);
                }
            }

            Collections.sort(regionList, regionSorter);

            final int start = regionList.size() - 1;
            for (int i = start; i >= 0; i--) {
                final Region region = regionList.get(i);

                recursiveAddUpdates(regionList, region.getLevel(), region.getX(), region.getY(), region.getWidth(),
                        region.getHeight());
            }

            for (int i = regionList.size() - 1; i >= 0; i--) {
                final Region region = regionList.get(i);
                final Region clipRegion = levelDataList.get(region.getLevel()).clipRegion;

                if (clipRegion.intersects(region)) {
                    clipRegion.intersection(region);
                } else {
                    regionList.remove(i);
                }
            }

            Collections.sort(regionList, regionSorter);

            final Set<Integer> affectedUnits = Sets.newHashSet();
            for (int i = regionList.size() - 1; i >= 0; i--) {
                final Region region = regionList.get(i);

                final int unit = region.getLevel();
                affectedUnits.add(unit);

                final LevelData levelData = levelDataList.get(unit);
                final TextureCache cache = cacheList.get(unit);
                final ByteBuffer imageDestination = levelData.sliceData;

                final int sX = region.getX();
                final int sY = region.getY();
                int dX = region.getX() + textureSize / 2;
                int dY = region.getY() + textureSize / 2;
                dX = MathUtils.moduloPositive(dX, textureSize);
                dY = MathUtils.moduloPositive(dY, textureSize);

                cache.updateRegion(imageDestination, sX, sY, dX + 1, dY + 1, region.getWidth(), region.getHeight());
            }

            for (final int unit : affectedUnits) {
                final LevelData levelData = levelDataList.get(unit);
                final ByteBuffer imageDestination = levelData.sliceData;

                // TODO: only update subpart
                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, 0, unit, textureSize, textureSize, 1,
                        imageDestination, 0, 0, 0, textureSize, textureSize);
            }
        }
        updateTimer %= updateThreshold;
        updateThrottleTimer();
    }

    private void updateThrottleTimer() {
        final long time = System.currentTimeMillis();
        updateTimer += time - oldTime;
        oldTime = time;
    }

    private void recursiveAddUpdates(final List<Region> regionList, final int level, final int x, final int y,
            final int width, final int height) {
        if (level == 0) {
            return;
        }

        final Region region = new Region(level - 1, x * 2, y * 2, width * 2, height * 2);
        if (!regionList.contains(region)) {
            regionList.add(region);
            recursiveAddUpdates(regionList, region.getLevel(), region.getX(), region.getY(), region.getWidth(),
                    region.getHeight());
        }
    }

    private void updateLevel(final Renderer renderer, final LevelData levelData, final int x, final int y) {
        final int diffX = x - levelData.x;
        final int diffY = y - levelData.y;
        levelData.x = x;
        levelData.y = y;

        final int sX = x - textureSize / 2;
        final int sY = y - textureSize / 2;

        levelData.offsetX += diffX;
        levelData.offsetY += diffY;
        levelData.offsetX = MathUtils.moduloPositive(levelData.offsetX, textureSize);
        levelData.offsetY = MathUtils.moduloPositive(levelData.offsetY, textureSize);

        updateQuick(renderer, levelData, diffX, diffY, sX, sY, levelData.offsetX, levelData.offsetY, textureSize,
                textureSize);
    }

    public void regenerate(final Renderer renderer) {
        for (int unit = validLevels - 1; unit >= 0; unit--) {
            float x = eyePosition.getXf();
            float y = eyePosition.getZf();

            final int exp2 = (int) Math.pow(2, unit);
            x /= exp2;
            y /= exp2;

            final int offX = MathUtils.floor(x);
            final int offY = MathUtils.floor(y);

            final LevelData levelData = levelDataList.get(unit);

            final int sX = offX - textureSize / 2;
            final int sY = offY - textureSize / 2;

            updateQuick(renderer, levelData, textureSize + 1, textureSize + 1, sX, sY, levelData.offsetX,
                    levelData.offsetY, textureSize, textureSize);
        }
    }

    private void updateQuick(final Renderer renderer, final LevelData levelData, final int diffX, final int diffY,
            int sX, int sY, int dX, int dY, int width, int height) {
        final int unit = levelData.unit;
        final ByteBuffer imageDestination = levelData.sliceData;

        final TextureCache cache = cacheList.get(unit);

        if (Math.abs(diffX) > textureSize || Math.abs(diffY) > textureSize) {
            // Copy the whole slice
            cache.updateRegion(imageDestination, sX, sY, dX, dY, width, height);

            imageDestination.rewind();
            renderer.updateTexture3DSubImage(textureClipmap, 0, 0, unit, textureSize, textureSize, 1, imageDestination,
                    0, 0, 0, textureSize, textureSize);
        } else if (diffX != 0 && diffY != 0) {
            // Copy three rectangles. Horizontal, vertical and corner

            final int tmpSX = sX;
            final int tmpDX = dX;
            final int tmpWidth = width;

            // Vertical
            if (diffX > 0) {
                sX = sX + textureSize - diffX;
                dX = dX - diffX;
            }
            width = Math.abs(diffX);

            cache.updateRegion(imageDestination, sX, sY, dX, dY, width, height);

            dX = MathUtils.moduloPositive(dX, textureSize);
            if (dX + width > textureSize) {
                int dX1 = dX;
                int width1 = textureSize - dX;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, dX1, 0, unit, width1, textureSize, 1,
                        imageDestination, dX1, 0, 0, textureSize, textureSize);

                dX1 = 0;
                width1 = width - width1;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, dX1, 0, unit, width1, textureSize, 1,
                        imageDestination, dX1, 0, 0, textureSize, textureSize);
            } else {
                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, dX, 0, unit, width, textureSize, 1, imageDestination,
                        dX, 0, 0, textureSize, textureSize);
            }

            sX = tmpSX;
            dX = tmpDX;
            width = tmpWidth;

            // Horizontal
            if (diffY > 0) {
                sY = sY + textureSize - diffY;
                dY = dY - diffY;
            }
            height = Math.abs(diffY);

            cache.updateRegion(imageDestination, sX, sY, dX, dY, width, height);

            dY = MathUtils.moduloPositive(dY, textureSize);
            if (dY + height > textureSize) {
                int dY1 = dY;
                int height1 = textureSize - dY;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, dY1, unit, textureSize, height1, 1,
                        imageDestination, 0, dY1, 0, textureSize, textureSize);

                dY1 = 0;
                height1 = height - height1;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, dY1, unit, textureSize, height1, 1,
                        imageDestination, 0, dY1, 0, textureSize, textureSize);
            } else {
                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, dY, unit, textureSize, height, 1, imageDestination,
                        0, dY, 0, textureSize, textureSize);
            }
        } else if (diffX != 0) {
            // Copy vertical only
            if (diffX > 0) {
                sX = sX + textureSize - diffX;
                dX = dX - diffX;
            }
            width = Math.abs(diffX);

            cache.updateRegion(imageDestination, sX, sY, dX, dY, width, height);

            dX = MathUtils.moduloPositive(dX, textureSize);
            if (dX + width > textureSize) {
                int dX1 = dX;
                int width1 = textureSize - dX;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, dX1, 0, unit, width1, textureSize, 1,
                        imageDestination, dX1, 0, 0, textureSize, textureSize);

                dX1 = 0;
                width1 = width - width1;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, dX1, 0, unit, width1, textureSize, 1,
                        imageDestination, dX1, 0, 0, textureSize, textureSize);
            } else {
                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, dX, 0, unit, width, textureSize, 1, imageDestination,
                        dX, 0, 0, textureSize, textureSize);
            }
        } else if (diffY != 0) {
            // Copy horizontal only
            if (diffY > 0) {
                sY = sY + textureSize - diffY;
                dY = dY - diffY;
            }
            height = Math.abs(diffY);

            cache.updateRegion(imageDestination, sX, sY, dX, dY, width, height);

            dY = MathUtils.moduloPositive(dY, textureSize);
            if (dY + height > textureSize) {
                int dY1 = dY;
                int height1 = textureSize - dY;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, dY1, unit, textureSize, height1, 1,
                        imageDestination, 0, dY1, 0, textureSize, textureSize);

                dY1 = 0;
                height1 = height - height1;

                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, dY1, unit, textureSize, height1, 1,
                        imageDestination, 0, dY1, 0, textureSize, textureSize);
            } else {
                imageDestination.rewind();
                renderer.updateTexture3DSubImage(textureClipmap, 0, dY, unit, textureSize, height, 1, imageDestination,
                        0, dY, 0, textureSize, textureSize);
            }
        }
    }

    public Texture getTexture() {
        return textureClipmap;
    }

    public void reloadShader() {
        textureClipmapShader = new ShaderState();
        try {
            textureClipmapShader.setShader(ShaderType.Vertex, ResourceLocatorTool.getClassPathResourceAsString(
                    TextureClipmap.class, "com/ardor3d/extension/terrain/textureClipmapShader.vert"));
            textureClipmapShader.setShader(ShaderType.Fragment, ResourceLocatorTool.getClassPathResourceAsString(
                    TextureClipmap.class, "com/ardor3d/extension/terrain/textureClipmapShader.frag"));
        } catch (final IOException ex) {
            TextureClipmap.logger.logp(Level.SEVERE, getClass().getName(), "init(Renderer)", "Could not load shaders.",
                    ex);
        }
        textureClipmapShader.setUniform("texture", 0);

        textureClipmapShader.setUniform("scale", 1f / scale);
        textureClipmapShader.setUniform("textureSize", (float) textureSize);
        textureClipmapShader.setUniform("texelSize", 1f / textureSize);

        textureClipmapShader.setUniform("levels", (float) textureLevels);
        textureClipmapShader.setUniform("validLevels", (float) validLevels - 1);
        textureClipmapShader.setUniform("minLevel", 0f);

        textureClipmapShader.setUniform("showDebug", showDebug ? 1.0f : 0.0f);
    }

    public ShaderState getShaderState() {
        if (textureClipmapShader == null) {
            reloadShader();
        }
        return textureClipmapShader;
    }

    public void setShaderState(final ShaderState textureClipmapShader) {
        this.textureClipmapShader = textureClipmapShader;
    }

    public static int clamp(final int x, final int low, final int high) {
        return x < low ? low : x > high ? high : x;
    }

    private Texture createTexture() {
        textureClipmap = new Texture3D();
        textureClipmap.setMinificationFilter(MinificationFilter.NearestNeighborNoMipMaps);
        textureClipmap.setMagnificationFilter(MagnificationFilter.NearestNeighbor);
        // textureClipmap.setMinificationFilter(MinificationFilter.BilinearNoMipMaps);
        // textureClipmap.setMagnificationFilter(MagnificationFilter.Bilinear);
        final Image img = new Image();
        img.setWidth(textureSize);
        img.setHeight(textureSize);
        img.setDepth(textureLevels);
        img.setDataFormat(useAlpha ? ImageDataFormat.RGBA : ImageDataFormat.RGB);
        img.setDataType(PixelDataType.UnsignedByte);
        textureClipmap.setTextureKey(TextureKey.getRTTKey(textureClipmap.getMinificationFilter()));

        for (int l = 0; l < textureLevels; l++) {
            final ByteBuffer sliceData = BufferUtils.createByteBuffer(textureSize * textureSize * colorBits);
            img.setData(l, sliceData);
            if (l < validLevels) {
                levelDataList.get(l).sliceData = sliceData;
            }
        }
        textureClipmap.setImage(img);

        return textureClipmap;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(final float scale) {
        this.scale = scale;
    }

    private int roundUpPowerTwo(int v) {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v++;
        return v;
    }

    /**
     * @return true if this clipmap should be drawn during terrain rendering.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled
     *            true (default) if this clipmap should be drawn during terrain rendering.
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isShowDebug() {
        return showDebug;
    }

    public void setShowDebug(final boolean showDebug) {
        this.showDebug = showDebug;
    }

    public int getTextureSize() {
        return textureSize;
    }

    public int getTextureLevels() {
        return textureLevels;
    }

    public int getValidLevels() {
        return validLevels;
    }

    public TextureSource getSource() {
        return source;
    }

    /**
     * set the minimum (highest resolution) clipmap level visible
     *
     * @param level
     *            clamped to valid range
     */
    public void setMinVisibleLevel(final int level) {
        if (level < 0) {
            minVisibleLevel = 0;
        } else if (level >= validLevels) {
            minVisibleLevel = validLevels - 1;
        } else {
            minVisibleLevel = level;
        }
    }

    public int getMinVisibleLevel() {
        return minVisibleLevel;
    }

    public List<TextureCache> getCacheList() {
        return cacheList;
    }
}
