/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.obj;

import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.renderer.material.uniform.BlinnPhongKeys;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Spatial;

public class ObjMaterial {
    private final String name;

    float[] Ka = null;
    float[] Kd = null;
    float[] Ks = null;
    float Ns = -1;

    String textureName;
    Texture map_Kd = null;

    int illumType = 2;

    boolean forceBlend = false;
    float d = -1;

    public ObjMaterial(final String name) {
        this.name = name;
    }

    public BlendState getBlendState() {
        if (forceBlend || d != -1 && d < 1.0f) {
            final BlendState blend = new BlendState();
            blend.setBlendEnabled(true);
            blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
            blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
            blend.setTestEnabled(true);
            blend.setTestFunction(BlendState.TestFunction.GreaterThan);
            blend.setReference(0);
            return blend;
        }
        return null;
    }

    public TextureState getTextureState() {
        if (map_Kd != null) {
            final TextureState tState = new TextureState();
            tState.setTexture(map_Kd, 0);
            return tState;
        }
        return null;
    }

    public void applyMaterialProperties(final Spatial spat) {
        if (Ka != null || Kd != null || Ks != null || d != -1 || Ns != -1) {
            final float alpha = d != -1 ? MathUtils.clamp(d, 0, 1) : 1;
            if (Ka != null) {
                spat.setProperty(BlinnPhongKeys.AmbientColor, new ColorRGBA(Ka[0], Ka[1], Ka[2], alpha));
            }
            if (Kd != null) {
                spat.setProperty(BlinnPhongKeys.DiffuseColor, new ColorRGBA(Kd[0], Kd[1], Kd[2], alpha));
            }
            if (Ks != null) {
                spat.setProperty(BlinnPhongKeys.SpecularColor, new ColorRGBA(Ks[0], Ks[1], Ks[2], alpha));
            }

            if (Ns != -1) {
                spat.setProperty(BlinnPhongKeys.Shininess, Ns);
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getTextureName() {
        return textureName;
    }

    public Texture getMap_Kd() {
        return map_Kd;
    }
}
