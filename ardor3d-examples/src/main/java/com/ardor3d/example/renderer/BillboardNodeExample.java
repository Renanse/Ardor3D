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

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.input.control.FirstPersonControl;
import com.ardor3d.input.control.OrbitCamControl;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.extension.BillboardNode;
import com.ardor3d.scenegraph.extension.BillboardNode.BillboardAlignment;
import com.ardor3d.scenegraph.shape.AxisRods;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Illustrates the BillboardNode class; which defines a node that always orients towards the camera.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.renderer.BillboardNodeExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_BillboardNodeExample.jpg", //
    maxHeapMemory = 64)
public class BillboardNodeExample extends ExampleBase {

  private static final int BB_SPACING = 10;

  private static final BillboardAlignment StartingAlignment = BillboardAlignment.AxialY;

  private BasicText text;

  private OrbitCamControl control;

  public static void main(final String[] args) {
    start(BillboardNodeExample.class);
  }

  public BillboardNodeExample() {
    super();
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    // update orbiter
    control.update(timer.getTimePerFrame());
  }

  @Override
  protected void registerInputTriggers() {
    super.registerInputTriggers();

    // clean out the first person handler
    FirstPersonControl.removeTriggers(_logicalLayer, _controlHandle);

    // add Orbit handler - set it up to control the main camera
    control = new OrbitCamControl(_canvas.getCanvasRenderer().getCamera(), _root);
    control.setupMouseTriggers(_logicalLayer, true);
    control.setSphereCoords(25, MathUtils.HALF_PI, 0);
    control.setZoomSpeed(.25);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("BillboardNode - Example");

    text = BasicText.createDefaultTextLabel("Text", "[SPACE] " + StartingAlignment);
    text.setTranslation(new Vector3(5, 20, 0));
    _orthoRoot.attachChild(text);

    final TextureState ts = new TextureState();
    ts.setEnabled(true);
    ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
        TextureStoreFormat.GuessCompressedFormat, true));

    _root.setRenderState(ts);

    final BillboardNode[] billboards = new BillboardNode[3];
    for (int i = 0; i < billboards.length; i++) {
      final Box box = new Box("box-" + i, Vector3.ZERO, 2.5, 2.5, 0.1);
      final BillboardNode bbn = billboards[i] = new BillboardNode("bbn-" + i);
      bbn.setAlignment(StartingAlignment);
      bbn.setTranslation((i - (billboards.length / 2)) * BB_SPACING, 0, 0);
      bbn.attachChild(box);
      _root.attachChild(bbn);

      final AxisRods rods = new AxisRods("axis", true, 1.25);
      rods.setTranslation(bbn.getTranslation());
      _root.attachChild(rods);
    }

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), (source, inputStates, tpf) -> {
      int ordinal = billboards[0].getAlignment().ordinal() + 1;
      if (ordinal > BillboardAlignment.values().length - 1) {
        ordinal = 0;
      }

      for (final BillboardNode bbn : billboards) {
        bbn.setAlignment(BillboardAlignment.values()[ordinal]);
      }

      text.setText("[SPACE] " + billboards[0].getAlignment());
    }));

    _root.attachChild(new Box("ground", new Vector3(0, -2.5, 0), 100, 0.1, 100));
  }
}
