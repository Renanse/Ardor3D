/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.ui;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.backdrop.SolidBackdrop;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * Illustrates how to display and move GUI primitives (e.g. RadioButton, Label, TabbedPane) on a
 * canvas.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.ui.RotatingUIExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/ui_RotatingUIExample.jpg", //
    maxHeapMemory = 64)
public class RotatingUIExample extends ExampleBase {
  UIHud hud;

  public static void main(final String[] args) {
    start(RotatingUIExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Rotating UI Example");

    UIComponent.setUseTransparency(true);

    final UIPanel panel = makePanel();

    final UIFrame frame = new UIFrame("Sample");
    frame.setContentPanel(panel);
    frame.pack();

    frame.setUseStandin(false);
    frame.setOpacity(1f);
    frame.setName("sample");

    final Matrix3 rotate = new Matrix3();
    final Vector3 axis = new Vector3(0, 0, 1).normalizeLocal();
    rotate.fromAngleNormalAxis(45 * MathUtils.DEG_TO_RAD, axis);
    frame.setRotation(rotate);

    hud = new UIHud(_canvas);
    hud.add(frame);
    hud.setupInput(_physicalLayer, _logicalLayer);

    frame.centerOn(hud);
  }

  private UIPanel makePanel() {

    final UIPanel panel = new UIPanel();
    panel.setForegroundColor(ColorRGBA.DARK_GRAY);
    panel.setLayout(new RowLayout(true));

    final UILabel staticLabel = new UILabel("Hello World");
    staticLabel.setBackdrop(new SolidBackdrop(ColorRGBA.CYAN));
    staticLabel.setLayoutData(BorderLayoutData.CENTER);
    panel.add(staticLabel);

    final UICheckBox rotatingLabel = new UICheckBox("Look at me! :)");
    rotatingLabel.setBackdrop(new SolidBackdrop(ColorRGBA.GREEN));
    rotatingLabel.setRotation(new Matrix3().fromAngleNormalAxis(45 * MathUtils.DEG_TO_RAD, new Vector3(0, 0, 1)));
    panel.add(rotatingLabel);

    final Matrix3 rotate = new Matrix3();
    final Vector3 axis = new Vector3(0, 0, 1);
    rotatingLabel.addController(new SpatialController<UICheckBox>() {
      double angle = 0;

      @Override
      public void update(final double time, final UICheckBox caller) {
        angle += time * 10;
        angle %= 360;
        rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
        caller.setRotation(rotate);
        caller.fireComponentDirty();
        panel.layout();
      }
    });
    rotatingLabel.setLayoutData(BorderLayoutData.NORTH);

    panel.setMinimumContentSize(300, 200);

    return panel;
  }

  @Override
  protected void updateLogicalLayer(final ReadOnlyTimer timer) {
    hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
  }

  @Override
  protected void renderExample(final Renderer renderer) {
    super.renderExample(renderer);
    renderer.renderBuckets();
    renderer.draw(hud);
  }

  private double counter = 0;
  private int frames = 0;

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    counter += timer.getTimePerFrame();
    frames++;
    if (counter > 1) {
      final double fps = (frames / counter);
      counter = 0;
      frames = 0;
      System.out.printf("%7.1f FPS\n", fps);
    }
    hud.updateGeometricState(timer.getTimePerFrame());
  }
}
