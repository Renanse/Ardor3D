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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.scenegraph.shape.Quad;

/**
 * Tests for the stroke builders in {@link GizmoGeometry} and the stroke-related handle plumbing
 * in {@link GizmoHandle} and {@link AbstractGizmo}.
 */
public class GizmoStrokeTest {

  private static final float EPS = 1e-6f;

  // --- GizmoGeometry.polylineStroke ---

  @Test
  public void testOpenStrokeIndices() {
    final Line line = GizmoGeometry.polylineStroke("open", new ReadOnlyVector3[] {new Vector3(0, 0, 0),
        new Vector3(1, 0, 0), new Vector3(1, 1, 0)}, false, 2f);

    assertEquals(IndexMode.LineStripAdjacency, line.getMeshData().getIndexMode(0));
    // Open strips mirror their neighbors at the ends: [1, 0, 1, 2, 1] for three vertices.
    final IndexBufferData<?> indices = line.getMeshData().getIndices();
    assertEquals(5, indices.getBufferLimit());
    assertEquals(1, indices.get(0));
    assertEquals(0, indices.get(1));
    assertEquals(1, indices.get(2));
    assertEquals(2, indices.get(3));
    assertEquals(1, indices.get(4));
  }

  @Test
  public void testClosedStrokeIndicesWrap() {
    final Line line = GizmoGeometry.polylineStroke("closed", new ReadOnlyVector3[] {new Vector3(0, 0, 0),
        new Vector3(1, 0, 0), new Vector3(1, 1, 0), new Vector3(0, 1, 0)}, true, 2f);

    // Closed strips walk every vertex and return to the start, with wrap-around adjacency:
    // [3, 0, 1, 2, 3, 0, 1] for four vertices - four segments, each drawn exactly once.
    final IndexBufferData<?> indices = line.getMeshData().getIndices();
    assertEquals(7, indices.getBufferLimit());
    final int[] expected = {3, 0, 1, 2, 3, 0, 1};
    for (int i = 0; i < expected.length; i++) {
      assertEquals("index " + i, expected[i], indices.get(i));
    }
  }

  @Test
  public void testStrokesAreAntialiasedUnpickableAndSized() {
    final Line line = GizmoGeometry.segmentStroke("seg", new Vector3(0, 0, 0.2), new Vector3(0, 0, 0.8), 2.5f);

    assertTrue(line.isAntialiased());
    assertEquals(2.5f, line.getLineWidth(), EPS);
    assertFalse(line.getSceneHints().isPickingHintEnabled(PickingHint.Pickable));
  }

  @Test
  public void testStrokesDepthTestButNeverWrite() {
    final Line line = GizmoGeometry.segmentStroke("seg", new Vector3(0, 0, 0.2), new Vector3(0, 0, 0.8), 2.5f);

    final ZBufferState zstate = (ZBufferState) line.getLocalRenderStates().get(RenderState.StateType.ZBuffer);
    assertNotNull("strokes must carry their own zbuffer state", zstate);
    assertTrue(zstate.isEnabled());
    assertEquals(ZBufferState.TestFunction.LessThanOrEqualTo, zstate.getFunction());
    assertFalse("overlay strokes must not write depth", zstate.isWritable());
  }

  @Test
  public void testArcStrokeClosedCircleHasNoDuplicateSeamPoint() {
    final int samples = 8;
    final Line circle = GizmoGeometry.arcStroke("circle", 1.0, 0, 2 * Math.PI, samples, 2f);
    // A closed circle keeps exactly one vertex per sample; the seam segment comes from the
    // wrapped indices, not a duplicated point.
    assertEquals(samples, circle.getMeshData().getVertexCount());

    final Line arc = GizmoGeometry.arcStroke("half", 1.0, 0, Math.PI, samples, 2f);
    assertEquals(samples + 1, arc.getMeshData().getVertexCount());
  }

  // --- GizmoHandle alpha scales and width rescaling ---

  @Test
  public void testApplyColorHonorsPerMeshAlphaScale() {
    final Node root = new Node("part");
    final Quad fill = new Quad("fill", 1, 1);
    final Line border = GizmoGeometry.polylineStroke("border", new ReadOnlyVector3[] {new Vector3(0, 0, 0),
        new Vector3(1, 0, 0), new Vector3(1, 1, 0), new Vector3(0, 1, 0)}, true, 2f);
    root.attachChild(fill);
    root.attachChild(border);

    final GizmoHandle handle =
        new GizmoHandle(GizmoPart.PlaneXY, root, ColorRGBA.RED, Vector3.UNIT_Z, GizmoHandle.FadeMode.Plane);
    handle.setAlphaScale(fill, 0.35f);
    handle.applyColor(ColorRGBA.RED, 0.8f);

    assertEquals(0.8f * 0.35f, fill.getDefaultColor().getAlpha(), EPS);
    assertEquals(0.8f, border.getDefaultColor().getAlpha(), EPS);
  }

  @Test
  public void testLineWidthRescalesFromAuthoredWidth() {
    final Node root = new Node("part");
    final Line stroke = GizmoGeometry.segmentStroke("shaft", new Vector3(), new Vector3(0, 0, 1), 2.5f);
    root.attachChild(stroke);
    final GizmoHandle handle =
        new GizmoHandle(GizmoPart.AxisX, root, ColorRGBA.RED, Vector3.UNIT_X, GizmoHandle.FadeMode.Axis);

    handle.applyLineWidthScale(2f);
    assertEquals(5f, stroke.getLineWidth(), EPS);

    // Rescaling always starts from the authored width - not the previously scaled one.
    handle.applyLineWidthScale(1.5f);
    assertEquals(3.75f, stroke.getLineWidth(), EPS);
  }

  // --- AbstractGizmo.highlightColorFor ---

  @Test
  public void testChromaticHandlesBrightenInHue() {
    final TranslateGizmo gizmo = new TranslateGizmo().withAxes();
    final GizmoHandle xAxis = gizmo.getGizmoHandles().get(0);

    final ReadOnlyColorRGBA base = xAxis.getBaseColor();
    final ReadOnlyColorRGBA highlight = gizmo.highlightColorFor(xAxis);

    // Every channel moves toward white, dominant channels stay dominant, alpha is untouched.
    assertTrue(highlight.getRed() > base.getRed() - EPS);
    assertTrue(highlight.getGreen() > base.getGreen());
    assertTrue(highlight.getBlue() > base.getBlue());
    assertTrue(highlight.getRed() > highlight.getGreen());
    assertTrue(highlight.getRed() > highlight.getBlue());
    assertEquals(base.getAlpha(), highlight.getAlpha(), EPS);

    final float keep = 1f - AbstractGizmo.HIGHLIGHT_BRIGHTEN;
    assertEquals(1f - (1f - base.getGreen()) * keep, highlight.getGreen(), EPS);
  }

  @Test
  public void testGrayHandlesUseHighlightTint() {
    final TranslateGizmo gizmo = new TranslateGizmo().withViewPlaneHandle();
    final GizmoHandle center = gizmo.getGizmoHandles().get(0);

    // The near-white center circle cannot visibly brighten in-hue; it gets the configured tint.
    assertEquals(AbstractGizmo.DEFAULT_HIGHLIGHT_COLOR, gizmo.highlightColorFor(center));
  }
}
