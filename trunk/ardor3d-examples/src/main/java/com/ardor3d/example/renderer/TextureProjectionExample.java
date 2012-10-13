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
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.ApplyMode;
import com.ardor3d.image.util.TextureProjector;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Illustrates the TextureProjector class, which projects a two-dimensional texture onto a three-dimensional surface.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.TextureProjectionExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_TextureProjectionExample.jpg", //
maxHeapMemory = 64)
public class TextureProjectionExample extends ExampleBase {

    private final TextureProjector projector = new TextureProjector();
    private Texture projectedTexture;
    Vector3 projectLoc = new Vector3();
    Vector3 projectTarget = new Vector3();

    public static void main(final String[] args) {
        start(TextureProjectionExample.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        projectLoc.set(
                MathUtils.sin(timer.getTimeInSeconds() - MathUtils.PI)
                        * (MathUtils.sin(timer.getTimeInSeconds() * 1.1) * 15.0 + 20.0),
                MathUtils.sin(timer.getTimeInSeconds() * 0.7 - MathUtils.PI) * 10.0 + 30.0,
                MathUtils.cos(timer.getTimeInSeconds() * 0.4 - MathUtils.PI)
                        * (MathUtils.sin(timer.getTimeInSeconds()) * 15.0 + 20.0));
        projectTarget.set(
                MathUtils.sin(timer.getTimeInSeconds() * 0.4)
                        * (MathUtils.sin(timer.getTimeInSeconds() * 0.7) * 10.0 + 10.0), 0.0,
                MathUtils.cos(timer.getTimeInSeconds() * 0.3)
                        * (MathUtils.sin(timer.getTimeInSeconds() * 0.8) * 10.0 + 10.0));

        // update texture matrix
        projector.setLocation(projectLoc);
        projector.lookAt(projectTarget, Vector3.UNIT_Y);
        projector.updateTextureMatrix(projectedTexture);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D: Texture Projection Example");

        _canvas.getCanvasRenderer().getCamera().setLocation(20, 40, 80);
        _canvas.getCanvasRenderer().getCamera().lookAt(0, 0, 0, Vector3.UNIT_Y);

        projector.setFrustumPerspective(20.0f, 1.0f, 1.0f, 1000.0f);

        final Quad floor = new Quad("floor", 100, 100);
        floor.setRotation(new Matrix3().fromAngleNormalAxis(MathUtils.HALF_PI, Vector3.UNIT_X));
        _root.attachChild(floor);

        final Teapot teapot = new Teapot("teapot");
        teapot.setScale(5);
        _root.attachChild(teapot);

        // Add a texture to the scene.
        final TextureState ts = new TextureState();
        projectedTexture = TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                true);
        ts.setTexture(projectedTexture);
        projectedTexture.setTextureMatrix(new Matrix4());
        projectedTexture.setWrap(Texture.WrapMode.BorderClamp);
        projectedTexture.setEnvironmentalMapMode(Texture.EnvironmentalMapMode.EyeLinear);
        projectedTexture.setApply(ApplyMode.Add);
        projectedTexture.setConstantColor(ColorRGBA.WHITE);
        _root.setRenderState(ts);
    }
}
