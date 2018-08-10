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

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.renderer.effect.EffectManager;
import com.ardor3d.renderer.effect.EffectStep_RenderScreenOverlay;
import com.ardor3d.renderer.effect.EffectStep_SetRenderTarget;
import com.ardor3d.renderer.effect.RenderEffect;
import com.ardor3d.renderer.effect.RenderTarget;
import com.ardor3d.renderer.effect.RenderTarget_Texture2D;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ShaderState;
import com.ardor3d.renderer.state.ShaderState.ShaderType;
import com.ardor3d.util.resource.ResourceLocatorTool;

public class HDREffect extends RenderEffect {
    private String shaderDirectory = "com/ardor3d/extension/effect/";

    protected static final String RT_DOWNSAMPLED = "HDREffect.DOWNSAMPLED";
    protected static final String RT_LUM64x64 = "HDREffect.LUM64x64";
    protected static final String RT_LUM16x16 = "HDREffect.LUM16x16";
    protected static final String RT_LUM1x1 = "HDREffect.LUM1x1";
    protected static final String RT_BRIGHTMAP = "HDREffect.BRIGHTMAP";
    protected static final String RT_BLOOM_HORIZONTAL = "HDREffect.BLOOM_HORIZONTAL";
    protected static final String RT_BLOOM = "HDREffect.BLOOM";

    protected float _downsampleRatio = 0.25f;

    @Override
    public void prepare(final EffectManager manager) {
        // init targets used in this effect
        initTargets(manager);

        _steps.clear();

        // step 1: downsample previous rendering in chain
        {
            _steps.add(new EffectStep_SetRenderTarget(RT_DOWNSAMPLED));
            final EffectStep_RenderScreenOverlay downsample = new EffectStep_RenderScreenOverlay();
            downsample.getTargetMap().put("*Previous", 0);
            _steps.add(downsample);
        }

        // step 2: extract our average luminance value
        {
            _steps.add(new EffectStep_SetRenderTarget(RT_LUM64x64));
            final EffectStep_RenderScreenOverlay extract64 = new EffectStep_RenderScreenOverlay();
            extract64.getEnforcedStates().put(StateType.Shader, getLuminanceExtractionShader());
            extract64.getTargetMap().put(RT_DOWNSAMPLED, 0);
            _steps.add(extract64);

            _steps.add(new EffectStep_SetRenderTarget(RT_LUM16x16));
            final EffectStep_RenderScreenOverlay extract4 = new EffectStep_RenderScreenOverlay();
            extract4.getTargetMap().put(RT_LUM64x64, 0);
            _steps.add(extract4);

            _steps.add(new EffectStep_SetRenderTarget(RT_LUM1x1));
            final EffectStep_RenderScreenOverlay extract1 = new EffectStep_RenderScreenOverlay();
            extract1.getTargetMap().put(RT_LUM16x16, 0);
            _steps.add(extract1);
        }

        // step 3: apply bright pass, extracting the brighter than normal portions of the scene
        {
            _steps.add(new EffectStep_SetRenderTarget(RT_BRIGHTMAP));
            final EffectStep_RenderScreenOverlay bright = new EffectStep_RenderScreenOverlay();
            bright.getEnforcedStates().put(StateType.Shader, getBrightMapShader());
            bright.getTargetMap().put(RT_DOWNSAMPLED, 0);
            bright.getTargetMap().put(RT_LUM1x1, 1);
            _steps.add(bright);
        }

        // finally: draw bloom texture and previous texture on fsq, blended.
        _steps.add(new EffectStep_SetRenderTarget("*Next"));

        final EffectStep_RenderScreenOverlay blendOverlay = new EffectStep_RenderScreenOverlay();
        blendOverlay.getTargetMap().put(RT_BRIGHTMAP, 0);
        _steps.add(blendOverlay);

        super.prepare(manager);
    }

