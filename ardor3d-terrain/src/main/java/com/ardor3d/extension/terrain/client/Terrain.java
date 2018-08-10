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
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.extension.terrain.util.AbstractBresenhamTracer;
import com.ardor3d.extension.terrain.util.ClipmapTerrainPicker;
import com.ardor3d.extension.terrain.util.DoubleBufferedList;
import com.ardor3d.extension.terrain.util.Region;
import com.ardor3d.intersection.IntersectionRecord;
import com.ardor3d.intersection.Pickable;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.MaterialFace;
import com.ardor3d.renderer.state.ShaderState;
import com.ardor3d.renderer.state.ShaderState.ShaderType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;

/**
 * An implementation of geometry clipmapping
 */
public class Terrain extends Node implements Pickable, Runnable {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(Terrain.class.getName());

    /** Our picker. */
    private ClipmapTerrainPicker _picker = null;

    private List<ClipmapLevel> _clips;
    private int _visibleLevels = 0;
    private int _minVisibleLevel = 0;
    private final Camera _terrainCamera;
    private final int _clipSideSize;

    private final BlendState blendState;

    private boolean _initialized = false;

    /** Shader for rendering clipmap geometry with morphing. */
    private ShaderState _geometryClipmapShader;

    /** Reference to the texture clipmap */
    private final List<TextureClipmap> _textureClipmaps = Lists.newArrayList();

    /** Reference to normal map */
    private TextureClipmap _normalClipmap;
    private int _normalUnit = 5;

    private final Vector3 transformedFrustumPos = new Vector3();

    private final DoubleBufferedList<Region> mailBox = new DoubleBufferedList<Region>();

    private ByteSource vertexShader;
    private ByteSource pixelShader;

    /** Timers for mailbox updates */
    private long oldTime = 0;
    private long updateTimer = 0;
    private final long updateThreashold = 300;

    protected boolean runCacheThread = true;
    protected Thread cacheThread;

    protected final int CACHE_UPDATE_SLEEP = 250;

    final TextureState clipTextureState = new TextureState();

    private final Comparator<Region> regionSorter = new Comparator<Region>() {
        @Override
        public int compare(final Region r1, final Region r2) {
            return r1.getLevel() - r2.getLevel();
        }
    };

