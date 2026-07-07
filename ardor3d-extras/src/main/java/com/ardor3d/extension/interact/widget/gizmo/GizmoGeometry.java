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
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;

/**
 * Mesh builders for gizmo handle geometry that the stock shapes do not cover.
 */
public final class GizmoGeometry {

  private GizmoGeometry() {}

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
