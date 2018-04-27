/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.swing.JFrame;

import com.ardor3d.extension.terrain.util.BresenhamYUpGridTracer;
import com.ardor3d.extension.terrain.util.PriorityExecutors;
import com.ardor3d.extension.terrain.util.TerrainGridCachePanel;
import com.ardor3d.extension.terrain.util.TextureGridCachePanel;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class TerrainBuilder {
    private int cacheBufferSize = 4;

    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(TerrainBuilder.class.getName());

    public static int MAX_PICK_CHECKS = 500;

    private final TerrainDataProvider terrainDataProvider;
    private final Camera camera;

    private int clipmapTerrainCount = 20;
    private int clipmapTerrainSize = 127; // pow2 - 1
    private int clipmapTextureCount = 20;
    private int clipmapTextureSize = 128;

    private int mapId = -1;

    private boolean showDebugPanels = false;

    private final List<TextureSource> extraTextureSources = Lists.newArrayList();
    private final ExecutorService tileThreadService;

    public TerrainBuilder(final TerrainDataProvider terrainDataProvider, final Camera camera) {
        this(terrainDataProvider, camera, //
                PriorityExecutors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                        new ThreadFactoryBuilder() //
                .setThreadFactory(Executors.defaultThreadFactory())//
                .setDaemon(true).setNameFormat("TileCacheThread-%s")//
                .build()));
    }

    public TerrainBuilder(final TerrainDataProvider terrainDataProvider, final Camera camera,
            final ExecutorService threadService) {
        this.terrainDataProvider = terrainDataProvider;
        this.camera = camera;

        tileThreadService = threadService;
    }

    public void addTextureConnection(final TextureSource textureSource) {
        extraTextureSources.add(textureSource);
    }

    public Terrain build() throws Exception {
        final Map<Integer, String> availableMaps = terrainDataProvider.getAvailableMaps();
        if (availableMaps.isEmpty()) {
            throw new Exception("No available maps found on this terrain provider.");
        }

        int selectedMapId = 0;
        if (mapId < 0) {
            selectedMapId = availableMaps.keySet().iterator().next();
        } else {
            if (!availableMaps.containsKey(mapId)) {
                throw new IllegalArgumentException(mapId + " is not a valid terrain ID on this terrain provider.");
            }
            selectedMapId = mapId;
        }

        final TerrainSource terrainSource = terrainDataProvider.getTerrainSource(selectedMapId);
        final Terrain terrain = buildTerrainSystem(terrainSource);

        final TextureSource textureSource = terrainDataProvider.getTextureSource(selectedMapId);
        if (textureSource != null) {
            terrain.addTextureClipmap(buildTextureSystem(textureSource));

            for (final TextureSource extraSource : extraTextureSources) {
                terrain.addTextureClipmap(buildTextureSystem(extraSource));
            }
        }

        final TextureSource normalSource = terrainDataProvider.getNormalMapSource(selectedMapId);
        if (normalSource != null) {
            terrain.setNormalClipmap(buildTextureSystem(normalSource));
        }

        return terrain;
    }

    private Terrain buildTerrainSystem(final TerrainSource terrainSource) throws Exception {
        final TerrainConfiguration terrainConfiguration = terrainSource.getConfiguration();
        logger.info(terrainConfiguration.toString());

        final int clipmapLevels = terrainConfiguration.getTotalNrClipmapLevels();
        final int clipLevelCount = Math.min(clipmapLevels, clipmapTerrainCount);

        final int tileSize = terrainConfiguration.getCacheGridSize();

        int cacheSize = (clipmapTerrainSize + 1) / tileSize + cacheBufferSize;
        cacheSize += cacheSize & 1 ^ 1;

        logger.info("server clipmapLevels: " + clipmapLevels);

        final List<TerrainCache> cacheList = Lists.newArrayList();
        TerrainCache parentCache = null;

        final int baseLevel = Math.max(clipmapLevels - clipLevelCount, 0);
        int meshLevel = clipLevelCount - 1;

        logger.info("baseLevel: " + baseLevel);
        logger.info("meshLevel: " + meshLevel);

        for (int i = baseLevel; i < clipmapLevels; i++) {
            final TerrainCache gridCache = new TerrainGridCache(parentCache, cacheSize, terrainSource, tileSize,
                    clipmapTerrainSize, terrainConfiguration, meshLevel--, i, tileThreadService);

            parentCache = gridCache;
            cacheList.add(gridCache);
        }
        Collections.reverse(cacheList);

        final Terrain terrain = new Terrain(camera, cacheList, clipmapTerrainSize, terrainConfiguration);

        terrain.makePickable(BresenhamYUpGridTracer.class, MAX_PICK_CHECKS, new Vector3(1, 0, 1));

        logger.info("client clipmapLevels: " + cacheList.size());

        if (showDebugPanels) {
            final TerrainGridCachePanel panel = new TerrainGridCachePanel(cacheList, cacheSize);
            final JFrame frame = new JFrame("Terrain Cache Debug");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(panel);
            frame.setBounds(10, 10, panel.getSize().width, panel.getSize().height);
            frame.setVisible(true);
        }

        return terrain;
    }

    private TextureClipmap buildTextureSystem(final TextureSource textureSource) throws Exception {
        final TextureConfiguration textureConfiguration = textureSource.getConfiguration();
        logger.info(textureConfiguration.toString());

        final int clipmapLevels = textureConfiguration.getTotalNrClipmapLevels();
        final int textureClipLevelCount = Math.min(clipmapLevels, clipmapTextureCount);

        final int tileSize = textureConfiguration.getCacheGridSize();

        int cacheSize = (clipmapTextureSize + 1) / tileSize + cacheBufferSize;
        cacheSize += cacheSize & 1 ^ 1;

        logger.info("server clipmapLevels: " + clipmapLevels);

        final List<TextureCache> cacheList = Lists.newArrayList();
        TextureCache parentCache = null;
        final int baseLevel = Math.max(clipmapLevels - textureClipLevelCount, 0);
        int meshLevel = textureClipLevelCount - 1;

        logger.info("baseLevel: " + baseLevel);
        logger.info("meshLevel: " + meshLevel);

        for (int i = baseLevel; i < clipmapLevels; i++) {
            final TextureCache gridCache = new TextureGridCache(parentCache, cacheSize, textureSource, tileSize,
                    clipmapTextureSize, textureConfiguration, meshLevel--, i, tileThreadService);

            parentCache = gridCache;
            cacheList.add(gridCache);
        }
        Collections.reverse(cacheList);

        logger.info("client clipmapLevels: " + cacheList.size());

        final TextureClipmap textureClipmap = new TextureClipmap(cacheList, clipmapTextureSize, textureConfiguration);

        if (showDebugPanels) {
            final TextureGridCachePanel panel = new TextureGridCachePanel(cacheList, cacheSize);
            final JFrame frame = new JFrame("Texture Cache Debug");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(panel);
            frame.setBounds(10, 120, panel.getSize().width, panel.getSize().height);
            frame.setVisible(true);
        }

        return textureClipmap;
    }

    public TerrainBuilder setCacheBufferSize(final int size) {
        cacheBufferSize = size;
        return this;
    }

    public int getCacheBufferSize() {
        return cacheBufferSize;
    }

    public TerrainBuilder setClipmapTerrainCount(final int clipmapTerrainCount) {
        this.clipmapTerrainCount = clipmapTerrainCount;
        return this;
    }

    public int getClipmapTerrainCount() {
        return clipmapTerrainCount;
    }

    public TerrainBuilder setClipmapTerrainSize(final int clipmapTerrainSize) {
        this.clipmapTerrainSize = clipmapTerrainSize;
        return this;
    }

    public int getClipmapTerrainSize() {
        return clipmapTerrainSize;
    }

    public TerrainBuilder setClipmapTextureCount(final int clipmapTextureCount) {
        this.clipmapTextureCount = clipmapTextureCount;
        return this;
    }

    public int getClipmapTextureCount() {
        return clipmapTextureCount;
    }

    public TerrainBuilder setClipmapTextureSize(final int clipmapTextureSize) {
        this.clipmapTextureSize = clipmapTextureSize;
        return this;
    }

    public int getClipmapTextureSize() {
        return clipmapTextureSize;
    }

    public TerrainBuilder setShowDebugPanels(final boolean showDebugPanels) {
        this.showDebugPanels = showDebugPanels;
        return this;
    }

    public boolean isShowDebugPanels() {
        return showDebugPanels;
    }

    public TerrainBuilder setMapId(final int mapId) {
        this.mapId = mapId;
        return this;
    }

    public int getMapId() {
        return mapId;
    }
}
