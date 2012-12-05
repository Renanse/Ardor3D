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
import com.ardor3d.extension.effect.ColorReplaceEffect;
import com.ardor3d.extension.effect.SimpleBloomEffect;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.effect.EffectManager;
import com.ardor3d.renderer.effect.FrameBufferOutputEffect;
import com.ardor3d.renderer.effect.SpatialRTTEffect;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.URLResourceSource;

/**
 * A simple example illustrating use of RenderEffects
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.RenderEffectsExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_RenderEffectsExample.jpg", //
maxHeapMemory = 64)
public class RenderEffectsExample extends ExampleBase {

    private EffectManager effectManager;
    private final Node textNodes = new Node("Text");
    private final BasicText exampleInfo[] = new BasicText[2];

    public static void main(final String[] args) {
        start(RenderEffectsExample.class);
    }

    @Override
    protected void initExample() {

        _canvas.setTitle("RenderEffects Example");

        // Create a new sphere that rotates
        final Sphere sphere = new Sphere("Sphere", new Vector3(0, 0, 0), 32, 32, 5);
        sphere.setModelBound(new BoundingBox());
        sphere.setTranslation(new Vector3(0, 0, -15));
        sphere.addController(new SpatialController<Spatial>() {
            private final Vector3 _axis = new Vector3(1, 1, 0.5f).normalizeLocal();
            private final Matrix3 _rotate = new Matrix3();
            private double _angle = 0;

            public void update(final double time, final Spatial caller) {
                // update our rotation
                _angle = _angle + (_timer.getTimePerFrame() * 25);
                if (_angle > 180) {
                    _angle = -180;
                }

                _rotate.fromAngleNormalAxis(_angle * MathUtils.DEG_TO_RAD, _axis);
                sphere.setRotation(_rotate);
            }
        });
        _root.attachChild(sphere);

        // Add a texture to the sphere.
        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        sphere.setRenderState(ts);

        // Setup our manager
        effectManager = new EffectManager(_settings, TextureStoreFormat.RGBA8);
        effectManager.setSceneCamera(_canvas.getCanvasRenderer().getCamera());

        // Add a step to draw our scene to our scene texture.
        effectManager.addEffect(new SpatialRTTEffect("*Next", null, _root));

        // Add a bloom effect
        final SimpleBloomEffect bloomEffect = new SimpleBloomEffect();
        effectManager.addEffect(bloomEffect);

        // Add a sepia tone post effect
        final Texture sepiaTexture = TextureManager.load(
                new URLResourceSource(ResourceLocatorTool.getClassPathResource(ColorReplaceEffect.class,
                        "com/ardor3d/extension/effect/sepiatone.png")), Texture.MinificationFilter.Trilinear, true);
        final ColorReplaceEffect sepiaEffect = new ColorReplaceEffect(sepiaTexture);
        effectManager.addEffect(sepiaEffect);

        // Finally, add a step to draw the result to the framebuffer
        final FrameBufferOutputEffect out = new FrameBufferOutputEffect();
        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(SourceFunction.SourceAlpha);
        blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
        out.setBlend(blend);
        effectManager.addEffect(out);

        // setup effects
        effectManager.setupEffects();

        // add toggle for the effects
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                bloomEffect.setEnabled(!bloomEffect.isEnabled());
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                sepiaEffect.setEnabled(!sepiaEffect.isEnabled());
                updateText();
            }
        }));

        // setup text labels
        for (int i = 0; i < exampleInfo.length; i++) {
            exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);
            exampleInfo[i].setTranslation(new Vector3(10, _canvas.getCanvasRenderer().getCamera().getHeight() - (i + 1)
                    * 20, 0));
            textNodes.attachChild(exampleInfo[i]);
        }

        textNodes.updateGeometricState(0.0);
        updateText();
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        effectManager.renderEffects(renderer);
        textNodes.onDraw(renderer);
    }

    /**
     * Update text information.
     */
    private void updateText() {
        exampleInfo[0].setText("[1] Bloom effect: " + effectManager.getEffects().get(1).isEnabled());
        exampleInfo[1].setText("[2] Sepia effect: " + effectManager.getEffects().get(2).isEnabled());
    }
}
