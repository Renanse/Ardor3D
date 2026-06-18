/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import com.ardor3d.buffer.BufferUtils;

public class TestMeshReorder {

  /**
   * reorderVertexData must rebuild the normal working buffer from the normals, not from the vertex
   * coordinates. The reorder {1,1} never writes target slot 0, so that slot keeps its seeded value -
   * which exposes what the buffer was seeded from: the original normal (correct) vs. the vertex
   * coordinate (the bug).
   */
  @Test
  public void testReorderRebuildsNormalsFromNormals() {
    final Mesh mesh = new Mesh("m");
    final MeshData md = new MeshData();
    md.setVertexBuffer(BufferUtils.createFloatBuffer(new float[] {10, 10, 10, 20, 20, 20}));
    md.setNormalBuffer(BufferUtils.createFloatBuffer(new float[] {0, 0, 1, 0, 1, 0}));
    mesh.setMeshData(md);

    mesh.reorderVertexData(new int[] {1, 1}); // both source verts map to slot 1; slot 0 never written

    final float[] firstNormal = new float[3];
    mesh.getMeshData().getNormalBuffer().rewind();
    mesh.getMeshData().getNormalBuffer().get(firstNormal, 0, 3);

    // slot 0 keeps its seed: must be the original normal (0,0,1), not the vertex coord (10,10,10)
    assertArrayEquals(new float[] {0, 0, 1}, firstNormal, 0f);
  }
}
