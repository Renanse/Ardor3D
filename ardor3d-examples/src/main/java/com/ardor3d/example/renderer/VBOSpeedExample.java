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

import java.util.Random;
import java.util.concurrent.Callable;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.DataMode;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.scenegraph.visitor.DeleteVBOsVisitor;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Illustrates the DataMode class, which describe how we prefer data to be sent to the card.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.VBOSpeedExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_VBOSpeedExample.jpg", //
maxHeapMemory = 64)
public class VBOSpeedExample extends ExampleBase {

    private BasicText frameRateLabel;
    private int frames = 0;
    private long startTime = System.currentTimeMillis();

    private int vboMode = 0;

    public static void main(final String[] args) {
        start(VBOSpeedExample.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {

        final long now = System.currentTimeMillis();
        final long dt = now - startTime;
        if (dt > 1000) {
            final long fps = Math.round(1e3 * frames / dt);
            frameRateLabel.setText(fps + " fps");

            startTime = now;
            frames = 0;
        }

        frames++;
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("VBOSpeedExample");

        final BasicText t = BasicText.createDefaultTextLabel("Text", "[SPACE] VBO Off");
        t.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        t.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t.setTranslation(new Vector3(0, 20, 0));
        _root.attachChild(t);

        final CullState cs = new CullState();
        cs.setCullFace(CullState.Face.Back);
        cs.setEnabled(true);
        _root.setRenderState(cs);

        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        _root.setRenderState(ms);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                TextureStoreFormat.GuessCompressedFormat, true));

        final Node sphereBase = new Node("node");
        _root.attachChild(sphereBase);

        final Random rand = new Random(1337);
        for (int i = 0; i < 100; i++) {
            final Sphere sphere = new Sphere("Sphere", 32, 32, 2);
            sphere.setRandomColors();
            sphere.setModelBound(new BoundingBox());
            sphere.setRenderState(ts);
            sphere.setTranslation(new Vector3(rand.nextDouble() * 100.0 - 50.0, rand.nextDouble() * 100.0 - 50.0, rand
                    .nextDouble() * 100.0 - 250.0));

            sphereBase.attachChild(sphere);
        }

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                vboMode = (vboMode + 1) % 3;
                if (vboMode == 0) {
                    t.setText("[SPACE] VBO Off");
                    sphereBase.getSceneHints().setDataMode(DataMode.Arrays);
                    // run this in the opengl thread
                    GameTaskQueueManager.getManager(_canvas.getCanvasRenderer().getRenderContext()).render(
                            new Callable<Void>() {
                                public Void call() throws Exception {
                                    final DeleteVBOsVisitor viz = new DeleteVBOsVisitor(_canvas.getCanvasRenderer()
                                            .getRenderer());
                                    sphereBase.acceptVisitor(viz, false);
                                    return null;
                                }
                            });
                } else if (vboMode == 1) {
                    t.setText("[SPACE] VBO On");
                    sphereBase.getSceneHints().setDataMode(DataMode.VBO);
                } else if (vboMode == 2) {
                    t.setText("[SPACE] VBO Interleaved On");
                    sphereBase.getSceneHints().setDataMode(DataMode.VBOInterleaved);
                }
            }
        }));

        // Add fps display
        frameRateLabel = BasicText.createDefaultTextLabel("fpsLabel", "");
        frameRateLabel.setTranslation(5, _canvas.getCanvasRenderer().getCamera().getHeight() - 5
                - frameRateLabel.getHeight(), 0);
        frameRateLabel.setTextColor(ColorRGBA.WHITE);
        frameRateLabel.getSceneHints().setOrthoOrder(-1);
        _root.attachChild(frameRateLabel);
    }
}
