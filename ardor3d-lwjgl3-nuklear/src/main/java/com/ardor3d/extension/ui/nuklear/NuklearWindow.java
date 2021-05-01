/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.nuklear;

import org.lwjgl.nuklear.NkContext;

public abstract class NuklearWindow {

  protected int _xOffset;
  protected int _yOffset;

  public abstract void layout(NkContext ctx);

  public int getXOffset() { return _xOffset; }

  public void setXOffset(final int x) { _xOffset = x; }

  public int getYOffset() { return _yOffset; }

  public void setYOffset(final int y) { _yOffset = y; }

  public void setOffSet(final int x, final int y) {
    _xOffset = x;
    _yOffset = y;
  }

}
