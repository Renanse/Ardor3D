/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.effect;

import java.util.function.Supplier;

import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.renderer.effect.step.EffectStep_RenderScreenOverlay;
import com.ardor3d.renderer.effect.step.EffectStep_SetRenderTarget;
import com.ardor3d.renderer.material.MaterialManager;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;

public class ColorReplaceEffect extends RenderEffect {

    protected float _redWeight = 0.3086f;
    protected float _greenWeight = 0.6094f;
    protected float _blueWeight = 0.0820f;
    protected Texture _colorRampTexture;

    protected RenderMaterial _replaceMaterial;

    public ColorReplaceEffect(final Texture colorRampTexture) {
        _colorRampTexture = colorRampTexture;
        _colorRampTexture.setWrap(WrapMode.EdgeClamp);
    }

    @Override
    public void prepare(final EffectManager manager) {
        _replaceMaterial = MaterialManager.INSTANCE.findMaterial("effect/color_replace.yaml");
        injectUniforms(_replaceMaterial);

        _steps.clear();
        _steps.add(new EffectStep_SetRenderTarget(EffectManager.RT_NEXT));

        final EffectStep_RenderScreenOverlay colorizeStep = new EffectStep_RenderScreenOverlay();
        colorizeStep.getTextureState().setTexture(_colorRampTexture, 1);
        colorizeStep.getTargetMap().put(EffectManager.RT_PREVIOUS, 0);
        colorizeStep.setEnforcedMaterial(_replaceMaterial);
        _steps.add(colorizeStep);

        super.prepare(manager);
    }

    private void injectUniforms(final RenderMaterial replaceMaterial) {
        replaceMaterial.getTechniques().forEach(tech -> {
            tech.getPasses().forEach(pass -> {
                pass.addUniform(new UniformRef("redWeight", UniformType.Float1, UniformSource.Supplier,
                        (Supplier<Float>) this::getRedWeight));
                pass.addUniform(new UniformRef("greenWeight", UniformType.Float1, UniformSource.Supplier,
                        (Supplier<Float>) this::getGreenWeight));
                pass.addUniform(new UniformRef("blueWeight", UniformType.Float1, UniformSource.Supplier,
                        (Supplier<Float>) this::getBlueWeight));
            });
        });
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

    public Texture getColorRampTexture() {
        return _colorRampTexture;
    }

    public void setColorRampTexture(final Texture colorRampTexture) {
        _colorRampTexture = colorRampTexture;
    }
}
