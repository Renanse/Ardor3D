/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.collada.jdom;

import java.io.Serial;

/**
 * Customer exception thrown when something unexpected is encountered in a Collada file.
 */
public class ColladaException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public ColladaException(final String message, final Object source) {
    super(ColladaException.createMessage(message, source));
  }

  public ColladaException(final String msg, final Object source, final Throwable cause) {
    super(ColladaException.createMessage(msg, source), cause);
  }

  private static String createMessage(final String message, final Object source) {
    return "Collada problem for source: " + (source != null ? source.toString() : "null") + ": " + message;
  }
}
