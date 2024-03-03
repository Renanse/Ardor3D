/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.skin.generic;

import com.ardor3d.extension.ui.LabelState;
import com.ardor3d.extension.ui.Orientation;
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIComboBox;
import com.ardor3d.extension.ui.UIDrawer;
import com.ardor3d.extension.ui.UIDrawerBar;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIFrameBar;
import com.ardor3d.extension.ui.UIFrameStatusBar;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIMenuItem;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIPieMenu;
import com.ardor3d.extension.ui.UIPieMenuItem;
import com.ardor3d.extension.ui.UIPopupMenu;
import com.ardor3d.extension.ui.UIProgressBar;
import com.ardor3d.extension.ui.UIRadioButton;
import com.ardor3d.extension.ui.UIScrollBar;
import com.ardor3d.extension.ui.UISlider;
import com.ardor3d.extension.ui.UISliderKnob;
import com.ardor3d.extension.ui.UIState;
import com.ardor3d.extension.ui.UITab;
import com.ardor3d.extension.ui.UITabbedPane.TabPlacement;
import com.ardor3d.extension.ui.UITooltip;
import com.ardor3d.extension.ui.backdrop.EmptyBackdrop;
import com.ardor3d.extension.ui.backdrop.GradientBackdrop;
import com.ardor3d.extension.ui.backdrop.ImageArcBackdrop;
import com.ardor3d.extension.ui.backdrop.ImageBackdrop;
import com.ardor3d.extension.ui.backdrop.SolidArcBackdrop;
import com.ardor3d.extension.ui.backdrop.SolidBackdrop;
import com.ardor3d.extension.ui.backdrop.UIBackdrop;
import com.ardor3d.extension.ui.border.EmptyBorder;
import com.ardor3d.extension.ui.border.ImageBorder;
import com.ardor3d.extension.ui.border.SolidBorder;
import com.ardor3d.extension.ui.border.UIBorder;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.extension.ui.skin.Skin;
import com.ardor3d.extension.ui.text.StyleConstants;
import com.ardor3d.extension.ui.text.UIIntegerRollerField;
import com.ardor3d.extension.ui.text.UIPasswordField;
import com.ardor3d.extension.ui.text.UITextArea;
import com.ardor3d.extension.ui.text.UITextField;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.Dimension;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceSource;

public class GenericSkin extends Skin {
  protected Texture _sharedTex;

  public GenericSkin() {
    loadTexture("com/ardor3d/extension/ui/skin/generic/genericSkin.png");
  }

  public GenericSkin(final String skinTexture) {
    loadTexture(skinTexture);
  }

  protected void loadTexture(final String skinTexture) {
    try {
      _sharedTex = TextureManager.load(skinTexture, MinificationFilter.BilinearNoMipMaps,
          TextureStoreFormat.GuessNoCompressedFormat, false);
    } catch (final Exception e) {
      e.printStackTrace();
    }

  }

  public GenericSkin(final ResourceSource skinTexture) {
    loadTexture(skinTexture);
  }

