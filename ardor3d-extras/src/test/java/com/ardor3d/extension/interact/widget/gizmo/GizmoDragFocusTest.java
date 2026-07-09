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

import java.nio.FloatBuffer;

import org.junit.Before;
import org.junit.Test;

import com.ardor3d.extension.interact.widget.DragState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.hint.CullHint;

/**
 * Headless tests of the drag-focus axis guide gating: the guide line shows along the dragged axis
 * during a single-axis drag and stays hidden for plane, center and ring drags or when idle. The
 * alpha dim of the inactive handles is a rendered effect, covered by the GL gizmo smoke test.
 */
public class GizmoDragFocusTest {

  private static final double EPS = 1e-9;
  private static final float L = (float) AbstractGizmo.AXIS_GUIDE_HALF_LENGTH;

  private TestGizmo _gizmo;

  /** Forces a drag on a chosen handle (no GL / picking) and exposes the guide refresh. */
  private static class TestGizmo extends TranslateGizmo {
    void forceDrag(final GizmoPart part) {
      for (final GizmoHandle handle : getGizmoHandles()) {
        if (handle.getPart() == part) {
          _lastDragSpatial = handle.getMeshes().get(0);
          setDragState(DragState.DRAG);
          return;
        }
      }
      throw new IllegalArgumentException("no handle for " + part);
    }

    void refreshGuide() {
      updateAxisGuide();
    }
  }

  @Before
  public void setup() {
    _gizmo = new TestGizmo();
    _gizmo.withAllHandles();
  }

  private CullHint guideCull() {
    return _gizmo.getAxisGuide().getSceneHints().getCullHint();
  }

  @Test
  public void testAxisDragShowsGuideAlongThatAxis() {
    // Y so the endpoints prove a real reorient off the guide's built-along-X rest state.
    _gizmo.forceDrag(GizmoPart.AxisY);
    _gizmo.refreshGuide();

    assertEquals(CullHint.Never, guideCull());
    final Line guide = _gizmo.getAxisGuide();
    final FloatBuffer verts = guide.getMeshData().getVertexBuffer();
    assertEquals(0.0f, verts.get(0), EPS);
    assertEquals(-L, verts.get(1), EPS);
    assertEquals(0.0f, verts.get(2), EPS);
    assertEquals(0.0f, verts.get(3), EPS);
    assertEquals(L, verts.get(4), EPS);
    assertEquals(0.0f, verts.get(5), EPS);
  }

  @Test
  public void testPlaneDragHidesGuide() {
    _gizmo.forceDrag(GizmoPart.PlaneXY);
    _gizmo.refreshGuide();
    assertEquals(CullHint.Always, guideCull());
  }

  @Test
  public void testCenterDragHidesGuide() {
    _gizmo.forceDrag(GizmoPart.Center);
    _gizmo.refreshGuide();
    assertEquals(CullHint.Always, guideCull());
  }

  @Test
  public void testGuideHiddenWhenIdle() {
    _gizmo.refreshGuide();
    assertEquals(CullHint.Always, guideCull());
  }

  @Test
  public void testGuideHidesAgainAfterAxisDragEnds() {
    _gizmo.forceDrag(GizmoPart.AxisX);
    _gizmo.refreshGuide();
    assertEquals(CullHint.Never, guideCull());

    _gizmo.setDragState(DragState.NONE);
    _gizmo.refreshGuide();
    assertEquals(CullHint.Always, guideCull());
  }
}
