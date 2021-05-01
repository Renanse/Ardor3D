/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text;

import com.ardor3d.extension.ui.UIState;
import com.ardor3d.extension.ui.util.Alignment;

public class UITextArea extends AbstractUITextEntryComponent {

  protected UIKeyHandler _keyHandler;

  public UITextArea() {
    setAlignment(Alignment.TOP_LEFT);
    _disabledState = new UIState();
    _defaultState = new DefaultTextEntryState();
    _writingState = new TextEntryWritingState(this);
    setEditable(true);
    applySkin();
    switchState(getDefaultState());
  }

  @Override
  protected UIKeyHandler getKeyHandler() {
    if (_keyHandler == null) {
      _keyHandler = new DefaultLatinTextEntryKeyHandler(this);
    }
    return _keyHandler;
  }

  public void setKeyHandler(final UIKeyHandler handler) { _keyHandler = handler; }
}
