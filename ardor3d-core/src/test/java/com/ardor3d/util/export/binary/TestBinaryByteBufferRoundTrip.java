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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import org.junit.Test;

public class TestBinaryByteBufferRoundTrip {

  /**
   * A heap ByteBuffer whose limit is below its capacity must serialize exactly limit() bytes. The
   * buggy code wrote the whole backing array while recording length=limit(), leaving stray bytes
   * that desynchronised every following field - here, the int written after the buffer.
   */
  @Test
  public void testHeapByteBufferDoesNotCorruptFollowingField() throws Exception {
    final SavableByteBufferHolder holder = new SavableByteBufferHolder();
    final ByteBuffer bb = ByteBuffer.allocate(16);
    bb.put(new byte[] {10, 20, 30, 40});
    bb.flip(); // position=0, limit=4, capacity=16

    holder.buffer = bb;
    holder.marker = 42;

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    new BinaryExporter().save(holder, out);
    final SavableByteBufferHolder restored =
        (SavableByteBufferHolder) new BinaryImporter().load(new ByteArrayInputStream(out.toByteArray()));

    // The field written AFTER the buffer must survive intact.
    assertEquals(42, restored.marker);

    // ...and the buffer content itself must be correct.
    assertNotNull(restored.buffer);
    assertEquals(4, restored.buffer.remaining());
    final byte[] restoredBytes = new byte[restored.buffer.remaining()];
    restored.buffer.get(restoredBytes);
    assertArrayEquals(new byte[] {10, 20, 30, 40}, restoredBytes);
  }

  /**
   * A sliced heap ByteBuffer has arrayOffset() > 0. Serialization must copy length bytes starting at
   * arrayOffset (not index 0), so both the slice's content and the field written after it survive.
   */
  @Test
  public void testSlicedHeapByteBufferRoundTrips() throws Exception {
    final ByteBuffer backing = ByteBuffer.allocate(16);
    backing.put(new byte[] {99, 99}); // junk preceding the slice -> slice.arrayOffset() == 2
    backing.position(2);
    final ByteBuffer slice = backing.slice();
    slice.put(new byte[] {10, 20, 30, 40});
    slice.flip(); // position 0, limit 4, arrayOffset 2

    final SavableByteBufferHolder holder = new SavableByteBufferHolder();
    holder.buffer = slice;
    holder.marker = 7;

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    new BinaryExporter().save(holder, out);
    final SavableByteBufferHolder restored =
        (SavableByteBufferHolder) new BinaryImporter().load(new ByteArrayInputStream(out.toByteArray()));

    assertEquals(7, restored.marker);
    assertNotNull(restored.buffer);
    assertEquals(4, restored.buffer.remaining());
    final byte[] restoredBytes = new byte[restored.buffer.remaining()];
    restored.buffer.get(restoredBytes);
    assertArrayEquals(new byte[] {10, 20, 30, 40}, restoredBytes);
  }
}
