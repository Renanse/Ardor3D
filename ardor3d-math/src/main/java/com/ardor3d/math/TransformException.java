/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math;

public class TransformException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TransformException() {}

  public TransformException(final String message) {
    super(message);
  }

}
