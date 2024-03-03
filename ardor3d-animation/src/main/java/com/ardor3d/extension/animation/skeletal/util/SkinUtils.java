/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.util;

import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.scenegraph.Spatial;

/**
 * General utility methods useful for Skin manipulation.
 */
public class SkinUtils {

  /**
   * Simple utility to turn on / off bounding volume updating on skinned mesh objects in a given
   * scenegraph.
   *
   * @param root
   *          the root node on the scenegraph
   * @param doUpdate
   *          if true, skinned mesh objects will automatically update their model bounds when applying
   *          pose.
   */
  public static void setAutoUpdateBounds(final Spatial root, final boolean doUpdate) {
    root.acceptVisitor((final Spatial spatial) -> {
      // we only care about SkinnedMesh
      if (spatial instanceof SkinnedMesh) {
        ((SkinnedMesh) spatial).setAutoUpdateSkinBounds(doUpdate);
      }
    }, true);
  }

  /**
   * Convert a short array to a float array
   *
   * @param shorts
   *          the short values
   * @return our new float array
   */
  public static float[] convertToFloat(final short... shorts) {
    final float[] rval = new float[shorts.length];
    for (int i = 0; i < rval.length; i++) {
      rval[i] = shorts[i];
    }
    return rval;
  }

  /**
   * Rearrange the data from data per element, to a list of matSide x matSide matrices, output by row
   * as such:
   *
   * row0element0, row0element1, row0element2...<br>
   * row1element0, row1element1, row1element2...<br>
   * row2element0, row2element1, row2element2...<br>
   *
   * If there is not enough values in the source data to fill out a row, 0 is used.
   *
   * @param src
   *          our source data, stored as element0, element1, etc.
   * @param srcElementSize
   *          the number of values per element in our source data
   * @param matSide
   *          the size of the matrix edge... eg. 4 would mean a 4x4 matrix.
   * @return our new data array.
   */
  public static float[] reorderAndPad(final float[] src, final int srcElementSize, final int matSide) {
    final int elements = src.length / srcElementSize;
    final float[] rVal = new float[elements * matSide * matSide];

    // size of each attribute (a row from each element)
    final int length = matSide * elements;

    for (int i = 0; i < elements; i++) {
      // index into src for our element data
      final int srcStart = i * srcElementSize;
      // index into a destination row.
      final int dstOffset = i * matSide;

      // Go through each row of the current src element. Go through only as many rows of data as we have.
      // (eg. if size is 6 and matSide is 4, we only need to go through j=0 and j=1)
      for (int j = 0; j <= (srcElementSize - 1) / matSide; j++) {
        // How much to copy. Generally matSide, except for last bit of data.
        final int copySize = Math.min(srcElementSize - j * matSide, matSide);
        // Copy the data from src to rVal
        System.arraycopy(src, srcStart + j * matSide, rVal, j * length + dstOffset, copySize);
      }
    }

    return rVal;
  }

  /**
   * Expand out our src data so that each attribute is a certain size, padding with 0's as needed. If
   * src is already correct size, we just return that without creating a new data array.
   *
   * @param src
   *          our source data, stored as element0, element1, etc.
   * @param srcElementSize
   *          the number of values per element in our source data
   * @param attribSize
   *          the desired size of each element in the return array.
   * @return the padded array.
   */
  public static float[] pad(final float[] src, final int srcElementSize, final int attribSize) {
    if (srcElementSize == attribSize) {
      return src;
    }
    final int elements = src.length / srcElementSize;
    final float[] rVal = new float[elements * attribSize];

    for (int i = 0; i < elements; i++) {
      // index into src for our element data
      final int srcStart = i * srcElementSize;
      // index into rVal to store
      final int dstStart = i * attribSize;
      // Copy the data from src to rVal
      System.arraycopy(src, srcStart, rVal, dstStart, srcElementSize);
    }

    return rVal;
  }

  /**
   * Expand out our src data so that each attribute is a certain size, padding with 0's as needed. If
   * src is already correct size, we just return that without creating a new data array.
   *
   * @param src
   *          our source data, stored as element0, element1, etc.
   * @param srcElementSize
   *          the number of values per element in our source data
   * @param attribSize
   *          the desired size of each element in the return array.
   * @return the padded array.
   */
  public static short[] pad(final short[] src, final int srcElementSize, final int attribSize) {
    if (srcElementSize == attribSize) {
      return src;
    }
    final int elements = src.length / srcElementSize;
    final short[] rVal = new short[elements * attribSize];

    for (int i = 0; i < elements; i++) {
      // index into src for our element data
      final int srcStart = i * srcElementSize;
      // index into rVal to store
      final int dstStart = i * attribSize;
      // Copy the data from src to rVal
      System.arraycopy(src, srcStart, rVal, dstStart, srcElementSize);
    }

    return rVal;
  }
}
