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
import java.util.Objects;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Tiny value-bearing Savable used as the element type for collection / map / shared-reference tests.
 * Carries content equality so restored graphs can be compared by value, while identity comparisons
 * remain meaningful for shared-reference tests.
 */
public class SavableLeaf implements Savable {

  public int id;
  public String label;

  public SavableLeaf() {}

  public SavableLeaf(final int id, final String label) {
    this.id = id;
    this.label = label;
  }

  @Override
  public Class<? extends SavableLeaf> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(id, "id", 0);
    capsule.write(label, "label", null);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    id = capsule.readInt("id", 0);
    label = capsule.readString("label", null);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SavableLeaf other)) {
      return false;
    }
    return id == other.id && Objects.equals(label, other.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, label);
  }

  @Override
  public String toString() {
    return "SavableLeaf[" + id + "," + label + "]";
  }
}
