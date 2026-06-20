/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

/**
 * SimpleResourceLocator and MultiFormatResourceLocator override equals but inherited Object's
 * identity hashCode, so two equal locators got different hash codes - broken as HashSet/HashMap keys.
 * The hashCode is keyed on the base directory, shared down the hierarchy: because
 * SimpleResourceLocator.equals treats any SimpleResourceLocator (including a MultiFormatResourceLocator)
 * with the same base dir as equal, a base-dir-only hash is what keeps the contract consistent across
 * both types.
 */
public class TestResourceLocatorHashCode {

  @Test
  public void testEqualSimpleLocatorsShareHashCode() throws Exception {
    final SimpleResourceLocator a = new SimpleResourceLocator(new URI("file:/assets/models/"));
    final SimpleResourceLocator b = new SimpleResourceLocator(new URI("file:/assets/models/"));
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void testEqualMultiLocatorsShareHashCode() throws Exception {
    final MultiFormatResourceLocator a = new MultiFormatResourceLocator(new URI("file:/assets/"), "png", "jpg");
    final MultiFormatResourceLocator b = new MultiFormatResourceLocator(new URI("file:/assets/"), "png", "jpg");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void testSimpleEqualToMultiSharesHashCode() throws Exception {
    // Simple.equals treats a Multi with the same base dir as equal, so the hashCode contract
    // requires them to hash the same.
    final SimpleResourceLocator simple = new SimpleResourceLocator(new URI("file:/assets/"));
    final MultiFormatResourceLocator multi = new MultiFormatResourceLocator(new URI("file:/assets/"), "png");
    assertTrue(simple.equals(multi));
    assertEquals(simple.hashCode(), multi.hashCode());
  }
}
