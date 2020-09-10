/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.util;

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
  public static LinkedList<Integer> subdivide(final Method method, final List<ReadOnlyVector3> vertices,
      final Predicate<Triangle> stopCriteria) {
    final var indices = new LinkedList<Integer>();
    for (int i = 0, maxI = vertices.size(); i < maxI; i++) {
      indices.addLast(i);
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
  public static void subdivide(final Method method, final List<ReadOnlyVector3> vertices,
      final LinkedList<Integer> indices, final Predicate<Triangle> stopCriteria) {
    if (indices.size() % 3 != 0) {
      throw new IllegalArgumentException("indices should describe triangles and thus be a size divisible by 3.");
    }

    // initialize our depth list - used for tracking how many splits we have done to get to a given
    // triangle.
    final var depthList = new LinkedList<Integer>();
    for (int i = 0, maxI = indices.size() / 3; i < maxI; i++) {
      depthList.add(0);
    }

    final Triangle tri = Triangle.fetchTempInstance();
    try {
      int a, b, c, ctr, ab, bc, ca, depth;
      // walk through our indices
      for (int i = 0; i < indices.size(); i += 3) {
        // grab next triangle
        tri.setA(vertices.get(a = indices.get(i + 0)));
        tri.setB(vertices.get(b = indices.get(i + 1)));
        tri.setC(vertices.get(c = indices.get(i + 2)));
        tri.setIndex(depth = depthList.get(i / 3));

        // Are we small enough?
        if (stopCriteria.test(tri)) {
          continue;
        }

        // Not yet small enough, so let's break it down via the requested method
        if (method == Method.TriangleCenter) {
          // Add triangle center to our vertices
          ctr = vertices.size();
          vertices.add(new Vector3(tri.getCenter()));

          // Remove current triangle indices and set back our current index
          removeCurrentTriangle(indices, depthList, i);
          i -= 3;

          addTriangle(indices, depthList, a, b, ctr, depth + 1);
          addTriangle(indices, depthList, b, c, ctr, depth + 1);
          addTriangle(indices, depthList, c, a, ctr, depth + 1);
        }

        else if (method == Method.EdgeCenter) {
          // add edge centers to our vertices
          ab = vertices.size();
          vertices.add(tri.getA().add(tri.getB(), null).multiplyLocal(0.5));
          bc = vertices.size();
          vertices.add(tri.getB().add(tri.getC(), null).multiplyLocal(0.5));
          ca = vertices.size();
          vertices.add(tri.getC().add(tri.getA(), null).multiplyLocal(0.5));

          // Remove current triangle indices and set back our current index
          removeCurrentTriangle(indices, depthList, i);
          i -= 3;

          // Add indices for our 4 sub-triangles
          // We add to the end so we can process siblings first before diving deeper.
          addTriangle(indices, depthList, a, ab, ca, depth + 1);
          addTriangle(indices, depthList, ab, b, bc, depth + 1);
          addTriangle(indices, depthList, bc, c, ca, depth + 1);
          addTriangle(indices, depthList, ab, bc, ca, depth + 1);
        }

        else {
          throw new IllegalArgumentException("Unhandled method: " + method);
        }
      }
    } finally {
      Triangle.releaseTempInstance(tri);
    }
  }

  protected static void removeCurrentTriangle(final LinkedList<Integer> indices, final LinkedList<Integer> depthList,
      final int i) {
    indices.remove(i);
    indices.remove(i);
    indices.remove(i);
    depthList.remove(i / 3);
  }

  protected static void addTriangle(final LinkedList<Integer> indices, final LinkedList<Integer> depthList, final int a,
      final int b, final int c, final int depth) {
    indices.addLast(a);
    indices.addLast(b);
    indices.addLast(c);
    depthList.add(depth);
  }
}
