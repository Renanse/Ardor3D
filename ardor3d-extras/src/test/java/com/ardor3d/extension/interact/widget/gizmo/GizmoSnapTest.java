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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ardor3d.extension.interact.filter.AngleSnapFilter;
import com.ardor3d.extension.interact.filter.GridSnapFilter;
import com.ardor3d.math.util.MathUtils;

/**
 * Headless tests of the snap-tick wiring: the snap filters report themselves as SnapSources, and a
 * gizmo finds the active snap increment from its filters. The tick geometry and pulse are rendered
 * effects, covered by the interactive probe.
 */
public class GizmoSnapTest {

  private static final double EPS = MathUtils.ZERO_TOLERANCE;

  /** Exposes the protected snap lookup. */
  private static class TestGizmo extends RotateGizmo {
    double snapIncrement() {
      return activeSnapIncrement();
    }
  }

  @Test
  public void testAngleSnapFilterIsSnapSource() {
    final AngleSnapFilter filter = new AngleSnapFilter(15 * MathUtils.DEG_TO_RAD);
    assertTrue(filter.isSnapping());
    assertEquals(15 * MathUtils.DEG_TO_RAD, filter.getSnapIncrement(), EPS);
    filter.setEnabled(false);
    assertFalse("disabled filter does not snap", filter.isSnapping());
  }

  @Test
  public void testGridSnapFilterIsSnapSource() {
    final GridSnapFilter filter = new GridSnapFilter(2.0);
    assertTrue(filter.isSnapping());
    assertEquals(2.0, filter.getSnapIncrement(), EPS);
    filter.setEnabled(false);
    assertFalse("disabled filter does not snap", filter.isSnapping());
  }

  @Test
  public void testActiveSnapIncrementFindsEnabledSnapFilter() {
    final TestGizmo gizmo = new TestGizmo();
    gizmo.withAllHandles();
    assertEquals("no snap filter attached", 0.0, gizmo.snapIncrement(), EPS);

    final AngleSnapFilter filter = new AngleSnapFilter(0.5);
    gizmo.addFilter(filter);
    assertEquals(0.5, gizmo.snapIncrement(), EPS);

    filter.setEnabled(false);
    assertEquals("disabled snap reports no increment", 0.0, gizmo.snapIncrement(), EPS);
  }
}
