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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * The format keys saved objects by identity and caches restored objects by id, so a graph that
 * references the same instance twice must restore as a single shared instance, a cycle must restore
 * as a cycle, and two distinct-but-equal instances must stay distinct. These invariants are what let
 * the binary format double as a structure-preserving deep-copy.
 */
public class TestBinarySharedReference {

  /** The same instance referenced from two slots must restore as one shared instance. */
  @Test
  public void testSharedReferenceCollapsesToOneInstance() throws Exception {
    final RefHolder root = new RefHolder(1);
    final SavableLeaf shared = new SavableLeaf(7, "shared");
    root.left = shared;
    root.right = shared;

    final RefHolder r = (RefHolder) TestBinaryRoundTrip.roundTrip(root);

    assertNotNull(r.left);
    assertSame("a single instance referenced twice must stay a single instance", r.left, r.right);
    assertEquals(shared, r.left);
  }

  /** Two distinct instances with equal content must remain two distinct instances. */
  @Test
  public void testDistinctButEqualReferencesStayDistinct() throws Exception {
    final RefHolder root = new RefHolder(2);
    root.left = new SavableLeaf(7, "same");
    root.right = new SavableLeaf(7, "same");

    final RefHolder r = (RefHolder) TestBinaryRoundTrip.roundTrip(root);

    assertEquals(r.left, r.right); // equal by content
    assertNotSame("distinct instances must not be merged on load", r.left, r.right);
  }

  /** A reference cycle (a<->b) must round-trip back into a cycle, not infinite-recurse or break. */
  @Test
  public void testReferenceCycleSurvives() throws Exception {
    final RefHolder a = new RefHolder(10);
    final RefHolder b = new RefHolder(20);
    a.left = b;
    b.left = a;

    final RefHolder ra = (RefHolder) TestBinaryRoundTrip.roundTrip(a);

    assertEquals(10, ra.tag);
    final RefHolder rb = (RefHolder) ra.left;
    assertNotNull(rb);
    assertEquals(20, rb.tag);
    assertSame("the cycle must close back on the original instance", ra, rb.left);
  }

  /** Null reference slots round-trip as null. */
  @Test
  public void testNullReferencesRoundTrip() throws Exception {
    final RefHolder root = new RefHolder(3);
    root.left = new SavableLeaf(1, "only-left");
    root.right = null;

    final RefHolder r = (RefHolder) TestBinaryRoundTrip.roundTrip(root);

    assertNotNull(r.left);
    assertNull(r.right);
  }
}
