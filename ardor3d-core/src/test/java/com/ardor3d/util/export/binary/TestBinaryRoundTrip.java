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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.BitSet;

import org.junit.Test;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.export.binary.AllTypesHolder.Flavor;

/**
 * Round-trips every leaf type the capsule API supports (scalars, 1D/2D arrays, all four nio buffer
 * kinds, BitSet, Enum and String) through {@link BinaryExporter}/{@link BinaryImporter} and asserts
 * each value survives intact. This is the GL-independent persistence net the codebase lacked; it
 * exercises exactly the machinery that latent serialization bugs (heap-buffer length desync, wrong
 * capsule keys) corrupted.
 */
public class TestBinaryRoundTrip {

  static Savable roundTrip(final Savable in) throws Exception {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    new BinaryExporter().save(in, out);
    return new BinaryImporter().load(new ByteArrayInputStream(out.toByteArray()));
  }

  private static float[] toArray(final FloatBuffer b) {
    final FloatBuffer dup = b.duplicate();
    dup.rewind();
    final float[] a = new float[dup.remaining()];
    dup.get(a);
    return a;
  }

  private static int[] toArray(final IntBuffer b) {
    final IntBuffer dup = b.duplicate();
    dup.rewind();
    final int[] a = new int[dup.remaining()];
    dup.get(a);
    return a;
  }

  private static short[] toArray(final ShortBuffer b) {
    final ShortBuffer dup = b.duplicate();
    dup.rewind();
    final short[] a = new short[dup.remaining()];
    dup.get(a);
    return a;
  }

  private static byte[] toArray(final ByteBuffer b) {
    final ByteBuffer dup = b.duplicate();
    dup.rewind();
    final byte[] a = new byte[dup.remaining()];
    dup.get(a);
    return a;
  }

