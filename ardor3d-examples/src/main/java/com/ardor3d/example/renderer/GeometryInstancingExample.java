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
import com.ardor3d.buffer.FloatBufferData;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * Demonstrates the use of geometry instancing and compares framerate with and without instancing.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.renderer.GeometryInstancingExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_GeometryInstancingExample.jpg", //
    maxHeapMemory = 64)
public class GeometryInstancingExample extends ExampleBase {

  private BasicText frameRateLabel;
  private int frames = 0;
  private long startTime = System.currentTimeMillis();

  private boolean instancingEnabled = true;

  private Node _base;

  public static void main(final String[] args) {
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
    frames++;
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("GeometryInstancingExample");

    final BasicText t2 = BasicText.createDefaultTextLabel("Text", "[I] Instancing On");
    t2.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
    t2.setTranslation(new Vector3(0, 50, 0));
    _orthoRoot.attachChild(t2);

    final CullState cs = new CullState();
    cs.setCullFace(CullState.Face.Back);
    cs.setEnabled(true);
    _root.setRenderState(cs);

    _base = new Node("node");
    _base.addTranslation(0, -20, -100);
    _canvas.getCanvasRenderer().getCamera().lookAt(_base.getTranslation(), Vector3.UNIT_Y);
    _root.attachChild(_base);

    final Sphere center = new Sphere("center", 64, 64, 20);
    center.setRandomColors();
    _base.attachChild(center);

    final Node instancedBase = new Node("instancedBase");
    _base.attachChild(instancedBase);
    instancedBase.setRenderMaterial("lit/untextured/basic_phong_instanced.yaml");
    instancedBase.getSceneHints().setCullHint(CullHint.Dynamic);

    final Node unInstancedBase = new Node("unInstancedBase");
    _base.attachChild(unInstancedBase);
    unInstancedBase.setRenderMaterial("lit/untextured/basic_phong.yaml");
    unInstancedBase.getSceneHints().setCullHint(CullHint.Always);

    final int nrOfObjects = 1000;

    generateSpheres(instancedBase, true, nrOfObjects);
    generateSpheres(unInstancedBase, false, nrOfObjects);

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.I), (source, inputStates, tpf) -> {
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
    }));

    // Add fps display
    frameRateLabel = BasicText.createDefaultTextLabel("fpsLabel", "");
    frameRateLabel.setTranslation(5,
        _canvas.getCanvasRenderer().getCamera().getHeight() - 5 - frameRateLabel.getHeight(), 0);
    frameRateLabel.setTextColor(ColorRGBA.WHITE);
    frameRateLabel.getSceneHints().setOrthoOrder(-1);
    _orthoRoot.attachChild(frameRateLabel);
  }

  protected void generateSpheres(final Node modelBase, final boolean useInstancing, final int nrOfObjects) {
    final Sphere sphereroot = new Sphere("Sphere", 16, 16, 2);

    Random rand = new Random(1337);
    if (useInstancing) {
      modelBase.attachChild(sphereroot);
      sphereroot.setInstanceCount(nrOfObjects);
      final FloatBufferData matrixStore = new FloatBufferData(nrOfObjects * 16, 4);
      sphereroot.getMeshData().setCoords(MeshData.KEY_InstanceMatrix, matrixStore);

      for (int i = 0; i < nrOfObjects; i++) {
        final Transform trans = generateAsteroidTransform(rand, (i / (double) nrOfObjects) * MathUtils.TWO_PI);
        trans.getGLApplyMatrix(matrixStore.getBuffer());
      }
      matrixStore.getBuffer().flip();

      return;
    }

    rand = new Random(1337);
    for (int i = 0; i < nrOfObjects; i++) {
      final Sphere sphere = (Sphere) sphereroot.makeCopy(true);
      sphere.setModelBound(new BoundingSphere());
      final Transform trans = generateAsteroidTransform(rand, (i / (double) nrOfObjects) * MathUtils.TWO_PI);
      sphere.setTransform(trans);
      sphere.getSceneHints().setCullHint(CullHint.Dynamic);
      modelBase.attachChild(sphere);
    }
  }

  private Transform generateAsteroidTransform(final Random rand, final double angle) {
    final Transform trans = new Transform();
    final double radius = 50;
    final double offsetX = rand.nextDouble() * 5 - 2.5;
    final double offsetY = (rand.nextDouble() * 5 - 2.5) * .4;
    final double offsetZ = rand.nextDouble() * 5 - 2.5;
    trans.translate(Math.sin(angle) * radius + offsetX, offsetY, Math.cos(angle) * radius + offsetZ);
    trans.setScale(rand.nextDouble() * 0.2 + .05, rand.nextDouble() * 0.2 + .05, rand.nextDouble() * 0.2 + .05);
    trans.setRotation(new Quaternion().fromAngleAxis(rand.nextDouble() * MathUtils.TWO_PI, new Vector3(.4, .6, .8)));
    return trans;
  }
}
