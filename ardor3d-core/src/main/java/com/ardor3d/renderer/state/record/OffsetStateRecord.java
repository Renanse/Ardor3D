/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.state.record;

import java.util.EnumSet;

import com.ardor3d.renderer.state.OffsetState.OffsetType;

public class OffsetStateRecord extends StateRecord {
  public boolean enabled = false;
  public float factor = 0;
  public float units = 0;
  public EnumSet<OffsetType> enabledOffsets = EnumSet.noneOf(OffsetType.class);

  @Override
  public void invalidate() {
    super.invalidate();

    enabled = false;
    factor = 0;
    units = 0;
    enabledOffsets.clear();
  }
}
