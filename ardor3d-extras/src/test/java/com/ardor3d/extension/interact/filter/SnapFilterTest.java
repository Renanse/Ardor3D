/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.widget.gizmo.RotateGizmo;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.scenegraph.Node;

public class SnapFilterTest {

  private static final double EPS = 1e-9;

  private Node _target;
  private InteractManager _manager;
  private RotateGizmo _widget;

  @Before
  public void setup() {
    _target = new Node("target");
    _target.updateGeometricState(0);

    _manager = new InteractManager();
    _widget = new RotateGizmo();
    _manager.addWidget(_widget);
    _manager.setSpatialTarget(_target);
    // mirror the manager's input cycle: state starts from the target's transform
    _manager.getSpatialState().copyState(_target);
  }

  private static void assertVectorEquals(final double x, final double y, final double z, final ReadOnlyVector3 v) {
    assertEquals(x, v.getX(), EPS);
    assertEquals(y, v.getY(), EPS);
    assertEquals(z, v.getZ(), EPS);
  }

  private static void assertMatrixEquals(final ReadOnlyMatrix3 expected, final ReadOnlyMatrix3 actual) {
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 3; c++) {
        assertEquals("[" + r + "," + c + "]", expected.getValue(r, c), actual.getValue(r, c), 1e-9);
      }
    }
  }

  // --- GridSnapFilter ---

  @Test
  public void testGridSnapRoundsTranslation() {
    final GridSnapFilter filter = new GridSnapFilter(0.5);
    _manager.getSpatialState().getTransform().setTranslation(1.2, 0.26, -0.9);
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(1.0, 0.5, -1.0, _manager.getSpatialState().getTransform().getTranslation());
  }

  @Test
  public void testGridSnapDisabled() {
    final GridSnapFilter filter = new GridSnapFilter(0.5);
    filter.setEnabled(false);
    _manager.getSpatialState().getTransform().setTranslation(1.2, 0.26, -0.9);
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(1.2, 0.26, -0.9, _manager.getSpatialState().getTransform().getTranslation());
  }

  // --- AngleSnapFilter ---

  @Test
  public void testAngleSnapQuantizesDragDelta() {
    final AngleSnapFilter filter = new AngleSnapFilter(15 * MathUtils.DEG_TO_RAD);
    filter.beginDrag(_manager, _widget, null);

    // Drag has rotated the state 20 degrees about Z: snaps to 15.
    _manager.getSpatialState().getTransform()
        .setRotation(new Matrix3().fromAngleNormalAxis(20 * MathUtils.DEG_TO_RAD, Vector3.UNIT_Z));
    filter.applyFilter(_manager, _widget);

    assertMatrixEquals(new Matrix3().fromAngleNormalAxis(15 * MathUtils.DEG_TO_RAD, Vector3.UNIT_Z),
        _manager.getSpatialState().getTransform().getMatrix());
  }

  @Test
  public void testAngleSnapSmallDeltaSnapsBackToStart() {
    final AngleSnapFilter filter = new AngleSnapFilter(15 * MathUtils.DEG_TO_RAD);
    filter.beginDrag(_manager, _widget, null);

    _manager.getSpatialState().getTransform()
        .setRotation(new Matrix3().fromAngleNormalAxis(7 * MathUtils.DEG_TO_RAD, Vector3.UNIT_Z));
    filter.applyFilter(_manager, _widget);

    assertMatrixEquals(Matrix3.IDENTITY, _manager.getSpatialState().getTransform().getMatrix());
  }

  @Test
  public void testAngleSnapComposesWithStartRotation() {
    // Start the target at 30 degrees about X.
    final Matrix3 start = new Matrix3().fromAngleNormalAxis(30 * MathUtils.DEG_TO_RAD, Vector3.UNIT_X);
    _target.setRotation(start);
    _target.updateGeometricState(0);
    _manager.getSpatialState().copyState(_target);

    final AngleSnapFilter filter = new AngleSnapFilter(15 * MathUtils.DEG_TO_RAD);
    filter.beginDrag(_manager, _widget, null);

    // Drag applies a further 20 degrees about Z (pre-multiplied, like the rotate widgets do).
    final Matrix3 drag = new Matrix3().fromAngleNormalAxis(20 * MathUtils.DEG_TO_RAD, Vector3.UNIT_Z);
    _manager.getSpatialState().getTransform().setRotation(drag.multiply(start, new Matrix3()));
    filter.applyFilter(_manager, _widget);

    // Expect the drag portion snapped to 15 degrees, still on top of the start rotation.
    final Matrix3 expected =
        new Matrix3().fromAngleNormalAxis(15 * MathUtils.DEG_TO_RAD, Vector3.UNIT_Z).multiply(start, new Matrix3());
    assertMatrixEquals(expected, _manager.getSpatialState().getTransform().getMatrix());
  }

  @Test
  public void testAngleSnapWithoutDragIsANoOp() {
    final AngleSnapFilter filter = new AngleSnapFilter(15 * MathUtils.DEG_TO_RAD);
    // no beginDrag
    final Matrix3 rot = new Matrix3().fromAngleNormalAxis(20 * MathUtils.DEG_TO_RAD, Vector3.UNIT_Z);
    _manager.getSpatialState().getTransform().setRotation(rot);
    filter.applyFilter(_manager, _widget);
    assertMatrixEquals(rot, _manager.getSpatialState().getTransform().getMatrix());
  }

  @Test
  public void testAngleSnapEndDragClearsCapture() {
    final AngleSnapFilter filter = new AngleSnapFilter(15 * MathUtils.DEG_TO_RAD);
    filter.beginDrag(_manager, _widget, null);
    filter.endDrag(_manager, _widget, null);

    final Matrix3 rot = new Matrix3().fromAngleNormalAxis(20 * MathUtils.DEG_TO_RAD, Vector3.UNIT_Z);
    _manager.getSpatialState().getTransform().setRotation(rot);
    filter.applyFilter(_manager, _widget);
    assertMatrixEquals(rot, _manager.getSpatialState().getTransform().getMatrix());
  }

  @Test
  public void testAngleSnapExactIncrementUnchanged() {
    final AngleSnapFilter filter = new AngleSnapFilter(15 * MathUtils.DEG_TO_RAD);
    filter.beginDrag(_manager, _widget, null);

    final Matrix3 rot = new Matrix3().fromAngleNormalAxis(45 * MathUtils.DEG_TO_RAD, Vector3.UNIT_Y);
    _manager.getSpatialState().getTransform().setRotation(rot);
    filter.applyFilter(_manager, _widget);
    assertMatrixEquals(rot, _manager.getSpatialState().getTransform().getMatrix());

    assertTrue(filter.isEnabled());
  }
}
