/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.bounding;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Test;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;

public class TestCollisionTreeLeaf {

  private final int originalMax = CollisionTreeManager.getInstance().getMaxPrimitivesPerLeaf();

  @After
  public void restoreMax() {
    CollisionTreeManager.getInstance().setMaxPrimitivesPerLeaf(originalMax);
  }

  /**
   * A node holding exactly maxPrimitivesPerLeaf primitives must be a leaf. The leaf test used
   * _end - _start + 1 on a half-open [_start,_end) range, overcounting by one, so a full (== max)
   * node split needlessly (one level too deep; at max == 1 it recursed into itself forever).
   */
  @Test
  public void testNodeWithMaxPrimitivesIsLeaf() {
    CollisionTreeManager.getInstance().setMaxPrimitivesPerLeaf(2);

    final Mesh mesh = new Mesh("m");
    final MeshData md = new MeshData();
    // two triangles (6 vertices) -> getPrimitiveCount(0) == 2 == maxPrimitivesPerLeaf
    md.setVertexBuffer(BufferUtils.createFloatBuffer(new float[] { //
        0, 0, 0, 1, 0, 0, 0, 1, 0, // triangle 0
        0, 0, 1, 1, 0, 1, 0, 1, 1})); // triangle 1
    mesh.setMeshData(md);

    final CollisionTree tree = new CollisionTree(CollisionTree.Type.AABB);
    tree.construct(mesh, false);

    assertNotNull(tree._bounds);
    assertNull("a node with exactly maxPrimitivesPerLeaf primitives should be a leaf", tree._left);
    assertNull(tree._right);
  }
}
