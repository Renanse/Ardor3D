/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.widget.gizmo;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import com.ardor3d.extension.interact.widget.SetCursorCallback;
import com.ardor3d.input.mouse.MouseCursor;

/**
 * The opt-in cursor feedback: a gizmo wires a {@link SetCursorCallback} for the shared
 * {@link AbstractGizmo#DEFAULT_CURSOR} when one is set, and none when it is not.
 */
public class GizmoCursorFeedbackTest {

  @After
  public void clearDefaultCursor() {
    // Static opt-in - reset so it does not leak into other tests in the same JVM.
    AbstractGizmo.DEFAULT_CURSOR = null;
  }

  @Test
  public void testDefaultCursorWiresMouseOverCallback() {
    AbstractGizmo.DEFAULT_CURSOR = MouseCursor.SYSTEM_DEFAULT;
    final TranslateGizmo gizmo = new TranslateGizmo();
    assertTrue("a set DEFAULT_CURSOR should wire a SetCursorCallback",
        gizmo.getMouseOverCallback() instanceof SetCursorCallback);
  }

  @Test
  public void testNoDefaultCursorLeavesCallbackUnset() {
    AbstractGizmo.DEFAULT_CURSOR = null;
    final TranslateGizmo gizmo = new TranslateGizmo();
    assertNull("no DEFAULT_CURSOR should leave the mouse-over callback unset", gizmo.getMouseOverCallback());
  }
}