  protected void loadTexture(final ResourceSource skinTexture) {
    try {
      _sharedTex = TextureManager.load(skinTexture, MinificationFilter.BilinearNoMipMaps,
          TextureStoreFormat.GuessNoCompressedFormat, false);
    } catch (final Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  protected void applyToTab(final UITab component) {

    component.setMargin(new Insets(1, 1, 1, 1));
    component.setPadding(new Insets(2, 14, 2, 14));

    // State values...

    final int leftE = component.getPlacement() != TabPlacement.EAST ? 4 : 0;
    final int rightE = component.getPlacement() != TabPlacement.WEST ? 4 : 0;
    final int topE = component.getPlacement() != TabPlacement.SOUTH ? 4 : 0;
    final int bottomE = component.getPlacement() != TabPlacement.NORTH ? 4 : 0;

    final SubTex defaultTex = new SubTex(_sharedTex, 51 - leftE, 11 - topE, 26 + leftE + rightE, 10 + topE + bottomE);
    defaultTex.setBorders(topE, leftE, bottomE, rightE);
    final UIBorder defaultBorder = new ImageBorder(defaultTex);

    final SubTex overTex = new SubTex(_sharedTex, 51 - leftE, 33 - topE, 26 + leftE + rightE, 10 + topE + bottomE);
    overTex.setBorders(topE, leftE, bottomE, rightE);
    final UIBorder overBorder = new ImageBorder(overTex);

    final SubTex pressedTex = new SubTex(_sharedTex, 51 - leftE, 55 - topE, 26 + leftE + rightE, 10 + topE + bottomE);
    pressedTex.setBorders(topE, leftE, bottomE, rightE);
    final UIBorder pressedBorder = new ImageBorder(pressedTex);

    final ColorRGBA upTop = new ColorRGBA(235 / 255f, 235 / 255f, 235 / 255f, 1);
    final ColorRGBA upBottom = new ColorRGBA(200 / 255f, 200 / 255f, 200 / 255f, 1);
    final GradientBackdrop upBack = new GradientBackdrop(upTop, upTop, upBottom, upBottom);
    final ColorRGBA downTop = new ColorRGBA(181 / 255f, 181 / 255f, 181 / 255f, 1);
    final ColorRGBA downBottom = new ColorRGBA(232 / 255f, 232 / 255f, 232 / 255f, 1);
    final GradientBackdrop downBack = new GradientBackdrop(downTop, downTop, downBottom, downBottom);
    // DEFAULT
    {
      component.getDefaultState().setBorder(defaultBorder);
      component.getDefaultState().setBackdrop(upBack);
      component.getDefaultState().setForegroundColor(ColorRGBA.BLACK);
    }
    // DISABLED
    {
      component.getDisabledState().setBorder(defaultBorder);
      component.getDisabledState().setBackdrop(upBack);
      component.getDisabledState().setForegroundColor(ColorRGBA.GRAY);

      component.getDisabledSelectedState().setBorder(pressedBorder);
      component.getDisabledSelectedState().setBackdrop(downBack);
      component.getDisabledSelectedState().setForegroundColor(ColorRGBA.GRAY);
    }
    // MOUSE OVER
    {
      final ColorRGBA top = new ColorRGBA(241 / 255f, 241 / 255f, 241 / 255f, 1);
      final ColorRGBA bottom = new ColorRGBA(216 / 255f, 216 / 255f, 216 / 255f, 1);
      final GradientBackdrop back = new GradientBackdrop(top, top, bottom, bottom);

      component.getMouseOverState().setBorder(overBorder);
      component.getMouseOverState().setBackdrop(back);
      component.getMouseOverState().setForegroundColor(ColorRGBA.BLACK);
    }
    // PRESSED AND SELECTED
    {
      component.getPressedState().setBorder(pressedBorder);
      component.getPressedState().setBackdrop(downBack);
      component.getPressedState().setForegroundColor(ColorRGBA.BLACK);

      component.getSelectedState().setBorder(pressedBorder);
      component.getSelectedState().setBackdrop(downBack);
      component.getSelectedState().setForegroundColor(ColorRGBA.BLACK);

      component.getMouseOverSelectedState().setBorder(pressedBorder);
      component.getMouseOverSelectedState().setBackdrop(downBack);
      component.getMouseOverSelectedState().setForegroundColor(ColorRGBA.GRAY);
    }
  }

  @Override
  protected void applyToButton(final UIButton component) {

    component.setAlignment(Alignment.MIDDLE);
    component.setMargin(new Insets(1, 1, 1, 1));
    component.setPadding(new Insets(2, 14, 2, 14));

    // State values...
    final SubTex defaultTex = new SubTex(_sharedTex, 47, 7, 34, 18, 4, 4, 4, 4);
    final UIBorder defaultBorder = new ImageBorder(defaultTex);

    final SubTex overTex = new SubTex(_sharedTex, 47, 29, 34, 18, 4, 4, 4, 4);
    final UIBorder overBorder = new ImageBorder(overTex);

    final SubTex pressedTex = new SubTex(_sharedTex, 47, 51, 34, 18, 4, 4, 4, 4);
    final UIBorder pressedBorder = new ImageBorder(pressedTex);

    final ColorRGBA upTop = new ColorRGBA(235 / 255f, 235 / 255f, 235 / 255f, 1);
    final ColorRGBA upBottom = new ColorRGBA(200 / 255f, 200 / 255f, 200 / 255f, 1);
    final GradientBackdrop upBack = new GradientBackdrop(upTop, upTop, upBottom, upBottom);
    final ColorRGBA downTop = new ColorRGBA(181 / 255f, 181 / 255f, 181 / 255f, 1);
    final ColorRGBA downBottom = new ColorRGBA(232 / 255f, 232 / 255f, 232 / 255f, 1);
    final GradientBackdrop downBack = new GradientBackdrop(downTop, downTop, downBottom, downBottom);
    // DEFAULT
    {
      component.getDefaultState().setBorder(defaultBorder);
      component.getDefaultState().setBackdrop(upBack);
      component.getDefaultState().setForegroundColor(ColorRGBA.BLACK);
    }
    // DISABLED
    {
      component.getDisabledState().setBorder(defaultBorder);
      component.getDisabledState().setBackdrop(upBack);
      component.getDisabledState().setForegroundColor(ColorRGBA.GRAY);

      component.getDisabledSelectedState().setBorder(pressedBorder);
      component.getDisabledSelectedState().setBackdrop(downBack);
      component.getDisabledSelectedState().setForegroundColor(ColorRGBA.GRAY);
    }
    // MOUSE OVER
    {
      final ColorRGBA top = new ColorRGBA(241 / 255f, 241 / 255f, 241 / 255f, 1);
      final ColorRGBA bottom = new ColorRGBA(216 / 255f, 216 / 255f, 216 / 255f, 1);
      final GradientBackdrop back = new GradientBackdrop(top, top, bottom, bottom);

      component.getMouseOverState().setBorder(overBorder);
      component.getMouseOverState().setBackdrop(back);
      component.getMouseOverState().setForegroundColor(ColorRGBA.BLACK);
    }
    // PRESSED AND SELECTED
    {
      component.getPressedState().setBorder(pressedBorder);
      component.getPressedState().setBackdrop(downBack);
      component.getPressedState().setForegroundColor(ColorRGBA.BLACK);

      component.getSelectedState().setBorder(pressedBorder);
      component.getSelectedState().setBackdrop(downBack);
      component.getSelectedState().setForegroundColor(ColorRGBA.BLACK);

      component.getMouseOverSelectedState().setBorder(pressedBorder);
      component.getMouseOverSelectedState().setBackdrop(downBack);
      component.getMouseOverSelectedState().setForegroundColor(ColorRGBA.GRAY);
    }
  }

  @Override
  protected void applyToCheckBox(final UICheckBox component) {

    component.setMargin(new Insets(1, 1, 1, 1));
    component.setPadding(new Insets(1, 1, 1, 1));
    component.setBorder(new EmptyBorder());
    component.setBackdrop(new EmptyBackdrop());
    component.setAlignment(Alignment.LEFT);
    component.setGap(4);

    // DEFAULT
    {
      component.getDefaultState().setForegroundColor(ColorRGBA.BLACK);
      component.getDefaultState().setIcon(new SubTex(_sharedTex, 94, 9, 14, 14));
    }
    // DISABLED
    {
      component.getDisabledState().setForegroundColor(ColorRGBA.GRAY);
      component.getDisabledState().setIcon(new SubTex(_sharedTex, 132, 9, 14, 14));
    }
    // MOUSEOVER
    {
      component.getMouseOverState().setForegroundColor(ColorRGBA.BLACK);
      component.getMouseOverState().setIcon(new SubTex(_sharedTex, 113, 9, 14, 14));
    }
    // SELECTED
    {
      component.getSelectedState().setForegroundColor(ColorRGBA.BLACK);
      component.getSelectedState().setIcon(new SubTex(_sharedTex, 94, 25, 14, 14));
    }
    // MOUSEOVER SELECTED
    {
      component.getMouseOverSelectedState().setForegroundColor(ColorRGBA.BLACK);
      component.getMouseOverSelectedState().setIcon(new SubTex(_sharedTex, 113, 25, 14, 14));
    }
    // DISABLED SELECTED
    {
      component.getDisabledSelectedState().setForegroundColor(ColorRGBA.GRAY);
      component.getDisabledSelectedState().setIcon(new SubTex(_sharedTex, 132, 25, 14, 14));
    }
  }

  @Override
  protected void applyToFrame(final UIFrame component) {
    component.setOpacity(1.0f);
    // TITLE BAR
    {
      final UIFrameBar titleBar = component.getTitleBar();
      // Make sure exists and is attached
      if (titleBar != null && titleBar.getParent() == component) {
        titleBar.setMargin(new Insets(0, 0, 0, 0));
        titleBar.setPadding(new Insets(0, 0, 0, 0));
        final SubTex borderTex = new SubTex(_sharedTex, 4, 5, 32, 13, 6, 6, 1, 6);
        final UIBorder border = new ImageBorder(borderTex);
        titleBar.setBorder(border);
        final ColorRGBA top = new ColorRGBA(203 / 255f, 203 / 255f, 203 / 255f, 1);
        final ColorRGBA bottom = new ColorRGBA(208 / 255f, 208 / 255f, 208 / 255f, 1);
        final GradientBackdrop grad = new GradientBackdrop(top, top, bottom, bottom);
        titleBar.setBackdrop(grad);

        titleBar.getTitleLabel().setMargin(new Insets(0, 5, 0, 0));
        titleBar.getTitleLabel().setForegroundColor(ColorRGBA.BLACK);

        // CLOSE BUTTON
        {
          final UIButton closeButton = titleBar.getCloseButton();
          if (closeButton != null) {
            closeButton.setButtonText("");
            closeButton.setButtonIcon(new SubTex(_sharedTex, 94, 76, 16, 16));
            closeButton.getPressedState().setIcon(new SubTex(_sharedTex, 94, 94, 16, 16));
            for (final UIState state : closeButton.getStates()) {
              state.setBackdrop(new EmptyBackdrop());
              state.setBorder(new EmptyBorder());
              state.setPadding(new Insets(0, 0, 0, 0));
              state.setMargin(new Insets(1, 1, 1, 1));
            }
            closeButton.refreshState();
            closeButton.pack();
            closeButton.setMaximumContentSize(closeButton.getContentWidth(), closeButton.getContentHeight());
          }
        }

        // MINIMIZE BUTTON
        {
          final UIButton minimizeButton = titleBar.getMinimizeButton();
          if (minimizeButton != null) {
            minimizeButton.setButtonText("");
            minimizeButton.setButtonIcon(new SubTex(_sharedTex, 113, 76, 16, 16));
            minimizeButton.getPressedState().setIcon(new SubTex(_sharedTex, 113, 94, 16, 16));
            for (final UIState state : minimizeButton.getStates()) {
              state.setBackdrop(new EmptyBackdrop());
              state.setBorder(new EmptyBorder());
              state.setPadding(new Insets(0, 0, 0, 0));
              state.setMargin(new Insets(1, 1, 1, 1));
            }
            minimizeButton.refreshState();
            minimizeButton.pack();
            minimizeButton.setMaximumContentSize(minimizeButton.getContentWidth(), minimizeButton.getContentHeight());
          }
        }

        // EXPAND BUTTON
        {
          final UIButton expandButton = titleBar.getExpandButton();
          if (expandButton != null) {
            expandButton.setButtonText("");
            expandButton.setButtonIcon(new SubTex(_sharedTex, 132, 76, 16, 16));
            expandButton.getPressedState().setIcon(new SubTex(_sharedTex, 132, 94, 16, 16));
            for (final UIState state : expandButton.getStates()) {
              state.setBackdrop(new EmptyBackdrop());
              state.setBorder(new EmptyBorder());
              state.setPadding(new Insets(0, 0, 0, 0));
              state.setMargin(new Insets(1, 1, 1, 1));
            }
            expandButton.refreshState();
            expandButton.pack();
            expandButton.setMaximumContentSize(expandButton.getContentWidth(), expandButton.getContentHeight());
          }
        }

        // HELP BUTTON
        {
          final UIButton helpButton = titleBar.getHelpButton();
          if (helpButton != null) {
            helpButton.setButtonText("");
            helpButton.setButtonIcon(new SubTex(_sharedTex, 151, 76, 16, 16));
            helpButton.getPressedState().setIcon(new SubTex(_sharedTex, 151, 94, 16, 16));
            for (final UIState state : helpButton.getStates()) {
              state.setBackdrop(new EmptyBackdrop());
              state.setBorder(new EmptyBorder());
              state.setPadding(new Insets(0, 0, 0, 0));
              state.setMargin(new Insets(1, 1, 1, 1));
            }
            helpButton.refreshState();
            helpButton.pack();
            helpButton.setMaximumContentSize(helpButton.getContentWidth(), helpButton.getContentHeight());
          }
        }
      }
    }

    // BASE PANEL
    {
      final UIPanel base = component.getBasePanel();

      base.setMargin(new Insets(0, 0, 0, 0));
      base.setPadding(new Insets(0, 0, 0, 0));

      final SubTex borderTex = new SubTex(_sharedTex, 4, 17, 32, 36, 0, 6, 7, 6);
      final UIBorder border = new ImageBorder(borderTex);
      base.setBorder(border);
      final ColorRGBA top = new ColorRGBA(210 / 255f, 210 / 255f, 210 / 255f, 1);
      final ColorRGBA bottom = new ColorRGBA(244 / 255f, 244 / 255f, 244 / 255f, 1);
      final GradientBackdrop grad = new GradientBackdrop(top, top, bottom, bottom);
      base.setBackdrop(grad);
    }

    // STATUS BAR
    {
      final UIFrameStatusBar statusBar = component.getStatusBar();
      // Make sure exists and is attached
      if (statusBar != null && statusBar.getParent() == component.getBasePanel()) {
        statusBar.setLocalComponentHeight(12);
        statusBar.setMaximumContentHeight(statusBar.getContentHeight());

        final UIButton resize = statusBar.getResizeButton();
        if (resize != null && resize.getParent() == statusBar) {
          for (final UIState state : resize.getStates()) {
            state.setBackdrop(new EmptyBackdrop());
            state.setBorder(new EmptyBorder());
            state.setPadding(new Insets(0, 0, 0, 0));
            state.setMargin(new Insets(0, 0, 0, 0));
            state.setForegroundColor(ColorRGBA.GRAY);
          }
          resize.refreshState();
          resize.updateMinimumSizeFromContents();
          resize.setMinimumContentSize(resize.getContentWidth(), resize.getContentHeight());
          resize.setMaximumContentSize(resize.getContentWidth(), resize.getContentHeight());
        }
      }
    }
  }

  @Override
  protected void applyToDrawer(final UIDrawer component) {
    component.setOpacity(1.0f);
    // TITLE BAR
    {
      final UIDrawerBar titleBar = component.getTitleBar();
      // Make sure exists
      if (titleBar != null) {
        titleBar.setMargin(new Insets(0, 0, 0, 0));
        titleBar.setPadding(new Insets(0, 0, 0, 0));
        switch (component.getEdge()) {
          case BOTTOM: {
            titleBar.setBorder(new ImageBorder(new SubTex(_sharedTex, 4, 5, 32, 13, 6, 6, 1, 6)));
            titleBar.getTitleLabel().setMargin(new Insets(0, 5, 0, 0));

            final ColorRGBA top = new ColorRGBA(203 / 255f, 203 / 255f, 203 / 255f, 1);
            final ColorRGBA bottom = new ColorRGBA(208 / 255f, 208 / 255f, 208 / 255f, 1);
            titleBar.setBackdrop(new GradientBackdrop(top, top, bottom, bottom));
            break;
          }
          case TOP: {
            titleBar.setBorder(new ImageBorder(new SubTex(_sharedTex, 4, 38, 32, 15, 1, 6, 8, 6)));
            titleBar.getTitleLabel().setMargin(new Insets(0, 5, 0, 0));

            final ColorRGBA top = new ColorRGBA(236 / 255f, 236 / 255f, 236 / 255f, 1);
            final ColorRGBA bottom = new ColorRGBA(244 / 255f, 244 / 255f, 244 / 255f, 1);
            titleBar.setBackdrop(new GradientBackdrop(top, top, bottom, bottom));
            break;
          }
          case LEFT: {// 18, 5 18, 48
            titleBar.setBorder(new ImageBorder(new SubTex(_sharedTex, 18, 5, 18, 48, 6, 1, 8, 6)));
            titleBar.getTitleLabel().setMargin(new Insets(0, 0, 5, 0));

            final ColorRGBA top = new ColorRGBA(203 / 255f, 203 / 255f, 203 / 255f, 1);
            final ColorRGBA bottom = new ColorRGBA(243 / 255f, 243 / 255f, 243 / 255f, 1);
            titleBar.setBackdrop(new GradientBackdrop(top, top, bottom, bottom));
            break;
          }
          case RIGHT: {
            titleBar.setBorder(new ImageBorder(new SubTex(_sharedTex, 4, 5, 18, 48, 6, 6, 8, 1)));
            titleBar.getTitleLabel().setMargin(new Insets(0, 0, 5, 0));

            final ColorRGBA top = new ColorRGBA(203 / 255f, 203 / 255f, 203 / 255f, 1);
            final ColorRGBA bottom = new ColorRGBA(243 / 255f, 243 / 255f, 243 / 255f, 1);
            titleBar.setBackdrop(new GradientBackdrop(top, top, bottom, bottom));
            break;
          }
        }
        titleBar.getTitleLabel().setForegroundColor(ColorRGBA.BLACK);

        // CLOSE BUTTON
        {
          final UIButton closeButton = titleBar.getCloseButton();
          if (closeButton != null) {
            closeButton.setButtonText("");
            closeButton.setButtonIcon(new SubTex(_sharedTex, 94, 76, 16, 16));
            closeButton.getPressedState().setIcon(new SubTex(_sharedTex, 94, 94, 16, 16));
            closeButton.setBackdrop(new EmptyBackdrop(), true);
            closeButton.setBorder(new EmptyBorder(), true);
            closeButton.setPadding(new Insets(0, 0, 0, 0), true);
            closeButton.setMargin(new Insets(1, 1, 1, 1), true);
            closeButton.refreshState();
            closeButton.pack();
            closeButton.setMaximumContentSize(closeButton.getContentWidth(), closeButton.getContentHeight());
          }
        }
      }
    }

    // CONTENT PANEL
    {
      final UIPanel content = component.getContentPanel();

      content.setMargin(new Insets(0, 0, 0, 0));
      content.setPadding(new Insets(0, 0, 0, 0));

      switch (component.getEdge()) {
        case BOTTOM: {
          final ColorRGBA top = new ColorRGBA(210 / 255f, 210 / 255f, 210 / 255f, 1);
          final ColorRGBA bottom = new ColorRGBA(244 / 255f, 244 / 255f, 244 / 255f, 1);
          content.setBackdrop(new GradientBackdrop(top, top, bottom, bottom));
          content.setBorder(new ImageBorder(new SubTex(_sharedTex, 4, 17, 32, 29, 0, 6, 0, 6)));
          break;
        }
        case TOP: {
          final ColorRGBA top = new ColorRGBA(202 / 255f, 202 / 255f, 202 / 255f, 1);
          final ColorRGBA bottom = new ColorRGBA(235 / 255f, 235 / 255f, 235 / 255f, 1);
          content.setBackdrop(new GradientBackdrop(top, top, bottom, bottom));
          content.setBorder(new ImageBorder(new SubTex(_sharedTex, 4, 10, 32, 28, 0, 6, 0, 6)));
          break;
        }
        case LEFT: {
          final ColorRGBA top = new ColorRGBA(203 / 255f, 203 / 255f, 203 / 255f, 1);
          final ColorRGBA bottom = new ColorRGBA(243 / 255f, 243 / 255f, 243 / 255f, 1);
          content.setBackdrop(new GradientBackdrop(top, top, bottom, bottom));
          content.setBorder(new ImageBorder(new SubTex(_sharedTex, 12, 5, 6, 48, 6, 0, 8, 0)));
          break;
        }

        case RIGHT: {
          final ColorRGBA top = new ColorRGBA(203 / 255f, 203 / 255f, 203 / 255f, 1);
          final ColorRGBA bottom = new ColorRGBA(243 / 255f, 243 / 255f, 243 / 255f, 1);
          content.setBackdrop(new GradientBackdrop(top, top, bottom, bottom));
          content.setBorder(new ImageBorder(new SubTex(_sharedTex, 22, 5, 6, 48, 6, 0, 8, 0)));
          break;
        }
      }
    }
  }

  @Override
  protected void applyToLabel(final UILabel component) {
    component.getDefaultState().setForegroundColor(ColorRGBA.BLACK);
    component.getDisabledState().setForegroundColor(ColorRGBA.GRAY);
  }

  @Override
  protected void applyToTextField(final UITextField component) {

    final SolidBorder border = new SolidBorder(1, 1, 1, 1);
    border.setLeftColor(ColorRGBA.GRAY);
    border.setTopColor(ColorRGBA.GRAY);
    border.setRightColor(ColorRGBA.LIGHT_GRAY);
    border.setBottomColor(ColorRGBA.LIGHT_GRAY);

    final SolidBackdrop backdrop = new SolidBackdrop(ColorRGBA.WHITE);

    component.setPadding(new Insets(1, 1, 1, 1));

    for (final UIState state : component.getStates()) {
      state.setBorder(border);
      state.setBackdrop(backdrop);
      if (state.equals(component.getDisabledState())) {
        state.setForegroundColor(ColorRGBA.GRAY);
      } else {
        state.setForegroundColor(ColorRGBA.BLACK);
      }
    }

  }

  @Override
  protected void applyToPasswordField(final UIPasswordField component) {
    applyToTextField(component);
  }

  @Override
  protected void applyToTextArea(final UITextArea component) {

    final SolidBorder border = new SolidBorder(1, 1, 1, 1);
    border.setLeftColor(ColorRGBA.GRAY);
    border.setTopColor(ColorRGBA.GRAY);
    border.setRightColor(ColorRGBA.LIGHT_GRAY);
    border.setBottomColor(ColorRGBA.LIGHT_GRAY);

    final SolidBackdrop backdrop = new SolidBackdrop(ColorRGBA.WHITE);

    component.setPadding(new Insets(1, 1, 1, 1));

    for (final UIState state : component.getStates()) {
      state.setBorder(border);
      state.setBackdrop(backdrop);
      if (state.equals(component.getDisabledState())) {
        state.setForegroundColor(ColorRGBA.GRAY);
      } else {
        state.setForegroundColor(ColorRGBA.BLACK);
      }
    }

  }

  @Override
  protected void applyToIntegerRollerField(final UIIntegerRollerField component) {

    final SolidBorder border = new SolidBorder(1, 1, 1, 1);
    border.setLeftColor(ColorRGBA.GRAY);
    border.setTopColor(ColorRGBA.GRAY);
    border.setRightColor(ColorRGBA.LIGHT_GRAY);
    border.setBottomColor(ColorRGBA.LIGHT_GRAY);

    final SolidBackdrop backdrop = new SolidBackdrop(ColorRGBA.WHITE);

    component.setPadding(new Insets(1, 1, 1, 1));

    for (final UIState state : component.getField().getStates()) {
      state.setBorder(border);
      state.setBackdrop(backdrop);
      if (state.equals(component.getField().getDisabledState())) {
        state.setForegroundColor(ColorRGBA.GRAY);
      } else {
        state.setForegroundColor(ColorRGBA.BLACK);
      }
    }

    {
      final UIButton button = component.getRollUpButton();
      button.setBackdrop(null);
      button.setBorder(new EmptyBorder());
      button.setPadding(new Insets(0, 0, 0, 0));
      button.setMargin(new Insets(0, 0, 0, 0));
      for (final UIState state : button.getStates()) {
        state.setBorder(null);
        state.setBackdrop(null);
      }
      button.setButtonText("");
      button.setButtonIcon(new SubTex(_sharedTex, 97, 120, 15, 16));
      button.setIconDimensions(new Dimension(10, 10));
      button.getMouseOverState().setIcon(new SubTex(_sharedTex, 113, 120, 15, 16));
    }
    {
      final UIButton button = component.getRollDownButton();
      button.setBackdrop(null);
      button.setBorder(new EmptyBorder());
      button.setPadding(new Insets(0, 0, 0, 0));
      button.setMargin(new Insets(0, 0, 0, 0));
      for (final UIState state : button.getStates()) {
        state.setBorder(null);
        state.setBackdrop(null);
      }
      button.setButtonText("");
      button.setButtonIcon(new SubTex(_sharedTex, 97, 137, 15, 16));
      button.setIconDimensions(new Dimension(10, 10));
      button.getMouseOverState().setIcon(new SubTex(_sharedTex, 113, 137, 15, 16));
    }
  }

  @Override
  protected void applyToPieMenu(final UIPieMenu component) {
    final UIBackdrop pieBack = new SolidArcBackdrop(new ColorRGBA(.9f, .9f, .9f, .6f));
    component.setBackdrop(pieBack);
  }

  @Override
  protected void applyToPieMenuItem(final UIPieMenuItem component) {
    final EmptyBorder itemBorder = new EmptyBorder();
    final SubTex bgImage = new SubTex(_sharedTex, 10, 102, 7, 45, 4, 4, 3, 0);
    final UIBackdrop overBackdrop = new ImageArcBackdrop(bgImage, new ColorRGBA(.9f, .9f, .9f, 1f));
    final UIBackdrop pieBack = new ImageArcBackdrop(bgImage, new ColorRGBA(.9f, .9f, .9f, .8f));
    component.setBackdrop(pieBack);
    component.setBorder(itemBorder);
    component.addFontStyle(StyleConstants.KEY_BOLD, Boolean.TRUE);
    component.setMargin(new Insets(0, 0, 0, 0));
    component.setPadding(new Insets(0, 2, 0, 2));
    component.setForegroundColor(ColorRGBA.BLACK);
    component.setAlignment(Alignment.LEFT);
    for (final UIState state : component.getStates()) {
      state.setBorder(null);
      state.setBackdrop(pieBack);
      state.setForegroundColor(ColorRGBA.BLACK);
    }

    final LabelState over = component.getMouseOverState();
    over.setForegroundColor(ColorRGBA.BLACK);
    over.setBackdrop(overBackdrop);

  }

  @Override
  protected void applyToPanel(final UIPanel component) {
    // nothing to do
  }

  @Override
  protected void applyToProgressBar(final UIProgressBar component) {
    final ColorRGBA top = new ColorRGBA(235 / 255f, 235 / 255f, 235 / 255f, 1);
    final ColorRGBA bottom = new ColorRGBA(200 / 255f, 200 / 255f, 200 / 255f, 1);
    final GradientBackdrop mainBack = new GradientBackdrop(top, top, bottom, bottom);
    component.getMainPanel().setBackdrop(mainBack);
    component.getMainPanel().setBorder(new EmptyBorder(0, 0, 0, 0));

    final ImageBackdrop barBack = new ImageBackdrop(new SubTex(_sharedTex, 11, 59, 22, 15));
    component.getBar().setBackdrop(barBack);
  }

  @Override
  protected void applyToRadioButton(final UIRadioButton component) {

    component.setMargin(new Insets(1, 1, 1, 1));
    component.setPadding(new Insets(1, 1, 1, 1));
    component.setBorder(new EmptyBorder());
    component.setBackdrop(new EmptyBackdrop());
    component.setAlignment(Alignment.LEFT);
    component.setGap(4);

    // DEFAULT
    {
      component.getDefaultState().setForegroundColor(ColorRGBA.BLACK);
      component.getDefaultState().setIcon(new SubTex(_sharedTex, 94, 42, 14, 14));
    }
    // DISABLED
    {
      component.getDisabledState().setForegroundColor(ColorRGBA.GRAY);
      component.getDisabledState().setIcon(new SubTex(_sharedTex, 132, 42, 14, 14));
    }
    // MOUSEOVER
    {
      component.getMouseOverState().setForegroundColor(ColorRGBA.BLACK);
      component.getMouseOverState().setIcon(new SubTex(_sharedTex, 113, 42, 14, 14));
    }
    // SELECTED
    {
      component.getSelectedState().setForegroundColor(ColorRGBA.BLACK);
      component.getSelectedState().setIcon(new SubTex(_sharedTex, 94, 59, 14, 14));
    }
    // MOUSEOVER SELECTED
    {
      component.getMouseOverSelectedState().setForegroundColor(ColorRGBA.BLACK);
      component.getMouseOverSelectedState().setIcon(new SubTex(_sharedTex, 113, 59, 14, 14));
    }
    // DISABLED SELECTED
    {
      component.getDisabledSelectedState().setForegroundColor(ColorRGBA.GRAY);
      component.getDisabledSelectedState().setIcon(new SubTex(_sharedTex, 132, 59, 14, 14));
    }
  }

  @Override
  protected void applyToTooltip(final UITooltip component) {
    component.setBackdrop(new SolidBackdrop(ColorRGBA.LIGHT_GRAY));
    component.setBorder(new SolidBorder(1, 1, 1, 1));
    component.setForegroundColor(ColorRGBA.BLACK);
    component.setOpacity(1.0f);
  }

  @Override
  protected void applyToSlider(final UISlider component) {
    final UISliderKnob knob = component.getKnob();
    knob.setBackdrop(null);
    knob.setPadding(new Insets(0, 0, 0, 0));

    if (component.getOrientation() == Orientation.Horizontal) {
      knob.getKnobLabel().setIcon(new SubTex(_sharedTex, 42, 80, 16, 14));
      knob.setMargin(new Insets(0, 1, 0, 1));
    } else {
      knob.getKnobLabel().setIcon(new SubTex(_sharedTex, 69, 72, 14, 16));
      knob.setMargin(new Insets(1, 0, 1, 0));
    }

    final UIPanel back = component.getBackPanel();
    if (component.getOrientation() == Orientation.Horizontal) {
      final SubTex borderTex = new SubTex(_sharedTex, 7, 79, 30, 17, 6, 4, 4, 4);
      final UIBorder border = new ImageBorder(borderTex);
      back.setBorder(border);
      back.setMinimumContentSize(1, 7);
    } else {
      final SubTex borderTex = new SubTex(_sharedTex, 67, 91, 18, 29, 6, 5, 4, 5);
      final UIBorder border = new ImageBorder(borderTex);
      back.setBorder(border);
      back.setMinimumContentSize(8, 1);
    }
    back.setLayout(null);
    back.setBackdrop(new SolidBackdrop(ColorRGBA.WHITE));
  }

  @Override
  protected void applyToPopupMenu(final UIPopupMenu component) {
    component.setOpacity(1.0f);

    component.setMargin(new Insets(0, 0, 0, 0));
    component.setPadding(new Insets(0, 0, 0, 0));

    final SubTex borderTex = new SubTex(_sharedTex, 4, 17, 32, 36, 0, 6, 7, 6);
    final UIBorder border = new ImageBorder(borderTex);
    component.setBorder(border);
    final ColorRGBA top = new ColorRGBA(210 / 255f, 210 / 255f, 210 / 255f, 1);
    final ColorRGBA bottom = new ColorRGBA(244 / 255f, 244 / 255f, 244 / 255f, 1);
    final GradientBackdrop grad = new GradientBackdrop(top, top, bottom, bottom);
    component.setBackdrop(grad);
  }

  @Override
  protected void applyToComboBox(final UIComboBox component) {
    final ColorRGBA upTop = new ColorRGBA(235 / 255f, 235 / 255f, 235 / 255f, 1);
    final ColorRGBA upBottom = new ColorRGBA(200 / 255f, 200 / 255f, 200 / 255f, 1);
    final GradientBackdrop upBack = new GradientBackdrop(upTop, upTop, upBottom, upBottom);

    // value label
    {
      final SubTex borderTex = new SubTex(_sharedTex, 155, 7, 21, 18, 4, 4, 4, 1);
      final UIBorder labelBorder = new ImageBorder(borderTex);

      final UILabel label = component.getValueLabel();
      label.setBackdrop(upBack);
      label.setBorder(labelBorder);
      label.setAlignment(Alignment.LEFT);
      label.setPadding(new Insets(0, 2, 0, 2));
    }

    // drop down button
    {
      final SubTex borderTex = new SubTex(_sharedTex, 177, 7, 12, 18, 4, 1, 4, 4);
      final UIBorder buttonBorder = new ImageBorder(borderTex);

      final UIButton button = component.getOpenButton();
      button.setButtonText("");
      button.setButtonIcon(new SubTex(_sharedTex, 196, 12, 10, 9));
      button.getMouseOverState().setIcon(new SubTex(_sharedTex, 210, 12, 10, 9));
      button.setBorder(buttonBorder);
      button.setBackdrop(upBack);
      button.setMargin(new Insets(0, 0, 0, 0));
      button.setPadding(new Insets(0, 1, 0, 1));
      for (final UIState state : button.getStates()) {
        state.setBorder(buttonBorder);
        state.setBackdrop(upBack);
      }
    }

    // skin for menuitems
    {
      final EmptyBorder itemBorder = new EmptyBorder();
      final EmptyBackdrop itemBackdrop = new EmptyBackdrop();
      final SolidBackdrop overBackdrop = new SolidBackdrop(new ColorRGBA(50 / 255f, 50 / 255f, 200 / 255f, 1));
      component.setItemSkinCallback(c -> {
        c.setBorder(itemBorder);
        c.setBackdrop(itemBackdrop);
        c.setMargin(new Insets(0, 0, 0, 0));
        c.setPadding(new Insets(0, 2, 0, 2));
        c.setForegroundColor(ColorRGBA.BLACK);
        if (c instanceof UIButton button) {
          button.setAlignment(Alignment.LEFT);
          for (final UIState state : button.getStates()) {
            state.setBorder(null);
            state.setBackdrop(itemBackdrop);
            state.setForegroundColor(ColorRGBA.BLACK);
          }
          final LabelState over = button.getMouseOverState();
          over.setForegroundColor(ColorRGBA.WHITE);
          over.setBackdrop(overBackdrop);
        }
      });
    }
  }

  @Override
  protected void applyToMenuItem(final UIMenuItem component) {
    final EmptyBorder itemBorder = new EmptyBorder();
    final EmptyBackdrop itemBackdrop = new EmptyBackdrop();
    final SolidBackdrop overBackdrop = new SolidBackdrop(new ColorRGBA(50 / 255f, 50 / 255f, 200 / 255f, 1));
    component.setBorder(itemBorder);
    component.setBackdrop(itemBackdrop);
    component.setMargin(new Insets(0, 0, 0, 0));
    component.setPadding(new Insets(0, 2, 0, 2));
    component.setForegroundColor(ColorRGBA.BLACK);
    component.setAlignment(Alignment.LEFT);
    for (final UIState state : component.getStates()) {
      state.setBorder(null);
      state.setBackdrop(itemBackdrop);
      state.setForegroundColor(ColorRGBA.BLACK);
    }
    final LabelState over = component.getMouseOverState();
    over.setForegroundColor(ColorRGBA.WHITE);
    over.setBackdrop(overBackdrop);
  }

  @Override
  protected void applyToScrollBar(final UIScrollBar component) {
    final SolidBorder border = new SolidBorder(1, 1, 1, 1);
    border.setColor(new ColorRGBA(165 / 255f, 165 / 255f, 165 / 255f, 1f));
    component.setMargin(new Insets());
    component.setPadding(new Insets());
    component.setBorder(border);
    {
      final UIButton button = component.getBtTopLeft();
      button.setBackdrop(null);
      button.setBorder(new EmptyBorder());
      button.setPadding(new Insets(0, 0, 0, 0));
      button.setMargin(new Insets(0, 0, 0, 0));
      for (final UIState state : button.getStates()) {
        state.setBorder(null);
        state.setBackdrop(null);
      }
      button.setButtonText("");
      if (component.getOrientation() == Orientation.Horizontal) {
        button.setButtonIcon(new SubTex(_sharedTex, 130, 121, 16, 15));
        button.getMouseOverState().setIcon(new SubTex(_sharedTex, 130, 137, 16, 15));
      } else {
        button.setButtonIcon(new SubTex(_sharedTex, 97, 120, 15, 16));
        button.getMouseOverState().setIcon(new SubTex(_sharedTex, 113, 120, 15, 16));
      }
    }
    {
      final UIButton button = component.getBtBottomRight();
      button.setBackdrop(null);
      button.setBorder(new EmptyBorder());
      button.setPadding(new Insets(0, 0, 0, 0));
      button.setMargin(new Insets(0, 0, 0, 0));
      for (final UIState state : button.getStates()) {
        state.setBorder(null);
        state.setBackdrop(null);
      }
      button.setButtonText("");
      if (component.getOrientation() == Orientation.Horizontal) {
        button.setButtonIcon(new SubTex(_sharedTex, 147, 121, 16, 15));
        button.getMouseOverState().setIcon(new SubTex(_sharedTex, 147, 137, 16, 15));
      } else {
        button.setButtonIcon(new SubTex(_sharedTex, 97, 137, 15, 16));
        button.getMouseOverState().setIcon(new SubTex(_sharedTex, 113, 137, 15, 16));
      }
    }
    {
      final UISlider slider = component.getSlider();
      slider.getBackPanel().setBorder(new EmptyBorder());
      slider.setMargin(new Insets());
      slider.setPadding(new Insets());
      slider.getBackPanel().setLayout(new RowLayout(false));

      final UISliderKnob knob = slider.getKnob();
      knob.getKnobLabel().setIcon(null);
      knob.setPadding(new Insets(0, 0, 0, 0));
      knob.setMargin(new Insets());
      final ColorRGBA colorTop = new ColorRGBA(235 / 255f, 235 / 255f, 235 / 255f, 1);
      final ColorRGBA colorBtm = new ColorRGBA(200 / 255f, 200 / 255f, 200 / 255f, 1);
      final GradientBackdrop knobColor = new GradientBackdrop(colorTop, colorTop, colorBtm, colorBtm);
      knob.getKnobLabel().setBackdrop(knobColor);
      knob.getKnobLabel().setBorder(border);
    }
  }
}