    private RenderState getLuminanceExtractionShader() {
        final ShaderState shader = new ShaderState();
        try {
            shader.setShader(
                    ShaderType.Vertex,
                    "fsq",
                    ResourceLocatorTool.getClassPathResourceAsString(ColorReplaceEffect.class, shaderDirectory
                            + "fsq.vert"));
            shader.setShader(
                    ShaderType.Fragment,
                    "luminance",
                    ResourceLocatorTool.getClassPathResourceAsString(ColorReplaceEffect.class, shaderDirectory
                            + "luminance.frag"));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        shader.setUniform("inputTex", 0);
        return shader;
    }

    private RenderState getBrightMapShader() {
        final ShaderState shader = new ShaderState();
        try {
            shader.setShader(
                    ShaderType.Vertex,
                    "fsq",
                    ResourceLocatorTool.getClassPathResourceAsString(ColorReplaceEffect.class, shaderDirectory
                            + "fsq.vert"));
            shader.setShader(
                    ShaderType.Fragment,
                    "brightmap",
                    ResourceLocatorTool.getClassPathResourceAsString(ColorReplaceEffect.class, shaderDirectory
                            + "brightmap.frag"));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        shader.setUniform("inputTex", 0);
        shader.setUniform("lum1x1Tex", 1);
        shader.setUniform("exposurePow", 3.0f);
        shader.setUniform("exposureCutoff", 0.0f);
        return shader;
    }

    private void initTargets(final EffectManager manager) {
        final DisplaySettings canvas = manager.getCanvasSettings();
        final int downsampledHeight = Math.round(canvas.getHeight() * _downsampleRatio);
        final int downsampledWidth = Math.round(canvas.getWidth() * _downsampleRatio);

        final RenderTarget_Texture2D downsampled = new RenderTarget_Texture2D(downsampledWidth, downsampledHeight,
                TextureStoreFormat.RGBA16F);
        downsampled.getTexture().setWrap(WrapMode.EdgeClamp);
        manager.getRenderTargetMap().put(RT_DOWNSAMPLED, downsampled);

        manager.getRenderTargetMap().put(RT_LUM64x64, getLuminanceDownsampleTexture(64));
        manager.getRenderTargetMap().put(RT_LUM16x16, getLuminanceDownsampleTexture(16));
        manager.getRenderTargetMap().put(RT_LUM1x1, getLuminanceDownsampleTexture(1));

        final RenderTarget_Texture2D brightmap = new RenderTarget_Texture2D(downsampledWidth, downsampledHeight,
                TextureStoreFormat.RGBA16F);
        brightmap.getTexture().setWrap(WrapMode.EdgeClamp);
        manager.getRenderTargetMap().put(RT_BRIGHTMAP, brightmap);

        final RenderTarget_Texture2D bloomHoriz = new RenderTarget_Texture2D(downsampledWidth, downsampledHeight,
                TextureStoreFormat.RGBA8);
        bloomHoriz.getTexture().setWrap(WrapMode.EdgeClamp);
        manager.getRenderTargetMap().put(RT_BLOOM_HORIZONTAL, bloomHoriz);

        final RenderTarget_Texture2D bloom = new RenderTarget_Texture2D(downsampledWidth, downsampledHeight,
                TextureStoreFormat.RGBA8);
        bloom.getTexture().setWrap(WrapMode.EdgeClamp);
        manager.getRenderTargetMap().put(RT_BLOOM, bloom);
    }

    private RenderTarget getLuminanceDownsampleTexture(final int size) {
        final RenderTarget_Texture2D target = new RenderTarget_Texture2D(size, size, TextureStoreFormat.RGBA16F);
        if (size != 1) {
            target.getTexture().setMinificationFilter(MinificationFilter.Trilinear);
            target.getTexture().setMagnificationFilter(MagnificationFilter.Bilinear);
        } else {
            target.getTexture().setMinificationFilter(MinificationFilter.NearestNeighborNoMipMaps);
            target.getTexture().setMagnificationFilter(MagnificationFilter.NearestNeighbor);
        }

        target.getTexture().setWrap(WrapMode.EdgeClamp);
        return target;
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
}
