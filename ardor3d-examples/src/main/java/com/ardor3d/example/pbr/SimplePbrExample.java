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
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.surface.PbrSurface;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * Port of a simple PBR example, originally from https://learnopengl.com/PBR/Lighting
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pbr.SimplePbrExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/pbr_SimplePbrExample.jpg", //
        maxHeapMemory = 64)
public class SimplePbrExample extends ExampleBase {

    int _lightCount = 4;
    PointLight _lights[] = new PointLight[_lightCount];

    public static void main(final String[] args) {
        start(SimplePbrExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3d - Pbr Example ported from LearnOpenGL.com");
        _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
        _controlHandle.setMoveSpeed(20.0);

        final int nrRows = 7, nrColumns = 7;
        final float spacing = 8f;

        final Teapot master = new Teapot("teapot");
        for (int row = 0; row < nrRows; ++row) {
            final float metallic = (float) row / (float) nrRows;
            for (int col = 0; col < nrColumns; ++col) {
                final float roughness = MathUtils.clamp((float) col / (float) nrColumns, 0.05f, 1.0f);

                final Vector3 vec = new Vector3((col - (nrColumns / 2)) * spacing, (row - (nrRows / 2)) * spacing,
                        0.0f);

                final Mesh mesh = master.makeCopy(true);
                mesh.setTranslation(vec);
                mesh.setProperty("surface", new PbrSurface(new ColorRGBA(0.5f, 0f, 0f, 1f), metallic, roughness, 1.0f));

                _root.attachChild(mesh);
            }
        }

        _lightState.detachAll();
        for (int i = 0; i < _lightCount; i++) {
            _lights[i] = new PointLight();
            _lights[i].setDiffuse(new ColorRGBA(900, 900, 900, 1));
            _lightState.attach(_lights[i]);
        }

        _root.setRenderMaterial("pbr/pbr_untextured_simple.yaml");
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        for (int i = 0; i < _lightCount; i++) {
            _lights[i].setLocation(((i % 2 == 1) ? -30 : 30) + MathUtils.sin(timer.getTimeInSeconds() * 2) * 15,
                    ((i / 2) % 2 == 1) ? -30 : 30, 30f);
        }
    }
}
