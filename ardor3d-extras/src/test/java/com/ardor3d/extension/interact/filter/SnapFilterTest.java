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

  private static final double EPS = MathUtils.ZERO_TOLERANCE;

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
        assertEquals("[" + r + "," + c + "]", expected.getValue(r, c), actual.getValue(r, c), EPS);
      }
    }
  }

  // --- GridSnapFilter ---

  @Test
  public void testGridSnapRoundsDragOffset() {
    final GridSnapFilter filter = new GridSnapFilter(0.5);
    filter.beginDrag(_manager, _widget, null);
    _manager.getSpatialState().getTransform().setTranslation(1.2, 0.26, -0.9);
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(1.0, 0.5, -1.0, _manager.getSpatialState().getTransform().getTranslation());
  }

  @Test
  public void testGridSnapStepsFromDragStart() {
    // Snapping is relative to where the drag began, so off-grid targets move in whole steps
    // rather than jumping onto the absolute grid.
    _target.setTranslation(0.3, 0, 0);
    _target.updateGeometricState(0);
    _manager.getSpatialState().copyState(_target);

    final GridSnapFilter filter = new GridSnapFilter(1.0);
    filter.beginDrag(_manager, _widget, null);
    _manager.getSpatialState().getTransform().setTranslation(1.0, 0, 0);
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(1.3, 0.0, 0.0, _manager.getSpatialState().getTransform().getTranslation());
  }

  @Test
  public void testGridSnapDisabled() {
    final GridSnapFilter filter = new GridSnapFilter(0.5);
    filter.setEnabled(false);
    filter.beginDrag(_manager, _widget, null);
    _manager.getSpatialState().getTransform().setTranslation(1.2, 0.26, -0.9);
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(1.2, 0.26, -0.9, _manager.getSpatialState().getTransform().getTranslation());
  }

  @Test
  public void testGridSnapWithoutDragIsANoOp() {
    final GridSnapFilter filter = new GridSnapFilter(0.5);
    // no beginDrag
    _manager.getSpatialState().getTransform().setTranslation(1.2, 0.26, -0.9);
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(1.2, 0.26, -0.9, _manager.getSpatialState().getTransform().getTranslation());
  }

  @Test
  public void testGridSnapAccumulatesAcrossInputCycles() {
    final GridSnapFilter filter = new GridSnapFilter(1.0);
    filter.beginDrag(_manager, _widget, null);

    // The manager copies the state from the target before every input event, so each event's
    // widget delta (0.2 units here) is far below the grid size. Ten of them must accumulate to
    // 2 units - not round away to nothing cycle after cycle.
    for (int i = 0; i < 10; i++) {
      _manager.getSpatialState().copyState(_target);
      _manager.getSpatialState().getTransform().setTranslation(
          _manager.getSpatialState().getTransform().getTranslation().add(0.2, 0, 0, new Vector3()));
      filter.applyFilter(_manager, _widget);
      _manager.getSpatialState().applyState(_target);
    }

    assertVectorEquals(2.0, 0.0, 0.0, _target.getTranslation());
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
  public void testAngleSnapAccumulatesAcrossInputCycles() {
    final AngleSnapFilter filter = new AngleSnapFilter(15 * MathUtils.DEG_TO_RAD);
    filter.beginDrag(_manager, _widget, null);

    // Per-event deltas of 2 degrees are far below the 7.5 degree rounding threshold; a slow
    // 20 degree drag must still snap the target to 15 degrees, not stay pinned at the start.
    final Matrix3 eventDelta = new Matrix3().fromAngleNormalAxis(2 * MathUtils.DEG_TO_RAD, Vector3.UNIT_Z);
    for (int i = 0; i < 10; i++) {
      _manager.getSpatialState().copyState(_target);
      _manager.getSpatialState().getTransform()
          .setRotation(eventDelta.multiply(_manager.getSpatialState().getTransform().getMatrix(), new Matrix3()));
      filter.applyFilter(_manager, _widget);
      _manager.getSpatialState().applyState(_target);
    }

    assertMatrixEquals(new Matrix3().fromAngleNormalAxis(15 * MathUtils.DEG_TO_RAD, Vector3.UNIT_Z),
        _target.getRotation());
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
  public void testAngleSnapDisabled() {
    final AngleSnapFilter filter = new AngleSnapFilter(15 * MathUtils.DEG_TO_RAD);
    filter.setEnabled(false);
    filter.beginDrag(_manager, _widget, null);

    final Matrix3 rot = new Matrix3().fromAngleNormalAxis(20 * MathUtils.DEG_TO_RAD, Vector3.UNIT_Z);
    _manager.getSpatialState().getTransform().setRotation(rot);
    filter.applyFilter(_manager, _widget);
    assertMatrixEquals(rot, _manager.getSpatialState().getTransform().getMatrix());
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

  // --- ScaleSnapFilter ---

  private void startScale(final double x, final double y, final double z) {
    _target.setScale(x, y, z);
    _target.updateGeometricState(0);
    _manager.getSpatialState().copyState(_target);
  }

  @Test
  public void testScaleSnapRoundsFactor() {
    startScale(1, 1, 1);
    final ScaleSnapFilter filter = new ScaleSnapFilter(0.25);
    filter.beginDrag(_manager, _widget, null);

    // Drag has grown X by 1.37x: snaps to 1.25x. Y and Z are untouched, so they stay put.
    _manager.getSpatialState().getTransform().setScale(1.37, 1, 1);
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(1.25, 1.0, 1.0, _manager.getSpatialState().getTransform().getScale());
  }

  @Test
  public void testScaleSnapStepsFromDragStart() {
    // Snapping is relative to the start scale, so the factor - not the absolute scale - lands on a
    // clean multiple: a 2.0-scaled object snapped to a 1.25x factor ends at 2.5, not 1.25.
    startScale(2, 2, 2);
    final ScaleSnapFilter filter = new ScaleSnapFilter(0.25);
    filter.beginDrag(_manager, _widget, null);

    _manager.getSpatialState().getTransform().setScale(2.74, 2, 2); // 1.37x
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(2.5, 2.0, 2.0, _manager.getSpatialState().getTransform().getScale());
  }

  @Test
  public void testScaleSnapLeavesUntouchedAxesExactlyAlone() {
    // Y and Z start off any grid; an X-only drag must not nudge them onto the factor grid.
    startScale(1, 1.1, 1.1);
    final ScaleSnapFilter filter = new ScaleSnapFilter(0.25);
    filter.beginDrag(_manager, _widget, null);

    _manager.getSpatialState().getTransform().setScale(2.0, 1.1, 1.1);
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(2.0, 1.1, 1.1, _manager.getSpatialState().getTransform().getScale());
  }

  @Test
  public void testScaleSnapAccumulatesAcrossInputCycles() {
    startScale(1, 1, 1);
    final ScaleSnapFilter filter = new ScaleSnapFilter(0.25);
    filter.beginDrag(_manager, _widget, null);

    // The manager copies the state from the target before every input event, so each event's raw
    // factor (1.1x here) alone rounds back to 1.0x and would lose the drag. Ten of them must
    // compound to 1.1^10 ~= 2.59x and snap to 2.5x - not round away to nothing cycle after cycle.
    for (int i = 0; i < 10; i++) {
      _manager.getSpatialState().copyState(_target);
      final ReadOnlyVector3 s = _manager.getSpatialState().getTransform().getScale();
      _manager.getSpatialState().getTransform().setScale(s.getX() * 1.1, s.getY(), s.getZ());
      filter.applyFilter(_manager, _widget);
      _manager.getSpatialState().applyState(_target);
    }

    assertVectorEquals(2.5, 1.0, 1.0, _target.getScale());
  }

  @Test
  public void testScaleSnapSurvivesZeroTargetScale() {
    // A degenerate target scale of 0 on an axis would divide to Infinity and poison the running
    // per-axis factor; the guard keeps it finite. (Asserted on _accumulated - where the poison
    // lands - since the axis' output is 0 either way; this test shares the filter's package.)
    startScale(0, 1, 1);
    final ScaleSnapFilter filter = new ScaleSnapFilter(0.25);
    filter.beginDrag(_manager, _widget, null);

    _manager.getSpatialState().getTransform().setScale(0.001, 2.0, 1); // X clamped off 0; Y grown 2x
    filter.applyFilter(_manager, _widget);

    assertTrue("a zero target scale must not poison the accumulated factor",
        Double.isFinite(filter._accumulated.getX()) && Double.isFinite(filter._accumulated.getY())
            && Double.isFinite(filter._accumulated.getZ()));
    assertEquals("the touched axis still snaps", 2.0,
        _manager.getSpatialState().getTransform().getScale().getY(), EPS);
  }

  @Test
  public void testScaleSnapFloorsAtOneStep() {
    // Shrinking below half a step would round the factor to zero; it floors at one step instead.
    startScale(1, 1, 1);
    final ScaleSnapFilter filter = new ScaleSnapFilter(0.25);
    filter.beginDrag(_manager, _widget, null);

    _manager.getSpatialState().getTransform().setScale(0.05, 1, 1);
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(0.25, 1.0, 1.0, _manager.getSpatialState().getTransform().getScale());
  }

  @Test
  public void testScaleSnapExactStepUnchanged() {
    startScale(1, 1, 1);
    final ScaleSnapFilter filter = new ScaleSnapFilter(0.25);
    filter.beginDrag(_manager, _widget, null);

    _manager.getSpatialState().getTransform().setScale(1.5, 1, 1); // exactly 6 steps
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(1.5, 1.0, 1.0, _manager.getSpatialState().getTransform().getScale());
    assertTrue(filter.isEnabled());
  }

  @Test
  public void testScaleSnapDisabled() {
    startScale(1, 1, 1);
    final ScaleSnapFilter filter = new ScaleSnapFilter(0.25);
    filter.setEnabled(false);
    filter.beginDrag(_manager, _widget, null);

    _manager.getSpatialState().getTransform().setScale(1.37, 1, 1);
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(1.37, 1.0, 1.0, _manager.getSpatialState().getTransform().getScale());
  }

  @Test
  public void testScaleSnapWithoutDragIsANoOp() {
    startScale(1, 1, 1);
    final ScaleSnapFilter filter = new ScaleSnapFilter(0.25);
    // no beginDrag
    _manager.getSpatialState().getTransform().setScale(1.37, 1, 1);
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(1.37, 1.0, 1.0, _manager.getSpatialState().getTransform().getScale());
  }

  @Test
  public void testScaleSnapEndDragClearsCapture() {
    startScale(1, 1, 1);
    final ScaleSnapFilter filter = new ScaleSnapFilter(0.25);
    filter.beginDrag(_manager, _widget, null);
    filter.endDrag(_manager, _widget, null);

    _manager.getSpatialState().getTransform().setScale(1.37, 1, 1);
    filter.applyFilter(_manager, _widget);
    assertVectorEquals(1.37, 1.0, 1.0, _manager.getSpatialState().getTransform().getScale());
  }
}
