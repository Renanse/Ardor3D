/**
 * Copyright (c) 2008-2022 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.ui;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.backdrop.SolidBackdrop;
import com.ardor3d.extension.ui.backdrop.SolidDiskBackdrop;
import com.ardor3d.extension.ui.border.SolidBorder;
import com.ardor3d.extension.ui.layout.RectLayout;
import com.ardor3d.extension.ui.layout.RectLayoutData;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.Dimension;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

public class UICompassExample extends ExampleBase {

  private UIHud hud;

  public static void main(final String[] args) {
    start(UICompassExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("UI Compass Example");
    _canvas.setBackgroundColor(ColorRGBA.BLUE);
    UIComponent.setUseTransparency(true);
    initUI();
  }

  double last = 0;

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    hud.updateGeometricState(timer.getTimePerFrame());

    // DEBUG - Allows you to run in debug mode in Eclipse and tweak to see updates to UI in real time
    last += timer.getTimePerFrame();
    if (last < 2.0) {
      return;
    }
    last %= 2.0;
    initUI();
  }

  @Override
  protected void renderExample(final Renderer renderer) {
    super.renderExample(renderer);
    hud.draw(renderer);
  }

  public void initUI() {
    // Set up a hud to control rendering our UI
    hud = new UIHud(_canvas);

    // Here I am setting up a main panel and coming up with some size/padding values.
    // The compass will be a main panel with a sub panel for compass elements, a subpanel for headings,
    // and several labels.
    final int cWidth = 600, cHeight = 80, edgePadding = 75, topPadding = (int) (cHeight * .25),
        bottomPadding = (int) (cHeight * .30);
    final var compassPanel = new UIPanel("compass", new RectLayout());
    compassPanel.setMinimumContentSize(cWidth, cHeight);
    compassPanel.setOpacity(.65f);
    compassPanel.setHudXY((_canvas.getContentWidth() - cWidth) / 2, _canvas.getContentHeight() - (int) (cHeight * 1.2));
    compassPanel.setBackdrop(new SolidBackdrop(new ColorRGBA(0f, 0f, 0f, 1f)));

    // Add an inner panel for icons and other elements
    final var innerCompassPanel = new UIPanel("innerCompass", new RectLayout());
    innerCompassPanel
        .setLayoutData(new RectLayoutData(0.0, 0.0, 1.0, 1.0, topPadding, edgePadding, bottomPadding, edgePadding));
    innerCompassPanel
        .setBorder(new SolidBorder(2, 2, 2, 2, ColorRGBA.BLACK, ColorRGBA.BLACK, ColorRGBA.BLACK, ColorRGBA.BLACK));
    innerCompassPanel.setBackdrop(new SolidBackdrop(new ColorRGBA(.25f, .25f, .25f, 1f)));
    compassPanel.add(innerCompassPanel);

    // edge labels
    final var titleLabel = new UILabel("MY COMPASS");
    titleLabel.setForegroundColor(ColorRGBA.LIGHT_GRAY);
    titleLabel.setAlignment(Alignment.LEFT);
    titleLabel.setLayoutData(new RectLayoutData(0, 1, 1.0, 1, 0, 25, -20, 5));
    compassPanel.add(titleLabel);

    final var leftLabel = new UILabel("PORT");
    leftLabel.setForegroundColor(ColorRGBA.WHITE);
    leftLabel.setAlignment(Alignment.RIGHT);
    leftLabel.setLayoutData(new RectLayoutData(0, .42, 0.0, 0.42, 0, 5, 0, 5 - edgePadding));
    compassPanel.add(leftLabel);

    final var rightLabel = new UILabel("STBD");
    rightLabel.setForegroundColor(ColorRGBA.WHITE);
    rightLabel.setAlignment(Alignment.LEFT);
    rightLabel.setLayoutData(new RectLayoutData(1.0, .42, 1.0, 0.42, 0, 5 - edgePadding, 0, 5));
    compassPanel.add(rightLabel);

    // Add edge decoration - bad areas, etc.
    final int edgeWidth = 25;
    final var leftFarEdge = new UIPanel("farleft");
    leftFarEdge.setLayoutData(new RectLayoutData(0, 0, 0.0, 1.0, 0, 0, 0, -edgeWidth));
    leftFarEdge.setBackdrop(new SolidBackdrop(new ColorRGBA(1, 0, 0, 1f)));
    innerCompassPanel.add(leftFarEdge);

    final var leftEdge = new UIPanel("left");
    leftEdge.setLayoutData(new RectLayoutData(0, 0, 0.0, 1.0, 0, edgeWidth, 0, -edgeWidth * 2));
    leftEdge.setBackdrop(new SolidBackdrop(new ColorRGBA(0, .66f, 0, 1f)));
    innerCompassPanel.add(leftEdge);

    final var rightEdge = new UIPanel("right");
    rightEdge.setLayoutData(new RectLayoutData(1.0, 0, 1.0, 1.0, 0, -edgeWidth * 2, 0, edgeWidth));
    rightEdge.setBackdrop(new SolidBackdrop(new ColorRGBA(0, .66f, 0, 1f)));
    innerCompassPanel.add(rightEdge);

    final var rightFarEdge = new UIPanel("farright");
    rightFarEdge.setLayoutData(new RectLayoutData(1.0, 0, 1.0, 1.0, 0, -edgeWidth, 0, 0));
    rightFarEdge.setBackdrop(new SolidBackdrop(new ColorRGBA(1, 0, 0, 1f)));
    innerCompassPanel.add(rightFarEdge);

    // Add some sort of "interest" icon to the compass
    final double iconLocPercent = .3;
    final int iconSize = 20;
    final Texture tex =
        TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, false);
    final var iconLabel = new UILabel("");
    iconLabel.setIcon(new SubTex(tex));
    iconLabel.setIconDimensions(new Dimension(iconSize, iconSize));
    iconLabel.setLayoutData(new RectLayoutData(iconLocPercent, .5, iconLocPercent, .5, -iconSize / 2, -iconSize / 2,
        -iconSize / 2, -iconSize / 2));
    innerCompassPanel.add(iconLabel);

    // Let's get really creative and add a circle to indicate where the sun is or something like that!
    final double sunLocPercent = .75;
    final int sunSize = 20;
    final var sunSpot = new UIPanel("sun");
    sunSpot.setLayoutData(new RectLayoutData(sunLocPercent, 1, sunLocPercent, 1, 0, -sunSize / 2, 0, -sunSize / 2));
    sunSpot.setBackdrop(new SolidDiskBackdrop(ColorRGBA.YELLOW));
    innerCompassPanel.add(sunSpot);

    // Add a line to indicate maybe the heading of something, or a direction, etc.
    final var dirIndicator = new UIPanel("fwd");
    final double dirLocPercent = .6;
    final int dirLocWidth = 4;
    dirIndicator.setLayoutData(
        new RectLayoutData(dirLocPercent, 0, dirLocPercent, 1, 0, -dirLocWidth / 2, 0, -dirLocWidth / 2));
    dirIndicator.setBackdrop(new SolidBackdrop(ColorRGBA.MAGENTA));
    innerCompassPanel.add(dirIndicator);

    // Ok, now lets add some heading values below the sub panel. We'll start by creating a cropping
    // panel because we want the text to crop to the same width as our inner compass panel.
    final var cropPanel = new UIPanel("text crop", new RectLayout());
    cropPanel.setLayoutData(new RectLayoutData(0, 0, 1, 0, 2 - bottomPadding, edgePadding, 2, edgePadding));
    compassPanel.add(cropPanel);

    // Here we are doing a very quick calculation of some places where heading text might go. In reality
    // this would be some kind of calculation based on character facing direction, maybe camera viewing
    // angle, etc.
    final double spacing = .2, offset = -.03;
    int index = 0;
    for (int d = -180; d <= 270; d += 90) {
      final var degLabel = new UILabel(Integer.toString(d));
      degLabel.setForegroundColor(ColorRGBA.WHITE);
      degLabel.setAlignment(Alignment.MIDDLE);
      degLabel.setLayoutData(new RectLayoutData(spacing * index + offset, 0, spacing * index + offset, 1, 0, 0, 0, 0));
      cropPanel.add(degLabel);
      index++;
    }

    // pack our compass panel to get things sized and laid out.
    compassPanel.pack();
    hud.add(compassPanel);

    hud.updateGeometricState(0);
  }
}
