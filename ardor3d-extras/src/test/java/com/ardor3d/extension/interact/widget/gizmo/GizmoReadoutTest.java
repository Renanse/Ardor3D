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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.widget.DragState;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;

/**
 * Headless tests of the shared drag readout's content and gating, on the translate gizmo:
 * getReadoutText is null unless a drag is active and reflects the delta from the drag's start once
 * it is. On-screen positioning needs the current camera and is covered by the GL gizmo smoke test.
 */
public class GizmoReadoutTest {

  private static final double EPS = MathUtils.ZERO_TOLERANCE;

  private Node _target;
  private InteractManager _manager;
  private TestGizmo _gizmo;
  private Camera _camera;

  /** Drives a real pick-started drag (no GL) and exposes the readout content. */
  private static class TestGizmo extends TranslateGizmo {
    void seedDrag(final Vector2 mouse, final Camera camera, final InteractManager manager) {
      manager.getSpatialState().copyState(manager.getSpatialTarget());
      findPick(mouse, camera);
      beginDrag(manager, new MouseState((int) mouse.getX(), (int) mouse.getY(), 0, 0, 0, null, null));
      setDragState(DragState.DRAG);
    }

    String readout(final InteractManager manager) {
      return getReadoutText(manager);
    }
  }

  @Before
  public void setup() {
    _target = new Node("target");
    _target.setTranslation(5, 2, -3);
    _target.updateGeometricState(0);

    _manager = new InteractManager();
    _gizmo = new TestGizmo();
    _gizmo.withAllHandles();
    _manager.addWidget(_gizmo);
    _manager.setSpatialTarget(_target);

    _camera = new Camera(100, 100);
    _camera.setFrustumPerspective(90, 1, 1, 1000);
    _camera.setLocation(5, 2, 7);
    _camera.lookAt(5, 2, -3, Vector3.UNIT_Y);

    _gizmo.getHandle().setTranslation(_target.getWorldTranslation());
    _gizmo.getHandle().updateGeometricState(0);
  }

  @Test
  public void testReadoutHiddenInitially() {
    assertEquals(CullHint.Always, _gizmo.getReadout().getSceneHints().getCullHint());
  }

  @Test
  public void testReadoutNullWithoutADrag() {
    assertNull("no readout without a drag", _gizmo.readout(_manager));
  }

  @Test
  public void testReadoutReflectsTranslationDeltaDuringDrag() {
    _gizmo.seedDrag(new Vector2(50, 50), _camera, _manager);
    final String atStart = _gizmo.readout(_manager);
    assertNotNull("readout shows during a drag", atStart);
    // ASCII only (no delta glyph in the readout font); a three-component vector.
    assertTrue("readout is a vector", atStart.chars().filter(c -> c == ',').count() == 2);

    // Move the target: the readout must change to reflect the new delta from the drag start.
    _target.setTranslation(9, 2, 1);
    _target.updateGeometricState(0);
    final String moved = _gizmo.readout(_manager);
    assertNotNull(moved);
    assertNotEquals("readout tracks the moving target", atStart, moved);
  }

  @Test
  public void testReadoutNullAfterDragEnds() {
    _gizmo.seedDrag(new Vector2(50, 50), _camera, _manager);
    assertNotNull(_gizmo.readout(_manager));
    _gizmo.endDrag(_manager, null);
    assertNull("no readout once the drag ends", _gizmo.readout(_manager));
  }

  @Test
  public void testFormatterOverridesDefaultAndReceivesDeltaAndPosition() {
    final double[] got = new double[6]; // dx, dy, dz, px, py, pz
    _gizmo.setReadoutFormatter((delta, position, manager) -> {
      got[0] = delta.getX();
      got[1] = delta.getY();
      got[2] = delta.getZ();
      got[3] = position.getX();
      got[4] = position.getY();
      got[5] = position.getZ();
      return "custom";
    });

    _gizmo.seedDrag(new Vector2(50, 50), _camera, _manager);
    _target.setTranslation(9, 2, 1); // from (5, 2, -3): delta (4, 0, 4), position (9, 2, 1)
    _target.updateGeometricState(0);

    assertEquals("formatter overrides the built-in text", "custom", _gizmo.readout(_manager));
    assertEquals(4.0, got[0], EPS);
    assertEquals(0.0, got[1], EPS);
    assertEquals(4.0, got[2], EPS);
    assertEquals(9.0, got[3], EPS);
    assertEquals(2.0, got[4], EPS);
    assertEquals(1.0, got[5], EPS);
  }
}
