/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.ui;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UITabbedPane;
import com.ardor3d.extension.ui.UITabbedPane.TabPlacement;
import com.ardor3d.extension.ui.layout.GridLayout;
import com.ardor3d.extension.ui.layout.GridLayoutData;
import com.ardor3d.extension.ui.text.UIDoubleField;
import com.ardor3d.extension.ui.text.UIIntegerField;
import com.ardor3d.extension.ui.text.UIIntegerRollerField;
import com.ardor3d.image.Texture;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Example of data entry UI types in action.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.ui.DataEntryUIExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/ui_DataEntryUIExample.jpg", //
    maxHeapMemory = 64)
public class DataEntryUIExample extends ExampleBase {
  UIHud hud;
  private UIFrame frame;

  public static void main(final String[] args) {
    start(DataEntryUIExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Data Entry Example");

    UIComponent.setUseTransparency(true);

    // Add a spinning 3D box to show behind UI.
    add3DSceneDecoration();

    final UIPanel panel = makeNumericPanel();

    final UITabbedPane pane = new UITabbedPane(TabPlacement.NORTH);
    pane.add(panel, "numeric");
    pane.setMinimumContentSize(400, 300);

    frame = new UIFrame("Data Entry Example");
    frame.setContentPanel(pane);
    frame.pack();

    frame.setUseStandin(true);
    frame.setOpacity(1f);
    frame.setName("sample");

    hud = new UIHud(_canvas);
    hud.add(frame);
    hud.setupInput(_physicalLayer, _logicalLayer);
    hud.setMouseManager(_mouseManager);

    frame.centerOn(hud);
  }

  private void add3DSceneDecoration() {
    final Box box = new Box("Box", new Vector3(0, 0, 0), 5, 5, 5);
    box.setModelBound(new BoundingBox());
    box.setTranslation(new Vector3(0, 0, -15));
    box.addController(new SpatialController<Box>() {
      private final Matrix3 rotate = new Matrix3();
      private double angle = 0;
      private final Vector3 axis = new Vector3(1, 1, 0.5f).normalizeLocal();

      @Override
      public void update(final double time, final Box caller) {
        angle += time * 50;
        angle %= 360;
        rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
        caller.setRotation(rotate);
      }
    });
    // Add a texture to the box.
    final TextureState ts = new TextureState();

    final Texture tex = TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true);
    ts.setTexture(tex);
    box.setRenderState(ts);
    _root.attachChild(box);
  }

  private UIPanel makeNumericPanel() {
    final UIPanel panel = new UIPanel(new GridLayout());

    final UILabel lHeader = new UILabel("Various Numeric Fields");
    lHeader.setLayoutData(new GridLayoutData(2, true, true));
    panel.add(lHeader);

    final UILabel lInteger = new UILabel("Integer Field");
    final UIIntegerField tfInteger = new UIIntegerField();
    tfInteger.setValue(42);
    tfInteger.setLayoutData(GridLayoutData.WrapAndGrow);
    panel.add(lInteger);
    panel.add(tfInteger);

    final UILabel lInteger2 = new UILabel("Integer Roller");
    final UIIntegerRollerField tfInteger2 = new UIIntegerRollerField();
    tfInteger2.setMinimumValue(0);
    tfInteger2.setMaximumValue(100);
    tfInteger2.setValue(50);
    tfInteger2.setLayoutData(GridLayoutData.WrapAndGrow);
    panel.add(lInteger2);
    panel.add(tfInteger2);

    final UILabel lDouble = new UILabel("Double Field");
    final UIDoubleField tfDouble = new UIDoubleField();
    tfDouble.setValue(3.14e2);
    tfDouble.setDisplayScientific(true);
    tfDouble.setLayoutData(GridLayoutData.WrapAndGrow);
    panel.add(lDouble);
    panel.add(tfDouble);

    final UICheckBox dblSciTrue = new UICheckBox("sci. notation");
    dblSciTrue.setSelected(true);
    dblSciTrue.addActionListener(event -> {
      tfDouble.setDisplayScientific(!tfDouble.isDisplayScientific());
      tfDouble.refreshState();
    });
    dblSciTrue.setLayoutData(GridLayoutData.WrapAndGrow);
    panel.add(new UILabel(" "));
    panel.add(dblSciTrue);

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

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    hud.updateGeometricState(timer.getTimePerFrame());
  }
}
