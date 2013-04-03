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

import java.nio.ByteBuffer;
import java.util.List;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.EnvironmentalMapMode;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.util.GeneratedImageFactory;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.TextureKey;
import com.google.common.collect.Lists;

/**
 * Very simple example showing use of a Texture3D texture.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.Texture3DExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_Texture3DExample.jpg", //
maxHeapMemory = 64)
public class Texture3DExample extends ExampleBase {

    public static void main(final String[] args) {
        start(Texture3DExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Texture3D Example - Ardor3D");

        final TextureState ts = new TextureState();
        final Texture texture = createTexture();
        texture.setEnvironmentalMapMode(EnvironmentalMapMode.ObjectLinear);
        ts.setTexture(texture);
        _root.setRenderState(ts);

        final Sphere sp = new Sphere("sphere", 16, 16, 2);
        _root.attachChild(sp);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final Texture tex = createTexture();
                tex.setEnvironmentalMapMode(EnvironmentalMapMode.ObjectLinear);
                ts.setTexture(tex);
                ts.setNeedsRefresh(true);
            }
        }));
    }

    private Texture createTexture() {
        final Texture3D tex = new Texture3D();
        tex.setMinificationFilter(MinificationFilter.BilinearNoMipMaps);
        tex.setTextureKey(TextureKey.getRTTKey(MinificationFilter.BilinearNoMipMaps));
        final Image img = new Image();
        img.setWidth(32);
        img.setHeight(32);
        img.setDepth(32);

        final List<ByteBuffer> data = Lists.newArrayList();
        for (int i = 0; i < 32; i++) {
            final Image colorImage = GeneratedImageFactory
                    .createSolidColorImage(ColorRGBA.randomColor(null), false, 32);
            data.add(colorImage.getData(0));
            if (i == 0) {
                img.setDataFormat(colorImage.getDataFormat());
                img.setDataType(colorImage.getDataType());
            }
        }
        img.setData(data);
        tex.setImage(img);
        return tex;
    }
}
