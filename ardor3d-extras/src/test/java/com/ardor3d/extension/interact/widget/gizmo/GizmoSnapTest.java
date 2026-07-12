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
import com.ardor3d.extension.interact.filter.ScaleSnapFilter;
import com.ardor3d.extension.interact.widget.DragState;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.hint.CullHint;

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

  /** Drives translate snap ticks headlessly (no GL / picking). */
  private static class TranslateProbe extends TranslateGizmo {
    void forceAxisDrag() {
      for (final GizmoHandle handle : getGizmoHandles()) {
        if (handle.getPart() == GizmoPart.AxisX) {
          _lastDragSpatial = handle.getMeshes().get(0);
          setDragState(DragState.DRAG);
          return;
        }
      }
    }

    void ticks(final Camera camera) {
      updateSnapTicks(camera);
    }

    Mesh tickMesh() {
      return _snapTicks;
    }
  }

  /** Drives scale snap ticks headlessly (no GL / picking). */
  private static class ScaleProbe extends ScaleGizmo {
    void forceAxisDrag() {
      for (final GizmoHandle handle : getGizmoHandles()) {
        if (handle.getPart() == GizmoPart.AxisX) {
          _lastDragSpatial = handle.getMeshes().get(0);
          setDragState(DragState.DRAG);
          return;
        }
      }
    }

    /** Set the dragged axis' applied factor directly - normally captured from the state in render. */
    void setActiveFactor(final double factor) {
      _snapActiveFactor = factor;
    }

    void ticks(final Camera camera) {
      updateSnapTicks(camera);
    }

    Mesh tickMesh() {
      return _snapTicks;
    }
  }

  private static Camera threeQuarterCamera() {
    final Camera camera = new Camera(100, 100);
    camera.setFrustumPerspective(90, 1, 1, 1000);
    camera.setLocation(0, 5, 10);
    camera.lookAt(0, 0, 0, Vector3.UNIT_Y);
    return camera;
  }

  @Test
  public void testTranslateTicksShowOnlyWhileSnappingAnAxisDrag() {
    final TranslateProbe gizmo = new TranslateProbe();
    gizmo.withAllHandles();
    gizmo.getHandle().setScale(1.0);
    gizmo.getHandle().updateGeometricState(0);
    final Camera camera = threeQuarterCamera();
    gizmo.forceAxisDrag();

    // Dragging an axis, but no snap filter -> ticks stay hidden.
    gizmo.ticks(camera);
    assertEquals(CullHint.Always, gizmo.tickMesh().getSceneHints().getCullHint());

    // With grid snap active, the ticks appear along the axis.
    gizmo.addFilter(new GridSnapFilter(1.0));
    gizmo.ticks(camera);
    assertEquals(CullHint.Never, gizmo.tickMesh().getSceneHints().getCullHint());
    assertTrue("ticks should have geometry", gizmo.tickMesh().getMeshData().getVertexCount() > 0);
  }

  @Test
  public void testTranslateTicksFadeOutWhenGridTooDenseOnScreen() {
    final TranslateProbe gizmo = new TranslateProbe();
    gizmo.withAllHandles();
    gizmo.getHandle().updateGeometricState(0);
    final Camera camera = threeQuarterCamera();
    gizmo.forceAxisDrag();
    gizmo.addFilter(new GridSnapFilter(1.0));

    // Normal zoom (scale 1): a 1-unit grid is ~100px on screen, so the ticks show.
    gizmo.getHandle().setScale(1.0);
    gizmo.getHandle().updateGeometricState(0);
    gizmo.ticks(camera);
    assertEquals(CullHint.Never, gizmo.tickMesh().getSceneHints().getCullHint());

    // Zoomed way out (large gizmo scale): the grid is sub-threshold on screen, so ticks hide.
    gizmo.getHandle().setScale(500.0);
    gizmo.getHandle().updateGeometricState(0);
    gizmo.ticks(camera);
    assertEquals(CullHint.Always, gizmo.tickMesh().getSceneHints().getCullHint());
  }

  @Test
  public void testTranslateSnapPulsesWhenTheSnappedPositionSteps() {
    final TranslateProbe gizmo = new TranslateProbe();
    gizmo.withAllHandles();
    gizmo.getHandle().setScale(1.0);
    gizmo.getHandle().updateGeometricState(0);
    final Camera camera = threeQuarterCamera();
    gizmo.forceAxisDrag();
    gizmo.addFilter(new GridSnapFilter(1.0));

    // First frame records the position; no pulse yet.
    gizmo.ticks(camera);
    // A step to a new grid cell re-arms the pulse.
    gizmo.getHandle().setTranslation(1, 0, 0);
    gizmo.getHandle().updateGeometricState(0);
    gizmo.ticks(camera);
    assertTrue("stepping to a new grid cell pulses", gizmo.getSnapPulse() > 0.5);
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
  public void testScaleSnapFilterIsSnapSource() {
    final ScaleSnapFilter filter = new ScaleSnapFilter(0.25);
    assertTrue(filter.isSnapping());
    assertEquals(0.25, filter.getSnapIncrement(), EPS);
    filter.setEnabled(false);
    assertFalse("disabled filter does not snap", filter.isSnapping());
  }

  @Test
  public void testScaleTicksShowOnlyWhileSnappingAnAxisDrag() {
    final ScaleProbe gizmo = new ScaleProbe();
    gizmo.withAllHandles();
    gizmo.getHandle().setScale(1.0);
    gizmo.getHandle().updateGeometricState(0);
    final Camera camera = threeQuarterCamera();
    gizmo.forceAxisDrag();

    // Dragging an axis, but no snap filter -> ticks stay hidden.
    gizmo.ticks(camera);
    assertEquals(CullHint.Always, gizmo.tickMesh().getSceneHints().getCullHint());

    // With scale snap active, the ticks appear along the axis.
    gizmo.addFilter(new ScaleSnapFilter(0.25));
    gizmo.ticks(camera);
    assertEquals(CullHint.Never, gizmo.tickMesh().getSceneHints().getCullHint());
    assertTrue("ticks should have geometry", gizmo.tickMesh().getMeshData().getVertexCount() > 0);
  }

  @Test
  public void testScaleTicksFadeOutWhenStepTooFineOnScreen() {
    final ScaleProbe gizmo = new ScaleProbe();
    gizmo.withAllHandles();
    gizmo.getHandle().setScale(1.0);
    gizmo.getHandle().updateGeometricState(0);
    final Camera camera = threeQuarterCamera();
    gizmo.forceAxisDrag();

    // A quarter step spans ~22px between ticks on screen (the factor grid is fixed to the gizmo's
    // pixel footprint, not the camera distance), so the ticks show.
    gizmo.addFilter(new ScaleSnapFilter(0.25));
    gizmo.ticks(camera);
    assertEquals(CullHint.Never, gizmo.tickMesh().getSceneHints().getCullHint());

    // A tiny 0.02 step packs the ticks a couple of pixels apart: sub-threshold, so they hide.
    gizmo.clearFilters();
    gizmo.addFilter(new ScaleSnapFilter(0.02));
    gizmo.ticks(camera);
    assertEquals(CullHint.Always, gizmo.tickMesh().getSceneHints().getCullHint());
  }

  @Test
  public void testScaleSnapPulsesWhenTheSnappedFactorSteps() {
    final ScaleProbe gizmo = new ScaleProbe();
    gizmo.withAllHandles();
    gizmo.getHandle().setScale(1.0);
    gizmo.getHandle().updateGeometricState(0);
    final Camera camera = threeQuarterCamera();
    gizmo.forceAxisDrag();
    gizmo.addFilter(new ScaleSnapFilter(0.25));

    // First frame records the factor step; no pulse yet.
    gizmo.setActiveFactor(1.0);
    gizmo.ticks(camera);
    // Stepping to the next quarter re-arms the pulse.
    gizmo.setActiveFactor(1.25);
    gizmo.ticks(camera);
    assertTrue("stepping to a new factor pulses", gizmo.getSnapPulse() > 0.5);
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