  @Test
  public void testEveryLeafTypeRoundTrips() throws Exception {
    final AllTypesHolder h = new AllTypesHolder();
    h.z = true;
    h.b = (byte) -7;
    h.s = (short) 1234;
    h.i = 0x0BADBEEF;
    h.l = 0x0123456789ABCDEFL;
    h.f = 3.14159f;
    h.d = -2.718281828459045;
    h.str = "héllo·wörld"; // multi-byte UTF-8 to catch byte-length bugs
    h.en = Flavor.GAMMA;

    h.zArr = new boolean[] {true, false, true};
    h.bArr = new byte[] {1, -2, 3, -128, 127};
    h.sArr = new short[] {-32768, 0, 32767};
    h.iArr = new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};
    h.lArr = new long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE};
    h.fArr = new float[] {Float.NEGATIVE_INFINITY, -0.5f, 0f, 0.5f, Float.POSITIVE_INFINITY};
    h.dArr = new double[] {Double.MIN_VALUE, 0.0, Double.MAX_VALUE};
    h.strArr = new String[] {"a", "", "déjà"};
    h.enArr = new Flavor[] {Flavor.BETA, Flavor.ALPHA};

    h.zArr2 = new boolean[][] {{true}, {false, true}};
    h.bArr2 = new byte[][] {{1, 2}, {3}};
    h.sArr2 = new short[][] {{10, 20}, {30}};
    h.iArr2 = new int[][] {{1, 2, 3}, {4, 5}};
    h.lArr2 = new long[][] {{1L}, {2L, 3L}};
    h.fArr2 = new float[][] {{1.5f, 2.5f}, {3.5f}};
    h.dArr2 = new double[][] {{1.25}, {2.25, 3.25}};
    h.strArr2 = new String[][] {{"x", "y"}, {"z"}};

    h.byteBuf = (ByteBuffer) BufferUtils.createByteBuffer(3).put(new byte[] {9, 8, 7}).flip();
    h.floatBuf = BufferUtils.createFloatBuffer(1.1f, 2.2f, 3.3f, 4.4f);
    h.intBuf = BufferUtils.createIntBuffer(100, 200, 300);
    h.shortBuf = BufferUtils.createShortBuffer(new short[] {5, 6, 7, 8});

    h.bits = new BitSet();
    h.bits.set(1);
    h.bits.set(64);
    h.bits.set(200);

    final AllTypesHolder r = (AllTypesHolder) roundTrip(h);

    assertEquals(h.z, r.z);
    assertEquals(h.b, r.b);
    assertEquals(h.s, r.s);
    assertEquals(h.i, r.i);
    assertEquals(h.l, r.l);
    assertEquals(h.f, r.f, 0f);
    assertEquals(h.d, r.d, 0.0);
    assertEquals(h.str, r.str);
    assertEquals(h.en, r.en);

    assertArrayEquals(h.zArr, r.zArr);
    assertArrayEquals(h.bArr, r.bArr);
    assertArrayEquals(h.sArr, r.sArr);
    assertArrayEquals(h.iArr, r.iArr);
    assertArrayEquals(h.lArr, r.lArr);
    assertArrayEquals(h.fArr, r.fArr, 0f);
    assertArrayEquals(h.dArr, r.dArr, 0.0);
    assertArrayEquals(h.strArr, r.strArr);
    assertArrayEquals(h.enArr, r.enArr);

    assertArrayEquals(h.zArr2, r.zArr2);
    assertArrayEquals(h.bArr2, r.bArr2);
    assertArrayEquals(h.sArr2, r.sArr2);
    assertArrayEquals(h.iArr2, r.iArr2);
    assertArrayEquals(h.lArr2, r.lArr2);
    for (int row = 0; row < h.fArr2.length; row++) {
      assertArrayEquals(h.fArr2[row], r.fArr2[row], 0f);
    }
    for (int row = 0; row < h.dArr2.length; row++) {
      assertArrayEquals(h.dArr2[row], r.dArr2[row], 0.0);
    }
    assertArrayEquals(h.strArr2, r.strArr2);

    assertArrayEquals(new byte[] {9, 8, 7}, toArray(r.byteBuf));
    assertArrayEquals(new float[] {1.1f, 2.2f, 3.3f, 4.4f}, toArray(r.floatBuf), 0f);
    assertArrayEquals(new int[] {100, 200, 300}, toArray(r.intBuf));
    assertArrayEquals(new short[] {5, 6, 7, 8}, toArray(r.shortBuf));

    assertEquals(h.bits, r.bits);
  }

  /**
   * A freshly-constructed holder writes nothing but defaults; everything must come back null/default
   * rather than as empty-but-non-null artifacts.
   */
  @Test
  public void testAllDefaultsRoundTripAsNull() throws Exception {
    final AllTypesHolder r = (AllTypesHolder) roundTrip(new AllTypesHolder());

    assertEquals(false, r.z);
    assertEquals(0, r.i);
    assertEquals(0.0, r.d, 0.0);
    assertNull(r.str);
    assertNull(r.en);
    assertNull(r.iArr);
    assertNull(r.strArr2);
    assertNull(r.floatBuf);
    assertNull(r.bits);
  }

  /**
   * Zero-length arrays and buffers are a distinct case from null: an empty array must restore as a
   * non-null length-0 array, not collapse to null.
   */
  @Test
  public void testEmptyArraysAndBuffersRoundTrip() throws Exception {
    final AllTypesHolder h = new AllTypesHolder();
    h.iArr = new int[0];
    h.strArr = new String[0];
    h.fArr2 = new float[0][];
    h.floatBuf = BufferUtils.createFloatBuffer(0);
    h.byteBuf = BufferUtils.createByteBuffer(0);
    h.bits = new BitSet(); // empty bitset

    final AllTypesHolder r = (AllTypesHolder) roundTrip(h);

    assertNotNull(r.iArr);
    assertEquals(0, r.iArr.length);
    assertNotNull(r.strArr);
    assertEquals(0, r.strArr.length);
    assertNotNull(r.fArr2);
    assertEquals(0, r.fArr2.length);
    assertNotNull(r.floatBuf);
    assertEquals(0, r.floatBuf.remaining());
    assertNotNull(r.byteBuf);
    assertEquals(0, r.byteBuf.remaining());
    assertNotNull(r.bits);
    assertTrue(r.bits.isEmpty());
  }

  /**
   * String arrays with embedded nulls round-trip those null slots (the per-element length is encoded
   * specially, so a null element must not be confused with an empty string).
   */
  @Test
  public void testStringArrayWithNullElement() throws Exception {
    final AllTypesHolder h = new AllTypesHolder();
    h.strArr = new String[] {"first", null, "", "last"};

    final AllTypesHolder r = (AllTypesHolder) roundTrip(h);

    assertArrayEquals(new String[] {"first", null, "", "last"}, r.strArr);
  }
}
