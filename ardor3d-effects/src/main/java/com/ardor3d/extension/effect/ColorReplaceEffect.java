/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect;

import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.renderer.effect.EffectManager;
import com.ardor3d.renderer.effect.EffectStep_RenderScreenOverlay;
import com.ardor3d.renderer.effect.EffectStep_SetRenderTarget;
import com.ardor3d.renderer.effect.RenderEffect;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ShaderState;
import com.ardor3d.renderer.state.ShaderState.ShaderType;
import com.ardor3d.util.resource.ResourceLocatorTool;

public class ColorReplaceEffect extends RenderEffect {

    private String shaderDirectory = "com/ardor3d/extension/effect/";
    private float _redWeight = 0.3086f;
    private float _greenWeight = 0.6094f;
    private float _blueWeight = 0.0820f;
    private Texture _colorRampTexture;

    public ColorReplaceEffect(final Texture colorRampTexture) {
        _colorRampTexture = colorRampTexture;
        _colorRampTexture.setWrap(WrapMode.EdgeClamp);
    }

    private ShaderState getColorizeShader() {
        final ShaderState shader = new ShaderState();
        try {
            shader.setShader(
                    ShaderType.Vertex,
                    "fsq",
                    ResourceLocatorTool.getClassPathResourceAsString(ColorReplaceEffect.class, shaderDirectory
                            + "fsq.vert"));
            shader.setShader(
                    ShaderType.Fragment,
                    "color_replace",
                    ResourceLocatorTool.getClassPathResourceAsString(ColorReplaceEffect.class, shaderDirectory
                            + "color_replace.frag"));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        shader.setUniform("inputTex", 0);
        shader.setUniform("colorRampTex", 1);
        shader.setUniform("redWeight", _redWeight);
        shader.setUniform("greenWeight", _greenWeight);
        shader.setUniform("blueWeight", _blueWeight);
        return shader;
    }

    @Override
    public void prepare(final EffectManager manager) {
        _steps.clear();
        _steps.add(new EffectStep_SetRenderTarget("*Next"));

        final EffectStep_RenderScreenOverlay colorizeStep = new EffectStep_RenderScreenOverlay();
        colorizeStep.getTextureState().setTexture(_colorRampTexture, 1);
        colorizeStep.getTargetMap().put("*Previous", 0);
        colorizeStep.getEnforcedStates().put(StateType.Shader, getColorizeShader());
        _steps.add(colorizeStep);

        super.prepare(manager);
    }

    public float getRedWeight() {
        return _redWeight;
    }

    public void setRedWeight(final float redWeight) {
        _redWeight = redWeight;
    }

    public float getGreenWeight() {
        return _greenWeight;
    }

    public void setGreenWeight(final float greenWeight) {
        _greenWeight = greenWeight;
    }

    public float getBlueWeight() {
        return _blueWeight;
    }

    public void setBlueWeight(final float blueWeight) {
        _blueWeight = blueWeight;
    }

    public String getShaderDirectory() {
        return shaderDirectory;
    }

    public void setShaderDirectory(final String shaderDirectory) {
        this.shaderDirectory = shaderDirectory;
    }

    public Texture getColorRampTexture() {
        return _colorRampTexture;
    }

    public void setColorRampTexture(final Texture colorRampTexture) {
        _colorRampTexture = colorRampTexture;
    }
}
