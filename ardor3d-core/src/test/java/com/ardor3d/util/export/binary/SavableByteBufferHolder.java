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
import java.nio.ByteBuffer;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Minimal test Savable that writes a ByteBuffer field followed by a plain int field, used to verify
 * that serializing a ByteBuffer does not desynchronise the fields written after it.
 */
public class SavableByteBufferHolder implements Savable {

  public ByteBuffer buffer;
  public int marker;

  public SavableByteBufferHolder() {}

  @Override
  public Class<? extends SavableByteBufferHolder> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(buffer, "buffer", null);
    capsule.write(marker, "marker", 0);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    buffer = capsule.readByteBuffer("buffer", null);
    marker = capsule.readInt("marker", 0);
  }
}
