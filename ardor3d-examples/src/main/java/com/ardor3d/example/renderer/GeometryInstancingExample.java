/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.renderer;

import java.util.Random;

import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * Demonstrates the use of geometry instancing and compares it to VBO.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.GeometryInstancingExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_GeometryInstancingExample.jpg", //
        maxHeapMemory = 64)
public class GeometryInstancingExample extends ExampleBase {

    private BasicText frameRateLabel;
    private int frames = 0;
    private long startTime = System.currentTimeMillis();

    private boolean instancingEnabled = true;

    private Node _base;

    public static void main(final String[] args) {
        // Turn on support for instanced geometry.
        System.setProperty("ardor3d.enableInstancedGeometrySupport", "true");
        start(GeometryInstancingExample.class);
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
        final Vector3 trans = new Vector3(Math.sin(timer.getTime() / 1000000000.0) * 70 - 35, 0, -100);
        _base.setTranslation(trans);
        light.setLocation(trans);
        frames++;
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("GeometryInstancingExample");

        final BasicText t2 = BasicText.createDefaultTextLabel("Text", "[I] Instancing On");
        t2.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
        t2.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t2.setTranslation(new Vector3(0, 50, 0));
        _orthoRoot.attachChild(t2);

        final CullState cs = new CullState();
        cs.setCullFace(CullState.Face.Back);
        cs.setEnabled(true);
        _root.setRenderState(cs);

//        _shader = new ShaderState();
//        try {
//            _shader.setShader(ShaderType.Vertex, "geometryBasic", ResourceLocatorTool.getClassPathResourceAsString(
//                    GLSLRibbonExample.class, "com/ardor3d/example/media/shaders/geometryBasic.vert.glsl"));
//            _shader.setShader(ShaderType.Fragment, "geometryBasic", ResourceLocatorTool.getClassPathResourceAsString(
//                    GLSLRibbonExample.class, "com/ardor3d/example/media/shaders/geometryBasic.frag.glsl"));
//
//        } catch (final IOException ex) {
//            ex.printStackTrace();
//        }

        _base = new Node("node");
        _root.attachChild(_base);

        final Node instancedBase = new Node("instancedBase");
//        instancedBase.setRenderState(_shader);
        _base.attachChild(instancedBase);
        instancedBase.getSceneHints().setCullHint(CullHint.Dynamic);

        final Node unInstancedBase = new Node("unInstancedBase");
//        unInstancedBase.setRenderState(_shader);
        _base.attachChild(unInstancedBase);
        unInstancedBase.getSceneHints().setCullHint(CullHint.Always);

        final int nrOfObjects = 500;

//        generateSpheres(instancedBase, true, nrOfObjects);
//        generateSpheres(unInstancedBase, false, nrOfObjects);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.I), new TriggerAction() {

            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                instancingEnabled = !instancingEnabled;
                if (instancingEnabled) {
                    t2.setText("[I] Instancing On");
                    instancedBase.getSceneHints().setCullHint(CullHint.Dynamic);
                    unInstancedBase.getSceneHints().setCullHint(CullHint.Always);

                } else {
                    t2.setText("[I] Instancing Off");
                    instancedBase.getSceneHints().setCullHint(CullHint.Always);
                    unInstancedBase.getSceneHints().setCullHint(CullHint.Dynamic);

                }
            }
        }));

        // Add fps display
        frameRateLabel = BasicText.createDefaultTextLabel("fpsLabel", "");
        frameRateLabel.setTranslation(5,
                _canvas.getCanvasRenderer().getCamera().getHeight() - 5 - frameRateLabel.getHeight(), 0);
        frameRateLabel.setTextColor(ColorRGBA.WHITE);
        frameRateLabel.getSceneHints().setOrthoOrder(-1);
        _root.attachChild(frameRateLabel);
    }

    protected void generateSpheres(final Node modelBase, final boolean useInstancing, final int nrOfObjects) {
        final Random rand = new Random(1337);

        final Sphere sphereroot = new Sphere("Sphere", 8, 8, 2);
        for (int i = 0; i < nrOfObjects; i++) {
            Sphere sphere;
            if (useInstancing) {
                sphere = (Sphere) sphereroot.makeInstanced();
            } else {
                sphere = (Sphere) sphereroot.makeCopy(true);
            }
            sphere.setRandomColors();
            sphere.setModelBound(new BoundingSphere());
            sphere.setTranslation(new Vector3(rand.nextDouble() * 100.0 - 50.0, rand.nextDouble() * 100.0 - 50.0,
                    rand.nextDouble() * 100.0 - 250.0));
            sphere.getSceneHints().setCullHint(CullHint.Dynamic);
            modelBase.attachChild(sphere);
        }

    }
}
