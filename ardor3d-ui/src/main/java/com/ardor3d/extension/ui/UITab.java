/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.util.SubTex;

/**
 * An extension of UIButton to be used as the tabs in a tabbed pane. Always "Selectable" (aka.
 * toggle style)
 */
public class UITab extends UIButton {

  private final UITabbedPane.TabPlacement placement;

  /**
   * Construct a new UITabe with the given label text and icon.
   * 
   * @param label
   *          optional tab label
   * @param icon
   *          optional tab text
   * @param placement
   *          the edge or border on which this tab will be used.
   */
  public UITab(final String label, final SubTex icon, final UITabbedPane.TabPlacement placement) {
    super(label, icon);
    this.placement = placement;
    super.setSelectable(true);
    super.applySkin();
    switchState(getDefaultState());
  }

  @Override
  protected void applySkin() {
    // no-op
  }

  @Override
  public void setSelectable(final boolean selectable) {
    // no-op
  }

  @Override
  public boolean isSelectable() { return true; }

  /**
   * @return the placement
   */
  public UITabbedPane.TabPlacement getPlacement() { return placement; }
}
