/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text;

public enum CopyPasteManager {
  INSTANCE;

  private CopyPasteImpl _impl;

  public void setCopyPasteImpl(final CopyPasteImpl impl) { _impl = impl; }

  public String getClipBoardContents() {
    if (_impl == null) {
      _impl = new AwtCopyPasteImpl();
    }
    return _impl.getClipBoardContents();
  }

  public void setClipBoardContents(final String contents) {
    if (_impl == null) {
      _impl = new AwtCopyPasteImpl();
    }
    _impl.setClipBoardContents(contents);
  }
}
