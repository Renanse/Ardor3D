/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect;

import java.util.List;

import com.ardor3d.extension.effect.bloom.BloomRenderPass;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.renderer.effect.EffectManager;
import com.ardor3d.renderer.effect.EffectStep_RenderScreenOverlay;
import com.ardor3d.renderer.effect.EffectStep_RenderSpatials;
import com.ardor3d.renderer.effect.EffectStep_SetRenderTarget;
import com.ardor3d.renderer.effect.RenderEffect;
import com.ardor3d.renderer.effect.RenderTarget_Texture2D;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ShaderState;
import com.ardor3d.renderer.state.ShaderState.ShaderType;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.google.common.collect.Lists;

public class SimpleBloomEffect extends RenderEffect {
    protected static final String RT_MAIN = "LDRBloomEffect.MAIN";
    protected static final String RT_SECONDARY = "LDRBloomEffect.SECONDARY";

    protected String shaderDirectory = "com/ardor3d/extension/effect/";
    protected final List<Spatial> _bloomItems = Lists.newArrayList();

    protected float _downsampleRatio = .33f;
    private final ShaderState _extractionShader, _blurHorizShader, _blurVertShader;

    public SimpleBloomEffect() {
        _extractionShader = getExtractionShader();
        _blurHorizShader = getBlurHorizShader();
        _blurVertShader = getBlurVertShader();
        setExposureIntensity(1.3f);
        setExposureCutoff(0.15f);
        setSampleDistance(0.02f);
    }

    @Override
    public void prepare(final EffectManager manager) {
        // init targets used in this effect
        initTargets(manager);

        final boolean useBloomItems = !_bloomItems.isEmpty();

        _steps.clear();
        // step 1: pick whether we are blooming the whole previous render buffer or just specific items
        if (useBloomItems) {
            // render these items to a texture
            _steps.add(new EffectStep_SetRenderTarget(RT_MAIN));
            _steps.add(new EffectStep_RenderSpatials(null));
        }

        // step 2: extract intensity
        {
            _steps.add(new EffectStep_SetRenderTarget(RT_SECONDARY));
            final EffectStep_RenderScreenOverlay extract = new EffectStep_RenderScreenOverlay();
            extract.getEnforcedStates().put(StateType.Shader, _extractionShader);
            extract.getTargetMap().put(useBloomItems ? RT_MAIN : "*Previous", 0);
            _steps.add(extract);
        }

        // step 3: blur
        {
            _steps.add(new EffectStep_SetRenderTarget(RT_MAIN));
            final EffectStep_RenderScreenOverlay blurHoriz = new EffectStep_RenderScreenOverlay();
            blurHoriz.getEnforcedStates().put(StateType.Shader, _blurHorizShader);
            blurHoriz.getTargetMap().put(RT_SECONDARY, 0);
            _steps.add(blurHoriz);

            _steps.add(new EffectStep_SetRenderTarget(RT_SECONDARY));
            final EffectStep_RenderScreenOverlay blurVert = new EffectStep_RenderScreenOverlay();
            blurVert.getEnforcedStates().put(StateType.Shader, _blurVertShader);
            blurVert.getTargetMap().put(RT_MAIN, 0);
            _steps.add(blurVert);
        }

        // finally: draw bloom texture and previous texture on fsq, blended.
        _steps.add(new EffectStep_SetRenderTarget("*Next"));

        final EffectStep_RenderScreenOverlay blendOverlay = new EffectStep_RenderScreenOverlay();
        blendOverlay.getEnforcedStates().put(StateType.Shader, getBlendShader());
        blendOverlay.getTargetMap().put("*Previous", 0);
        blendOverlay.getTargetMap().put(RT_SECONDARY, 1);
        _steps.add(blendOverlay);

        super.prepare(manager);
    }

