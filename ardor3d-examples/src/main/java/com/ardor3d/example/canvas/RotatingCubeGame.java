/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.canvas;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.BasicScene;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.Updater;
import com.ardor3d.image.Texture;
import com.ardor3d.input.character.CharacterInputEvent;
import com.ardor3d.input.control.OrbitCamControl;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.AnyCharacterCondition;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.KeyReleasedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Mesh;
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

  @Override
  @MainThread
  public void init() {
    if (inited) {
      return;
    }
    // add a cube to the scene
    // add a rotating controller to the cube
    // add a light
    box = new Box("The cube", new Vector3(-1, -1, -1), new Vector3(1, 1, 1));
    box.setRenderMaterial("lit/textured/basic_phong.yaml");

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

    light.setColor(new ColorRGBA(r, g, b, a));
    light.setTranslation(4, 4, 4);
    light.setEnabled(true);

    /** Attach the light to a lightState and the lightState to rootNode. */
    scene.getRoot().attachChild(light);

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

    scene.getRoot().addController((time, caller) -> control.update(time));

    logicalLayer.registerTrigger(
        new InputTrigger(new KeyPressedCondition(Key.ESCAPE), (source, inputStates, tpf) -> exit.set(true)));

    logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(toggleRotationKey),
        (source, inputStates, tpf) -> toggleRotationDirection()));
    logicalLayer.registerTrigger(
        new InputTrigger(new KeyReleasedCondition(Key.U), (source, inputStates, tpf) -> toggleRotationDirection()));

    logicalLayer.registerTrigger(
        new InputTrigger(new KeyPressedCondition(Key.ZERO), (source, inputStates, tpf) -> resetCamera(source)));
    logicalLayer.registerTrigger(
        new InputTrigger(new KeyPressedCondition(Key.NINE), (source, inputStates, tpf) -> lookAtZero(source)));

    logicalLayer.registerTrigger(new InputTrigger(new AnyCharacterCondition(), (source, inputState, tpf) -> {
      final List<CharacterInputEvent> events = inputState.getCurrent().getCharacterState().getEvents();
      for (final CharacterInputEvent e : events) {
        System.out.println("Character entered: " + e.getValue());
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

  @Override
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

  public Mesh getBox() { return box; }
}
