/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.ardor3d.extension.terrain.util.BresenhamYUpGridTracer;
import com.ardor3d.extension.terrain.util.PriorityExecutors;
import com.ardor3d.extension.terrain.util.TerrainGridCachePanel;
import com.ardor3d.extension.terrain.util.TextureGridCachePanel;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class TerrainBuilder {

    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(TerrainBuilder.class.getName());

    public static int MAX_PICK_CHECKS = 500;

    protected BuildConfiguration buildConfig = new BuildConfiguration();
    protected List<TextureSource> extraTextureSources = new ArrayList<>();

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
        buildConfig.terrainDataProvider = terrainDataProvider;
        buildConfig.camera = camera;

        buildConfig.tileThreadService = threadService;
    }

    public TerrainBuilder addTextureConnection(final TextureSource textureSource) {
        extraTextureSources.add(textureSource);
        return this;
    }

    public TerrainBuilder withCacheBufferSize(final int size) {
        buildConfig.cacheBufferSize = size;
        return this;
    }

    public int getCacheBufferSize() {
        return buildConfig.cacheBufferSize;
    }

    public TerrainBuilder withClipmapTerrainCount(final int clipmapTerrainCount) {
        buildConfig.clipmapTerrainCount = clipmapTerrainCount;
        return this;
    }

    public int getClipmapTerrainCount() {
        return buildConfig.clipmapTerrainCount;
    }

    public TerrainBuilder withClipmapTerrainSize(final int clipmapTerrainSize) {
        buildConfig.clipmapTerrainSize = clipmapTerrainSize;
        return this;
    }

    public int getClipmapTerrainSize() {
        return buildConfig.clipmapTerrainSize;
    }

    public TerrainBuilder withClipmapTextureCount(final int clipmapTextureCount) {
        buildConfig.clipmapTextureCount = clipmapTextureCount;
        return this;
    }

    public int getClipmapTextureCount() {
        return buildConfig.clipmapTextureCount;
    }

    public TerrainBuilder withClipmapTextureSize(final int clipmapTextureSize) {
        buildConfig.clipmapTextureSize = clipmapTextureSize;
        return this;
    }

    public int getClipmapTextureSize() {
        return buildConfig.clipmapTextureSize;
    }

    public TerrainBuilder withShowDebugPanels(final boolean showDebugPanels) {
        buildConfig.showDebugPanels = showDebugPanels;
        return this;
    }

    public boolean isShowDebugPanels() {
        return buildConfig.showDebugPanels;
    }

    public TerrainBuilder withMapId(final int mapId) {
        buildConfig.mapId = mapId;
        return this;
    }

    public int getMapId() {
        return buildConfig.mapId;
    }

    public Terrain build() throws Exception {
        final Map<Integer, String> availableMaps = buildConfig.terrainDataProvider.getAvailableMaps();
        if (availableMaps.isEmpty()) {
            throw new Exception("No available maps found on this terrain provider.");
        }

        int selectedMapId = 0;
        if (buildConfig.mapId < 0) {
            selectedMapId = availableMaps.keySet().iterator().next();
        } else {
            if (!availableMaps.containsKey(buildConfig.mapId)) {
                throw new IllegalArgumentException(
                        buildConfig.mapId + " is not a valid terrain ID on this terrain provider.");
            }
            selectedMapId = buildConfig.mapId;
        }

        final TerrainSource terrainSource = buildConfig.terrainDataProvider.getTerrainSource(selectedMapId);
        final Terrain terrain = buildTerrainSystem(terrainSource, buildConfig);

        final TextureSource textureSource = buildConfig.terrainDataProvider.getTextureSource(selectedMapId);
        if (textureSource != null) {
            terrain.addTextureClipmap(buildTextureClipmap(textureSource, buildConfig));
        }

        for (final TextureSource extraSource : extraTextureSources) {
            terrain.addTextureClipmap(buildTextureClipmap(extraSource, buildConfig));
        }

        final TextureSource normalSource = buildConfig.terrainDataProvider.getNormalMapSource(selectedMapId);
        if (normalSource != null) {
            terrain.setNormalClipmap(buildTextureClipmap(normalSource, buildConfig));
        }

        return terrain;
    }

    public static Terrain buildTerrainSystem(final TerrainSource terrainSource, final BuildConfiguration buildConfig)
            throws Exception {
        final TerrainConfiguration terrainConfiguration = terrainSource.getConfiguration();
        logger.fine(terrainConfiguration.toString());

        final int clipmapLevels = terrainConfiguration.getTotalNrClipmapLevels();
        final int clipLevelCount = Math.min(clipmapLevels, buildConfig.clipmapTerrainCount);

        final int tileSize = terrainConfiguration.getCacheGridSize();

        int cacheSize = (buildConfig.clipmapTerrainSize + 1) / tileSize + buildConfig.cacheBufferSize;
        cacheSize += cacheSize & 1 ^ 1;

        logger.fine("server clipmapLevels: " + clipmapLevels);

        final List<TerrainCache> cacheList = new ArrayList<>();
        TerrainCache parentCache = null;

        final int baseLevel = Math.max(clipmapLevels - clipLevelCount, 0);
        int meshLevel = clipLevelCount - 1;

        logger.fine("baseLevel: " + baseLevel);
        logger.fine("meshLevel: " + meshLevel);

        for (int i = baseLevel; i < clipmapLevels; i++) {
            final TerrainCache gridCache = new TerrainGridCache(parentCache, cacheSize, terrainSource, tileSize,
                    buildConfig.clipmapTerrainSize, terrainConfiguration, meshLevel--, i,
                    buildConfig.tileThreadService);

            parentCache = gridCache;
            cacheList.add(gridCache);
        }
        Collections.reverse(cacheList);

        final Terrain terrain = new Terrain(buildConfig, cacheList, terrainConfiguration);

        terrain.makePickable(BresenhamYUpGridTracer.class, MAX_PICK_CHECKS, new Vector3(1, 0, 1));

        logger.fine("client clipmapLevels: " + cacheList.size());

        if (buildConfig.showDebugPanels) {
            final TerrainGridCachePanel panel = new TerrainGridCachePanel(cacheList, cacheSize);
            final JFrame frame = new JFrame("Terrain Cache Debug");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.getContentPane().add(panel);
            frame.setBounds(10, 10, panel.getSize().width, panel.getSize().height);
            frame.setVisible(true);
        }

        return terrain;
    }

    public static TextureClipmap buildTextureClipmap(final TextureSource source, final BuildConfiguration buildConfig) {
        final TextureConfiguration textureConfiguration = source.getConfiguration();
        logger.fine(textureConfiguration.toString());

        final int clipmapLevels = textureConfiguration.getTotalNrClipmapLevels();
        final int textureClipLevelCount = Math.min(clipmapLevels, buildConfig.clipmapTextureCount);

        final int tileSize = textureConfiguration.getCacheGridSize();

        int cacheSize = (buildConfig.clipmapTextureSize + 1) / tileSize + buildConfig.cacheBufferSize;
        cacheSize += cacheSize & 1 ^ 1;

        logger.fine("server clipmapLevels: " + clipmapLevels);

        final List<TextureCache> cacheList = new ArrayList<>();
        TextureCache parentCache = null;
        final int baseLevel = Math.max(clipmapLevels - textureClipLevelCount, 0);
        int meshLevel = textureClipLevelCount - 1;

        logger.fine("baseLevel: " + baseLevel);
        logger.fine("meshLevel: " + meshLevel);

        for (int i = baseLevel; i < clipmapLevels; i++) {
            final TextureCache gridCache = new TextureGridCache(parentCache, cacheSize, source, tileSize,
                    buildConfig.clipmapTextureSize, textureConfiguration, meshLevel--, i,
                    buildConfig.tileThreadService);

            parentCache = gridCache;
            cacheList.add(gridCache);
        }
        Collections.reverse(cacheList);

        logger.fine("client clipmapLevels: " + cacheList.size());

        final TextureClipmap textureClipmap = new TextureClipmap(cacheList, buildConfig.clipmapTextureSize,
                textureConfiguration, source);

        if (buildConfig.showDebugPanels) {
            final TextureGridCachePanel panel = new TextureGridCachePanel(cacheList, cacheSize);
            final JFrame frame = new JFrame("Texture Cache Debug");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.getContentPane().add(panel);
            frame.setBounds(10, 120, panel.getSize().width, panel.getSize().height);
            frame.setVisible(true);
        }

        return textureClipmap;
    }

    public class BuildConfiguration {

        public TerrainDataProvider terrainDataProvider;
        public Camera camera;
        public ExecutorService tileThreadService;

        public int cacheBufferSize = 4;
        public int clipmapTerrainCount = 20;
        public int clipmapTerrainSize = 127; // pow2 - 1
        public int clipmapTextureCount = 20;
        public int clipmapTextureSize = 128;

        public boolean showDebugPanels = false;
        public int mapId = -1;

    }
}
