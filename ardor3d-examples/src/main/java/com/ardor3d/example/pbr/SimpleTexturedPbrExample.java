/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.pbr;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.scenegraph.shape.Sphere.TextureMode;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Port of a simple PBR example, with textures, originally from https://learnopengl.com/PBR/Lighting
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pbr.SimpleTexturedPbrExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/pbr_SimpleTexturedPbrExample.jpg", //
        maxHeapMemory = 64)
public class SimpleTexturedPbrExample extends ExampleBase {

    int _lightCount = 1;
    PointLight _lights[] = new PointLight[_lightCount];

    public static void main(final String[] args) {
        start(SimpleTexturedPbrExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3d - Textured Pbr Example ported from LearnOpenGL.com");
        _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
        _controlHandle.setMoveSpeed(10.0);

        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/pbr/rustediron1-alt2/basecolor.png",
                Texture.MinificationFilter.Trilinear, true), 0);
        ts.setTexture(TextureManager.load("images/pbr/rustediron1-alt2/normal.png",
                Texture.MinificationFilter.Trilinear, true), 1);
        ts.setTexture(TextureManager.load("images/pbr/rustediron1-alt2/metallic.png",
                Texture.MinificationFilter.Trilinear, true), 2);
        ts.setTexture(TextureManager.load("images/pbr/rustediron1-alt2/roughness.png",
                Texture.MinificationFilter.Trilinear, true), 3);
        ts.setTexture(TextureManager.load("images/white.png", Texture.MinificationFilter.Trilinear, true), 4); // AO map
        _root.setRenderState(ts);

        final int nrRows = 7, nrColumns = 7;
        final float spacing = 2.5f;

        final Sphere master = new Sphere("sphere", Vector3.ZERO, 64, 64, 1.0, TextureMode.Polar);
        for (int row = 0; row < nrRows; ++row) {
            for (int col = 0; col < nrColumns; ++col) {
                final Vector3 vec = new Vector3((col - (nrColumns / 2)) * spacing, (row - (nrRows / 2)) * spacing,
                        0.0f);

                final Mesh mesh = master.makeCopy(true);
                mesh.setTranslation(vec);

                _root.attachChild(mesh);
            }
        }

        _lightState.detachAll();
        for (int i = 0; i < _lightCount; i++) {
            _lights[i] = new PointLight();
            _lights[i].setDiffuse(new ColorRGBA(150, 150, 150, 1));
            _lightState.attach(_lights[i]);
        }

        _root.setRenderMaterial("pbr/pbr_simpleTextured.yaml");
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        for (int i = 0; i < _lightCount; i++) {
            _lights[i].setLocation(((i % 2 == 1) ? -10 : 10) + MathUtils.sin(timer.getTimeInSeconds() * 2) * 5,
                    ((i / 2) % 2 == 1) ? -10 : 10, 10f);
        }
    }
}
