/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.client;

import java.net.URISyntaxException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.extension.terrain.client.TerrainBuilder.BuildConfiguration;
import com.ardor3d.extension.terrain.util.AbstractBresenhamTracer;
import com.ardor3d.extension.terrain.util.ClipmapTerrainPicker;
import com.ardor3d.extension.terrain.util.DoubleBufferedList;
import com.ardor3d.extension.terrain.util.Region;
import com.ardor3d.intersection.IntersectionRecord;
import com.ardor3d.intersection.Pickable;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * An implementation of geometry clipmapping
 */
public class Terrain extends Node implements Pickable, Runnable {
  private static final long NON_VISIBLE_UPDATE_RATE_MS = 500L;

  /** Our picker. */
  protected ClipmapTerrainPicker _picker = null;

  protected final BuildConfiguration _buildConfig;

  protected List<ClipmapLevel> _clips;
  protected int _visibleLevels = 0;
  protected int _minVisibleLevel = 0;
  protected final Camera _terrainCamera;
  protected final int _clipSideSize;
  protected final TerrainConfiguration _terrainConfiguration;

  protected final BlendState blendState;

  /** Reference to the texture clipmap */
  protected final List<TextureClipmap> _textureClipmaps = new ArrayList<>();

  /** Reference to normal map */
  protected TextureClipmap _normalClipmap;
  protected int _normalUnit;

  protected final Vector3 transformedFrustumPos = new Vector3();

  protected final DoubleBufferedList<Region> mailBox = new DoubleBufferedList<>();

  /** Timers for mailbox updates */
  protected long oldTime = 0;
  protected long updateTimer = 0;
  protected final long updateThreashold = 300;

  protected boolean runCacheThread = true;
  protected Thread cacheThread;

  protected final int CACHE_UPDATE_SLEEP = 250;
  protected final List<Long> _timers = new ArrayList<>();

  protected final TextureState clipTextureState = new TextureState();

  protected final Comparator<Region> regionSorter = (r1, r2) -> r1.getLevel() - r2.getLevel();

  protected final Vector3 _boundsCenter = new Vector3();
  protected final Vector3 _boundsExtents = new Vector3();

  public Terrain(final BuildConfiguration buildConfig, final List<TerrainCache> cacheList,
    final TerrainConfiguration terrainConfiguration) {
    _buildConfig = buildConfig;
    _terrainCamera = buildConfig.camera;
    _clipSideSize = buildConfig.clipmapTerrainSize;
    _terrainConfiguration = terrainConfiguration;

    _worldBound =
        new BoundingBox(Vector3.ZERO, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    getSceneHints().setRenderBucketType(RenderBucketType.Opaque);
    final CullState cs = new CullState();
    cs.setEnabled(true);
    cs.setCullFace(CullState.Face.Back);
    setRenderState(cs);

    blendState = new BlendState();
    blendState.setBlendEnabled(true);
    blendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
    blendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
    setRenderState(blendState);

    setRenderState(clipTextureState);

    // getSceneHints().setLightCombineMode(LightCombineMode.Off);

    try {
      _clips = new ArrayList<>();

      final float heightScale = terrainConfiguration.getScale().getYf();

      for (int i = 0; i < cacheList.size(); i++) {
        final TerrainCache cache = cacheList.get(i);
        cache.setMailBox(mailBox);
        final ClipmapLevel clipmap = new ClipmapLevel(i, _terrainCamera, _clipSideSize, heightScale, cache);
        _clips.add(clipmap);
        attachChild(clipmap);
      }
    } catch (final Exception ex) {
      ex.printStackTrace();
    }

    // setScale(terrainConfiguration.getScale());
    // TODO: hack. unify scale handling over cache etc
    setScale(terrainConfiguration.getScale().getX(), 1, terrainConfiguration.getScale().getZ());
    setHeightRange(terrainConfiguration.getHeightRangeMin(), terrainConfiguration.getHeightRangeMax());

    setProperty("clipSideSize", _clipSideSize);
    setNormalUnit(5);

    this.updateWorldRenderStates(true);
  }

  @Override
  protected void updateChildren(final double time) {
    super.updateChildren(time);

    for (int i = _minVisibleLevel; i < _clips.size(); i++) {
      if (_clips.get(i).isReady()) {
        _visibleLevels = i;
        break;
      }
    }

    // TODO: improve calcs for removing levels based on height above terrain
    // getWorldTransform().applyInverse(_terrainCamera.getLocation(), transformedFrustumPos);
    // final float heightRangeMax = 1f;
    // if (transformedFrustumPos.getYf() > heightRangeMax) {
    // final float diff = transformedFrustumPos.getYf() - heightRangeMax;
    // final float x = (float) (diff * Math.tan(Math.toRadians(30)));
    // for (int unit = _visibleLevels; unit < _clips.size(); unit++) {
    // final float heightTest = _clipSideSize * MathUtils.pow2(unit) / x;
    // if (heightTest > 1) {
    // _visibleLevels = unit;
    // break;
    // }
    // }
    // }

    // Process cache updates for non-visible clip levels
    checkNonVisibleClips();

    // Update from mailbox
    updateFromMailbox();

    // Process vertex updates for visible clipmap levels.
    for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
      _clips.get(i).updateVertices();
    }

    // Update indices.
    for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
      if (i == _visibleLevels) {
        // Level 0 has no nested level, so pass null as parameter.
        _clips.get(i).updateIndices(null);
      } else {
        // All other levels i have the level i-1 nested in.
        _clips.get(i).updateIndices(_clips.get(i - 1));
      }
    }

