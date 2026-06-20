/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export.binary;

import java.io.IOException;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Holder with two Savable reference slots and an int tag, used to test reference identity semantics:
 * shared references collapsing to one instance, distinct-but-equal references staying distinct, and
 * reference cycles surviving a round-trip.
 */
public class RefHolder implements Savable {

  public Savable left;
  public Savable right;
  public int tag;

  public RefHolder() {}

  public RefHolder(final int tag) {
    this.tag = tag;
  }

  @Override
  public Class<? extends RefHolder> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(left, "left", null);
    capsule.write(right, "right", null);
    capsule.write(tag, "tag", 0);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    left = capsule.readSavable("left", null);
    right = capsule.readSavable("right", null);
    tag = capsule.readInt("tag", 0);
  }
}
