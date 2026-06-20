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
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ardor3d.buffer.BufferUtils;

/**
 * Round-trips the Savable-graph collection types - object arrays, lists, list-arrays, buffer lists
 * and the three map flavours. Together with {@link TestBinaryRoundTrip} this covers the full capsule
 * write/read surface.
 */
public class TestBinaryCollectionsRoundTrip {

  private static float[] toArray(final FloatBuffer b) {
    final FloatBuffer dup = b.duplicate();
    dup.rewind();
    final float[] a = new float[dup.remaining()];
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

  @SuppressWarnings("unchecked")
  @Test
  public void testSavableCollectionsRoundTrip() throws Exception {
    final CollectionsHolder h = new CollectionsHolder();

    h.savArr = new SavableLeaf[] {new SavableLeaf(1, "one"), new SavableLeaf(2, "two")};
    h.savArr2 = new SavableLeaf[][] {{new SavableLeaf(3, "three")}, {new SavableLeaf(4, "four"), new SavableLeaf(5, "five")}};
    h.savList = Arrays.asList(new SavableLeaf(10, "ten"), new SavableLeaf(11, "eleven"));
    h.savListArr = new List[] {Arrays.asList(new SavableLeaf(20, "twenty")),
        Arrays.asList(new SavableLeaf(21, "t21"), new SavableLeaf(22, "tt"))};
    h.floatBufList =
        Arrays.asList(BufferUtils.createFloatBuffer(1f, 2f), BufferUtils.createFloatBuffer(3f, 4f, 5f));
    h.byteBufList = Arrays.asList((ByteBuffer) BufferUtils.createByteBuffer(2).put(new byte[] {7, 8}).flip(),
        (ByteBuffer) BufferUtils.createByteBuffer(1).put(new byte[] {9}).flip());

    h.savMap = new LinkedHashMap<>();
    h.savMap.put(new SavableLeaf(100, "k1"), new SavableLeaf(200, "v1"));
    h.savMap.put(new SavableLeaf(101, "k2"), new SavableLeaf(201, "v2"));

    h.strSavMap = new LinkedHashMap<>();
    h.strSavMap.put("alpha", new SavableLeaf(300, "a"));
    h.strSavMap.put("beta", new SavableLeaf(301, "b"));

    h.strObjMap = new LinkedHashMap<>();
    h.strObjMap.put("anInt", Integer.valueOf(7));
    h.strObjMap.put("aStr", "hello");
    h.strObjMap.put("aFloatArr", new float[] {1.5f, 2.5f});
    h.strObjMap.put("aSavable", new SavableLeaf(42, "deep"));

    final CollectionsHolder r = (CollectionsHolder) TestBinaryRoundTrip.roundTrip(h);

    // object arrays - come back as Savable[] with SavableLeaf elements
    assertArrayEquals(h.savArr, r.savArr);
    assertEquals(2, r.savArr2.length);
    assertArrayEquals(h.savArr2[0], r.savArr2[0]);
    assertArrayEquals(h.savArr2[1], r.savArr2[1]);

    // lists
    assertEquals(h.savList, r.savList);
    assertEquals(2, r.savListArr.length);
    assertEquals(h.savListArr[0], r.savListArr[0]);
    assertEquals(h.savListArr[1], r.savListArr[1]);

    // buffer lists
    assertEquals(2, r.floatBufList.size());
    assertArrayEquals(new float[] {1f, 2f}, toArray(r.floatBufList.get(0)), 0f);
    assertArrayEquals(new float[] {3f, 4f, 5f}, toArray(r.floatBufList.get(1)), 0f);
    assertEquals(2, r.byteBufList.size());
    assertArrayEquals(new byte[] {7, 8}, toArray(r.byteBufList.get(0)));
    assertArrayEquals(new byte[] {9}, toArray(r.byteBufList.get(1)));

    // maps
    assertEquals(h.savMap, r.savMap);
    assertEquals(h.strSavMap, r.strSavMap);

    // string->object map: assert each typed value individually
    assertEquals(Integer.valueOf(7), r.strObjMap.get("anInt"));
    assertEquals("hello", r.strObjMap.get("aStr"));
    assertArrayEquals(new float[] {1.5f, 2.5f}, (float[]) r.strObjMap.get("aFloatArr"), 0f);
    assertEquals(new SavableLeaf(42, "deep"), r.strObjMap.get("aSavable"));
  }

  /**
   * An empty Savable list must survive as an empty (non-null) list, distinct from an unset/null one.
   */
  @Test
  public void testEmptyAndNullCollections() throws Exception {
    final CollectionsHolder h = new CollectionsHolder();
    h.savList = List.of();
    h.savArr = new SavableLeaf[0];

    final CollectionsHolder r = (CollectionsHolder) TestBinaryRoundTrip.roundTrip(h);

    assertTrue(r.savList != null && r.savList.isEmpty());
    assertEquals(0, r.savArr.length);
    // unset map fields stay null
    assertEquals(null, r.savMap);
    assertEquals(null, r.strObjMap);
  }
}
