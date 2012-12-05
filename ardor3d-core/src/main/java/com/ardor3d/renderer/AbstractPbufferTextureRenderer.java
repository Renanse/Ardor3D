/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

import java.util.EnumMap;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Spatial;

public abstract class AbstractPbufferTextureRenderer implements TextureRenderer {
    private static final Logger logger = Logger.getLogger(AbstractPbufferTextureRenderer.class.getName());

    /** List of states that override any set states on a spatial if not null. */
    protected final EnumMap<RenderState.StateType, RenderState> _enforcedStates = new EnumMap<RenderState.StateType, RenderState>(
            RenderState.StateType.class);

    protected final Camera _camera = new Camera(1, 1);

    protected final ColorRGBA _backgroundColor = new ColorRGBA(1, 1, 1, 1);
    protected boolean _bgColorDirty = true;

    protected boolean _useDirectRender = false;

    protected int _active;

    protected int _width = 0, _height = 0;

    protected final Renderer _parentRenderer;
    protected final DisplaySettings _settings;

    public AbstractPbufferTextureRenderer(final DisplaySettings settings, final Renderer parentRenderer,
            final ContextCapabilities caps) {
        _parentRenderer = parentRenderer;
        _settings = settings;

        int width = settings.getWidth();
        int height = settings.getHeight();
        if (!caps.isNonPowerOfTwoTextureSupported()) {
            // Check if we have non-power of two sizes. If so, find the smallest power of two size that is greater than
            // the provided size.
            if (!MathUtils.isPowerOfTwo(width)) {
                int newWidth = 2;
                do {
                    newWidth <<= 1;

                } while (newWidth < width);
                width = newWidth;
            }

            if (!MathUtils.isPowerOfTwo(height)) {
                int newHeight = 2;
                do {
                    newHeight <<= 1;

                } while (newHeight < height);
                height = newHeight;
            }
        }

        _width = width;
        _height = height;

        logger.fine("Created Pbuffer sized: " + _width + " x " + _height);

        _camera.resize(_width, _height);
        _camera.setFrustum(1.0f, 1000.0f, -0.50f, 0.50f, 0.50f, -0.50f);
        final Vector3 loc = new Vector3(0.0f, 0.0f, 0.0f);
        final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
        final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
        final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
        _camera.setFrame(loc, left, up, dir);
    }

    protected RenderContext _oldContext;

    protected void switchCameraIn(final int clear) {
        // Note: no need for storing and replacing old camera since pbuffer is a separate context.

        // swap to rtt settings
        _parentRenderer.getQueue().pushBuckets();

        // clear the scene
        if (clear != 0) {
            clearBuffers(clear);
        }

        getCamera().update();
        getCamera().apply(_parentRenderer);
    }

    protected abstract void clearBuffers(int clear);

    protected void switchCameraOut() {
        _parentRenderer.flushFrame(false);
        // back to the non rtt settings
        _parentRenderer.getQueue().popBuckets();
    }

    protected void doDraw(final Spatial spat) {
        // Override parent's last frustum test to avoid accidental incorrect cull
        if (spat.getParent() != null) {
            spat.getParent().setLastFrustumIntersection(Camera.FrustumIntersect.Intersects);
        }

        // do rtt scene render
        spat.onDraw(_parentRenderer);
    }

    protected void doDraw(final List<? extends Spatial> toDraw) {
        for (int x = 0, max = toDraw.size(); x < max; x++) {
            final Spatial spat = toDraw.get(x);
            doDraw(spat);
        }
    }

    protected void doDraw(final Scene toDraw) {
        toDraw.renderUnto(_parentRenderer);
    }

    /**
     * <code>getCamera</code> retrieves the camera this renderer is using.
     * 
     * @return the camera this renderer is using.
     */
    public Camera getCamera() {
        return _camera;
    }

    public void setBackgroundColor(final ReadOnlyColorRGBA c) {
        _backgroundColor.set(c);
        _bgColorDirty = true;
    }

    public ReadOnlyColorRGBA getBackgroundColor() {
        return _backgroundColor;
    }

    public int getWidth() {
        return _width;
    }

    public int getHeight() {
        return _height;
    }

    public void enforceState(final RenderState state) {
        _enforcedStates.put(state.getType(), state);
    }

    public void enforceStates(final EnumMap<StateType, RenderState> states) {
        _enforcedStates.putAll(states);
    }

    public void clearEnforcedState(final StateType type) {
        _enforcedStates.remove(type);
    }

    public void clearEnforcedStates() {
        _enforcedStates.clear();
    }
}
