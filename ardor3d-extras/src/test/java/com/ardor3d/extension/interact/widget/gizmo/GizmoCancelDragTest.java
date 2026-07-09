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

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.widget.DragState;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyboardState;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Node;

/**
 * Headless tests of the shared gizmo drag-cancel behavior: Escape during a drag restores the
 * target to its pre-drag transform and ends the interaction. Uses a synthetic camera aimed at the
 * gizmo center so a real pick starts the drag, then drives the actual beginDrag / cancelDrag path.
 */
public class GizmoCancelDragTest {

  private static final double EPS = MathUtils.ZERO_TOLERANCE;

  private Node _target;
  private InteractManager _manager;
  private TestGizmo _gizmo;
  private Camera _camera;

  /** Drives the real drag-begin (via a pick) and cancel seams for testing. */
  private static class TestGizmo extends TranslateGizmo {
    void seedDrag(final Vector2 mouse, final Camera camera, final InteractManager manager) {
      // Mirror what the input trigger does before a widget sees input: sync state from the target.
      manager.getSpatialState().copyState(manager.getSpatialTarget());
      findPick(mouse, camera);
      beginDrag(manager, new MouseState((int) mouse.getX(), (int) mouse.getY(), 0, 0, 0, null, null));
      setDragState(DragState.DRAG);
    }

    boolean cancelIfEsc(final KeyboardState keys, final InteractManager manager) {
      return cancelDragIfRequested(keys, new MouseState(0, 0, 0, 0, 0, null, null), new AtomicBoolean(false), manager);
    }

    boolean hasSnapshot() { return _hasDragStartTransform; }
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

    // Camera looking straight at the target, so a ray through the screen center strikes the gizmo.
    _camera = new Camera(100, 100);
    _camera.setFrustumPerspective(90, 1, 1, 1000);
    _camera.setLocation(5, 2, 7);
    _camera.lookAt(5, 2, -3, Vector3.UNIT_Y);

    _gizmo.getHandle().setTranslation(_target.getWorldTranslation());
    _gizmo.getHandle().updateGeometricState(0);
  }

  private static KeyboardState keys(final Key... down) {
    final EnumSet<Key> set = EnumSet.noneOf(Key.class);
    for (final Key key : down) {
      set.add(key);
    }
    return new KeyboardState(set, null);
  }

  @Test
  public void testEscapeCancelsDragAndRestoresPreDragTransform() {
    _gizmo.seedDrag(new Vector2(50, 50), _camera, _manager);
    assertTrue("a handle should have been picked at the gizmo center", _gizmo.getDragState() != DragState.NONE);
    assertTrue("the pre-drag transform should be snapshotted", _gizmo.hasSnapshot());

    // Simulate the drag having moved the target well away from where it started.
    _target.setTranslation(20, -8, 14);
    _target.updateGeometricState(0);

    final boolean cancelled = _gizmo.cancelIfEsc(keys(Key.ESCAPE), _manager);

    assertTrue("Escape during a drag should cancel it", cancelled);
    assertEquals("the drag should have ended", DragState.NONE, _gizmo.getDragState());
    assertEquals(5.0, _target.getTranslation().getX(), EPS);
    assertEquals(2.0, _target.getTranslation().getY(), EPS);
    assertEquals(-3.0, _target.getTranslation().getZ(), EPS);
  }

  @Test
  public void testEscapeWithoutADragDoesNothing() {
    final boolean cancelled = _gizmo.cancelIfEsc(keys(Key.ESCAPE), _manager);
    assertFalse("Escape with no active drag is not a cancel", cancelled);
    assertEquals(DragState.NONE, _gizmo.getDragState());
  }

  @Test
  public void testDragContinuesWhenEscapeIsNotHeld() {
    _gizmo.seedDrag(new Vector2(50, 50), _camera, _manager);
    final boolean cancelled = _gizmo.cancelIfEsc(keys(), _manager);
    assertFalse("no cancel without Escape", cancelled);
    assertTrue("the drag should still be active", _gizmo.getDragState() != DragState.NONE);
  }
}
