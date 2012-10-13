/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.effect;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class EffectManager {

    protected final DisplaySettings _canvasSettings;
    protected final List<RenderEffect> _effects = Lists.newArrayList();
    protected final Map<String, RenderTarget> _renderTargetMap = Maps.newHashMap();
    protected Renderer _currentRenderer = null;
    protected RenderTarget _currentRenderTarget = null;
    protected Camera _fsqCamera, _sceneCamera;
    protected Mesh _fsq;
    protected RenderTarget _inOutTargetA, _inOutTargetB;
    protected boolean _swapTargets = false;
    protected final TextureStoreFormat _outputFormat;

    public EffectManager(final DisplaySettings settings, final TextureStoreFormat outputformat) {
        _canvasSettings = settings;
        _fsqCamera = new Camera(settings.getWidth(), settings.getHeight());
        _fsqCamera.setFrustum(-1, 1, -1, 1, 1, -1);
        _fsqCamera.setProjectionMode(ProjectionMode.Parallel);
        _fsqCamera.setAxes(Vector3.NEG_UNIT_X, Vector3.UNIT_Y, Vector3.NEG_UNIT_Z);

        _outputFormat = outputformat;
        setupDefaultTargets(outputformat);
    }

    public void setupEffects() {
        for (final RenderEffect effect : _effects) {
            effect.prepare(this);
        }
    }

    protected void setupDefaultTargets(final TextureStoreFormat outputformat) {
        _renderTargetMap.put("*Framebuffer", new RenderTarget_Framebuffer());
        _inOutTargetA = new RenderTarget_Texture2D(_canvasSettings.getWidth(), _canvasSettings.getHeight(),
                outputformat);
        _inOutTargetB = new RenderTarget_Texture2D(_canvasSettings.getWidth(), _canvasSettings.getHeight(),
                outputformat);
    }

    public void renderEffects(final Renderer renderer) {
        _currentRenderer = renderer;
        for (final RenderEffect effect : _effects) {
            if (effect.isEnabled()) {
                effect.render(this);
                _swapTargets = !_swapTargets;
            }
        }
    }

    public DisplaySettings getCanvasSettings() {
        return _canvasSettings;
    }

    public List<RenderEffect> getEffects() {
        return _effects;
    }

    public Map<String, RenderTarget> getRenderTargetMap() {
        return _renderTargetMap;
    }

    public Renderer getCurrentRenderer() {
        return _currentRenderer;
    }

    public RenderTarget getCurrentRenderTarget() {
        return _currentRenderTarget;
    }

    public void setCurrentRenderTarget(final RenderTarget target) {
        _currentRenderTarget = target;
    }

    public void addEffect(final RenderEffect effect) {
        _effects.add(effect);
    }

    public RenderTarget getRenderTarget(final String name) {
        // Check for reserved words
        if ("*Previous".equals(name)) {
            return _swapTargets ? _inOutTargetB : _inOutTargetA;
        } else if ("*Next".equals(name)) {
            return _swapTargets ? _inOutTargetA : _inOutTargetB;
        } else {
            return _renderTargetMap.get(name);
        }
    }

    public boolean setCurrentRenderTarget(final String name) {
        final RenderTarget target = getRenderTarget(name);
        if (target != null) {
            _currentRenderTarget = target;
            return true;
        }
        return false;
    }

    public void renderFullScreenQuad(final EnumMap<StateType, RenderState> enforcedStates) {
        // render our -1,1 quad
        _currentRenderTarget.render(this, _fsqCamera, getFullScreenQuad(), enforcedStates);
    }

    protected Mesh getFullScreenQuad() {
        if (_fsq != null) {
            return _fsq;
        }

        _fsq = new Mesh("fsq");
        _fsq.getMeshData().setVertexBuffer(BufferUtils.createFloatBuffer(-1, -1, -1, 1, -1, -1, 1, 1, -1, -1, 1, -1));
        _fsq.getMeshData().setTextureBuffer(BufferUtils.createFloatBuffer(0, 0, 1, 0, 1, 1, 0, 1), 0);
        _fsq.getMeshData().setIndexBuffer(BufferUtils.createIntBuffer(0, 1, 3, 1, 2, 3));

        _fsq.getSceneHints().setCullHint(CullHint.Never);
        _fsq.getSceneHints().setLightCombineMode(LightCombineMode.Off);

        final ZBufferState zState = new ZBufferState();
        zState.setEnabled(false);
        _fsq.setRenderState(zState);

        _fsq.updateGeometricState(0);

        return _fsq;
    }

    public TextureStoreFormat getOutputFormat() {
        return _outputFormat;
    }

    public Camera getSceneCamera() {
        return _sceneCamera;
    }

    public void setSceneCamera(final Camera sceneCamera) {
        _sceneCamera = sceneCamera;
    }
}
