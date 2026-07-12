/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.util.awt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.input.mouse.MouseCursor;

/**
 * The generated manipulation cursors are the right size/format for the native mouse managers (40px
 * RGBA, centered hotspot), and each accessor hands back one cached, distinct instance.
 */
public class CursorFactoryTest {

  @Test
  public void testCursorsAre40pxRgbaWithCenteredHotspot() {
    for (final MouseCursor cursor : new MouseCursor[] {CursorFactory.move(), CursorFactory.scale(),
        CursorFactory.rotate()}) {
      assertEquals(CursorFactory.SIZE, cursor.getWidth());
      assertEquals(CursorFactory.SIZE, cursor.getHeight());
      assertEquals("native mouse managers expect RGBA cursor data", ImageDataFormat.RGBA,
          cursor.getImage().getDataFormat());
      assertEquals(CursorFactory.SIZE / 2, cursor.getHotspotX());
      assertEquals(CursorFactory.SIZE / 2, cursor.getHotspotY());
    }
  }

  @Test
  public void testCursorsAreCachedAndDistinct() {
    // Same instance on repeat, so a downstream cursor cache stays keyed on a stable object.
    assertSame(CursorFactory.move(), CursorFactory.move());
    assertSame(CursorFactory.scale(), CursorFactory.scale());
    assertSame(CursorFactory.rotate(), CursorFactory.rotate());
    // ...but the three are different cursors.
    assertNotEquals(CursorFactory.move(), CursorFactory.scale());
    assertNotEquals(CursorFactory.move(), CursorFactory.rotate());
    assertNotEquals(CursorFactory.scale(), CursorFactory.rotate());
  }
}
