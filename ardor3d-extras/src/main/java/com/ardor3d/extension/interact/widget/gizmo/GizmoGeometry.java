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

import java.nio.FloatBuffer;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.geom.GeometryTool;

/**
 * Mesh builders for gizmo handle geometry that the stock shapes do not cover.
 */
public final class GizmoGeometry {

  private GizmoGeometry() {}

  /**
   * Build an antialiased, screen-space stroke following the given polyline. The stroke is
   * expanded to a constant pixel width by the miter line material, so it stays crisp at any
   * camera distance and needs no radial tessellation. Strokes are not pickable - gizmos pick
   * against their invisible fat proxy meshes instead.
   *
   * @param name
   *          name for the line.
   * @param points
   *          polyline vertices, in gizmo-local space.
   * @param closed
   *          true to connect the last point back to the first.
   * @param widthPixels
   *          stroke width, in screen pixels.
   * @return the new line, model bound updated.
   */
  public static Line polylineStroke(final String name, final ReadOnlyVector3[] points, final boolean closed,
      final float widthPixels) {
    final Line line = new Line(name, points, null, null, null);
    final MeshData meshData = line.getMeshData();
    meshData.setIndexMode(IndexMode.LineStripAdjacency);
    meshData.setIndices(closed ? wrappedStripIndices(points.length)
        : GeometryTool.generateAdjacencyIndices(IndexMode.LineStripAdjacency, points.length));
    meshData.markIndicesDirty();

    line.setAntialiased(true);
    line.setLineWidth(widthPixels);
    line.getSceneHints().setAllPickingHints(false);
    line.updateModelBound();
    return line;
  }

  /** Build a single straight antialiased stroke between two points. See {@link #polylineStroke}. */
  public static Line segmentStroke(final String name, final ReadOnlyVector3 from, final ReadOnlyVector3 to,
      final float widthPixels) {
    return polylineStroke(name, new ReadOnlyVector3[] {from, to}, false, widthPixels);
  }

  /**
   * Build an antialiased stroke following a circular arc in the XY plane, centered on the origin,
   * sweeping counter-clockwise from startAngle to endAngle (radians, measured from +X). A full
   * 2*pi sweep produces a closed circle. See {@link #polylineStroke}.
   */
  public static Line arcStroke(final String name, final double radius, final double startAngle, final double endAngle,
      final int samples, final float widthPixels) {
    final boolean closed = endAngle - startAngle > 2 * Math.PI - 1e-6;
    final int count = closed ? samples : samples + 1;
    final ReadOnlyVector3[] points = new ReadOnlyVector3[count];
    for (int i = 0; i < count; i++) {
      final double theta = startAngle + (endAngle - startAngle) * i / samples;
      points[i] = new Vector3(Math.cos(theta) * radius, Math.sin(theta) * radius, 0);
    }
    return polylineStroke(name, points, closed, widthPixels);
  }

  /**
   * Adjacency indices for a closed line strip over count vertices: the strip walks every vertex
   * and returns to the start, with the wrap-around neighbors as adjacency at both ends.
   */
  private static IndexBufferData<?> wrappedStripIndices(final int count) {
    final IndexBufferData<?> indices = BufferUtils.createIndexBufferData(count + 3, count - 1);
    indices.put(count - 1);
    for (int i = 0; i < count; i++) {
      indices.put(i);
    }
    indices.put(0);
    indices.put(1);
    return indices;
  }

  /**
   * Build a tube following a circular arc in the XY plane, centered on the origin, sweeping
   * counter-clockwise from startAngle to endAngle (radians, measured from +X). Pass a full 2*pi
   * sweep to build a closed ring. No texture coordinates are generated.
   *
   * @param name
   *          name for the mesh.
   * @param arcRadius
   *          radius of the arc the tube follows.
   * @param tubeRadius
   *          radius of the tube itself.
   * @param startAngle
   *          arc start angle, in radians.
   * @param endAngle
   *          arc end angle, in radians. Must be greater than startAngle.
   * @param arcSamples
   *          segments along the arc.
   * @param tubeSamples
   *          segments around the tube.
   * @return the new mesh. The caller is responsible for updateModelBound().
   */
  public static Mesh arcTube(final String name, final double arcRadius, final double tubeRadius,
      final double startAngle, final double endAngle, final int arcSamples, final int tubeSamples) {
    final boolean closed = endAngle - startAngle > 2 * Math.PI - 1e-6;
    // Open arcs need a final ring of vertices at endAngle; closed ones wrap back to the first.
    final int rings = closed ? arcSamples : arcSamples + 1;

    final Mesh mesh = new Mesh(name);
    final MeshData meshData = mesh.getMeshData();

    final int verts = rings * tubeSamples;
    final FloatBuffer vertexBuffer = BufferUtils.createVector3Buffer(verts);
    final FloatBuffer normalBuffer = BufferUtils.createVector3Buffer(verts);

    for (int i = 0; i < rings; i++) {
      final double theta = startAngle + (endAngle - startAngle) * i / arcSamples;
      final double cosTheta = Math.cos(theta);
      final double sinTheta = Math.sin(theta);
      for (int j = 0; j < tubeSamples; j++) {
        final double phi = 2 * Math.PI * j / tubeSamples;
        final double cosPhi = Math.cos(phi);
        final double sinPhi = Math.sin(phi);
        // normal = radial component in the arc plane + tube's out-of-plane component
        final double nx = cosPhi * cosTheta;
        final double ny = cosPhi * sinTheta;
        final double nz = sinPhi;
        normalBuffer.put((float) nx).put((float) ny).put((float) nz);
        vertexBuffer.put((float) (cosTheta * arcRadius + nx * tubeRadius))
            .put((float) (sinTheta * arcRadius + ny * tubeRadius)) //
            .put((float) (nz * tubeRadius));
      }
    }
    meshData.setVertexBuffer(vertexBuffer);
    meshData.setNormalBuffer(normalBuffer);

    final int quadRows = closed ? rings : rings - 1;
    final IndexBufferData<?> indices = BufferUtils.createIndexBufferData(quadRows * tubeSamples * 6, verts - 1);
    for (int i = 0; i < quadRows; i++) {
      final int rowA = i * tubeSamples;
      final int rowB = (i + 1) % rings * tubeSamples;
      for (int j = 0; j < tubeSamples; j++) {
        final int j2 = (j + 1) % tubeSamples;
        indices.put(rowA + j).put(rowB + j).put(rowB + j2);
        indices.put(rowA + j).put(rowB + j2).put(rowA + j2);
      }
    }
    meshData.setIndices(indices);

    return mesh;
  }
}
