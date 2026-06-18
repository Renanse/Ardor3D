/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestUIContainerContains {

  /**
   * contains(component, true) must descend into sub-containers to find a nested component. The bug
   * recursed on the search target instead of the child, so a recursive search never actually
   * descended (and could StackOverflow when the target was itself a populated container).
   */
  @Test
  public void testContainsRecursesIntoSubContainers() {
    final UIPanel root = new UIPanel();
    final UIPanel mid = new UIPanel();
    final UIPanel leaf = new UIPanel();
    mid.attachChild(leaf);
    root.attachChild(mid);

    // leaf is nested root -> mid -> leaf; a recursive search must find it
    assertTrue(root.contains(leaf, true));
    // a non-recursive search must NOT find a grandchild
    assertFalse(root.contains(leaf, false));
    // a component that isn't present must not be found
    assertFalse(root.contains(new UIPanel(), true));
  }
}
