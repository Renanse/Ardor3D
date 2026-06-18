/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ardor3d.scenegraph.hint.PropertyMode;

public class TestSpatialProperty {

  /**
   * In {@link PropertyMode#UseOursLast} a root spatial (no parent) must still resolve properties.
   * The buggy implementation called {@code parent.hasProperty(...)} without a null guard, throwing
   * a NullPointerException on any parentless spatial.
   */
  @Test
  public void testGetPropertyUseOursLastOnRoot() {
    final Node root = new Node("root");
    root.getSceneHints().setPropertyMode(PropertyMode.UseOursLast);
    root.setProperty("color", "red");

    assertEquals("red", root.getProperty("color", "default"));
    assertEquals("default", root.getProperty("missing", "default"));
  }
}
