/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.util;

import org.junit.Assert;
import org.junit.Test;

public class TestRegion {
  @Test
  public void testIntersects() throws Exception {
    final Region r1 = new Region(0, 0, 20, 20);
    final Region r2 = new Region(5, 5, 10, 10);

    Assert.assertTrue(r1.intersects(r2));
    Assert.assertTrue(r2.intersects(r1));

    Assert.assertEquals(new Region(5, 5, 10, 10), r2.intersection(r1));

    final Region r3 = new Region(0, 0, 20, 20);
    Assert.assertEquals(new Region(5, 5, 10, 10), r3.intersection(r2));
  }
}
