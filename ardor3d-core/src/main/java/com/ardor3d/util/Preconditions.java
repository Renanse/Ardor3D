/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

public final class Preconditions {
  private Preconditions() {}

  public static <T> T checkNotNull(T ref, Object errorMessage) {
    if (ref == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return ref;
  }
}
