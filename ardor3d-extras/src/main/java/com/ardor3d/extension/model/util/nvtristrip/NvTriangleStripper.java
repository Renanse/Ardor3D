/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.util.nvtristrip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.buffer.IntBufferData;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.visitor.Visitor;

/**
 * Ported from <a href="http://developer.nvidia.com/object/nvtristrip_library.html">NVIDIA's
 * NvTriStrip Library</a>
 */
public class NvTriangleStripper implements Visitor {
  /** GeForce1 and 2 cache size */
  public static final int CACHESIZE_GEFORCE1_2 = 16;

  /** GeForce3 cache size */
  public static final int CACHESIZE_GEFORCE3 = 24;

  private int _cacheSize = NvTriangleStripper.CACHESIZE_GEFORCE3;
  private int _minStripSize = 0;
  private int _restartVal = 0;
  private boolean _stitchStrips = true;
  private boolean _listsOnly = false;
  private boolean _restart = false;
  private boolean _reorderVertices = false;

  /**
   * For GPUs that support primitive restart, this sets a value as the restart index
   * 
   * Restart is meaningless if strips are not being stitched together, so enabling restart makes
   * NvTriStrip forcing stitching. So, you'll get back one strip.
   * 
   * @param restartVal
   */
  public void enableRestart(final int restartVal) {
    _restart = true;
    _restartVal = restartVal;
  }

  /**
   * For GPUs that support primitive restart, this disables using primitive restart
   */
  public void disableRestart() {
    _restart = false;
  }

  public boolean isRestart() { return _restart; }

  /**
   * Sets the cache size which the stripfier uses to optimize the data. Controls the length of the
   * generated individual strips. This is the "actual" cache size, so 24 for GeForce3 and 16 for
   * GeForce1/2 You may want to play around with this number to tweak performance.
   * 
   * @param cacheSize
   *          (Default value: 24)
   */
  public void setCacheSize(final int cacheSize) { _cacheSize = cacheSize; }

  public int getCacheSize() { return _cacheSize; }

  /**
   * boolean to indicate whether to stitch together strips into one huge strip or not. If set to true,
   * you'll get back one huge strip stitched together using degenerate triangles. If set to false,
   * you'll get back a large number of separate strips.
   * 
   * @param bStitchStrips
   *          (Default value: true)
   */
  public void setStitchStrips(final boolean bStitchStrips) { _stitchStrips = bStitchStrips; }

  public boolean isStitchStrips() { return _stitchStrips; }

  /**
   * 
   * Sets the minimum acceptable size for a strip, in triangles. All strips generated which are
   * shorter than this will be thrown into one big, separate list.
   * 
   * @param minSize
   *          (Default value: 0)
   */
  public void setMinStripSize(final int minSize) { _minStripSize = minSize; }

  public int getMinStripSize() { return _minStripSize; }

  /**
   * If set to true, will return an optimized list, with no strips at all.
   * 
   * @param bListsOnly
   *          (Default value: false)
   */
  public void setListsOnly(final boolean bListsOnly) { _listsOnly = bListsOnly; }

  public boolean isListsOnly() { return _listsOnly; }

  /**
   * If set to true, will call remapIndices after generateStrips.
   * 
   * @param reorder
   *          (Default value: false)
   * @see #remapIndices(PrimitiveGroup[], AtomicReference, int)
   */
  public void setReorderVertices(final boolean reorder) { _reorderVertices = reorder; }

  public boolean isReorderVertices() { return _reorderVertices; }

  /**
   * 
   * Returns true if the two triangles defined by firstTri and secondTri are the same The "same" is
   * defined in this case as having the same indices with the same winding order
   * 
   * @param firstTri0
   * @param firstTri1
   * @param firstTri2
   * @param secondTri0
   * @param secondTri1
   * @param secondTri2
   * @return
   */
  boolean sameTriangle(final int firstTri0, final int firstTri1, final int firstTri2, final int secondTri0,
      final int secondTri1, final int secondTri2) {
    boolean isSame = false;

    if (firstTri0 == secondTri0) {
      if (firstTri1 == secondTri1) {
        if (firstTri2 == secondTri2) {
          isSame = true;
        }
      }
    } else if (firstTri0 == secondTri1) {
      if (firstTri1 == secondTri2) {
        if (firstTri2 == secondTri0) {
          isSame = true;
        }
      }
    } else if (firstTri0 == secondTri2) {
      if (firstTri1 == secondTri0) {
        if (firstTri2 == secondTri1) {
          isSame = true;
        }
      }
    }

    return isSame;
  }

