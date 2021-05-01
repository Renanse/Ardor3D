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
import java.util.List;

final class NvStripInfo {
  NvStripStartInfo _startInfo;
  List<NvFaceInfo> _faces = new ArrayList<>();
  int _stripId;
  int _experimentId;

  boolean _visited;

  int _numDegenerates;

  // A little information about the creation of the triangle strips
  NvStripInfo(final NvStripStartInfo startInfo, final int stripId) {
    this(startInfo, stripId, -1);
  }

  NvStripInfo(final NvStripStartInfo startInfo, final int stripId, final int experimentId) {
    _startInfo = startInfo;
    _stripId = stripId;
    _experimentId = experimentId;
    _visited = false;
    _numDegenerates = 0;
  }

  /**
   * @return true if the experiment id is >= 0
   */
  boolean isExperiment() { return _experimentId >= 0; }

  /**
   * @param faceInfo
   * @return
   */
  boolean IsInStrip(final NvFaceInfo faceInfo) {
    if (faceInfo == null) {
      return false;
    }

    return _experimentId >= 0 ? faceInfo._testStripId == _stripId : faceInfo._stripId == _stripId;
  }

  /**
   * 
   * @param faceInfo
   * @param edgeInfos
   * @return true if the input face and the current strip share an edge
   */
  boolean sharesEdge(final NvFaceInfo faceInfo, final List<NvEdgeInfo> edgeInfos) {
    // check v0->v1 edge
    NvEdgeInfo currEdge = NvStripifier.findEdgeInfo(edgeInfos, faceInfo._v0, faceInfo._v1);

    if (IsInStrip(currEdge._face0) || IsInStrip(currEdge._face1)) {
      return true;
    }

    // check v1->v2 edge
    currEdge = NvStripifier.findEdgeInfo(edgeInfos, faceInfo._v1, faceInfo._v2);

    if (IsInStrip(currEdge._face0) || IsInStrip(currEdge._face1)) {
      return true;
    }

    // check v2->v0 edge
    currEdge = NvStripifier.findEdgeInfo(edgeInfos, faceInfo._v2, faceInfo._v0);

    if (IsInStrip(currEdge._face0) || IsInStrip(currEdge._face1)) {
      return true;
    }

    return false;
  }

  /**
   * take the given forward and backward strips and combine them together
   * 
   * @param forward
   * @param backward
   */
  void combine(final List<NvFaceInfo> forward, final List<NvFaceInfo> backward) {
    // add backward faces
    int numFaces = backward.size();
    for (int i = numFaces - 1; i >= 0; i--) {
      _faces.add(backward.get(i));
    }

    // add forward faces
    numFaces = forward.size();
    for (int i = 0; i < numFaces; i++) {
      _faces.add(forward.get(i));
    }
  }

  /**
   * @param faceVec
   * @param face
   * @return true if the face is "unique", i.e. has a vertex which doesn't exist in the faceVec
   */
  boolean unique(final List<NvFaceInfo> faceVec, final NvFaceInfo face) {
    boolean bv0, bv1, bv2; // bools to indicate whether a vertex is in the faceVec or not
    bv0 = bv1 = bv2 = false;

    for (int i = 0; i < faceVec.size(); i++) {
      if (!bv0) {
        if (faceVec.get(i)._v0 == face._v0 || faceVec.get(i)._v1 == face._v0 || faceVec.get(i)._v2 == face._v0) {
          bv0 = true;
        }
      }

      if (!bv1) {
        if (faceVec.get(i)._v0 == face._v1 || faceVec.get(i)._v1 == face._v1 || faceVec.get(i)._v2 == face._v1) {
          bv1 = true;
        }
      }

      if (!bv2) {
        if (faceVec.get(i)._v0 == face._v2 || faceVec.get(i)._v1 == face._v2 || faceVec.get(i)._v2 == face._v2) {
          bv2 = true;
        }
      }

      // the face is not unique, all its vertices exist in the face vector
      if (bv0 && bv1 && bv2) {
        return false;
      }
    }

    // if we get out here, it's unique
    return true;
  }

  /**
   * If either the faceInfo has a real strip index because it is already assign to a committed strip
   * OR it is assigned in an experiment and the experiment index is the one we are building for, then
   * it is marked and unavailable
   */
  boolean isMarked(final NvFaceInfo faceInfo) {
    return faceInfo._stripId >= 0 || isExperiment() && faceInfo._experimentId == _experimentId;
  }

  /**
   * Marks the face with the current strip ID
   * 
   * @param faceInfo
   */
  void markTriangle(final NvFaceInfo faceInfo) {
    assert !isMarked(faceInfo);
    if (isExperiment()) {
      faceInfo._experimentId = _experimentId;
      faceInfo._testStripId = _stripId;
    } else {
      faceInfo._experimentId = -1;
      faceInfo._stripId = _stripId;
    }
  }

