/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state.record;

import java.util.Arrays;

import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.ApplyMode;
import com.ardor3d.image.Texture.CombinerFunctionAlpha;
import com.ardor3d.image.Texture.CombinerFunctionRGB;
import com.ardor3d.image.Texture.CombinerOperandAlpha;
import com.ardor3d.image.Texture.CombinerOperandRGB;
import com.ardor3d.image.Texture.CombinerScale;
import com.ardor3d.image.Texture.CombinerSource;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Vector3;

/**
 * Represents a texture unit in opengl
 */
public class TextureUnitRecord extends StateRecord {
    public boolean enabled[] = new boolean[Texture.Type.values().length];
    public Matrix4 texMatrix = new Matrix4();
    public Vector3 texScale = new Vector3();
    public int boundTexture = -1;
    public ApplyMode envMode = null;
    public CombinerScale envRGBScale = null;
    public CombinerScale envAlphaScale = null;
    public ColorRGBA blendColor = new ColorRGBA(-1, -1, -1, -1);
    public CombinerFunctionRGB rgbCombineFunc = null;
    public CombinerFunctionAlpha alphaCombineFunc = null;
    public CombinerSource combSrcRGB0 = null, combSrcRGB1 = null, combSrcRGB2 = null;
    public CombinerOperandRGB combOpRGB0 = null, combOpRGB1 = null, combOpRGB2 = null;
    public CombinerSource combSrcAlpha0 = null, combSrcAlpha1 = null, combSrcAlpha2 = null;
    public CombinerOperandAlpha combOpAlpha0 = null, combOpAlpha1 = null, combOpAlpha2 = null;
    public boolean identityMatrix = true;
    public float lodBias = 0f;

    public boolean textureGenQ = false, textureGenR = false, textureGenS = false, textureGenT = false;
    public int textureGenQMode = -1, textureGenRMode = -1, textureGenSMode = -1, textureGenTMode = -1;

    public TextureUnitRecord() {}

    @Override
    public void invalidate() {
        super.invalidate();

        Arrays.fill(enabled, false);
        texMatrix.setIdentity();
        texScale.zero();
        boundTexture = -1;
        lodBias = 0;
        envMode = null;
        envRGBScale = null;
        envAlphaScale = null;
        blendColor.set(-1, -1, -1, -1);
        rgbCombineFunc = null;
        alphaCombineFunc = null;
        combSrcRGB0 = null;
        combSrcRGB1 = null;
        combSrcRGB2 = null;
        combOpRGB0 = null;
        combOpRGB1 = null;
        combOpRGB2 = null;
        combSrcAlpha0 = null;
        combSrcAlpha1 = null;
        combSrcAlpha2 = null;
        combOpAlpha0 = null;
        combOpAlpha1 = null;
        combOpAlpha2 = null;
        identityMatrix = false;

        textureGenQ = false;
        textureGenR = false;
        textureGenS = false;
        textureGenT = false;
        textureGenQMode = -1;
        textureGenRMode = -1;
        textureGenSMode = -1;
        textureGenTMode = -1;
    }
}
