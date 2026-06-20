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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * {@link BinaryCloner} deep-copies through an in-memory export/import. These tests pin its contract:
 * the copy is a fully independent object graph (mutating it never touches the source) yet preserves
 * internal sharing (one instance referenced twice stays shared in the copy).
 */
public class TestBinaryClonerParity {

  @Test
  public void testCopyIsIndependentButContentEqual() {
    final RefHolder src = new RefHolder(5);
    src.left = new SavableLeaf(1, "a");
    src.right = new SavableLeaf(2, "b");

    final RefHolder copy = new BinaryCloner().copy(src);

    assertNotSame(src, copy);
    assertNotSame(src.left, copy.left);
    assertEquals(5, copy.tag);
    assertEquals(src.left, copy.left);
    assertEquals(src.right, copy.right);

    // mutating the copy must not bleed back into the source
    ((SavableLeaf) copy.left).id = 999;
    copy.tag = -1;
    assertEquals(1, ((SavableLeaf) src.left).id);
    assertEquals(5, src.tag);
  }

  @Test
  public void testCopyPreservesInternalSharing() {
    final RefHolder src = new RefHolder(0);
    final SavableLeaf shared = new SavableLeaf(8, "shared");
    src.left = shared;
    src.right = shared;

    final RefHolder copy = new BinaryCloner().copy(src);

    assertNotSame(shared, copy.left);
    assertSame("shared sub-graph must remain shared in the clone", copy.left, copy.right);
  }

  @Test
  public void testCopyPreservesCycles() {
    final RefHolder a = new RefHolder(1);
    final RefHolder b = new RefHolder(2);
    a.left = b;
    b.left = a;

    final RefHolder copy = new BinaryCloner().copy(a);

    assertNotSame(a, copy);
    assertSame("clone of a cyclic graph must be cyclic too", copy, ((RefHolder) copy.left).left);
  }
}