    protected void initTargets(final EffectManager manager) {
        final DisplaySettings canvas = manager.getCanvasSettings();
        final int downsampledHeight = Math.round(canvas.getHeight() * _downsampleRatio);
        final int downsampledWidth = Math.round(canvas.getWidth() * _downsampleRatio);

        final RenderTarget_Texture2D main = new RenderTarget_Texture2D(downsampledWidth, downsampledHeight,
                manager.getOutputFormat());
        main.getTexture().setWrap(WrapMode.EdgeClamp);
        manager.getRenderTargetMap().put(RT_MAIN, main);

        final RenderTarget_Texture2D secondary = new RenderTarget_Texture2D(downsampledWidth, downsampledHeight,
                manager.getOutputFormat());
        secondary.getTexture().setWrap(WrapMode.EdgeClamp);
        manager.getRenderTargetMap().put(RT_SECONDARY, secondary);
    }

    protected ShaderState getExtractionShader() {
        final ShaderState shader = new ShaderState();
        try {
            shader.setShader(
                    ShaderType.Vertex,
                    ResourceLocatorTool.getClassPathResourceAsString(BloomRenderPass.class, shaderDirectory
                            + "fsq.vert"));
            shader.setShader(
                    ShaderType.Fragment,
                    ResourceLocatorTool.getClassPathResourceAsString(BloomRenderPass.class, shaderDirectory
                            + "bloom_extract.frag"));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        shader.setUniform("inputTex", 0);
        return shader;
    }

    protected ShaderState getBlurHorizShader() {
        final ShaderState shader = new ShaderState();
        try {
            shader.setShader(
                    ShaderType.Vertex,
                    ResourceLocatorTool.getClassPathResourceAsString(BloomRenderPass.class, shaderDirectory
                            + "fsq.vert"));
            shader.setShader(
                    ShaderType.Fragment,
                    ResourceLocatorTool.getClassPathResourceAsString(BloomRenderPass.class, shaderDirectory
                            + "gausian_blur_horizontal9.frag"));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        shader.setUniform("inputTex", 0);
        return shader;
    }

    protected ShaderState getBlurVertShader() {
        final ShaderState shader = new ShaderState();
        try {
            shader.setShader(
                    ShaderType.Vertex,
                    ResourceLocatorTool.getClassPathResourceAsString(BloomRenderPass.class, shaderDirectory
                            + "fsq.vert"));
            shader.setShader(
                    ShaderType.Fragment,
                    ResourceLocatorTool.getClassPathResourceAsString(BloomRenderPass.class, shaderDirectory
                            + "gausian_blur_vertical9.frag"));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        shader.setUniform("inputTex", 0);
        return shader;
    }

    protected ShaderState getBlendShader() {
        final ShaderState shader = new ShaderState();
        try {
            shader.setShader(
                    ShaderType.Vertex,
                    ResourceLocatorTool.getClassPathResourceAsString(BloomRenderPass.class, shaderDirectory
                            + "fsq.vert"));
            shader.setShader(
                    ShaderType.Fragment,
                    ResourceLocatorTool.getClassPathResourceAsString(BloomRenderPass.class, shaderDirectory
                            + "add2textures.frag"));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        shader.setUniform("tex1", 0);
        shader.setUniform("tex2", 1);
        return shader;
    }

    public void setExposureIntensity(final float value) {
        _extractionShader.setUniform("exposureIntensity", value);
    }

    public void setExposureCutoff(final float value) {
        _extractionShader.setUniform("exposureCutoff", value);
    }

    public void setSampleDistance(final float value) {
        _blurHorizShader.setUniform("sampleDist", value);
        _blurVertShader.setUniform("sampleDist", value);
    }

    public float getDownsampleRatio() {
        return _downsampleRatio;
    }

    public void setDownsampleRatio(final float downsampleRatio) {
        _downsampleRatio = downsampleRatio;
    }

    public String getShaderDirectory() {
        return shaderDirectory;
    }

    public void setShaderDirectory(final String shaderDirectory) {
        this.shaderDirectory = shaderDirectory;
    }

    public List<Spatial> getBloomItems() {
        return _bloomItems;
    }
}
