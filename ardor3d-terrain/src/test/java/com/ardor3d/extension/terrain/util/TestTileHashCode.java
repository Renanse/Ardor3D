/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Tile.hashCode used `result += 31 * result + x` (a typo of the standard `result = 31 * result + x`),
 * which algebraically collapses to `32*x + y + const`. That makes tiles offset by (+1, -32) hash
 * identically - e.g. (1,0) and (0,32) - bucket-colliding in the terrain cache maps that key on Tile.
 */
public class TestTileHashCode {

  @Test
  public void testEqualTilesShareHashCode() {
    final Tile a = new Tile(3, 4);
    final Tile b = new Tile(3, 4);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void testOffsetTilesDoNotCollide() {
    // (1,0) and (0,32) both hashed to the same value under the += accumulation bug.
    assertNotEquals(new Tile(1, 0).hashCode(), new Tile(0, 32).hashCode());
  }
}
