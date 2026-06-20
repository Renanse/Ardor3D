/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.intersection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;

/**
 * Exercises the reported "ray that misses every triangle but crosses the mesh bounds is still
 * recorded as a pick (at distance infinity)" scenario against the real Mesh / CollisionTree pick
 * path.
 * <p>
 * The single triangle lies on the plane z == y with corners A(0,0,0), B(4,0,0), C(0,4,4), so its
 * axis-aligned bounds are the full cube [0,4]^3 while the triangle itself only covers the half where
 * u + v &lt;= 1. A ray fired straight down through (3,3,*) therefore passes through the bounds but
 * misses the triangle; a ray through (1,1,*) hits it.
 */
public class TestPrimitivePickMiss {

  private static Mesh singleTriangle() {
    final Mesh mesh = new Mesh("tri");
    final MeshData md = new MeshData();
    md.setVertexBuffer(BufferUtils.createFloatBuffer(new float[] { //
        0, 0, 0, // A
        4, 0, 0, // B
        0, 4, 4})); // C
    mesh.setMeshData(md);
    mesh.setModelBound(new BoundingBox());
    mesh.updateModelBound();
    mesh.updateWorldTransform(true);
    mesh.updateWorldBound(true);
    return mesh;
  }

  /** Origin above (x,y,5) aimed straight down -Z. */
  private static Ray3 downAt(final double x, final double y) {
    return new Ray3(new Vector3(x, y, 5), new Vector3(0, 0, -1));
  }

  /**
   * A ray that crosses the world bounds but misses every triangle must NOT register as a primitive
   * pick. This is the core of bug report #1: if intersectsPrimitivesWhere recorded the candidate at
   * distance infinity, the record would be non-null with a "hit" and addPick would accept it.
   */
  @Test
  public void missWithinBoundsIsNotAPick() {
    final Mesh mesh = singleTriangle();
    final Ray3 miss = downAt(3, 3);

    // Sanity: the ray really does cross the mesh's world bounds (otherwise the test proves nothing).
    assertTrue("ray should cross the world bounds", mesh.intersectsWorldBound(miss));

    // The actual claim under test: no triangle is hit, so no hit may be recorded. We assert the
    // observable contract ("no hit recorded") rather than the exact null-vs-empty-record encoding, so
    // a behavior-preserving refactor (e.g. returning a zero-length record instead of null) does not
    // false-alarm. The reported bug would instead yield a non-null record whose
    // getNumberOfIntersections() counts the ray-missed candidate(s) -> still caught here.
    final IntersectionRecord record = mesh.intersectsPrimitivesWhere(miss);
    assertTrue("a bounds-crossing ray that misses all triangles must record no hit",
        record == null || record.getNumberOfIntersections() == 0);

    // And end-to-end through the live pick path.
    final PrimitivePickResults results = new PrimitivePickResults();
    results.addPick(miss, mesh);
    assertEquals("no pick should be added for a ray that misses every triangle", 0,
        results.getNumber());
  }

  /** A ray that actually strikes the triangle yields exactly one finite intersection. */
  @Test
  public void hitProducesSingleFiniteIntersection() {
    final Mesh mesh = singleTriangle();
    final Ray3 hit = downAt(1, 1);

    final IntersectionRecord record = mesh.intersectsPrimitivesWhere(hit);
    assertNotNull(record);
    assertEquals(1, record.getNumberOfIntersections());

    final double distance = record.getClosestDistance();
    assertTrue("distance must be finite", Double.isFinite(distance));
    assertEquals(4.0, distance, 1.0e-9);

    final Vector3 point = record.getIntersectionPoint(record.getClosestIntersection());
    assertTrue("intersection point must be finite", Vector3.isFinite(point));
    assertEquals(1.0, point.getX(), 1.0e-9);
    assertEquals(1.0, point.getY(), 1.0e-9);
    assertEquals(1.0, point.getZ(), 1.0e-9);

    final PrimitivePickResults results = new PrimitivePickResults();
    results.addPick(hit, mesh);
    assertEquals(1, results.getNumber());
  }
}
