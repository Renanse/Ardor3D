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

import java.util.EnumSet;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.ui.Orientation;
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIFrame.FrameButtons;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UIMenuItem;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIPieMenu;
import com.ardor3d.extension.ui.UIPieMenuItem;
import com.ardor3d.extension.ui.UIPopupMenu;
import com.ardor3d.extension.ui.UISlider;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.extension.ui.text.UITextField;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Illustrates the use of Popup and Pie menus.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.ui.PopOverUIExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/ui_PopOverUIExample.jpg", //
    maxHeapMemory = 64)
public class PopOverUIExample extends ExampleBase implements ActionListener {
  private static final String[] COLORS = new String[] {"Red", "White", "Blue", "Black"};
  private static final String[] SPINS = new String[] {"None", "Around X", "Around Y", "Around Z"};
  private static final String[] TEXS = new String[] {"None", "Logo", "Ball", "Clock"};
  private static final String[] SCALE = new String[] {"Scale..."};

  UIHud hud;
  private Box box;

  public static void main(final String[] args) {
    start(PopOverUIExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("PopOver UI Example");

    UIComponent.setUseTransparency(true);

    // Add a spinning 3D box to show behind UI.
    box = new Box("Box", new Vector3(0, 0, 0), 5, 5, 5);
    box.setModelBound(new BoundingBox());
    box.setTranslation(new Vector3(0, 0, -15));
    box.setRenderMaterial("lit/textured/basic_phong.yaml");
    _root.attachChild(box);

    setTexture("Logo");
    setSpin("Around Y");

    hud = new UIHud(_canvas);
    hud.setupInput(_physicalLayer, _logicalLayer);
    hud.setMouseManager(_mouseManager);

    final UIButton dropButton = new UIButton("Drop Menu");
    dropButton.setPadding(new Insets(6, 15, 6, 15));
    dropButton.setHudXY(hud.getWidth() / 10 - dropButton.getLocalComponentWidth() / 2,
        hud.getHeight() - dropButton.getLocalComponentHeight() - 5);
    dropButton.addActionListener(event -> showPopupMenu(dropButton.getHudX(), dropButton.getHudY()));
    hud.add(dropButton);

    final UIButton pieButton = new UIButton("Pie Menu");
    pieButton.setPadding(new Insets(6, 15, 6, 15));
    pieButton.setHudXY(9 * hud.getWidth() / 10 - pieButton.getLocalComponentWidth() / 2,
        hud.getHeight() - pieButton.getLocalComponentHeight() - 5);
    pieButton.addActionListener(event -> showPieMenu(hud.getWidth() / 2, hud.getHeight() / 2));
    hud.add(pieButton);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    final UIButton src = (UIButton) event.getSource();
    final String command = src.getActionCommand();
    switch (command) {
      case "Color":
        setColor(src.getText());
        return;
      case "Spin":
        setSpin(src.getText());
        return;
      case "Texture":
        setTexture(src.getText());
        return;
      case "Scale":
        showScaleDialog();
        return;
    }
  }

  protected void showPopupMenu(final int hudX, final int hudY) {
    final UIPopupMenu menu = new UIPopupMenu();
    final int minWidth = 120;

    final UIPopupMenu colorMenu = new UIPopupMenu();
    colorMenu.setMinimumContentSize(minWidth, 5);
    menu.addItem(new UIMenuItem("Set Color...", null, colorMenu));
    AddMenuItems(colorMenu, "Color", false, COLORS);

    final UIPopupMenu spinMenu = new UIPopupMenu();
    spinMenu.setMinimumContentSize(minWidth, 5);
    menu.addItem(new UIMenuItem("Set Spin...", null, spinMenu));
    AddMenuItems(spinMenu, "Spin", false, SPINS);

    final UIPopupMenu texMenu = new UIPopupMenu();
    texMenu.setMinimumContentSize(minWidth, 5);
    menu.addItem(new UIMenuItem("Set Texture...", null, texMenu));
    AddMenuItems(texMenu, "Texture", false, TEXS);

    AddMenuItems(menu, "Scale", false, SCALE);

    menu.updateMinimumSizeFromContents();
    menu.layout();

    hud.closePopupMenus();

    hud.showSubPopupMenu(menu);
    menu.showAt(hudX, hudY);
  }

  protected void showPieMenu(final int hudX, final int hudY) {
    final UIPieMenu menu = new UIPieMenu(hud, 70, 200);

    final UIPieMenu colorMenu = new UIPieMenu(hud);
    menu.addItem(new UIPieMenuItem("Set Color...", null, colorMenu, 100));
    AddMenuItems(colorMenu, "Color", true, COLORS);

    final UIPieMenu spinMenu = new UIPieMenu(hud);
    menu.addItem(new UIPieMenuItem("Set Spin...", null, spinMenu, 100));
    AddMenuItems(spinMenu, "Spin", true, SPINS);

    final UIPieMenu texMenu = new UIPieMenu(hud);
    menu.addItem(new UIPieMenuItem("Set Texture...", null, texMenu, 100));
    AddMenuItems(texMenu, "Texture", true, TEXS);

    AddMenuItems(menu, "Scale", true, SCALE);

    menu.setCenterItem(new UIPieMenuItem("Cancel", null, true, null));

    menu.updateMinimumSizeFromContents();
    menu.layout();

    hud.closePopupMenus();

    hud.showSubPopupMenu(menu);
    menu.showAt(hudX, hudY);
    _mouseManager.setPosition(hudX, hudY);
    if (menu.getCenterItem() != null) {
      menu.getCenterItem().mouseEntered(hudX, hudY, null);
    }

  }

  private void AddMenuItems(final UIPopupMenu parent, final String actionCommand, final boolean pie,
      final String[] colors) {
    for (final String color : colors) {
      final UIMenuItem item =
          pie ? new UIPieMenuItem(color, null, true, this) : new UIMenuItem(color, null, true, this);
      item.setActionCommand(actionCommand);
      parent.addItem(item);
    }
  }

  private void setColor(final String text) {
    switch (text) {
      case "Red":
        box.setDefaultColor(ColorRGBA.RED);
        break;
      case "Blue":
        box.setDefaultColor(ColorRGBA.BLUE);
        break;
      case "Black":
        box.setDefaultColor(ColorRGBA.BLACK);
        break;
      default:
      case "White":
        box.setDefaultColor(ColorRGBA.WHITE);
        break;
    }
  }

  private void setSpin(final String text) {
    box.clearControllers();
    final ReadOnlyVector3 axis;
    switch (text) {
      case "None":
        return;
      case "Around X":
        axis = Vector3.UNIT_X;
        break;
      case "Around Y":
        axis = Vector3.UNIT_Y;
        break;
      default:
      case "Around Z":
        axis = Vector3.UNIT_Z;
        break;
    }
    box.addController(new SpatialController<Box>() {
      private final Matrix3 rotate = new Matrix3();
      private double angle = 0;

      @Override
      public void update(final double time, final Box caller) {
        angle += time * 50;
        angle %= 360;
        rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
        caller.setRotation(rotate);
      }
    });
  }

  private void setTexture(final String text) {
    // Add a texture to the box.
    final TextureState ts = new TextureState();

    String imageFile;
    switch (text) {
      case "None":
        box.clearRenderState(StateType.Texture);
        box.updateWorldRenderStates(true);
        return;
      case "Ball":
        imageFile = "images/ball.png";
        break;
      case "Clock":
        imageFile = "images/clock.png";
        break;
      case "Logo":
      default:
        imageFile = "images/ardor3d_white_256.jpg";
        break;
    }

    final Texture tex = TextureManager.load(imageFile, Texture.MinificationFilter.Trilinear, true);
    ts.setTexture(tex);
    box.setRenderState(ts);
    box.updateWorldRenderStates(true);
  }

  private void showScaleDialog() {
    final UIFrame scaleDialog = new UIFrame("Set Scale...", EnumSet.of(FrameButtons.CLOSE));
    scaleDialog.setResizeable(false);
    final UIPanel contentPanel = scaleDialog.getContentPanel();
    final RowLayout layout = new RowLayout(true, false, false);
    layout.setSpacing(4);
    contentPanel.setLayout(layout);
    contentPanel.setMargin(new Insets(0, 5, 0, 5));

    final UISlider scaleSlider = new UISlider(Orientation.Horizontal, 1, 20, (int) (box.getScale().getX() * 10));
    scaleSlider.setMinimumContentWidth(200);
    contentPanel.add(scaleSlider);

    final UITextField scaleTextField = new UITextField();
    scaleTextField.setEditable(false);
    scaleTextField.setText("1.0");
    scaleTextField.setMinimumContentWidth(30);
    contentPanel.add(scaleTextField);

    scaleSlider.addActionListener(event -> {
      box.setScale(scaleSlider.getValue() / 10.0);
      scaleTextField.setText(String.format("%.1f", box.getScale().getX()));
    });

    hud.add(scaleDialog);
    scaleDialog.pack();
    scaleDialog.centerOn(hud);
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