    public Terrain(final Camera camera, final List<TerrainCache> cacheList, final int clipSideSize,
            final TerrainConfiguration terrainConfiguration) {
        _terrainCamera = camera;
        _clipSideSize = clipSideSize;

        _worldBound = new BoundingBox(Vector3.ZERO, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        getSceneHints().setRenderBucketType(RenderBucketType.Opaque);
        final CullState cs = new CullState();
        cs.setEnabled(true);
        cs.setCullFace(CullState.Face.Back);
        setRenderState(cs);

        final MaterialState materialState = new MaterialState();
        materialState.setAmbient(MaterialFace.FrontAndBack, new ColorRGBA(1, 1, 1, 1));
        materialState.setDiffuse(MaterialFace.FrontAndBack, new ColorRGBA(1, 1, 1, 1));
        materialState.setSpecular(MaterialFace.FrontAndBack, new ColorRGBA(1, 1, 1, 1));
        materialState.setShininess(MaterialFace.FrontAndBack, 64.0f);
        setRenderState(materialState);

        blendState = new BlendState();
        blendState.setBlendEnabled(true);
        blendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        setRenderState(blendState);

        // getSceneHints().setLightCombineMode(LightCombineMode.Off);

        try {
            _clips = new ArrayList<ClipmapLevel>();

            final float heightScale = terrainConfiguration.getScale().getYf();

            for (int i = 0; i < cacheList.size(); i++) {
                final TerrainCache cache = cacheList.get(i);
                cache.setMailBox(mailBox);
                final ClipmapLevel clipmap = new ClipmapLevel(i, camera, clipSideSize, heightScale, cache);
                _clips.add(clipmap);
                attachChild(clipmap);

                // clipmap.getMeshData().getVertexCoords().setVboAccessMode(VBOAccessMode.DynamicDraw);
                // clipmap.getMeshData().getIndices().setVboAccessMode(VBOAccessMode.DynamicDraw);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }

        vertexShader = new UrlInputSupplier(ResourceLocatorTool.getClassPathResource(Terrain.class,
                "com/ardor3d/extension/terrain/texturedGeometryClipmapShader.vert"));
        pixelShader = new UrlInputSupplier(ResourceLocatorTool.getClassPathResource(Terrain.class,
                "com/ardor3d/extension/terrain/texturedGeometryClipmapShader.frag"));

        // setScale(terrainConfiguration.getScale());
        // TODO: hack. unify scale handling over cache etc
        setScale(terrainConfiguration.getScale().getX(), 1, terrainConfiguration.getScale().getZ());
        setHeightRange(terrainConfiguration.getHeightRangeMin(), terrainConfiguration.getHeightRangeMax());
    }

    private final List<Long> timers = Lists.newArrayList();

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

        if (timers.size() < _visibleLevels) {
            for (int unit = 0; unit < _visibleLevels; unit++) {
                timers.add(System.currentTimeMillis());
            }
        }
        for (int unit = 0; unit < _visibleLevels; unit++) {
            final long t = System.currentTimeMillis() - timers.get(unit);
            if (t > 500) {
                timers.set(unit, System.currentTimeMillis());
                _clips.get(unit).updateCache();
            }
        }

        // Update vertices.
        for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
            _clips.get(i).updateVertices();
        }

        // Update from mailbox
        updateFromMailbox();

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

        for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
            _clips.get(i).getMeshData().getVertexCoords().setNeedsRefresh(true);
            _clips.get(i).getMeshData().getIndices().setNeedsRefresh(true);
        }

        if (runCacheThread && cacheThread == null) {
            cacheThread = new Thread(this, "TerrainCacheUpdater");
            cacheThread.setDaemon(true);
            cacheThread.start();
        }
    }

