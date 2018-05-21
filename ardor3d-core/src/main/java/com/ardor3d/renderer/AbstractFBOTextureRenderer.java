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

import java.nio.IntBuffer;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.framework.Scene;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Spatial;

public abstract class AbstractFBOTextureRenderer implements TextureRenderer {
    private static final Logger logger = Logger.getLogger(AbstractFBOTextureRenderer.class.getName());

    /** List of states that override any set states on a spatial if not null. */
    protected final EnumMap<RenderState.StateType, RenderState> _enforcedStates = new EnumMap<RenderState.StateType, RenderState>(
            RenderState.StateType.class);

    protected final Camera _camera = new Camera(1, 1);

    protected final ColorRGBA _backgroundColor = new ColorRGBA(1, 1, 1, 1);

    protected int _active;

    protected int _fboID = 0, _depthRBID = 0;
    protected int _msfboID = 0, _msdepthRBID = 0, _mscolorRBID = 0;
    protected int _width = 0, _height = 0, _samples = 0, _depthBits = 0;

    protected IntBuffer _attachBuffer = null;
    protected boolean _usingDepthRB = false;
    protected final boolean _supportsDepthTexture;
    protected final boolean _supportsMultisample;
    protected boolean _neededClip;

    protected final Renderer _parentRenderer;

    public AbstractFBOTextureRenderer(final int width, final int height, final int depthBits, final int samples,
            final Renderer parentRenderer, final ContextCapabilities caps) {
        _parentRenderer = parentRenderer;
        _samples = Math.min(samples, caps.getMaxFBOSamples());
        _depthBits = depthBits;
        _supportsDepthTexture = caps.isDepthTextureSupported();
        _supportsMultisample = caps.getMaxFBOSamples() != 0;

        int w = width;
        int h = height;
        if (!caps.isNonPowerOfTwoTextureSupported()) {
            // Check if we have non-power of two sizes. If so, find the smallest power of two size that is greater than
            // the provided size.
            if (!MathUtils.isPowerOfTwo(w)) {
                int newWidth = 2;
                do {
                    newWidth <<= 1;

                } while (newWidth < w);
                w = newWidth;
            }

            if (!MathUtils.isPowerOfTwo(h)) {
                int newHeight = 2;
                do {
                    newHeight <<= 1;

                } while (newHeight < h);
                h = newHeight;
            }
        }

        logger.fine("Creating FBO sized: " + w + " x " + h);

        _width = w;
        _height = h;

        _camera.resize(_width, _height);
        _camera.setFrustum(1.0f, 1000.0f, -0.50f, 0.50f, 0.50f, -0.50f);
        final Vector3 loc = new Vector3(0.0f, 0.0f, 0.0f);
        final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
        final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
        final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
        _camera.setFrame(loc, left, up, dir);
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
    }

    public ReadOnlyColorRGBA getBackgroundColor() {
        return _backgroundColor;
    }

    public void render(final Spatial toDraw, final Texture tex, final int clear) {
        try {
            ContextManager.getCurrentContext().pushFBOTextureRenderer(this);

            setupForSingleTexDraw(tex);

            if (_samples > 0 && _supportsMultisample) {
                setMSFBO();
            }

            switchCameraIn(clear);
            doDraw(toDraw);
            switchCameraOut();

            if (_samples > 0 && _supportsMultisample) {
                blitMSFBO();
            }

            takedownForSingleTexDraw(tex);

            ContextManager.getCurrentContext().popFBOTextureRenderer();
        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture, boolean)", "Exception", e);
        }
    }

    public void render(final Scene toDraw, final Texture tex, final int clear) {
        try {
            ContextManager.getCurrentContext().pushFBOTextureRenderer(this);

            setupForSingleTexDraw(tex);

            if (_samples > 0 && _supportsMultisample) {
                setMSFBO();
            }

            switchCameraIn(clear);
            doDraw(toDraw);
            switchCameraOut();

            if (_samples > 0 && _supportsMultisample) {
                blitMSFBO();
            }

            takedownForSingleTexDraw(tex);

            ContextManager.getCurrentContext().popFBOTextureRenderer();
        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture, boolean)", "Exception", e);
        }
    }

    public void render(final List<? extends Spatial> toDraw, final Texture tex, final int clear) {
        try {
            ContextManager.getCurrentContext().pushFBOTextureRenderer(this);

            setupForSingleTexDraw(tex);

            if (_samples > 0 && _supportsMultisample) {
                setMSFBO();
            }

            switchCameraIn(clear);
            doDraw(toDraw);
            switchCameraOut();

            if (_samples > 0 && _supportsMultisample) {
                blitMSFBO();
            }

            takedownForSingleTexDraw(tex);

            ContextManager.getCurrentContext().popFBOTextureRenderer();
        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(List<Spatial>, Texture, boolean)",
                    "Exception", e);
        }
    }

    protected abstract void activate();

    protected abstract void setupForSingleTexDraw(Texture tex);

    protected abstract void takedownForSingleTexDraw(Texture tex);

    protected abstract void setMSFBO();

    protected abstract void blitMSFBO();

    protected abstract void deactivate();

    private Camera _oldCamera;

    protected void switchCameraIn(final int clear) {
        // grab non-rtt settings
        _oldCamera = Camera.getCurrentCamera();

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

        // reset previous camera
        if (_oldCamera != null) {
            _oldCamera.update();
            _oldCamera.apply(_parentRenderer);
        }

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

    public int getWidth() {
        return _width;
    }

    public int getHeight() {
        return _height;
    }

    public Renderer getParentRenderer() {
        return _parentRenderer;
    }

    public void setMultipleTargets(final boolean multi) {
        // ignore. Does not matter to FBO.
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