  boolean testTriangle(final int v0, final int v1, final int v2, final List<NvFaceInfo> in_bins[], final int NUMBINS) {
    // hash this triangle
    boolean isLegit = false;
    int ctr = v0 % NUMBINS;
    NvFaceInfo face;
    for (int k = 0; k < in_bins[ctr].size(); ++k) {
      // check triangles in this bin
      face = in_bins[ctr].get(k);
      if (sameTriangle(face._v0, face._v1, face._v2, v0, v1, v2)) {
        isLegit = true;
        break;
      }
    }
    if (!isLegit) {
      ctr = v1 % NUMBINS;
      for (int k = 0; k < in_bins[ctr].size(); ++k) {
        face = in_bins[ctr].get(k);
        // check triangles in this bin
        if (sameTriangle(face._v0, face._v1, face._v2, v0, v1, v2)) {
          isLegit = true;
          break;
        }
      }

      if (!isLegit) {
        ctr = v2 % NUMBINS;
        for (int k = 0; k < in_bins[ctr].size(); ++k) {
          face = in_bins[ctr].get(k);
          // check triangles in this bin
          if (sameTriangle(face._v0, face._v1, face._v2, v0, v1, v2)) {
            isLegit = true;
            break;
          }
        }

      }
    }

    return isLegit;
  }

