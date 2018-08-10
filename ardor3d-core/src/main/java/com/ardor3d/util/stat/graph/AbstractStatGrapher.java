/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.stat.graph;

import java.util.HashMap;
import java.util.TreeMap;

import com.ardor3d.image.Texture2D;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.util.stat.StatListener;
import com.ardor3d.util.stat.StatType;

/**
 * Base class for graphers.
 */
public abstract class AbstractStatGrapher implements StatListener {

    protected TextureRenderer _textureRenderer;
    protected Texture2D _texture;
    protected int _gWidth, _gHeight;

    protected TreeMap<StatType, HashMap<String, Object>> _config = new TreeMap<StatType, HashMap<String, Object>>();

    protected boolean _enabled = true;

    /**
     * Must be constructed in the GL thread.
     * 
     * @param factory
     */
    public AbstractStatGrapher(final int width, final int height, final Renderer renderer,
            final ContextCapabilities caps) {
        _gWidth = width;
        _gHeight = height;
        // prepare our TextureRenderer
        _textureRenderer = TextureRendererFactory.INSTANCE.createTextureRenderer(width, height, renderer, caps);

        if (_textureRenderer != null) {
            _textureRenderer.setBackgroundColor(new ColorRGBA(ColorRGBA.BLACK));
        }
    }

    // - set a texture for offscreen rendering
    public void setTexture(final Texture2D tex) {
        _textureRenderer.setupTexture(tex);
        _texture = tex;
    }

    public TextureRenderer getTextureRenderer() {
        return _textureRenderer;
    }

    public void clearConfig() {
        _config.clear();
    }

    public void clearConfig(final StatType type) {
        if (_config.get(type) != null) {
            _config.get(type).clear();
        }
    }

    public void clearConfig(final StatType type, final String key) {
        if (_config.get(type) != null) {
            _config.get(type).remove(key);
        }
    }

    public void addConfig(final StatType type, final HashMap<String, Object> configs) {
        _config.put(type, configs);
    }

    public void addConfig(final StatType type, final String key, final Object value) {
        HashMap<String, Object> vals = _config.get(type);
        if (vals == null) {
            vals = new HashMap<String, Object>();
            _config.put(type, vals);
        }
        vals.put(key, value);
    }

    protected ColorRGBA getColorConfig(final StatType type, final String configName, final ColorRGBA defaultVal) {
        final HashMap<String, Object> vals = _config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof ColorRGBA) {
                return (ColorRGBA) val;
            }
        }
        return defaultVal;
    }

    protected String getStringConfig(final StatType type, final String configName, final String defaultVal) {
        final HashMap<String, Object> vals = _config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof String) {
                return (String) val;
            }
        }
        return defaultVal;
    }

    protected short getShortConfig(final StatType type, final String configName, final short defaultVal) {
        final HashMap<String, Object> vals = _config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof Number) {
                return ((Number) val).shortValue();
            }
        }
        return defaultVal;
    }

    protected int getIntConfig(final StatType type, final String configName, final int defaultVal) {
        final HashMap<String, Object> vals = _config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
        }
        return defaultVal;
    }

    protected long getLongConfig(final StatType type, final String configName, final long defaultVal) {
        final HashMap<String, Object> vals = _config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof Number) {
                return ((Number) val).longValue();
            }
        }
        return defaultVal;
    }

    protected float getFloatConfig(final StatType type, final String configName, final float defaultVal) {
        final HashMap<String, Object> vals = _config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof Number) {
                return ((Number) val).floatValue();
            }
        }
        return defaultVal;
    }

    protected double getDoubleConfig(final StatType type, final String configName, final double defaultVal) {
        final HashMap<String, Object> vals = _config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof Number) {
                return ((Number) val).doubleValue();
            }
        }
        return defaultVal;
    }

    protected boolean getBooleanConfig(final StatType type, final String configName, final boolean defaultVal) {
        final HashMap<String, Object> vals = _config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof Boolean) {
                return (Boolean) val;
            }
        }
        return defaultVal;
    }

    public boolean hasConfig(final StatType type) {
        return _config.containsKey(type) && !_config.get(type).isEmpty();
    }

    public boolean isEnabled() {
        return _enabled;
    }

    public void setEnabled(final boolean enabled) {
        _enabled = enabled;
    }

    /**
     * Called when the graph needs to be reset back to the original display state. (iow, remove all points, lines, etc.)
     */
    public abstract void reset();
}
