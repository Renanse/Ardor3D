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

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.MaterialFace;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.scenegraph.CompileOptions;
import com.ardor3d.util.scenegraph.RenderDelegate;
import com.ardor3d.util.scenegraph.SceneCompiler;

/**
 * Illustrates creating a display list from two sets (i.e. original set and copied set) of Nodes.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.DisplayListDelegateExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_DisplayListDelegateExample.jpg", //
maxHeapMemory = 64)
public class DisplayListDelegateExample extends ExampleBase {

    /** Text fields used to present info about the example. */
    private final BasicText _exampleInfo[] = new BasicText[8];

    private Node heavyNode;

    private double counter = 0;
    private int frames = 0;

    private boolean initialized = false;

    protected Node compiledNodes = new Node();
    protected Node copiedNodes = new Node();

    private boolean showingCompiled = true;

    public static void main(final String[] args) {
        start(DisplayListDelegateExample.class);
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        if (!initialized) {
            initialized = true;

            buildDisplayListDelegate(renderer);
        }

        super.renderExample(renderer);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            counter = 0;
            frames = 0;
            System.out.printf("%7.1f FPS\n", fps);
        }
    }

    private void buildDisplayListDelegate(final Renderer renderer) {
        final CompileOptions options = new CompileOptions();
        options.setDisplayList(true);
        SceneCompiler.compile(heavyNode, renderer, options);

        final Object contextRef = ContextManager.getCurrentContext().getGlContextRep();
        final RenderDelegate delegate = heavyNode.getRenderDelegate(contextRef);

        for (final Spatial spatial : compiledNodes.getChildren()) {
            spatial.setRenderDelegate(delegate, contextRef);
        }
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("DisplayListDelegate - Example");
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 100, 0));
        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 0, 0), new Vector3(0, 1, 0));

        heavyNode = createTestNode();
        _root.attachChild(heavyNode);
        _root.updateGeometricState(0.0f, true);
        _root.detachChild(heavyNode);

        final Random r = new Random();

        r.setSeed(1337);
        for (int i = 0; i < 50; i++) {
            final Node node = new Node("delegate" + i);

            node.setTranslation(new Vector3(-15 + r.nextFloat() * 30, r.nextFloat() * 30, -15 + r.nextFloat() * 30));

            compiledNodes.attachChild(node);
        }
        _root.attachChild(compiledNodes);

        r.setSeed(1337);
        for (int i = 0; i < 50; i++) {
            final Node node = heavyNode.makeCopy(true);

            node.setTranslation(new Vector3(-15 + r.nextFloat() * 30, r.nextFloat() * 30, -15 + r.nextFloat() * 30));

            copiedNodes.attachChild(node);
        }
        _root.attachChild(copiedNodes);
        copiedNodes.getSceneHints().setCullHint(CullHint.Always);

        // Setup labels for presenting example info.
        final Node textNodes = new Node("Text");
        _root.attachChild(textNodes);
        textNodes.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        textNodes.getSceneHints().setLightCombineMode(LightCombineMode.Off);

        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;
        for (int i = 0; i < _exampleInfo.length; i++) {
            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);
            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));
            textNodes.attachChild(_exampleInfo[i]);
        }

        textNodes.updateGeometricState(0.0);
        updateText();

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                showingCompiled = !showingCompiled;
                if (showingCompiled) {
                    compiledNodes.getSceneHints().setCullHint(CullHint.Dynamic);
                    copiedNodes.getSceneHints().setCullHint(CullHint.Always);
                } else {
                    compiledNodes.getSceneHints().setCullHint(CullHint.Always);
                    copiedNodes.getSceneHints().setCullHint(CullHint.Dynamic);
                }
                updateText();
            }
        }));
    }

    protected Node createTestNode() {
        // just a node with lots of random boxes
        final Node node = new Node();

        final Random r = new Random(1337);
        for (int i = 0; i < 150; i++) {
            final Box box = new Box("b" + i, new Vector3(0, 0, 0), new Vector3(0.1f, 0.1f, 0.1f));
            box.updateModelBound();
            box.setRandomColors();
            final MaterialState ms = new MaterialState();
            ms.setDiffuse(MaterialFace.FrontAndBack, new ColorRGBA(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1));
            box.setRenderState(ms);
            box.setTranslation(new Vector3(r.nextFloat(), r.nextFloat(), r.nextFloat()));
            box.setRotation(new Quaternion(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1));
            node.attachChild(box);
        }

        return node;
    }

    /**
     * Update text information.
     */
    private void updateText() {
        _exampleInfo[0].setText("[Space] Showing: " + (showingCompiled ? "DisplayListDelegates" : "Copied nodes"));
    }
}
