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

import org.junit.Test;

import com.ardor3d.extension.interact.widget.DragState;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;

/**
 * Headless tests of the origin-ghost gating and placement: the faint start-point marker appears
 * only during a drag that has actually moved the gizmo origin (a translate drag), and anchors at
 * the captured start position. The ghost's rendered appearance is covered by the interactive probe.
 */
public class GizmoOriginGhostTest {

  private static final double EPS = MathUtils.ZERO_TOLERANCE;

  /** Drives the origin ghost headlessly (no GL / picking). */
  private static class GhostProbe extends TranslateGizmo {
    void forceDrag(final double sx, final double sy, final double sz) {
      _dragStartWorldTranslation.set(sx, sy, sz);
      _hasOriginGhost = true;
      setDragState(DragState.DRAG);
    }

    /** Move the gizmo origin, standing in for render()'s per-frame follow of the target. */
    void setOrigin(final double x, final double y, final double z) {
      getHandle().setTranslation(x, y, z);
      getHandle().updateGeometricState(0);
    }

    void ghost(final Camera camera) {
      updateOriginGhost(camera);
    }

    Node ghostNode() {
      return _originGhost;
    }
  }

  private static Camera threeQuarterCamera() {
    final Camera camera = new Camera(100, 100);
    camera.setFrustumPerspective(90, 1, 1, 1000);
    camera.setLocation(0, 5, 10);
    camera.lookAt(0, 0, 0, Vector3.UNIT_Y);
    return camera;
  }

  private static void assertVectorEquals(final double x, final double y, final double z, final ReadOnlyVector3 v) {
    assertEquals(x, v.getX(), EPS);
    assertEquals(y, v.getY(), EPS);
    assertEquals(z, v.getZ(), EPS);
  }

  @Test
  public void testGhostHiddenWhenNotDragging() {
    final GhostProbe gizmo = new GhostProbe();
    gizmo.withAllHandles();
    final Camera camera = threeQuarterCamera();
    gizmo.setOrigin(5, 0, 0);
    gizmo.ghost(camera);
    assertEquals(CullHint.Always, gizmo.ghostNode().getSceneHints().getCullHint());
  }

  @Test
  public void testGhostShownAndAnchoredWhenOriginMoved() {
    final GhostProbe gizmo = new GhostProbe();
    gizmo.withAllHandles();
    final Camera camera = threeQuarterCamera();
    gizmo.forceDrag(0, 0, 0);
    gizmo.setOrigin(5, 0, 0); // dragged 5 units along +X
    gizmo.ghost(camera);
    assertEquals(CullHint.Never, gizmo.ghostNode().getSceneHints().getCullHint());
    // The ghost anchors at the captured drag-start origin, not the current one.
    assertVectorEquals(0, 0, 0, gizmo.ghostNode().getTranslation());
  }

  @Test
  public void testGhostHiddenWhenOriginUnmoved() {
    // A rotate or scale drag never moves the origin, so the ghost stays hidden.
    final GhostProbe gizmo = new GhostProbe();
    gizmo.withAllHandles();
    final Camera camera = threeQuarterCamera();
    gizmo.forceDrag(2, 3, 4);
    gizmo.setOrigin(2, 3, 4); // origin unchanged from the start
    gizmo.ghost(camera);
    assertEquals(CullHint.Always, gizmo.ghostNode().getSceneHints().getCullHint());
  }

  @Test
  public void testGhostRespectsDisableFlag() {
    final GhostProbe gizmo = new GhostProbe();
    gizmo.withAllHandles();
    final Camera camera = threeQuarterCamera();
    gizmo.forceDrag(0, 0, 0);
    gizmo.setOrigin(5, 0, 0);
    gizmo.setShowOriginGhost(false);
    gizmo.ghost(camera);
    assertEquals(CullHint.Always, gizmo.ghostNode().getSceneHints().getCullHint());
  }
}
