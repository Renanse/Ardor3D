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

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Quaternion;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.MaterialState.MaterialFace;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Dome;
import com.ardor3d.scenegraph.shape.Quad;

/**
 * A demonstration using MaterialState to set lighting equation parameters.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.MaterialFaceExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_MaterialFaceExample.jpg", //
maxHeapMemory = 64)
public class MaterialFaceExample extends ExampleBase {

    public static void main(final String[] args) {
        start(MaterialFaceExample.class);
    }

    @Override
    public void initExample() {
        _root.attachChild(createFloor());
        _root.attachChild(createSky());

        _lightState.setTwoSidedLighting(true);
    }

    private Mesh createFloor() {
        final Mesh floor = new Quad("Floor", 100, 100);
        floor.updateModelBound();
        // set the color to green using a single color value
        floor.setDefaultColor(ColorRGBA.GREEN);
        // move back from camera.
        floor.setTranslation(0, -5, -20);
        // rotate to point up
        floor.setRotation(new Quaternion(-1, 0, 0, 1));

        // Add a material state.
        final MaterialState ms = new MaterialState();
        floor.setRenderState(ms);
        // Pull diffuse color for front and back from mesh color
        ms.setColorMaterial(ColorMaterial.Diffuse);
        ms.setColorMaterialFace(MaterialFace.FrontAndBack);

        return floor;
    }

    private Mesh createSky() {
        final Dome sky = new Dome("Sky", 30, 30, 10);
        sky.updateModelBound();
        // set the vertex colors to red. Same effect as setDefaultColor here, but uses more memory.
        sky.setSolidColor(ColorRGBA.RED);
        // move back from camera.
        sky.setTranslation(0, 10, -20);

        // Add a material state
        final MaterialState ms = new MaterialState();
        sky.setRenderState(ms);
        // Pull diffuse color for front from mesh color
        ms.setColorMaterial(ColorMaterial.Diffuse);
        ms.setColorMaterialFace(MaterialFace.Front);
        // Set shininess for front and back
        ms.setShininess(MaterialFace.FrontAndBack, 100);
        // Set specular color for front
        ms.setSpecular(MaterialFace.Front, ColorRGBA.RED);
        // Set specular color for back
        ms.setSpecular(MaterialFace.Back, ColorRGBA.WHITE);
        // set the back diffuse color to blue
        ms.setDiffuse(MaterialFace.Back, ColorRGBA.BLUE);

        return sky;
    }

}
