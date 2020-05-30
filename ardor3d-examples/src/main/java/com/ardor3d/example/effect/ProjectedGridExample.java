/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.effect;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.effect.water.ProjectedGrid;
import com.ardor3d.extension.effect.water.WaterHeightGenerator;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * A demonstration of the ProjectedGrid class; used to efficiently determine which vertices's in a
 * terrain to draw.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.effect.ProjectedGridExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/effect_ProjectedGridExample.jpg", //
    maxHeapMemory = 64)
public class ProjectedGridExample extends ExampleBase {
  /** The Projected Grid mesh */
  private ProjectedGrid projectedGrid;

  private final Camera externalCamera = new Camera();
  private boolean animateExternalCamera = false;

  /** Text fields used to present info about the example. */
  private final BasicText _exampleInfo[] = new BasicText[4];

  public static void main(final String[] args) {
    start(ProjectedGridExample.class);
  }

  double counter = 0;
  int frames = 0;

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    if (!animateExternalCamera) {
      externalCamera.set(_canvas.getCanvasRenderer().getCamera());
    } else {
      final double time = _timer.getTimeInSeconds() * 0.5;
      externalCamera.setLocation(0, Math.sin(time) * 100 + 110.0, 0);
      externalCamera.lookAt(new Vector3(Math.sin(time * 1.5) * 100, 50, Math.cos(time) * 100), Vector3.UNIT_Y);
    }

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
  protected void initExample() {
    _canvas.setTitle("ProjectedGrid - Example");

    _controlHandle.setMoveSpeed(200);

    _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 100, 200));
    _canvas.getCanvasRenderer().getCamera().setFrustumPerspective(40.0,
        (float) _canvas.getCanvasRenderer().getCamera().getWidth()
            / (float) _canvas.getCanvasRenderer().getCamera().getHeight(),
        1.0, 5000);
    _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 0, 0), Vector3.UNIT_Y);

    projectedGrid =
        new ProjectedGrid("ProjectedGrid", externalCamera, 100, 70, 0.01f, new WaterHeightGenerator(), _timer);
    _root.attachChild(projectedGrid);
    _root.setRenderMaterial("unlit/textured/basic.yaml");

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), (source, inputStates, tpf) -> {
      projectedGrid.setFreezeUpdate(!projectedGrid.isFreezeUpdate());
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), (source, inputStates, tpf) -> {
      projectedGrid.setNrUpdateThreads(projectedGrid.getNrUpdateThreads() - 1);
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), (source, inputStates, tpf) -> {
      projectedGrid.setNrUpdateThreads(projectedGrid.getNrUpdateThreads() + 1);
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), (source, inputStates, tpf) -> {
      projectedGrid.setDrawDebug(!projectedGrid.isDrawDebug());
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FOUR), (source, inputStates, tpf) -> {
      projectedGrid.setDrawDebug(true);
      animateExternalCamera = !animateExternalCamera;
      updateText();
    }));

    final TextureState ts = new TextureState();
    ts.setEnabled(true);
    ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
        TextureStoreFormat.GuessCompressedFormat, true));
    _root.setRenderState(ts);

    _root.getSceneHints().setCullHint(CullHint.Never);

    // Setup textfields for presenting example info.
    final Node textNodes = new Node("Text");
    _orthoRoot.attachChild(textNodes);
    textNodes.getSceneHints().setLightCombineMode(LightCombineMode.Off);

    final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;
    for (int i = 0; i < _exampleInfo.length; i++) {
      _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);
      _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));
      textNodes.attachChild(_exampleInfo[i]);
    }

    textNodes.updateGeometricState(0.0);
    updateText();
  }

  /**
   * Update text information.
   */
  private void updateText() {
    _exampleInfo[0].setText("[1/2] Number of update threads: " + projectedGrid.getNrUpdateThreads());
    _exampleInfo[1].setText("[3] Draw debug frustums: " + projectedGrid.isDrawDebug());
    _exampleInfo[2].setText("[4] Animate external camera: " + animateExternalCamera);
    _exampleInfo[3].setText("[SPACE] Freeze update: " + projectedGrid.isFreezeUpdate());
  }
}
