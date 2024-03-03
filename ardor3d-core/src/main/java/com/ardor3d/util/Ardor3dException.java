/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

import java.io.Serial;

public class Ardor3dException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public Ardor3dException() {
    super();
  }

  public Ardor3dException(final String desc) {
    super(desc);
  }

  public Ardor3dException(final Throwable cause) {
    super(cause);
  }

  public Ardor3dException(final String desc, final Throwable cause) {
    super(desc, cause);
  }
}