  /**
   * 
   * @param in_indices
   *          input index list, the indices you would use to render
   * @param validate
   * @return array of optimized/stripified PrimitiveGroups
   */
  @SuppressWarnings("unchecked")
  public PrimitiveGroup[] generateStrips(final int[] in_indices, final boolean validate) {
    if (in_indices == null || in_indices.length == 0) {
      return new PrimitiveGroup[0];
    }

    int numGroups = 0;
    PrimitiveGroup[] primGroups;

    // put data in format that the stripifier likes
    final List<Integer> tempIndices = new ArrayList<>();
    int maxIndex = 0;
    for (int i = 0; i < in_indices.length; i++) {
      tempIndices.add(in_indices[i]);
      if (in_indices[i] > maxIndex) {
        maxIndex = in_indices[i];
      }
    }
    final List<NvStripInfo> tempStrips = new ArrayList<>();
    final List<NvFaceInfo> tempFaces = new ArrayList<>();

    final NvStripifier stripifier = new NvStripifier();

    // do actual stripification
    stripifier.stripify(tempIndices, _cacheSize, _minStripSize, maxIndex, tempStrips, tempFaces);

    // stitch strips together
    final List<Integer> stripIndices = new ArrayList<>();
    int numSeparateStrips = 0;

    if (_listsOnly) {
      // if we're outputting only lists, we're done
      numGroups = 1;
      primGroups = new PrimitiveGroup[numGroups];
      primGroups[0] = new PrimitiveGroup();
      final PrimitiveGroup[] primGroupArray = primGroups;

      // count the total number of indices
      int numIndices = 0;
      for (int i = 0; i < tempStrips.size(); i++) {
        numIndices += tempStrips.get(i)._faces.size() * 3;
      }

      // add in the list
      numIndices += tempFaces.size() * 3;

      primGroupArray[0].setType(IndexMode.Triangles);
      primGroupArray[0].setIndices(new int[numIndices]);
      primGroupArray[0].setNumIndices(numIndices);

      // do strips
      int indexCtr = 0;
      for (final NvStripInfo strip : tempStrips) {
        for (final NvFaceInfo face : strip._faces) {
          // degenerates are of no use with lists
          if (!NvStripifier.isDegenerate(face)) {
            primGroupArray[0]._getIndices()[indexCtr++] = face._v0;
            primGroupArray[0]._getIndices()[indexCtr++] = face._v1;
            primGroupArray[0]._getIndices()[indexCtr++] = face._v2;
          } else {
            // we've removed a tri, reduce the number of indices
            primGroupArray[0].setNumIndices(primGroupArray[0].getNumIndices() - 3);
          }
        }
      }

      // do lists
      for (final NvFaceInfo face : tempFaces) {
        primGroupArray[0]._getIndices()[indexCtr++] = face._v0;
        primGroupArray[0]._getIndices()[indexCtr++] = face._v1;
        primGroupArray[0]._getIndices()[indexCtr++] = face._v2;
      }
    } else {
      numSeparateStrips = stripifier.createStrips(tempStrips, stripIndices, _stitchStrips, _restart, _restartVal);

      // if we're stitching strips together, we better get back only one strip from createStrips()
      assert _stitchStrips && numSeparateStrips == 1 || !_stitchStrips;

      // convert to output format
      numGroups = numSeparateStrips; // for the strips
      if (tempFaces.size() != 0) {
        numGroups++;
      } // we've got a list as well, increment
      primGroups = new PrimitiveGroup[numGroups];
      for (int i = 0; i < primGroups.length; i++) {
        primGroups[i] = new PrimitiveGroup();
      }
      final PrimitiveGroup[] primGroupArray = primGroups;

      // first, the strips
      int startingLoc = 0;
      for (int stripCtr = 0; stripCtr < numSeparateStrips; stripCtr++) {
        int stripLength = 0;

        if (!_stitchStrips) {
          int i = startingLoc;
          // if we've got multiple strips, we need to figure out the correct length
          for (; i < stripIndices.size(); i++) {
            if (stripIndices.get(i) == -1) {
              break;
            }
          }

          stripLength = i - startingLoc;
        } else {
          stripLength = stripIndices.size();
        }

        primGroupArray[stripCtr].setType(IndexMode.TriangleStrip);
        primGroupArray[stripCtr].setIndices(new int[stripLength]);
        primGroupArray[stripCtr].setNumIndices(stripLength);

        int indexCtr = 0;
        for (int i = startingLoc; i < stripLength + startingLoc; i++) {
          primGroupArray[stripCtr]._getIndices()[indexCtr++] = stripIndices.get(i);
        }

        // we add 1 to account for the -1 separating strips
        // this doesn't break the stitched case since we'll exit the loop
        startingLoc += stripLength + 1;
      }

      // next, the list
      if (tempFaces.size() != 0) {
        final int faceGroupLoc = numGroups - 1; // the face group is the last one
        primGroupArray[faceGroupLoc].setType(IndexMode.Triangles);
        primGroupArray[faceGroupLoc].setIndices(new int[tempFaces.size() * 3]);
        primGroupArray[faceGroupLoc].setNumIndices(tempFaces.size() * 3);
        int indexCtr = 0;
        for (final NvFaceInfo face : tempFaces) {
          primGroupArray[faceGroupLoc]._getIndices()[indexCtr++] = face._v0;
          primGroupArray[faceGroupLoc]._getIndices()[indexCtr++] = face._v1;
          primGroupArray[faceGroupLoc]._getIndices()[indexCtr++] = face._v2;
        }
      }
    }

    // validate generated data against input
    if (validate) {
      final int NUMBINS = 100;

      final List<NvFaceInfo> in_bins[] = new List[NUMBINS];
      for (int i = 0; i < NUMBINS; i++) {
        in_bins[i] = new ArrayList<>();
      }

      // hash input indices on first index
      for (int i = 0; i < in_indices.length; i += 3) {
        final NvFaceInfo faceInfo = new NvFaceInfo(in_indices[i], in_indices[i + 1], in_indices[i + 2]);
        in_bins[in_indices[i] % NUMBINS].add(faceInfo);
      }

      for (int i = 0; i < numGroups; ++i) {
        switch (primGroups[i].getType()) {
          case Triangles: {
            for (int j = 0; j < primGroups[i].getNumIndices(); j += 3) {
              final int v0 = primGroups[i]._getIndices()[j];
              final int v1 = primGroups[i]._getIndices()[j + 1];
              final int v2 = primGroups[i]._getIndices()[j + 2];

              // ignore degenerates
              if (NvStripifier.isDegenerate(v0, v1, v2)) {
                continue;
              }

              if (!testTriangle(v0, v1, v2, in_bins, NUMBINS)) {
                throw new IllegalStateException("failed validation");
              }
            }
            break;
          }

          case TriangleStrip: {
            boolean flip = false;
            for (int j = 2; j < primGroups[i].getNumIndices(); ++j) {
              final int v0 = primGroups[i]._getIndices()[j - 2];
              int v1 = primGroups[i]._getIndices()[j - 1];
              int v2 = primGroups[i]._getIndices()[j];

              if (flip) {
                // swap v1 and v2
                final int swap = v1;
                v1 = v2;
                v2 = swap;
              }

              // ignore degenerates
              if (NvStripifier.isDegenerate(v0, v1, v2)) {
                flip = !flip;
                continue;
              }

              if (!testTriangle(v0, v1, v2, in_bins, NUMBINS)) {
                throw new IllegalStateException("failed validation");
              }

              flip = !flip;
            }
            break;
          }

          case TriangleFan:
          default:
            break;
        }
      }

    }

    return primGroups;
  }

