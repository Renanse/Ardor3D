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
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIDrawer;
import com.ardor3d.extension.ui.UIDrawer.DrawerEdge;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.image.Texture;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Illustrates how a UI Drawer frame works.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.ui.UIDrawerExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/ui_UIDrawerExample.jpg", //
    maxHeapMemory = 64)
public class UIDrawerExample extends ExampleBase {
  UIHud hud;

  public static void main(final String[] args) {
    start(UIDrawerExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("UI Drawer Example");

    UIComponent.setUseTransparency(true);

    // Add a spinning 3D box to show behind UI.
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
    // box.setRenderState(ts);
    _root.attachChild(box);

    hud = new UIHud(_canvas);
    hud.setupInput(_physicalLayer, _logicalLayer);
    hud.setMouseManager(_mouseManager);

    final UIDrawer drawer1 = new UIDrawer("Buttons", DrawerEdge.TOP);
    drawer1.getContentPanel().add(new UIButton("Load"));
    drawer1.getContentPanel().add(new UIButton("Save"));
    drawer1.getContentPanel().add(new UIButton("Delete"));
    drawer1.pack();
    hud.add(drawer1);
    drawer1.setExpanded(false, true);

    final UIDrawer drawer2 = new UIDrawer("More Buttons", DrawerEdge.BOTTOM);
    drawer2.getContentPanel().add(new UIButton("CUT"));
    drawer2.getContentPanel().add(new UIButton("COPY"));
    drawer2.getContentPanel().add(new UIButton("PASTE"));
    drawer2.getContentPanel().add(new UIButton("SELECT"));
    drawer2.getContentPanel().add(new UIButton("SELECT ALL"));
    drawer2.pack();
    hud.add(drawer2);
    drawer2.setExpanded(false, true);

    final UIDrawer drawer3 = new UIDrawer("Left Buttons", DrawerEdge.LEFT);
    drawer3.getContentPanel().add(new UIButton("A"));
    drawer3.getContentPanel().add(new UIButton("B"));
    drawer3.getContentPanel().add(new UIButton("C"));
    drawer3.getContentPanel().add(new UIButton("D"));
    drawer3.pack();
    hud.add(drawer3);
    drawer3.setExpanded(false, true);

    final UIDrawer drawer4 = new UIDrawer("Right Buttons", DrawerEdge.RIGHT);
    drawer4.getContentPanel().add(new UIButton("1"));
    drawer4.getContentPanel().add(new UIButton("2"));
    drawer4.getContentPanel().add(new UIButton("3"));
    drawer4.getContentPanel().add(new UIButton("4"));
    drawer4.pack();
    hud.add(drawer4);
    drawer4.setExpanded(false, true);
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
