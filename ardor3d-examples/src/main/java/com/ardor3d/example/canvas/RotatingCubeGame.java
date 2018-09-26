/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.canvas;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.BasicScene;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.Updater;
import com.ardor3d.image.Texture;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.control.OrbitCamControl;
import com.ardor3d.input.logical.AnyKeyCondition;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.KeyReleasedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

public class RotatingCubeGame implements Updater {
    // private final Canvas view;
    private final BasicScene scene;
    private final AtomicBoolean exit;
    private final LogicalLayer logicalLayer;
    private final Key toggleRotationKey;

    private final static float CUBE_ROTATE_SPEED = 1;
    private final Vector3 rotationAxis = new Vector3(1, 1, 0);
    private double angle = 0;
    private Mesh box;
    private final Matrix3 rotation = new Matrix3();

    private int rotationSign = 1;
    private boolean rotationEnabled = true;
    private boolean inited;

    public RotatingCubeGame(final BasicScene scene, final AtomicBoolean exit, final LogicalLayer logicalLayer,
            final Key toggleRotationKey) {
        this.scene = scene;
        this.exit = exit;
        this.logicalLayer = logicalLayer;
        this.toggleRotationKey = toggleRotationKey;
    }

    @MainThread
    public void init() {
        if (inited) {
            return;
        }
        // add a cube to the scene
        // add a rotating controller to the cube
        // add a light
        box = new Box("The cube", new Vector3(-1, -1, -1), new Vector3(1, 1, 1));
        box.setRenderMaterial("unlit/textured/basic.yaml");

        final ZBufferState buf = new ZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        scene.getRoot().setRenderState(buf);

        // Add a texture to the box.
        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        box.setRenderState(ts);

        final PointLight light = new PointLight();

        final Random random = new Random();

        final float r = random.nextFloat();
        final float g = random.nextFloat();
        final float b = random.nextFloat();
        final float a = random.nextFloat();

        light.setDiffuse(new ColorRGBA(r, g, b, a));
        light.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        light.setLocation(new Vector3(4, 4, 4));
        light.setEnabled(true);

        /** Attach the light to a lightState and the lightState to rootNode. */
        final LightState lightState = new LightState();
        lightState.setEnabled(true);
        lightState.attach(light);
        scene.getRoot().setRenderState(lightState);

        scene.getRoot().attachChild(box);

        registerInputTriggers();

        inited = true;
    }

    private void registerInputTriggers() {
        final OrbitCamControl control = new OrbitCamControl(box);
        control.setInvertedY(true);
        control.setupMouseTriggers(logicalLayer, true);
        control.setupGestureTriggers(logicalLayer);
        control.setSphereCoords(15, 0, 0);

        scene.getRoot().addController(new SpatialController<Spatial>() {
            public void update(final double time, final Spatial caller) {
                control.update(time);
            };
        });

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ESCAPE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                exit.set(true);
            }
        }));

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(toggleRotationKey), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                toggleRotationDirection();
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.U), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                toggleRotationDirection();
            }
        }));

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ZERO), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                resetCamera(source);
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.NINE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                lookAtZero(source);
            }
        }));

        logicalLayer.registerTrigger(new InputTrigger(new AnyKeyCondition(), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final InputState current = inputStates.getCurrent();

                System.out.println("Key character pressed: " + current.getKeyboardState().getKeyEvent().getKeyChar());
            }
        }));
    }

    private void lookAtZero(final Canvas source) {
        source.getCanvasRenderer().getCamera().lookAt(Vector3.ZERO, Vector3.UNIT_Y);
    }

    private void resetCamera(final Canvas source) {
        final Vector3 loc = new Vector3(0.0f, 0.0f, 10.0f);
        final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
        final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
        final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);

        source.getCanvasRenderer().getCamera().setFrame(loc, left, up, dir);
    }

    private void toggleRotationDirection() {
        rotationSign *= -1;
    }

    public void toggleRotation() {
        rotationEnabled = !rotationEnabled;
    }

    @MainThread
    public void update(final ReadOnlyTimer timer) {
        final double tpf = timer.getTimePerFrame();

        logicalLayer.checkTriggers(tpf);

        if (rotationEnabled) {
            angle += tpf * CUBE_ROTATE_SPEED * rotationSign;

            rotation.fromAngleAxis(angle, rotationAxis);
            box.setRotation(rotation);
        }

        scene.getRoot().updateGeometricState(tpf, true);
    }

    public Mesh getBox() {
        return box;
    }
}
