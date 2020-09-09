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

import com.ardor3d.math.Triangle;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * Simple utility for performing subdivision on existing triangle mesh data.
 */
public class TriangleSubdivisionUtil {
  /**
   * Performs triangle subdivision on the provided vertices, with the assumption that they are
   * specified as triangles.
   *
   * @param vertices
   *          the triangle vertices. Additional vertices will be appended to this list as needed.
   * @param minArea
   *          do not subdivide triangles that have an area less than or equal to this value.
   * @return a new list of indices to use against the provided triangle vertices.
   * @throws IllegalArgumentException
   *           if our vertices list size is not divisible by 3.
   */
  public static LinkedList<Integer> subdivide(final List<ReadOnlyVector3> vertices, final double minArea) {
    final var indices = new LinkedList<Integer>();
    for (int i = 0, maxI = vertices.size(); i < maxI; i++) {
      indices.addLast(i);
    }
    subdivide(vertices, indices, minArea);
    return indices;
  }

  /**
   * Performs triangle subdivision on the provided vertices, interpreted as individual triangles via
   * the given indices.
   *
   * @param vertices
   *          the triangle vertices. Additional vertices will be appended to this list as needed.
   * @param indices
   *          the triangle indices. Additional indices will be inserted into this list as needed.
   * @param minArea
   *          do not subdivide triangles that have an area less than or equal to this value.
   * @throws IllegalArgumentException
   *           if our indices list size is not divisible by 3.
   */
  public static void subdivide(final List<ReadOnlyVector3> vertices, final LinkedList<Integer> indices,
      final double minArea) {
    if (indices.size() % 3 != 0) {
      throw new IllegalArgumentException("indices should describe triangles and thus be a size divisible by 3.");
    }
    final Triangle tri = Triangle.fetchTempInstance();
    try {
      int a, b, c, ctr;
      // walk through our indices
      for (int i = 0; i < indices.size(); i += 3) {
        // grab next triangle
        tri.setA(vertices.get(a = indices.get(i + 0)));
        tri.setB(vertices.get(b = indices.get(i + 1)));
        tri.setC(vertices.get(c = indices.get(i + 2)));

        // Are we small enough?
        if (tri.getArea() <= minArea) {
          continue;
        }

        // Not yet small enough, so let's break it down
        // Add triangle center to our vertices
        ctr = vertices.size();
        vertices.add(new Vector3(tri.getCenter()));

        // Remove current triangle indices and set back our current index
        indices.remove(i);
        indices.remove(i);
        indices.remove(i);
        i -= 3;

        // Add indices for our 3 sub-triangles
        // We add to the end so we can process siblings first before diving deeper.
        indices.addLast(a);
        indices.addLast(b);
        indices.addLast(ctr);

        indices.addLast(b);
        indices.addLast(c);
        indices.addLast(ctr);

        indices.addLast(c);
        indices.addLast(a);
        indices.addLast(ctr);
      }
    } finally {
      Triangle.releaseTempInstance(tri);
    }
  }
}
