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

import java.io.IOException;
import java.io.InputStream;

public class MockInputStream extends InputStream {
  private int bytesAvailable = 0;
  private boolean eof = false;

  @Override
  public int read() throws IOException {
    while (true) {
      final int result = returnSomething();

      if (result != 0) {
        return result;
      }

      try {
        Thread.sleep(2);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private synchronized int returnSomething() {
    if (eof) {
      return -1;
    }

    if (bytesAvailable > 0) {
      bytesAvailable--;
      return 1;
    }

    return 0;
  }

  @Override
  public synchronized int available() throws IOException {
    return bytesAvailable;
  }

  public synchronized void addBytesAvailable(final int bytesAvailable) {
    this.bytesAvailable += bytesAvailable;
  }

  public synchronized void setEof(final boolean eof) { this.eof = eof; }
}
