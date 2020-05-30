/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.basic;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.extension.SwitchNode;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.TextureManager;

/**
 * A demonstration of the SwitchNode class; used to control which Node to actively display from a
 * set of Nodes.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.basic.SwitchNodeExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/basic_SwitchNodeExample.jpg", //
    maxHeapMemory = 64)
public class SwitchNodeExample extends ExampleBase {
  public static void main(final String[] args) {
    start(SwitchNodeExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("SwitchNode - Example");

    final BasicText t = BasicText.createDefaultTextLabel("Text", "[SPACE] Switch to next child");
    t.getSceneHints().setLightCombineMode(LightCombineMode.Off);
    t.setTranslation(new Vector3(0, 20, 0));
    _orthoRoot.attachChild(t);

    final SwitchNode switchNode = new SwitchNode();

    Box box = new Box("Box", new Vector3(), 2, 1, 1);
    box.setModelBound(new BoundingBox());
    box.setTranslation(new Vector3(0, 0, 0));
    box.setRenderMaterial("unlit/textured/basic.yaml");
    switchNode.attachChild(box);

    box = new Box("Box", new Vector3(), 1, 2, 1);
    box.setModelBound(new BoundingBox());
    box.setTranslation(new Vector3(0, 0, 0));
    box.setRandomColors();
    switchNode.attachChild(box);

    box = new Box("Box", new Vector3(), 1, 1, 2);
    box.setModelBound(new BoundingBox());
    box.setTranslation(new Vector3(0, 0, 0));
    box.setRandomColors();
    switchNode.attachChild(box);
    switchNode.getSceneHints().setCullHint(CullHint.Dynamic);

    _root.attachChild(switchNode);
    _root.getSceneHints().setCullHint(CullHint.Never);

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE),
        (source, inputStates, tpf) -> switchNode.shiftVisibleRight()));

    final TextureState ts = new TextureState();
    ts.setEnabled(true);
    ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));

    _root.setRenderState(ts);
    _root.setRenderMaterial("unlit/textured/vertex_color.yaml");
  }
}
