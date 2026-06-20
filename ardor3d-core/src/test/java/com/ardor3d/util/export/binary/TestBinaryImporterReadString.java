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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * BinaryImporter.readString(InputStream, length) - used to parse class and field names from the
 * header - filled its buffer with a single read() call and ignored the count. A stream returning
 * fewer bytes than requested (legal for the BufferedInputStream/GZIPInputStream it actually reads
 * through) left the name truncated with trailing NULs. This feeds one byte per read to make the
 * short read deterministic.
 */
public class TestBinaryImporterReadString {

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
  public void testReadStringSurvivesShortReads() throws Exception {
    final String value = "com.ardor3d.scenegraph.Node"; // representative of a header class name
    final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

    final String result = new BinaryImporter().readString(new OneBytePerReadStream(bytes), bytes.length);

    assertEquals(value, result);
  }

  /**
   * A corrupt/tampered header can decode a negative length. That must surface as an IOException (the
   * importer's declared corrupt-file channel), not an unchecked IllegalArgumentException out of
   * readNBytes that bypasses callers' catch (IOException) handling.
   */
  @Test(expected = IOException.class)
  public void testNegativeLengthThrowsIOException() throws Exception {
    new BinaryImporter().readString(new ByteArrayInputStream(new byte[] {1, 2, 3}), -1);
  }
}
