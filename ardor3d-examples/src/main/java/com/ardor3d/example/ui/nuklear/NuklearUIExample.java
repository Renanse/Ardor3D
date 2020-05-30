/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.ui.nuklear;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.extension.ui.nuklear.NuklearHud;
import com.ardor3d.extension.ui.nuklear.NuklearWindow;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

public class NuklearUIExample extends ExampleBase {
  NuklearHud hud;
  Box box;
  double angle = 0;
  final Matrix3 rotate = new Matrix3();
  final Vector3 axis = new Vector3(1, 1, 0.5f).normalizeLocal();

  public static void main(final String[] args) {
    start(NuklearUIExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Nuklear UI Example");
    _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(ColorRGBA.DARK_GRAY);

    hud = new NuklearHud(_canvas);
    _root.attachChild(hud);
    hud.setupInput(_physicalLayer, _logicalLayer);

    final NuklearWindow demo = new DemoWindow();
    demo.setOffSet(50, 50);
    hud.add(demo);

    final NuklearWindow calc = new CalculatorWindow();
    calc.setOffSet(300, 50);
    hud.add(calc);

    box = new Box("Box", new Vector3(0, 0, 0), 5, 5, 5);
    box.setTranslation(new Vector3(0, 0, -15));
    box.setRandomColors();
    box.setRenderMaterial("lit/textured/vertex_color_phong.yaml");
    _root.attachChild(box);

    final TextureState ts = new TextureState();
    ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
    box.setRenderState(ts);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    angle += timer.getTimePerFrame() * 50;
    angle %= 360;

    rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
    box.setRotation(rotate);
  }

  @Override
  protected void updateLogicalLayer(final ReadOnlyTimer timer) {
    hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
  }

}
