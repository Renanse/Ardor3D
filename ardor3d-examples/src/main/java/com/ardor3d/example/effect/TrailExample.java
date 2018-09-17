/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.effect;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.useful.TrailMesh;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * An example of using TrailMesh.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.effect.TrailExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/effect_TrailExample.jpg", //
        maxHeapMemory = 64)
public class TrailExample extends ExampleBase {

    private Sphere sphere;
    private TrailMesh trailMesh;

    private boolean updateTrail = true;
    private boolean variableWidth = false;

    private final Vector3 tangent = new Vector3();

    /** Text fields used to present info about the example. */
    private final BasicText _exampleInfo[] = new BasicText[10];

    public static void main(final String[] args) {
        start(TrailExample.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        // Update the trail front position
        if (updateTrail) {
            // Do some crappy moving around
            final double speed = timer.getTimeInSeconds() * 4.0;
            final double xPos = Math.sin(speed) * (Math.sin(speed * 0.7) * 80.0f + 0.0);
            final double yPos = Math.sin(speed * 1.5) * 20.0 + 20.0;
            final double zPos = Math.cos(speed * 1.2) * (Math.sin(speed) * 80.0 + 0.0);

            sphere.setTranslation(xPos, yPos, zPos);
            sphere.updateGeometricState(0.0f, true);

            // Create a spin for tangent mode
            tangent.set(xPos, yPos, zPos);
            tangent.normalizeLocal();

            // Setup width
            double width = 7.0;
            if (variableWidth) {
                width = Math.sin(speed * 3.7) * 10.0 + 15.0;
            }

            // If you use the Tangent mode you have to send a tangent vector as
            // well (spin), otherwise you can just drop that variable like this:
            // trailMesh.setTrailFront(sphere.getWorldTranslation(), width,
            // Timer
            // .getTimer().getTimePerFrame());

            trailMesh.setTrailFront(sphere.getWorldTranslation(), tangent, width, timer.getTimePerFrame());
        }

        // Update the mesh
        trailMesh.update(_canvas.getCanvasRenderer().getCamera().getLocation());
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Trail Example");
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(150, 150, 0));
        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 0, 1), Vector3.UNIT_Y);
        _canvas.getCanvasRenderer().getCamera().setFrustumPerspective(65.0,
                (float) _canvas.getCanvasRenderer().getCamera().getWidth()
                        / _canvas.getCanvasRenderer().getCamera().getHeight(),
                1, 1000);

        // Create the trail
        trailMesh = new TrailMesh("TrailMesh", 100);
        trailMesh.setUpdateSpeed(60.0f);
        trailMesh.setFacingMode(TrailMesh.FacingMode.Billboard);
        trailMesh.setUpdateMode(TrailMesh.UpdateMode.Step);

        // Try out some additive blending etc
        trailMesh.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
        trailMesh.getSceneHints().setCullHint(CullHint.Never);

        // Add a texture to the box.
        TextureState ts = new TextureState();
        final Texture texture = TextureManager.load("images/trail.png", Texture.MinificationFilter.Trilinear, false);
        texture.setWrap(WrapMode.EdgeClamp);
        ts.setTexture(texture);
        trailMesh.setRenderState(ts);

        final BlendState bs = new BlendState();
        bs.setBlendEnabled(true);
        bs.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        bs.setDestinationFunction(BlendState.DestinationFunction.One);
        bs.setTestEnabled(true);
        trailMesh.setRenderState(bs);

        final ZBufferState zs = new ZBufferState();
        zs.setWritable(false);
        trailMesh.setRenderState(zs);

        final CullState cs = new CullState();
        cs.setCullFace(CullState.Face.None);
        cs.setEnabled(true);
        trailMesh.setRenderState(cs);

        _root.attachChild(trailMesh);

        final Box box = new Box("Box", new Vector3(0, 0, 0), 1000, 1, 1000);
        box.setModelBound(new BoundingBox());
        box.setTranslation(new Vector3(0, -50, 0));
        box.setRandomColors();
        _root.attachChild(box);
        ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        box.setRenderState(ts);

        sphere = new Sphere("Sphere", 16, 16, 4);
        _root.attachChild(sphere);

        // Setup labels for presenting example info.
        final Node textNodes = new Node("Text");
        _orthoRoot.attachChild(textNodes);
        textNodes.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
        textNodes.getSceneHints().setLightCombineMode(LightCombineMode.Off);

        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() - 20;
        for (int i = 0; i < _exampleInfo.length; i++) {
            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);
            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));
            textNodes.attachChild(_exampleInfo[i]);
        }

        textNodes.updateGeometricState(0.0);
        updateText();

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                trailMesh.setFacingMode(TrailMesh.FacingMode.Tangent);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                trailMesh.setFacingMode(TrailMesh.FacingMode.Billboard);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                trailMesh.setUpdateMode(TrailMesh.UpdateMode.Step);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FOUR), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                trailMesh.setUpdateMode(TrailMesh.UpdateMode.Interpolate);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FIVE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                trailMesh.setUpdateSpeed(trailMesh.getUpdateSpeed() * 2.0f);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SIX), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                trailMesh.setUpdateSpeed(trailMesh.getUpdateSpeed() * 0.5f);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                updateTrail = !updateTrail;
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.G), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                variableWidth = !variableWidth;
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.E), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                trailMesh.resetPosition(sphere.getWorldTranslation());
                updateText();
            }
        }));
    }

    /**
     * Update text information.
     */
    private void updateText() {
        _exampleInfo[0].setText("[1/2] Tangent/Billboard: ");
        _exampleInfo[1].setText("[3/4] Step/Interpolate: ");
        _exampleInfo[2].setText("[5/6] Raise/Lower update speed: ");
        _exampleInfo[4].setText("[F] Freeze: ");
        _exampleInfo[5].setText("[G] Use variable width: ");
        _exampleInfo[6].setText("[E] Reset position");
    }
}
