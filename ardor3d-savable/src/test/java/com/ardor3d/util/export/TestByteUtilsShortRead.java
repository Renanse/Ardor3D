/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.EOFException;

import org.junit.Test;

/**
 * The ByteUtils.read* helpers fill a fixed-size array with a single InputStream.read() call and
 * ignore its return count. A stream that hands back fewer bytes than requested - exactly what
 * BufferedInputStream/GZIPInputStream are permitted to do at buffer boundaries - then leaves the
 * trailing bytes zeroed, silently corrupting the decoded value. These tests feed a stream that
 * returns one byte per call so the short read is deterministic.
 */
public class TestByteUtilsShortRead {

  /** An InputStream that hands back at most one byte per read(byte[], int, int) call. */
  private static final class OneBytePerReadStream extends ByteArrayInputStream {
    OneBytePerReadStream(final byte[] buf) {
      super(buf);
    }

    @Override
    public synchronized int read(final byte[] b, final int off, final int len) {
      return super.read(b, off, Math.min(len, 1));
    }
  }

  @Test
  public void testReadShortSurvivesShortReads() throws Exception {
    final short value = (short) 0x1234;
    assertEquals(value, ByteUtils.readShort(new OneBytePerReadStream(ByteUtils.convertToBytes(value))));
  }

  @Test
  public void testReadIntSurvivesShortReads() throws Exception {
    final int value = 0x01020304;
    assertEquals(value, ByteUtils.readInt(new OneBytePerReadStream(ByteUtils.convertToBytes(value))));
  }

  @Test
  public void testReadLongSurvivesShortReads() throws Exception {
    final long value = 0x0102030405060708L;
    assertEquals(value, ByteUtils.readLong(new OneBytePerReadStream(ByteUtils.convertToBytes(value))));
  }

  @Test
  public void testReadFloatSurvivesShortReads() throws Exception {
    final float value = 3.14159f;
    assertEquals(value, ByteUtils.readFloat(new OneBytePerReadStream(ByteUtils.convertToBytes(value))), 0f);
  }

  @Test
  public void testReadDoubleSurvivesShortReads() throws Exception {
    final double value = -2.718281828459045;
    assertEquals(value, ByteUtils.readDouble(new OneBytePerReadStream(ByteUtils.convertToBytes(value))), 0.0);
  }

  /** A genuinely truncated stream must fail loudly rather than return a half-read value. */
  @Test(expected = EOFException.class)
  public void testReadIntOnTruncatedStreamThrows() throws Exception {
    ByteUtils.readInt(new ByteArrayInputStream(new byte[] {1, 2})); // only 2 of 4 bytes
  }
}