    @Override
    public void run() {
        while (runCacheThread) {
            try {
                TimeUnit.MILLISECONDS.sleep(CACHE_UPDATE_SLEEP);
            } catch (final InterruptedException e) {
            }

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
        updateShader(r);

        boolean first = true;
        if (_normalClipmap != null) {
            clipTextureState.setTexture(_normalClipmap.getTexture(), _normalUnit);
            _geometryClipmapShader.setUniform("normalMap", _normalUnit);
        }
        for (final TextureClipmap textureClipmap : _textureClipmaps) {
            if (!textureClipmap.isEnabled()) {
                continue;
            }

            clipTextureState.setTexture(textureClipmap.getTexture());
            if (first) {
                blendState.setEnabled(false);
                first = false;
            } else {
                blendState.setEnabled(true);
            }

            if (_textureClipmaps.size() > 1) {
                r.getQueue().pushBuckets();
            }

            for (int i = _clips.size() - 1; i >= 0; i--) {
                final ClipmapLevel clip = _clips.get(i);
                clip.setRenderState(clipTextureState);
            }

            if (_textureClipmaps.size() > 1) {
                _geometryClipmapShader.setUniform("scale", 1f / textureClipmap.getScale());
                _geometryClipmapShader.setUniform("textureSize", (float) textureClipmap.getTextureSize());
                _geometryClipmapShader.setUniform("texelSize", 1f / textureClipmap.getTextureSize());
                _geometryClipmapShader.setUniform("levels", (float) textureClipmap.getTextureLevels());
                _geometryClipmapShader.setUniform("validLevels", (float) textureClipmap.getValidLevels() - 1);
                _geometryClipmapShader.setUniform("showDebug", textureClipmap.isShowDebug() ? 1.0f : 0.0f);
                _geometryClipmapShader.setNeedsRefresh(true);
            }

            blendState.setNeedsRefresh(true);
            this.updateWorldRenderStates(true);

            if (!_initialized) {
                for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
                    final ClipmapLevel clip = _clips.get(i);

                    clip.getMeshData().getIndices().limit(clip.getMeshData().getIndices().capacity());
                }

                _initialized = true;
            }

            // draw levels from coarse to fine.
            for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
                final ClipmapLevel clip = _clips.get(i);

                if (clip.getStripIndex() > 0) {
                    clip.draw(r);
                }
            }

            if (_textureClipmaps.size() > 1) {
                r.renderBuckets();
                r.getQueue().popBuckets();
            }
        }
    }

    private void updateFromMailbox() {
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
                    final FloatBuffer vertices = clip.getMeshData().getVertexBuffer();
                    final int vertexDistance = clip.getVertexDistance();

                    clip.getCache().updateRegion(vertices, region.getX() / vertexDistance,
                            region.getY() / vertexDistance, region.getWidth() / vertexDistance,
                            region.getHeight() / vertexDistance);
                }
            }
            updateTimer %= updateThreashold;
        }
        final long time = System.currentTimeMillis();
        updateTimer += time - oldTime;
        oldTime = time;
    }

    private void recursiveAddUpdates(final List<Region> regionList, final int level, final int x, final int y,
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

    private final Vector3 _boundsCenter = new Vector3();
    private final Vector3 _boundsExtents = new Vector3();

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

    /**
     * Initialize/Update shaders
     */
    public void updateShader(final Renderer r) {
        if (_geometryClipmapShader != null) {
            getWorldTransform().applyInverse(_terrainCamera.getLocation(), transformedFrustumPos);
            _geometryClipmapShader.setUniform("eyePosition", transformedFrustumPos);
            for (final TextureClipmap textureClipmap : _textureClipmaps) {
                textureClipmap.update(r, transformedFrustumPos);
            }
            if (_normalClipmap != null) {
                _normalClipmap.update(r, transformedFrustumPos);
            }

            return;
        }

        reloadShader();
    }

    public void reloadShader() {
        final ContextCapabilities caps = ContextManager.getCurrentContext().getCapabilities();
        _geometryClipmapShader = new ShaderState();
        try {
            _geometryClipmapShader.setShader(ShaderType.Vertex, vertexShader.openStream());
            _geometryClipmapShader.setShader(ShaderType.Fragment, pixelShader.openStream());
        } catch (final IOException ex) {
            Terrain.logger.logp(Level.SEVERE, getClass().getName(), "init(Renderer)", "Could not load shaders.", ex);
        }

        _geometryClipmapShader.setUniform("texture", 0);
        _geometryClipmapShader.setUniform("clipSideSize", (float) _clipSideSize);

        if (!_textureClipmaps.isEmpty()) {
            final TextureClipmap textureClipmap = _textureClipmaps.get(0);
            _geometryClipmapShader.setUniform("scale", 1f / textureClipmap.getScale());
            _geometryClipmapShader.setUniform("textureSize", (float) textureClipmap.getTextureSize());
            _geometryClipmapShader.setUniform("texelSize", 1f / textureClipmap.getTextureSize());

            _geometryClipmapShader.setUniform("levels", (float) textureClipmap.getTextureLevels());
            _geometryClipmapShader.setUniform("validLevels", (float) textureClipmap.getValidLevels() - 1);
            _geometryClipmapShader.setUniform("minLevel", 0f);

            _geometryClipmapShader.setUniform("showDebug", textureClipmap.isShowDebug() ? 1.0f : 0.0f);
        }

        // _geometryClipmapShader.setShaderDataLogic(new GLSLShaderDataLogic() {
        // public void applyData(final ShaderState shader, final Mesh mesh, final Renderer renderer) {
        // if (mesh instanceof ClipmapLevel) {
        // shader.setUniform("vertexDistance", (float) ((ClipmapLevel) mesh).getVertexDistance());
        // }
        // }
        // });

        applyToClips();

        for (final TextureClipmap textureClipmap : _textureClipmaps) {
            textureClipmap.setShaderState(_geometryClipmapShader);
        }

        if (_normalClipmap != null) {
            _normalClipmap.setShaderState(_geometryClipmapShader);
        }

        updateWorldRenderStates(false);
    }

    protected void applyToClips() {
        for (int i = _clips.size() - 1; i >= 0; i--) {
            final ClipmapLevel clip = _clips.get(i);
            clip.setRenderState(_geometryClipmapShader);
        }
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

        for (int i = _clips.size() - 1; i >= _visibleLevels; i--) {
            _clips.get(i).getMeshData().getVertexCoords().setNeedsRefresh(true);
            _clips.get(i).getMeshData().getIndices().setNeedsRefresh(true);
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
    public int getVisibleLevels() {
        return _visibleLevels;
    }

    /**
     * @param visibleLevels
     *            the visibleLevels to set
     */
    public void setVisibleLevels(final int visibleLevels) {
        _visibleLevels = visibleLevels;
    }

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

    public TextureClipmap getTextureClipmap() {
        return _textureClipmaps.get(0);
    }

    public List<TextureClipmap> getTextureClipmaps() {
        return _textureClipmaps;
    }

    public TextureClipmap findTextureClipmap(final TextureSource source) {
        for (final TextureClipmap cm : _textureClipmaps) {
            if (cm.getSource() == source) {
                return cm;
            }
        }

        return null;
    }

    public ShaderState getGeometryClipmapShader() {
        return _geometryClipmapShader;
    }

    public void setGeometryClipmapShader(final ShaderState shaderState) {
        _geometryClipmapShader = shaderState;

        applyToClips();

        for (final TextureClipmap textureClipmap : _textureClipmaps) {
            textureClipmap.setShaderState(_geometryClipmapShader);
        }

        if (_normalClipmap != null) {
            _normalClipmap.setShaderState(_geometryClipmapShader);
        }
    }

    public ClipmapTerrainPicker getPicker() {
        return _picker;
    }

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
            final Vector3 intersect = _picker.getTerrainIntersection(getWorldTransform(), _terrainCamera.getLocation(),
                    ray, null, normalStore);
            if (intersect != null) {
                final double distance = intersect.distance(ray.getOrigin());
                final IntersectionRecord record = new IntersectionRecord(new double[] { distance },
                        new Vector3[] { intersect }, new Vector3[] { normalStore }, null);
                return record;
            }
        }
        return null;
    }

    public List<ClipmapLevel> getClipmaps() {
        return _clips;
    }

    public void setVertexShader(final ByteSource vertexShader) {
        this.vertexShader = vertexShader;
    }

    public void setPixelShader(final ByteSource pixelShader) {
        this.pixelShader = pixelShader;
    }

    public void addTextureClipmap(final TextureClipmap textureClipmap) {
        _textureClipmaps.add(textureClipmap);
    }

    /**
     * set the minimum (highest resolution) clipmap level visible
     *
     * @param level
     *            clamped to valid range
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

    public int getMinVisibleLevel() {
        return _minVisibleLevel;
    }

    /**
     * convenience function to set minimum (highest resolution) texture clipmap level on all TextureClipmaps and any
     * NormalMap held by this terrain
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

    /**
     * Get height of the terrain at the given world coordinates. This height will correlate to the finest level of
     * detail, currently valid clipmap level at the given coordinates.
     *
     * @param x
     *            world x-coordinate
     * @param z
     *            world z-coordinate
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

    public TextureState getClipTextureState() {
        return clipTextureState;
    }

    public void setNormalClipmap(final TextureClipmap normalClipmap) {
        _normalClipmap = normalClipmap;
    }

    public TextureClipmap getNormalClipmap() {
        return _normalClipmap;
    }

    public int getNormalUnit() {
        return _normalUnit;
    }

    public void setNormalUnit(final int unit) {
        _normalUnit = unit;
    }
}
