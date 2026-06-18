/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.buffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.FloatBuffer;

import org.junit.Test;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;

public class TestBufferUtilsOffset {

  /**
   * The offset variant must emit {@code length} tuples starting at {@code offset}. The buggy loop
   * bound ({@code x < length} instead of {@code x < offset + length}) emitted too few when offset
   * was non-zero.
   */
  @Test
  public void testCreateFloatBufferVector3WithOffset() {
    final Vector3 v0 = new Vector3(0, 0, 0);
    final Vector3 v1 = new Vector3(1, 1, 1);
    final Vector3 v2 = new Vector3(2, 2, 2);

    final FloatBuffer buf = BufferUtils.createFloatBuffer(1, 2, v0, v1, v2);

    assertEquals(6, buf.remaining());
    final float[] out = new float[buf.remaining()];
    buf.get(out);
    assertArrayEquals(new float[] {1, 1, 1, 2, 2, 2}, out, 0f);
  }

  /**
   * A null Vector4 entry must contribute four zero floats (not three), otherwise the buffer is
   * desynchronised for every following entry.
   */
  @Test
  public void testCreateFloatBufferVector4NullEntryWritesFourFloats() {
    final Vector4 v = new Vector4(1, 2, 3, 4);

    final FloatBuffer buf = BufferUtils.createFloatBuffer(0, 2, v, null);

    assertEquals(8, buf.remaining());
    final float[] out = new float[buf.remaining()];
    buf.get(out);
    assertArrayEquals(new float[] {1, 2, 3, 4, 0, 0, 0, 0}, out, 0f);
  }
}
