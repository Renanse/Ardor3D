/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.intersection;

public class PrimitiveKey {
  private final int _primitiveIndex;
  private final int _section;

  public PrimitiveKey(final int primitiveIndex, final int section) {
    _primitiveIndex = primitiveIndex;
    _section = section;
  }

  /**
   * @return the primitiveIndex
   */
  public int getPrimitiveIndex() { return _primitiveIndex; }

  /**
   * @return the section
   */
  public int getSection() { return _section; }

  @Override
  public int hashCode() {
    int result = 17;

    result = 31 * result + _primitiveIndex;
    result = 31 * result + _section;

    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PrimitiveKey)) {
      return false;
    }
    final PrimitiveKey comp = (PrimitiveKey) o;
    return _primitiveIndex == comp._primitiveIndex && _section == comp._section;
  }

}
