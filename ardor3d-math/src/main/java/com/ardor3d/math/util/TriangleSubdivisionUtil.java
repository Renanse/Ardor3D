/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import com.ardor3d.math.Triangle;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * Simple utility for performing subdivision on existing triangle mesh data.
 */
public class TriangleSubdivisionUtil {

  public enum Method {
    /**
     * Subdivide triangles by adding a point in the center of each of the triangle's edges and
     * connecting those 3 points to form 4 new triangles.
     */
    EdgeCenter,

    /**
     * Subdivide triangles by adding a point at the triangle's center and connecting that to each of the
     * three corners, forming 3 new triangles.
     */
    TriangleCenter,
  }

  protected static class SimpleTri {
    public int a, b, c, depth;
  }

  /**
   * Performs triangle subdivision on the provided vertices, with the assumption that they are
   * specified as triangles.
   *
   * @param method
   *          the subdivision method to use.
   * @param vertices
   *          the triangle vertices. Additional vertices will be appended to this list as needed.
   * @param stopCriteria
   *          logic detailing when we should stop subdividing a triangle. The triangle pass will have
   *          its points set to the three corners and its index set to the current depth traversed by
   *          the algorithm.
   * @return a new list of indices to use against the provided triangle vertices.
   * @throws IllegalArgumentException
   *           if our vertices list size is not divisible by 3 or the subdivision method passed is not
   *           currently supported.
   */
  public static List<Integer> subdivide(final Method method, final List<ReadOnlyVector3> vertices,
      final Predicate<Triangle> stopCriteria) {
    final var indices = new ArrayList<Integer>();
    for (int i = 0, maxI = vertices.size(); i < maxI; i++) {
      indices.add(i);
    }
    subdivide(method, vertices, indices, stopCriteria);
    return indices;
  }

  /**
   * Performs triangle subdivision on the provided vertices, interpreted as individual triangles via
   * the given indices.
   *
   * @param method
   *          the subdivision method to use.
   * @param vertices
   *          the triangle vertices. Additional vertices will be appended to this list as needed.
   * @param indices
   *          the triangle indices. Additional indices will be inserted into this list as needed.
   * @param stopCriteria
   *          logic detailing when we should stop subdividing a triangle. The triangle pass will have
   *          its points set to the three corners and its index set to the current depth traversed by
   *          the algorithm.
   * @throws IllegalArgumentException
   *           if our vertices list size is not divisible by 3 or the subdivision method passed is not
   *           currently supported.
   */
  public static void subdivide(final Method method, final List<ReadOnlyVector3> vertices, final List<Integer> indices,
      final Predicate<Triangle> stopCriteria) {
    if (indices.size() % 3 != 0) {
      throw new IllegalArgumentException("indices should describe triangles and thus be a size divisible by 3.");
    }

    // set up our triangle data to work on.
    final var workingTris = new LinkedList<SimpleTri>();
    for (final var it = indices.iterator(); it.hasNext();) {
      final var tri = new SimpleTri();
      tri.a = it.next();
      tri.b = it.next();
      tri.c = it.next();
      tri.depth = 0;
      workingTris.add(tri);
    }

    // clear out the indices we were given so we can write in new data.
    indices.clear();

    final Triangle testTri = Triangle.fetchTempInstance();
    try {
      int a, b, c, ctr, ab, bc, ca, depth;
      // walk through our triangles
      while (!workingTris.isEmpty()) {
        final var tri = workingTris.poll();

        // setup our math triangle for testing
        testTri.setA(vertices.get(a = tri.a));
        testTri.setB(vertices.get(b = tri.b));
        testTri.setC(vertices.get(c = tri.c));
        testTri.setIndex(depth = tri.depth);

        // Test if the triangle done?
        if (stopCriteria.test(testTri)) {
          // save indices to output
          indices.add(a);
          indices.add(b);
          indices.add(c);
          continue;
        }

        // Not yet small enough, so let's break it down via the requested method
        if (method == Method.TriangleCenter) {
          // Add triangle center to our vertices
          ctr = vertices.size();
          vertices.add(new Vector3(testTri.getCenter()));

          // Add 3 sub-triangles
          workingTris.add(makeTriangle(a, b, ctr, depth + 1));
          workingTris.add(makeTriangle(b, c, ctr, depth + 1));
          workingTris.add(makeTriangle(c, a, ctr, depth + 1));
        }

        else if (method == Method.EdgeCenter) {
          // add edge centers to our vertices
          ab = vertices.size();
          bc = ab + 1;
          ca = bc + 1;
          vertices.add(testTri.getA().add(testTri.getB(), null).multiplyLocal(0.5));
          vertices.add(testTri.getB().add(testTri.getC(), null).multiplyLocal(0.5));
          vertices.add(testTri.getC().add(testTri.getA(), null).multiplyLocal(0.5));

          // Add 4 sub-triangles
          workingTris.add(makeTriangle(a, ab, ca, depth + 1));
          workingTris.add(makeTriangle(ab, b, bc, depth + 1));
          workingTris.add(makeTriangle(bc, c, ca, depth + 1));
          workingTris.add(makeTriangle(ab, bc, ca, depth + 1));
        }

        else {
          throw new IllegalArgumentException("Unhandled method: " + method);
        }
      }
    } finally {
      Triangle.releaseTempInstance(testTri);
    }
  }

  private static SimpleTri makeTriangle(final int a, final int b, final int c, final int depth) {
    final var tri = new SimpleTri();
    tri.a = a;
    tri.b = b;
    tri.c = c;
    tri.depth = depth;
    return tri;
  }
}
