/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.effect;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.extension.QuadImposterNode;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.scenegraph.shape.Torus;
import com.ardor3d.scenegraph.visitor.UpdateModelBoundVisitor;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.TextureManager;

/**
 * A demonstration of the QuadImposterNode class; which sets the texture level of detail for a Node.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.effect.QuadImposterExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/effect_QuadImposterExample.jpg", //
        maxHeapMemory = 64)
public class QuadImposterExample extends ExampleBase {
    private boolean showImposter = true;

    public static void main(final String[] args) {
        start(QuadImposterExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Various size imposters - Example");

        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 60, 80));
        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(), Vector3.UNIT_Y);
        _root.setRenderMaterial("unlit/textured/basic.yaml");

        final BasicText keyText = BasicText.createDefaultTextLabel("Text", "[SPACE] Switch imposters off");
        keyText.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        keyText.setTranslation(new Vector3(0, 20, 0));
        _orthoRoot.attachChild(keyText);

        final Box box = new Box("Box", new Vector3(), 150, 1, 150);
        box.setModelBound(new BoundingBox());
        box.setTranslation(new Vector3(0, -10, 0));
        _root.attachChild(box);

        final QuadImposterNode imposter0 = new QuadImposterNode("Imposter1", 512, 512, _settings.getDepthBits(),
                _settings.getSamples(), _timer);
        imposter0.setRedrawRate(0.0); // No timed update
        imposter0.setCameraAngleThreshold(1.0 * MathUtils.DEG_TO_RAD);
        imposter0.setCameraDistanceThreshold(0.1);
        _root.attachChild(imposter0);

        final Node scene1 = createModel();
        scene1.setTranslation(0, 0, 0);
        imposter0.attachChild(scene1);

        final QuadImposterNode imposter1 = new QuadImposterNode("Imposter1", 256, 256, _settings.getDepthBits(),
                _settings.getSamples(), _timer);
        imposter1.setRedrawRate(0.0); // No timed update
        imposter1.setCameraAngleThreshold(1.0 * MathUtils.DEG_TO_RAD);
        imposter1.setCameraDistanceThreshold(0.1);
        _root.attachChild(imposter1);

        final Node scene2 = createModel();
        scene2.setTranslation(-15, 0, -25);
        imposter1.attachChild(scene2);

        final QuadImposterNode imposter2 = new QuadImposterNode("Imposter2", 128, 128, _settings.getDepthBits(),
                _settings.getSamples(), _timer);
        imposter2.setRedrawRate(0.0); // No timed update
        imposter2.setCameraAngleThreshold(1.0 * MathUtils.DEG_TO_RAD);
        imposter2.setCameraDistanceThreshold(0.1);
        _root.attachChild(imposter2);

        final Node scene3 = createModel();
        scene3.setTranslation(15, 0, -25);
        imposter2.attachChild(scene3);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                showImposter = !showImposter;
                if (showImposter) {
                    _root.detachChild(scene1);
                    _root.detachChild(scene2);
                    _root.detachChild(scene3);
                    imposter0.attachChild(scene1);
                    imposter1.attachChild(scene2);
                    imposter2.attachChild(scene3);
                    _root.attachChild(imposter0);
                    _root.attachChild(imposter1);
                    _root.attachChild(imposter2);

                    keyText.setText("[SPACE] Switch imposters off");
                } else {
                    _root.detachChild(imposter0);
                    _root.detachChild(imposter1);
                    _root.detachChild(imposter2);
                    _root.attachChild(scene1);
                    _root.attachChild(scene2);
                    _root.attachChild(scene3);

                    keyText.setText("[SPACE] Switch imposters on");
                }
            }
        }));

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));

        _root.setRenderState(ts);

        _root.acceptVisitor(new UpdateModelBoundVisitor(), false);
    }

    private Node createModel() {
        final Node node = new Node("Node");

        final Box box = new Box("Box", new Vector3(), 5, 5, 5);
        box.setModelBound(new BoundingBox());
        box.setTranslation(new Vector3(-3, 0, 0));
        box.setRandomColors();
        node.attachChild(box);

        final Teapot teapot = new Teapot("Teapot");
        teapot.setScale(2.0);
        teapot.setTranslation(new Vector3(3, 0, 0));
        node.attachChild(teapot);

        final Torus torus = new Torus("Torus", 128, 128, 2, 4);
        torus.setTranslation(new Vector3(-8, 3, 0));
        node.attachChild(torus);

        return node;
    }
}
