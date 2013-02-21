/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.effect;

import java.util.logging.Logger;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.effect.bloom.BloomRenderPass;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.pass.BasicPassManager;
import com.ardor3d.renderer.pass.RenderPass;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Torus;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * A simple example showing bloom.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.effect.BloomExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/effect_BloomExample.jpg", //
maxHeapMemory = 64)
public class BloomExample extends ExampleBase {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(BloomExample.class.getName());

    /** Pass manager. */
    private BasicPassManager _passManager;

    private BloomRenderPass bloomRenderPass;

    /** Text fields used to present info about the example. */
    private final BasicText _exampleInfo[] = new BasicText[8];

    public static void main(final String[] args) {
        start(BloomExample.class);
    }

    double counter = 0;
    int frames = 0;

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        _passManager.updatePasses(timer.getTimePerFrame());

        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            counter = 0;
            frames = 0;
            System.out.printf("%7.1f FPS\n", fps);
        }
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        _passManager.renderPasses(renderer);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Bloom - Example");
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(200, 150, 200));
        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 0, 0), Vector3.UNIT_Y);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                updateText();
            }
        }));

        _passManager = new BasicPassManager();

        final RenderPass rootPass = new RenderPass();
        rootPass.add(_root);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        _root.setRenderState(ts);

        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        _root.setRenderState(ms);

        _root.attachChild(createObjects());

        bloomRenderPass = new BloomRenderPass(_canvas.getCanvasRenderer().getCamera(), 4);

        if (!bloomRenderPass.isSupported()) {
            logger.severe("Bloom not supported!");
            return;
        } else {
            bloomRenderPass.add(_root);
            // TODO: what?
            // bloomRenderPass.setUseCurrentScene(true);
        }

        // Setup textfields for presenting example info.
        final Node textNodes = new Node("Text");
        final RenderPass renderPass = new RenderPass();
        renderPass.add(textNodes);
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

        // Populate passmanager with passes.
        _passManager.add(rootPass);
        _passManager.add(bloomRenderPass);
        _passManager.add(renderPass);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomRenderPass.setEnabled(!bloomRenderPass.isEnabled());
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomRenderPass.setBlurSize(bloomRenderPass.getBlurSize() - 0.001f);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomRenderPass.setBlurSize(bloomRenderPass.getBlurSize() + 0.001f);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FOUR), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomRenderPass.setExposurePow(bloomRenderPass.getExposurePow() - 1.0f);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FIVE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomRenderPass.setExposurePow(bloomRenderPass.getExposurePow() + 1.0f);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SIX), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomRenderPass.setExposureCutoff(bloomRenderPass.getExposureCutoff() - 0.1f);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SEVEN), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomRenderPass.setExposureCutoff(bloomRenderPass.getExposureCutoff() + 0.1f);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.EIGHT), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomRenderPass.setBlurIntensityMultiplier(bloomRenderPass.getBlurIntensityMultiplier() - 0.1f);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.NINE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomRenderPass.setBlurIntensityMultiplier(bloomRenderPass.getBlurIntensityMultiplier() + 0.1f);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ZERO), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomRenderPass.resetParameters();
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.J), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomRenderPass.setUseCurrentScene(!bloomRenderPass.useCurrentScene());
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomRenderPass.setUseSeparateConvolution(!bloomRenderPass.isUseSeparateConvolution());
                updateText();
            }
        }));
    }

    /**
     * Update text information.
     */
    private void updateText() {
        _exampleInfo[0].setText("[1] Bloom renderpass is: " + (bloomRenderPass.isEnabled() ? "On" : "Off"));
        _exampleInfo[1].setText("[2/3] Blur size: " + bloomRenderPass.getBlurSize());
        _exampleInfo[2].setText("[4/5] Exposure strength: " + bloomRenderPass.getExposurePow());
        _exampleInfo[3].setText("[6/7] Exposure cutoff: " + bloomRenderPass.getExposureCutoff());
        _exampleInfo[4].setText("[8/9] Blur intensity: " + bloomRenderPass.getBlurIntensityMultiplier());
        _exampleInfo[5].setText("[0] Reset parameters");
        _exampleInfo[6].setText("[J] Use current scene: " + (bloomRenderPass.useCurrentScene() ? "On" : "Off"));
        _exampleInfo[7].setText("[Space] Use separate convolution: "
                + (bloomRenderPass.isUseSeparateConvolution() ? "On" : "Off"));
    }

    /**
     * Creates the scene objects.
     * 
     * @return the node containing the objects
     */
    private Node createObjects() {
        final Node objects = new Node("objects");

        final Torus torus = new Torus("Torus", 30, 20, 8, 17);
        torus.setTranslation(new Vector3(50, -5, 20));
        TextureState ts = new TextureState();
        torus.addController(new SpatialController<Torus>() {
            private double timer = 0;
            private final Matrix3 rotation = new Matrix3();

            public void update(final double time, final Torus caller) {
                timer += time * 0.5;
                caller.setTranslation(Math.sin(timer) * 40.0, Math.sin(timer) * 40.0, Math.cos(timer) * 40.0);
                rotation.fromAngles(timer * 0.5, timer * 0.5, timer * 0.5);
                caller.setRotation(rotation);
            }
        });

        Texture t0 = TextureManager.load("images/ardor3d_white_256.jpg",
                Texture.MinificationFilter.BilinearNearestMipMap, true);
        ts.setTexture(t0, 0);
        ts.setEnabled(true);
        torus.setRenderState(ts);
        objects.attachChild(torus);

        ts = new TextureState();
        t0 = TextureManager
                .load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
        t0.setWrap(Texture.WrapMode.Repeat);
        ts.setTexture(t0);

        Box box = new Box("box1", new Vector3(-10, -10, -10), new Vector3(10, 10, 10));
        box.setTranslation(new Vector3(0, -7, 0));
        box.setRenderState(ts);
        objects.attachChild(box);

        box = new Box("box2", new Vector3(-5, -5, -5), new Vector3(5, 5, 5));
        box.setTranslation(new Vector3(15, 10, 0));
        box.setRenderState(ts);
        objects.attachChild(box);

        box = new Box("box3", new Vector3(-5, -5, -5), new Vector3(5, 5, 5));
        box.setTranslation(new Vector3(0, -10, 15));
        box.setRenderState(ts);
        objects.attachChild(box);

        box = new Box("box4", new Vector3(-5, -5, -5), new Vector3(5, 5, 5));
        box.setTranslation(new Vector3(20, 0, 0));
        box.setRenderState(ts);
        objects.attachChild(box);

        ts = new TextureState();
        t0 = TextureManager
                .load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
        t0.setWrap(Texture.WrapMode.Repeat);
        ts.setTexture(t0);

        box = new Box("box5", new Vector3(-50, -2, -50), new Vector3(50, 2, 50));
        box.setTranslation(new Vector3(0, -15, 0));
        box.setRenderState(ts);
        box.setModelBound(new BoundingBox());
        objects.attachChild(box);

        return objects;
    }
}