  /**
   * Builds a strip forward as far as we can go, then builds backwards, and joins the two lists
   * 
   * @param edgeInfos
   * @param faceInfos
   */
  void build(final List<NvEdgeInfo> edgeInfos, final List<NvFaceInfo> faceInfos) {
    // used in building the strips forward and backward
    final List<Integer> scratchIndices = new ArrayList<>();

    // build forward... start with the initial face
    final List<NvFaceInfo> forwardFaces = new ArrayList<>();
    final List<NvFaceInfo> backwardFaces = new ArrayList<>();
    forwardFaces.add(_startInfo._startFace);

    markTriangle(_startInfo._startFace);

    final int v0 = _startInfo._toV1 ? _startInfo._startEdge._v0 : _startInfo._startEdge._v1;
    final int v1 = _startInfo._toV1 ? _startInfo._startEdge._v1 : _startInfo._startEdge._v0;

    // easiest way to get v2 is to use this function which requires the
    // other indices to already be in the list.
    scratchIndices.add(v0);
    scratchIndices.add(v1);
    final int v2 = NvStripifier.getNextIndex(scratchIndices, _startInfo._startFace);
    scratchIndices.add(v2);

    //
    // build the forward list
    //
    int nv0 = v1;
    int nv1 = v2;

    NvFaceInfo nextFace = NvStripifier.findOtherFace(edgeInfos, nv0, nv1, _startInfo._startFace);
    while (nextFace != null && !isMarked(nextFace)) {
      // check to see if this next face is going to cause us to die soon
      int testnv0 = nv1;
      final int testnv1 = NvStripifier.getNextIndex(scratchIndices, nextFace);

      final NvFaceInfo nextNextFace = NvStripifier.findOtherFace(edgeInfos, testnv0, testnv1, nextFace);

      if (nextNextFace == null || isMarked(nextNextFace)) {
        // uh, oh, we're following a dead end, try swapping
        final NvFaceInfo testNextFace = NvStripifier.findOtherFace(edgeInfos, nv0, testnv1, nextFace);

        if (testNextFace != null && !isMarked(testNextFace)) {
          // we only swap if it buys us something

          // add a "fake" degenerate face
          final NvFaceInfo tempFace = new NvFaceInfo(nv0, nv1, nv0, true);

          forwardFaces.add(tempFace);
          markTriangle(tempFace);

          scratchIndices.add(nv0);
          testnv0 = nv0;

          ++_numDegenerates;
        }

      }

      // add this to the strip
      forwardFaces.add(nextFace);

      markTriangle(nextFace);

      // add the index
      // nv0 = nv1;
      // nv1 = NvStripifier.GetNextIndex(scratchIndices, nextFace);
      scratchIndices.add(testnv1);

      // and get the next face
      nv0 = testnv0;
      nv1 = testnv1;

      nextFace = NvStripifier.findOtherFace(edgeInfos, nv0, nv1, nextFace);

    }

    // tempAllFaces is going to be forwardFaces + backwardFaces
    // it's used for Unique()
    final List<NvFaceInfo> tempAllFaces = new ArrayList<>();
    for (int i = 0; i < forwardFaces.size(); i++) {
      tempAllFaces.add(forwardFaces.get(i));
    }

    //
    // reset the indices for building the strip backwards and do so
    //
    scratchIndices.clear();
    scratchIndices.add(v2);
    scratchIndices.add(v1);
    scratchIndices.add(v0);
    nv0 = v1;
    nv1 = v0;
    nextFace = NvStripifier.findOtherFace(edgeInfos, nv0, nv1, _startInfo._startFace);
    while (nextFace != null && !isMarked(nextFace)) {
      // this tests to see if a face is "unique", meaning that its vertices aren't already in the list
      // so, strips which "wrap-around" are not allowed
      if (!unique(tempAllFaces, nextFace)) {
        break;
      }

      // check to see if this next face is going to cause us to die soon
      int testnv0 = nv1;
      final int testnv1 = NvStripifier.getNextIndex(scratchIndices, nextFace);

      final NvFaceInfo nextNextFace = NvStripifier.findOtherFace(edgeInfos, testnv0, testnv1, nextFace);

      if (nextNextFace == null || isMarked(nextNextFace)) {
        // uh, oh, we're following a dead end, try swapping
        final NvFaceInfo testNextFace = NvStripifier.findOtherFace(edgeInfos, nv0, testnv1, nextFace);
        if (testNextFace != null && !isMarked(testNextFace)) {
          // we only swap if it buys us something

          // add a "fake" degenerate face
          final NvFaceInfo tempFace = new NvFaceInfo(nv0, nv1, nv0, true);

          backwardFaces.add(tempFace);
          markTriangle(tempFace);
          scratchIndices.add(nv0);
          testnv0 = nv0;

          ++_numDegenerates;
        }

      }

      // add this to the strip
      backwardFaces.add(nextFace);

      // this is just so Unique() will work
      tempAllFaces.add(nextFace);

      markTriangle(nextFace);

      // add the index
      // nv0 = nv1;
      // nv1 = NvStripifier.GetNextIndex(scratchIndices, nextFace);
      scratchIndices.add(testnv1);

      // and get the next face
      nv0 = testnv0;
      nv1 = testnv1;
      nextFace = NvStripifier.findOtherFace(edgeInfos, nv0, nv1, nextFace);
    }

    // Combine the forward and backwards stripification lists and put into our own face vector
    combine(forwardFaces, backwardFaces);
  }
}
