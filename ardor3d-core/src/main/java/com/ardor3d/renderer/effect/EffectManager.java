/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.effect;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.effect.rendertarget.RenderTarget;
import com.ardor3d.renderer.effect.rendertarget.RenderTarget_Framebuffer;
import com.ardor3d.renderer.effect.rendertarget.RenderTarget_Texture2D;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Quad;

public class EffectManager {

    public final static String RT_PREVIOUS = "*Previous";
    public final static String RT_NEXT = "*Next";
    public final static String RT_FRAMEBUFFER = "*Framebuffer";

    protected final DisplaySettings _canvasSettings;
    protected final List<RenderEffect> _effects = new ArrayList<>();
    protected final Map<String, RenderTarget> _renderTargetMap = new HashMap<>();
    protected Renderer _currentRenderer = null;
    protected RenderTarget _currentRenderTarget = null;
    protected Camera _sceneCamera;
    protected Mesh _fsq;
    protected RenderTarget _inOutTargetA, _inOutTargetB;
    protected boolean _swapTargets = false;
    protected final TextureStoreFormat _outputFormat;

    public EffectManager(final DisplaySettings settings, final TextureStoreFormat outputformat) {
        _canvasSettings = settings;

        _outputFormat = outputformat;
        setupDefaultTargets(outputformat);
    }

    public void setupEffects() {
        for (final RenderEffect effect : _effects) {
            effect.prepare(this);
        }
    }

    protected void setupDefaultTargets(final TextureStoreFormat outputformat) {
        _renderTargetMap.put(EffectManager.RT_FRAMEBUFFER, new RenderTarget_Framebuffer());
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
        if (RT_PREVIOUS.equals(name)) {
            return _swapTargets ? _inOutTargetB : _inOutTargetA;
        } else if (RT_NEXT.equals(name)) {
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

    public void renderFullScreenQuad(final RenderMaterial enforcedMaterial,
            final EnumMap<StateType, RenderState> enforcedStates) {
        // render our -1,1 quad
        _currentRenderTarget.render(this, getSceneCamera(), getFullScreenQuad(), enforcedMaterial, enforcedStates);
    }

    protected Mesh getFullScreenQuad() {
        if (_fsq != null) {
            return _fsq;
        }

        _fsq = Quad.newFullScreenQuad();

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
