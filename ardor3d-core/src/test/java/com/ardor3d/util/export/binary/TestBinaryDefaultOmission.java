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
import static org.junit.Assert.assertNull;

import java.util.BitSet;

import org.junit.Test;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.util.export.binary.AllTypesHolder.Flavor;

/**
 * The binary format omits any field equal to its default and walks the per-object field stream by a
 * leading alias byte. A field that emits the wrong number of bytes (e.g. latent bug #3) would shift
 * every following alias and silently corrupt subsequent fields. These tests scatter present and
 * omitted fields of differing widths - including a variable-length buffer in the middle - to prove
 * the alias walk stays synchronised regardless of which fields were written.
 */
public class TestBinaryDefaultOmission {

  @Test
  public void testScatteredPresentAndOmittedFieldsStayInSync() throws Exception {
    final AllTypesHolder h = new AllTypesHolder();
    // populate a non-contiguous subset spread across the whole field order; leave the rest default.
    h.b = (byte) 42;
    h.d = 9.99;
    h.en = Flavor.BETA;
    h.iArr = new int[] {7, 8, 9};
    h.floatBuf = BufferUtils.createFloatBuffer(1f, 2f, 3f, 4f, 5f); // variable-length field mid-stream
    h.bits = new BitSet();
    h.bits.set(3);
    h.bits.set(70);

    final AllTypesHolder r = (AllTypesHolder) TestBinaryRoundTrip.roundTrip(h);

    // the populated fields survive, in spite of the omitted neighbours between them
    assertEquals((byte) 42, r.b);
    assertEquals(9.99, r.d, 0.0);
    assertEquals(Flavor.BETA, r.en);
    assertArrayEquals(new int[] {7, 8, 9}, r.iArr);
    final float[] fb = new float[r.floatBuf.remaining()];
    r.floatBuf.duplicate().get(fb);
    assertArrayEquals(new float[] {1f, 2f, 3f, 4f, 5f}, fb, 0f);
    assertEquals(h.bits, r.bits);

    // the omitted fields come back as their read-side defaults, not as garbage shifted in from a
    // neighbouring field
    assertEquals(false, r.z);
    assertEquals(0, r.s);
    assertEquals(0, r.i);
    assertEquals(0L, r.l);
    assertEquals(0f, r.f, 0f);
    assertNull(r.str);
    assertNull(r.lArr);
    assertNull(r.strArr2);
    assertNull(r.intBuf);
  }

  /**
   * A value that happens to equal its declared default is intentionally not written, and must come
   * back as that default - the omission is invisible to the caller.
   */
  @Test
  public void testValueEqualToDefaultRoundTripsAsDefault() throws Exception {
    final AllTypesHolder h = new AllTypesHolder();
    h.i = 0; // equals the default passed to write(...,0) -> omitted
    h.str = null; // equals default -> omitted
    h.f = 5f; // not the default -> written

    final AllTypesHolder r = (AllTypesHolder) TestBinaryRoundTrip.roundTrip(h);

    assertEquals(0, r.i);
    assertNull(r.str);
    assertEquals(5f, r.f, 0f);
  }
}
