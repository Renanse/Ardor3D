/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.renderer.effect.rendertarget.RenderTarget_Texture2D;
import com.ardor3d.renderer.effect.step.EffectStep_RenderScreenOverlay;
import com.ardor3d.renderer.effect.step.EffectStep_RenderSpatials;
import com.ardor3d.renderer.effect.step.EffectStep_SetRenderTarget;
import com.ardor3d.renderer.material.MaterialManager;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.scenegraph.Spatial;

public class SimpleBloomEffect extends RenderEffect {
    protected static final String RT_MAIN = "LDRBloomEffect.MAIN";
    protected static final String RT_SECONDARY = "LDRBloomEffect.SECONDARY";

    protected final List<Spatial> _bloomItems = new ArrayList<>();

    protected float _downsampleRatio = .33f;
    protected int _blurPasses = 1;

    protected float _exposureIntensity = 1.3f;
    protected float _exposureCutoff = 0.15f;
    protected float _blendExposure = 1.0f;

    protected RenderMaterial _extractMaterial;
    protected RenderMaterial _blurHorizMaterial;
    protected RenderMaterial _blurVertMaterial;
    protected RenderMaterial _blendMaterial;

    public SimpleBloomEffect() {}

    @Override
    public void prepare(final EffectManager manager) {
        // init targets used in this effect
        initTargets(manager);

        // init materials used in this effect
        initMaterials();

        final boolean useBloomItems = !_bloomItems.isEmpty();

        _steps.clear();
        // step 1: pick whether we are blooming the whole previous render buffer or just specific items
        if (useBloomItems) {
            // render these items to a texture
            _steps.add(new EffectStep_SetRenderTarget(RT_MAIN));

            final EffectStep_RenderSpatials drawBloomStep = new EffectStep_RenderSpatials(null);
            drawBloomStep.getSpatials().addAll(_bloomItems);
            _steps.add(drawBloomStep);
        }

        // step 2: extract intensity
        {
            _steps.add(new EffectStep_SetRenderTarget(RT_SECONDARY));
            final EffectStep_RenderScreenOverlay extract = new EffectStep_RenderScreenOverlay();
            extract.setEnforcedMaterial(_extractMaterial);
            extract.getTargetMap().put(useBloomItems ? RT_MAIN : EffectManager.RT_PREVIOUS, 0);
            _steps.add(extract);
        }

        // step 3: blur
        {
            final EffectStep_RenderScreenOverlay blurHoriz = new EffectStep_RenderScreenOverlay();
            blurHoriz.setEnforcedMaterial(_blurHorizMaterial);
            blurHoriz.getTargetMap().put(RT_SECONDARY, 0);
            final EffectStep_RenderScreenOverlay blurVert = new EffectStep_RenderScreenOverlay();
            blurVert.setEnforcedMaterial(_blurVertMaterial);
            blurVert.getTargetMap().put(RT_MAIN, 0);

            for (int i = 0; i < _blurPasses; i++) {
                _steps.add(new EffectStep_SetRenderTarget(RT_MAIN));
                _steps.add(blurHoriz);
                _steps.add(new EffectStep_SetRenderTarget(RT_SECONDARY));
                _steps.add(blurVert);
            }
        }

        // finally: draw bloom texture and previous texture on fsq, blended.
        _steps.add(new EffectStep_SetRenderTarget(EffectManager.RT_NEXT));

        final EffectStep_RenderScreenOverlay blendOverlay = new EffectStep_RenderScreenOverlay();
        blendOverlay.setEnforcedMaterial(_blendMaterial);
        blendOverlay.getTargetMap().put(EffectManager.RT_PREVIOUS, 0);
        blendOverlay.getTargetMap().put(RT_SECONDARY, 1);
        _steps.add(blendOverlay);

        super.prepare(manager);
    }

    private void initMaterials() {
        _extractMaterial = MaterialManager.INSTANCE.findMaterial("effect/bloom/extract-ldr.yaml");
        _extractMaterial.getTechniques().forEach(tech -> {
            tech.getPasses().forEach(pass -> {
                pass.addUniform(new UniformRef("exposureCutoff", UniformType.Float1, UniformSource.Supplier,
                        (Supplier<Float>) this::getExposureCutoff));
                pass.addUniform(new UniformRef("exposureIntensity", UniformType.Float1, UniformSource.Supplier,
                        (Supplier<Float>) this::getExposureIntensity));
            });
        });

        _blurHorizMaterial = MaterialManager.INSTANCE.findMaterial("effect/bloom/blur_horizontal.yaml");
        _blurVertMaterial = MaterialManager.INSTANCE.findMaterial("effect/bloom/blur_vertical.yaml");

        _blendMaterial = MaterialManager.INSTANCE.findMaterial("effect/bloom/blend.yaml");
        _blendMaterial.getTechniques().forEach(tech -> {
            tech.getPasses().forEach(pass -> {
                pass.addUniform(new UniformRef("exposure", UniformType.Float1, UniformSource.Supplier,
                        (Supplier<Float>) this::getBlendExposure));
            });
        });
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

    public void setExposureIntensity(final float value) {
        _exposureIntensity = value;
    }

    public float getExposureIntensity() {
        return _exposureIntensity;
    }

    public void setExposureCutoff(final float value) {
        _exposureCutoff = value;
    }

    public float getExposureCutoff() {
        return _exposureCutoff;
    }

    public float getBlendExposure() {
        return _blendExposure;
    }

    public void setBlendExposure(final float blendExposure) {
        _blendExposure = blendExposure;
    }

    public void setDownsampleRatio(final float downsampleRatio) {
        _downsampleRatio = downsampleRatio;
    }

    public float getDownsampleRatio() {
        return _downsampleRatio;
    }

    public List<Spatial> getBloomItems() {
        return _bloomItems;
    }
}