  /**
   * Function to remap your indices to improve spatial locality in your vertex buffer.
   * 
   * Note that you must reorder your vertex buffer according to the remapping handed back to you.
   * 
   * Credit goes to the MS Xbox crew for the idea for this interface.
   * 
   * @param in_primGroups
   *          array of PrimitiveGroups you want remapped
   * @param numVerts
   *          number of vertices in your vertex buffer, also can be thought of as the range of
   *          acceptable values for indices in your primitive groups.
   * @return index remap. old index is key into array, value there is the old location for the vertex.
   *         -1 means vertex was never referenced
   */
  public PrimitiveGroup[] remapIndices(final PrimitiveGroup[] in_primGroups,
      final AtomicReference<int[]> remappedVertices, final int numVerts) {
    final PrimitiveGroup[] remappedGroups = new PrimitiveGroup[in_primGroups.length];

    // caches oldIndex --> newIndex conversion
    final int[] indexCache = new int[numVerts];
    Arrays.fill(indexCache, -1);

    // loop over primitive groups
    int indexCtr = 0;
    for (int i = 0; i < in_primGroups.length; i++) {
      final int numIndices = in_primGroups[i].getNumIndices();

      // init remapped group
      remappedGroups[i] = new PrimitiveGroup();
      remappedGroups[i].setType(in_primGroups[i].getType());
      remappedGroups[i].setNumIndices(numIndices);
      remappedGroups[i].setIndices(new int[numIndices]);

      for (int j = 0; j < numIndices; j++) {
        final int cachedIndex = indexCache[in_primGroups[i]._getIndices()[j]];
        if (cachedIndex == -1) // we haven't seen this index before
        {
          // point to "last" vertex in VB
          remappedGroups[i]._getIndices()[j] = indexCtr;

          // add to index cache, increment
          indexCache[in_primGroups[i]._getIndices()[j]] = indexCtr++;
        } else {
          // we've seen this index before
          remappedGroups[i]._getIndices()[j] = cachedIndex;
        }
      }
    }
    if (remappedVertices != null) {
      remappedVertices.set(indexCache);
    }

    return remappedGroups;
  }

  @Override
  public void visit(final Spatial spatial) {
    if (spatial instanceof Mesh) {
      final Mesh mesh = (Mesh) spatial;
      final MeshData md = mesh.getMeshData();
      if (md.getTotalPrimitiveCount() < 1 || md.getVertexCount() < 3) {
        return;
      }
      for (final IndexMode mode : md.getIndexModes()) {
        if (mode != IndexMode.Triangles) {
          return;
        }
      }

      final int[] indices;
      if (md.getIndices() == null) {
        indices = new int[md.getVertexCount()];
        for (int i = 0; i < indices.length; i++) {
          indices[i] = i;
        }
      } else {
        indices = BufferUtils.getIntArray(md.getIndices());
      }
      PrimitiveGroup[] strips = generateStrips(indices, false);

      if (_reorderVertices) {
        final AtomicReference<int[]> newOrder = new AtomicReference<>();
        strips = remapIndices(strips, newOrder, md.getVertexCount());

        // ask mesh to apply new vertex order
        mesh.reorderVertexData(newOrder.get());
      }

      // construct our new index buffer, modes and counts
      int indexCount = 0, j = 0, count = 0;
      for (final PrimitiveGroup group : strips) {
        if (group.getIndices().length > 0) {
          count++;
        }
      }
      final int[] counts = new int[count];
      final IndexMode[] modes = new IndexMode[count];
      for (final PrimitiveGroup group : strips) {
        indexCount += group.getIndices().length;
        if (group.getIndices().length > 0) {
          modes[j] = group.getType();
          counts[j++] = group.getIndices().length;
        }
      }
      final IndexBufferData<?> newIndices = BufferUtils.createIndexBufferData(indexCount, md.getVertexCount());
      for (final PrimitiveGroup group : strips) {
        final IntBufferData data = new IntBufferData(group.getIndices().length);
        data.getBuffer().put(group.getIndices());
        data.rewind();
        newIndices.put(data);
      }
      newIndices.rewind();

      // ask mesh to apply new index data
      mesh.reorderIndices(newIndices, modes, counts);
    }
  }
}
