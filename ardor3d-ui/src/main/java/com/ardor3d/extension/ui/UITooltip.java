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

/**
 * Defines a component used by the hud to display floating tool tips.
 */
public class UITooltip extends UIContainer {

  private final AbstractLabelUIComponent _label;

  /**
   * Construct a new UITooltip.
   */
  public UITooltip() {
    // setup our text label
    _label = new UILabel("");
    add(_label);

    // initially this is not visible
    setVisible(false);
  }

  /**
   * @return the label used to display tips.
   */
  public AbstractLabelUIComponent getLabel() { return _label; }

  @Override
  public UIComponent getUIComponent(final int hudX, final int hudY) {
    // We don't want the tool tip to be "pickable", so always return null.
    return null;
  }
}
