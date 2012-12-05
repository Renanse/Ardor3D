/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.ApplyMode;
import com.ardor3d.image.Texture.CombinerFunctionRGB;
import com.ardor3d.image.Texture.CombinerOperandRGB;
import com.ardor3d.image.Texture.CombinerSource;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Shows interpolated textures using texture combine.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.TexCombineExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_TexCombineExample.jpg", //
maxHeapMemory = 64)
public class TexCombineExample extends ExampleBase {

    public static void main(final String[] args) {
        start(TexCombineExample.class);
    }

    private final TextureState ts = new TextureState();
    float blend = 0;
    int direction = 1;

    @Override
    protected void initExample() {
        _canvas.setTitle("TexCombine Example");

        final Quad quad = new Quad("Box", 4, 4);
        quad.setModelBound(new BoundingBox());
        quad.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        _root.attachChild(quad);

        final Texture ardorLogo = TextureManager.load("images/ardor3d_white_256.jpg",
                Texture.MinificationFilter.Trilinear, true);
        addTexture(ardorLogo, 0, blend);
        quad.setRenderState(ts);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        blend += direction * (timer.getTimePerFrame() / 5);
        if (blend > 1.0f) {
            blend = 1.0f;
            direction = -1;
        } else if (blend < 0.0f) {
            blend = 0;
            direction = 1;
        }
        updateLayerBlend(0, blend);
    }

    private void addTexture(final Texture src, final int layer, final float blend) {
        final Texture tex = src.createSimpleClone();

        // set up texture blending properties
        tex.setApply(ApplyMode.Combine);
        tex.setCombineFuncRGB(CombinerFunctionRGB.Interpolate);
        // color 1
        tex.setCombineSrc0RGB(CombinerSource.CurrentTexture);
        tex.setCombineOp0RGB(CombinerOperandRGB.SourceColor);
        // color 2
        tex.setCombineSrc1RGB(CombinerSource.Previous);
        tex.setCombineOp1RGB(CombinerOperandRGB.SourceColor);
        // interpolate param will come from alpha of constant color
        tex.setCombineSrc2RGB(CombinerSource.Constant);
        tex.setCombineOp2RGB(CombinerOperandRGB.SourceAlpha);

        ts.setTexture(tex, layer);

        // set blend
        updateLayerBlend(layer, blend);
    }

    public void updateLayerBlend(final int layer, final float amount) {
        final Texture tex = ts.getTexture(layer);

        if (tex != null && tex.getConstantColor().getAlpha() != amount) {
            tex.setConstantColor(0, 0, 0, amount);
        }
    }
}