    if (runCacheThread && cacheThread == null) {
      cacheThread = new Thread(this, "TerrainCacheUpdater");
      cacheThread.setDaemon(true);
      cacheThread.start();
    }
  }

  /**
   * Check clipmap levels below our
   */
  private void checkNonVisibleClips() {
    // Lazy init the contents of _timers
    final long now = System.currentTimeMillis();
    if (_timers.size() < _visibleLevels) {
      for (int unit = 0; unit < _visibleLevels; unit++) {
        _timers.add(now);
      }
    }

    // walk through levels below the visible level
    for (int unit = 0; unit < _visibleLevels; unit++) {
      final long t = now - _timers.get(unit);
      if (t > NON_VISIBLE_UPDATE_RATE_MS) {
        // Enough time has passed. Reset our timer
        _timers.set(unit, now);

        // Ask clip to update its vertex buffer
        _clips.get(unit).updateVertices();
      }
    }
  }

  @Override
  public void run() {
    while (runCacheThread) {
      try {
        TimeUnit.MILLISECONDS.sleep(CACHE_UPDATE_SLEEP);
      } catch (final InterruptedException e) {}

      // check clipmaps
      for (int i = _clips.size(); --i >= 0;) {
        _clips.get(i).getCache().checkForUpdates();
      }

      // check texture clips
      for (int i = 0; i < _textureClipmaps.size(); i++) {
        final TextureClipmap cm = _textureClipmaps.get(i);
        if (!cm.isEnabled()) {
          continue;
        }
        final List<TextureCache> list = cm.getCacheList();
        for (int j = list.size(); --j >= 0;) {
          list.get(j).checkForUpdates();
        }
      }

      // check normalmap, if there is one
      if (_normalClipmap != null && _normalClipmap.isEnabled()) {
        final List<TextureCache> list = _normalClipmap.getCacheList();
        for (int j = list.size(); --j >= 0;) {
          list.get(j).checkForUpdates();
        }
      }
    }
  }

  @Override
  public void draw(final Renderer r) {
    // Figure out where we are
    getWorldTransform().applyInverse(_terrainCamera.getLocation(), transformedFrustumPos);
    setProperty("eyePosition", transformedFrustumPos);

    // If we have a normalmap clip, update and grab the texture for drawing later.
    if (_normalClipmap != null) {
      _normalClipmap.update(r, transformedFrustumPos);
      clipTextureState.setTexture(_normalClipmap.getTexture(), _normalUnit);
    }

    boolean firstDrawnClip = true;
    // Walk through our clipmaps
    for (int i = 0, maxI = _textureClipmaps.size(); i < maxI; i++) {
      final TextureClipmap textureClipmap = _textureClipmaps.get(i);

      // if this clipmap is disabled, ignore it
      if (!textureClipmap.isEnabled()) {
        continue;
      }

      // update clipmap contents
      textureClipmap.update(r, transformedFrustumPos);

      // prepare this clipmap for drawing
      textureClipmap.prepareToDrawClips(this);

      // grab the clipmap's texture for drawing
      clipTextureState.setTexture(textureClipmap.getTexture());

      // If we're the first clip to draw, we won't blend
      blendState.setEnabled(!firstDrawnClip);
      firstDrawnClip = false;

      // If we plan to draw more than one clipmap, push our buckets so we can render just this level
      // independent
      // of anything else.
      if (_textureClipmaps.size() > 1) {
        r.getQueue().pushBuckets();
      }

      // draw levels from coarse to fine.
      for (int j = _clips.size() - 1; j >= _visibleLevels; j--) {
        final ClipmapLevel clip = _clips.get(j);

        if (clip.getStripIndex() > 0) {
          clip.draw(r);
        }
      }

      // Again, if we plan to draw more than one, we pushed our buckets, so render the current buckets and
      // restore
      // what was in them previously.
      if (_textureClipmaps.size() > 1) {
        r.renderBuckets();
        r.getQueue().popBuckets();
      }
    }
  }

  protected void updateFromMailbox() {
    if (updateTimer > updateThreashold) {
      final List<Region> regionList = mailBox.switchAndGet();
      if (!regionList.isEmpty()) {
        for (int i = regionList.size() - 1; i >= 0; i--) {
          final Region region = regionList.get(i);

          final ClipmapLevel clip = _clips.get(region.getLevel());
          final Region clipRegion = clip.getIntersectionRegion();

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

          final ClipmapLevel clip = _clips.get(region.getLevel());
          final Region clipRegion = clip.getIntersectionRegion();

          if (clipRegion.intersects(region)) {
            clipRegion.intersection(region);
          } else {
            regionList.remove(i);
          }
        }

        Collections.sort(regionList, regionSorter);

        for (int i = regionList.size() - 1; i >= 0; i--) {
          final Region region = regionList.get(i);
          final ClipmapLevel clip = _clips.get(region.getLevel());
          final MeshData meshData = clip.getMeshData();
          final FloatBuffer vertices = meshData.getVertexBuffer();
          final int vertexDistance = clip.getVertexDistance();

          clip.getCache().updateRegion(vertices, region.getX() / vertexDistance, region.getY() / vertexDistance,
              region.getWidth() / vertexDistance, region.getHeight() / vertexDistance);

          meshData.markBufferDirty(MeshData.KEY_VertexCoords);
          clip.markDirty(DirtyType.Bounding);
        }
      }
      updateTimer %= updateThreashold;
    }
    final long time = System.currentTimeMillis();
    updateTimer += time - oldTime;
    oldTime = time;
  }

  protected void recursiveAddUpdates(final List<Region> regionList, final int level, final int x, final int y,
      final int width, final int height) {
    if (level == 0) {
      return;
    }

    final Region region = new Region(level - 1, x, y, width, height);
    if (!regionList.contains(region)) {
      regionList.add(region);
      recursiveAddUpdates(regionList, region.getLevel(), region.getX(), region.getY(), region.getWidth(),
          region.getHeight());
    }
  }

  @Override
  public void updateWorldBound(final boolean recurse) {
    final BoundingBox worldBound = (BoundingBox) _worldBound;
    final Vector3 center = _boundsCenter.set(_terrainCamera.getLocation());
    final double distanceToEdge = _clipSideSize * MathUtils.pow2(_clips.size() - 1) * 0.5;
    final double heightScale = _clips.get(0).getHeightScale();
    final double heightMin = _clips.get(0).getHeightRangeMin() * heightScale;
    final double heightMax = _clips.get(0).getHeightRangeMax() * heightScale;

    final Vector3 extents = _boundsExtents.set(distanceToEdge, (heightMax - heightMin) * 0.5, distanceToEdge);
    worldToLocal(center, center);
    worldBound.setXExtent(extents.getX());
    worldBound.setYExtent(extents.getY());
    worldBound.setZExtent(extents.getZ());
    worldBound.setCenter(center.getX(), (heightMax + heightMin) * 0.5, center.getZ());
    worldBound.transform(_worldTransform, worldBound);
    clearDirty(DirtyType.Bounding);
  }

  public void regenerate(final Renderer renderer) {
    for (int i = _clips.size() - 1; i >= 0; i--) {
      if (!_clips.get(i).isReady()) {
        _visibleLevels = i + 1;
        break;
      }
    }

    // Update vertices.
    for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
      _clips.get(i).regenerate();
    }

    // Update indices.
    for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
      if (i == _visibleLevels) {
        // Level 0 has no nested level, so pass null as parameter.
        _clips.get(i).updateIndices(null);
      } else {
        // All other levels i have the level i-1 nested in.
        _clips.get(i).updateIndices(_clips.get(i - 1));
      }
    }

    for (final TextureClipmap textureClipmap : _textureClipmaps) {
      textureClipmap.regenerate(renderer);
    }

    if (_normalClipmap != null) {
      _normalClipmap.regenerate(renderer);
    }
  }

  /**
   * @return the visibleLevels
   */
  public int getVisibleLevels() { return _visibleLevels; }

  /**
   * @param visibleLevels
   *          the visibleLevels to set
   */
  public void setVisibleLevels(final int visibleLevels) { _visibleLevels = visibleLevels; }

  public void setHeightRange(final float heightRangeMin, final float heightRangeMax) {
    for (int i = _clips.size() - 1; i >= 0; i--) {
      final ClipmapLevel clip = _clips.get(i);
      clip.setHeightRange(heightRangeMin, heightRangeMax);
    }
  }

  public void setCullingEnabled(final boolean cullingEnabled) {
    for (int i = _clips.size() - 1; i >= 0; i--) {
      final ClipmapLevel clip = _clips.get(i);
      clip.setCullingEnabled(cullingEnabled);
    }
  }

  public void makePickable(final Class<? extends AbstractBresenhamTracer> tracerClass, final int maxChecks,
      final Vector3 initialSpacing) throws InstantiationException, IllegalAccessException {
    // init the terrain picker
    _picker = new ClipmapTerrainPicker(_clips, tracerClass, maxChecks, initialSpacing);
  }

  public TextureClipmap getTextureClipmap() { return _textureClipmaps.get(0); }

  public List<TextureClipmap> getTextureClipmaps() { return _textureClipmaps; }

  public TextureClipmap findTextureClipmap(final TextureSource source) {
    for (final TextureClipmap cm : _textureClipmaps) {
      if (cm.getSource() == source) {
        return cm;
      }
    }

    return null;
  }

  public ClipmapTerrainPicker getPicker() { return _picker; }

  @Override
  public boolean supportsBoundsIntersectionRecord() {
    // for now we are not compatible with bounding volume picks
    return false;
  }

  @Override
  public boolean supportsPrimitivesIntersectionRecord() {
    return true;
  }

  @Override
  public boolean intersectsWorldBound(final Ray3 ray) {
    // XXX: could optimize this by grabbing edges of terrain and checking if we are outside of that...
    // for now we just return true.
    return true;
  }

  @Override
  public IntersectionRecord intersectsWorldBoundsWhere(final Ray3 ray) {
    // for now we are not compatible with bounding volume picks
    return null;
  }

  @Override
  public IntersectionRecord intersectsPrimitivesWhere(final Ray3 ray) {
    if (_picker != null) {
      final Vector3 normalStore = new Vector3();
      final Vector3 intersect =
          _picker.getTerrainIntersection(getWorldTransform(), _terrainCamera.getLocation(), ray, null, normalStore);
      if (intersect != null) {
        final double distance = intersect.distance(ray.getOrigin());
        final IntersectionRecord record = new IntersectionRecord(new double[] {distance}, new Vector3[] {intersect},
            new Vector3[] {normalStore}, null);
        return record;
      }
    }
    return null;
  }

  public List<ClipmapLevel> getClipmaps() { return _clips; }

  public void addTextureClipmap(final TextureClipmap textureClipmap) {
    _textureClipmaps.add(textureClipmap);
  }

  /**
   * set the minimum (highest resolution) clipmap level visible
   *
   * @param level
   *          clamped to valid range
   */
  public void setMinVisibleLevel(final int level) {
    if (level < 0) {
      _minVisibleLevel = 0;
    } else if (level >= _clips.size()) {
      _minVisibleLevel = _clips.size() - 1;
    } else {
      _minVisibleLevel = level;
    }
  }

  public int getMinVisibleLevel() { return _minVisibleLevel; }

  /**
   * convenience function to set minimum (highest resolution) texture clipmap level on all
   * TextureClipmaps and any NormalMap held by this terrain
   */
  public void setTextureMinVisibleLevel(final int level) {
    for (final TextureClipmap tc : _textureClipmaps) {
      tc.setMinVisibleLevel(level);
    }
    if (_normalClipmap != null) {
      _normalClipmap.setMinVisibleLevel(level);
    }
  }

  public int getTextureMinVisibleLevel() {
    if (!_textureClipmaps.isEmpty()) {
      return _textureClipmaps.get(0).getMinVisibleLevel();
    }
    return 0;
  }

  public Camera getTerrainCamera() { return _terrainCamera; }

  public TerrainConfiguration getTerrainConfiguration() { return _terrainConfiguration; }

  /**
   * Get height of the terrain at the given world coordinates. This height will correlate to the
   * finest level of detail, currently valid clipmap level at the given coordinates.
   *
   * @param x
   *          world x-coordinate
   * @param z
   *          world z-coordinate
   * @return the height, in world coordinate
   */
  public float getHeightAt(final double x, final double z) {
    final Vector3 heightCalc = new Vector3(x, 0, z);
    worldToLocal(heightCalc, heightCalc);
    final float height = getClipmaps().get(0).getCache().getSubHeight(heightCalc.getXf(), heightCalc.getZf());
    heightCalc.set(x, height, z);
    localToWorld(heightCalc, heightCalc);
    return heightCalc.getYf();
  }

  public void shutdown() {
    runCacheThread = false;
  }

  public BuildConfiguration getBuildConfig() { return _buildConfig; }

  public TextureState getClipTextureState() { return clipTextureState; }

  public void setNormalClipmap(final TextureClipmap normalClipmap) { _normalClipmap = normalClipmap; }

  public TextureClipmap getNormalClipmap() { return _normalClipmap; }

  public int getNormalUnit() { return _normalUnit; }

  public void setNormalUnit(final int unit) {
    _normalUnit = unit;
    setProperty("normalMap", _normalUnit);
  }

  public static void addDefaultResourceLocators() {
    try {
      ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MATERIAL, new SimpleResourceLocator(
          ResourceLocatorTool.getClassPathResource(Terrain.class, "com/ardor3d/extension/terrain/material")));
      ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_SHADER, new SimpleResourceLocator(
          ResourceLocatorTool.getClassPathResource(Terrain.class, "com/ardor3d/extension/terrain/shader")));
    } catch (final URISyntaxException ex) {
      ex.printStackTrace();
    }
  }
}
