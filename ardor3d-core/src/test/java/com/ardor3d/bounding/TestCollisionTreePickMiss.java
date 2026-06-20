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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.intersection.IntersectionRecord;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;

/**
 * Companion to {@link com.ardor3d.intersection.TestPrimitivePickMiss}, which exercises only a
 * single-triangle mesh (whose CollisionTree root <i>is</i> the leaf). This one forces the tree to
 * split so the pick descends through internal nodes before reaching the leaf that does the actual
 * ray/triangle test, then verifies the same invariant survives recursion: a ray that crosses the
 * bounds but misses every triangle records no hit, and a real hit is still found.
 * <p>
 * Geometry: triangle 0 lies on the plane z == y with corners (0,0,0),(4,0,0),(0,4,4) -- bounds
 * [0,4]^3, covering only the half where u + v &lt;= 1. Triangles 1-3 are far away on +X so the
 * descent and the ray/triangle tests are isolated to triangle 0. A ray straight down through
 * (3,3,*) crosses triangle 0's leaf bounds yet misses the triangle; one through (1,1,*) hits it.
 */
public class TestCollisionTreePickMiss {

  private final int originalMax = CollisionTreeManager.getInstance().getMaxPrimitivesPerLeaf();

  @After
  public void restoreMax() {
    CollisionTreeManager.getInstance().setMaxPrimitivesPerLeaf(originalMax);
  }

  /** Four triangles; with max-primitives-per-leaf forced to 1 the tree must split. */
  private static Mesh fourTriangles() {
    final Mesh mesh = new Mesh("tris");
    final MeshData md = new MeshData();
    md.setVertexBuffer(BufferUtils.createFloatBuffer(new float[] { //
        0, 0, 0, 4, 0, 0, 0, 4, 4, // triangle 0  -> bounds [0,4]^3, the one under test
        100, 0, 0, 104, 0, 0, 100, 4, 4, // triangle 1  (far on +X)
        110, 0, 0, 114, 0, 0, 110, 4, 4, // triangle 2  (far on +X)
        120, 0, 0, 124, 0, 0, 120, 4, 4})); // triangle 3  (far on +X)
    mesh.setMeshData(md);
    mesh.setModelBound(new BoundingBox());
    mesh.updateModelBound();
    mesh.updateWorldTransform(true);
    mesh.updateWorldBound(true);
    return mesh;
  }

  private static Ray3 downAt(final double x, final double y) {
    return new Ray3(new Vector3(x, y, 5), new Vector3(0, 0, -1));
  }

  @Test
  public void missThroughRecursionIsNotAPick() {
    CollisionTreeManager.getInstance().setMaxPrimitivesPerLeaf(1);
    final Mesh mesh = fourTriangles();

    // Non-vacuousness: prove the tree genuinely has internal nodes, so the pick really does recurse
    // rather than hitting a single leaf (which is the single-triangle case covered elsewhere).
    final CollisionTree tree = CollisionTreeManager.getInstance().getCollisionTree(mesh);
    assertNotNull(tree);
    assertNotNull("max-primitives-per-leaf == 1 with 4 triangles must split the tree", tree._left);
    assertNotNull(tree._right);

    final Ray3 miss = downAt(3, 3);
    assertTrue("ray should cross the world bounds", mesh.intersectsWorldBound(miss));

    final IntersectionRecord record = mesh.intersectsPrimitivesWhere(miss);
    assertTrue("a bounds-crossing ray that misses every triangle must record no hit",
        record == null || record.getNumberOfIntersections() == 0);

    final PrimitivePickResults results = new PrimitivePickResults();
    results.addPick(miss, mesh);
    assertEquals("no pick should be added for a ray that misses every triangle", 0,
        results.getNumber());
  }

  @Test
  public void hitThroughRecursionIsFound() {
    CollisionTreeManager.getInstance().setMaxPrimitivesPerLeaf(1);
    final Mesh mesh = fourTriangles();

    final Ray3 hit = downAt(1, 1);
    final IntersectionRecord record = mesh.intersectsPrimitivesWhere(hit);
    assertNotNull(record);
    assertEquals(1, record.getNumberOfIntersections());
    assertTrue("distance must be finite", Double.isFinite(record.getClosestDistance()));
    assertEquals(4.0, record.getClosestDistance(), 1.0e-9);

    final PrimitivePickResults results = new PrimitivePickResults();
    results.addPick(hit, mesh);
    assertEquals(1, results.getNumber());
  }
}
