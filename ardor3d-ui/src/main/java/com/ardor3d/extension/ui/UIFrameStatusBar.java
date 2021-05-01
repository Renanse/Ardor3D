/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.event.DragListener;
import com.ardor3d.extension.ui.event.FrameResizeListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.input.InputState;

/**
 * This panel extension defines a frame status bar (used at the bottom of a frame) with a text label
 * and resize handle.
 */
public class UIFrameStatusBar extends UIPanel {

  /** Our text label. */
  private final UILabel _statusLabel;

  /** Resize handle, used to drag out this content's size when the frame is set as resizeable. */
  private final FrameResizeButton _resizeButton;

  /** A drag listener used to perform resize operations on this frame. */
  private DragListener _resizeListener = new FrameResizeListener(this);

  /**
   * Construct a new status bar
   */
  public UIFrameStatusBar() {
    super("statusBar", new BorderLayout());

    _statusLabel = new UILabel("");
    _statusLabel.setLayoutData(BorderLayoutData.CENTER);
    add(_statusLabel);

    _resizeButton = new FrameResizeButton();
    _resizeButton.setLayoutData(BorderLayoutData.EAST);
    add(_resizeButton);
  }

  public FrameResizeButton getResizeButton() { return _resizeButton; }

  public UILabel getStatusLabel() { return _statusLabel; }

  @Override
  public void attachedToHud() {
    super.attachedToHud();
    final UIHud hud = getHud();
    if (hud != null) {
      hud.addDragListener(_resizeListener);
    }
  }

  @Override
  public void detachedFromHud() {
    super.detachedFromHud();
    final UIHud hud = getHud();
    if (hud != null) {
      hud.removeDragListener(_resizeListener);
    }
  }

  public void setResizeListener(final DragListener listener) { _resizeListener = listener; }

  public DragListener getResizeListener() { return _resizeListener; }

  class FrameResizeButton extends UIButton {

    public FrameResizeButton() {
      super("...");
      _pressedState = new MyPressedState();
      _defaultState = new MyDefaultState();
      _mouseOverState = new MyMouseOverState();
      switchState(_defaultState);
    }

    @Override
    protected void applySkin() {
      // keep this from happening by default
    }

    class MyPressedState extends UIButton.PressedState {
      @Override
      public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
        super.mouseDeparted(mouseX, mouseY, state);
        // TODO: Reset mouse cursor.
      }
    }

    class MyDefaultState extends UIButton.DefaultState {
      @Override
      public void mouseEntered(final int mouseX, final int mouseY, final InputState state) {
        super.mouseEntered(mouseX, mouseY, state);
        // TODO: Set mouse cursor to resize.
      }

      @Override
      public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
        super.mouseDeparted(mouseX, mouseY, state);
        // TODO: Reset mouse cursor.
      }
    }

    class MyMouseOverState extends UIButton.MouseOverState {
      @Override
      public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
        super.mouseDeparted(mouseX, mouseY, state);
        // TODO: Reset mouse cursor.
      }
    }
  }
}
