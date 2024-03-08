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

import java.util.Set;

/**
 * Basic implementation of {@link AbstractLabelUIComponent}.
 */
public class UILabel extends AbstractLabelUIComponent {

  private final LabelState _disabledState = new LabelState();
  private final LabelState _defaultState = new LabelState();

  /**
   * @param text
   *          the text value of this label.
   */
  public UILabel(final String text) {
    this(text, null);
  }

  /**
   * 
   * @param text
   *          the text value of this label.
   * @param icon
   *          the icon value of this label.
   */
  public UILabel(final String text, final SubTex icon) {
    setText(text);
    setIcon(icon);

    applySkin();
    switchState(getDefaultState());
  }

  @Override
  public LabelState getDefaultState() { return _defaultState; }

  @Override
  public LabelState getDisabledState() { return _disabledState; }

  @Override
  public Set<UIState> getStates() { return Set.of(_defaultState, _disabledState); }
}
