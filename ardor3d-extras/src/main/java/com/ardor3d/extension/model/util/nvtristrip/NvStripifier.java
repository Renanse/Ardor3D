/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.util.nvtristrip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Ported from <a href="http://developer.nvidia.com/object/nvtristrip_library.html">NVIDIA's
 * NvTriStrip Library</a>
 */
final class NvStripifier {
  private static final Logger logger = Logger.getLogger(NvStripifier.class.getName());

  public static int CACHE_INEFFICIENCY = 6;

  protected List<Integer> _indices = new ArrayList<>();
  protected int _cacheSize;
  protected int _minStripLength;
  protected float _meshJump;
  protected boolean _firstTimeResetPoint;

  /**
   *
   * @param in_indices
   *          the input indices of the mesh to stripify
   * @param in_cacheSize
   *          the target cache size
   * @param in_minStripLength
   * @param maxIndex
   * @param outStrips
   * @param outFaceList
   */
  void stripify(final List<Integer> in_indices, final int in_cacheSize, final int in_minStripLength, final int maxIndex,
      final List<NvStripInfo> outStrips, final List<NvFaceInfo> outFaceList) {
    _meshJump = 0.0f;
    _firstTimeResetPoint = true; // used in FindGoodResetPoint()

    // the number of times to run the experiments
    final int numSamples = 10;

    // the cache size, clamped to one
    _cacheSize = Math.max(1, in_cacheSize - NvStripifier.CACHE_INEFFICIENCY);

    // this is the strip size threshold below which we dump the strip into a list
    _minStripLength = in_minStripLength;

    _indices = in_indices;

    // build the stripification info
    final List<NvFaceInfo> allFaceInfos = new ArrayList<>();
    final List<NvEdgeInfo> allEdgeInfos = new ArrayList<>();

    buildStripifyInfo(allFaceInfos, allEdgeInfos, maxIndex);

    final List<NvStripInfo> allStrips = new ArrayList<>();

    // stripify
    findAllStrips(allStrips, allFaceInfos, allEdgeInfos, numSamples);

    // split up the strips into cache friendly pieces, optimize them, then dump these into outStrips
    splitUpStripsAndOptimize(allStrips, outStrips, allEdgeInfos, outFaceList);
  }

  /**
   * Generates actual strips from the list-in-strip-order.
   *
   * @param allStrips
   * @param stripIndices
   * @param bStitchStrips
   * @param numSeparateStrips
   * @param bRestart
   * @param restartVal
   */
  int createStrips(final List<NvStripInfo> allStrips, final List<Integer> stripIndices, final boolean bStitchStrips,
      final boolean bRestart, final int restartVal) {
    int numSeparateStrips = 0;

    NvFaceInfo tLastFace = new NvFaceInfo(0, 0, 0);
    final int nStripCount = allStrips.size();
    assert nStripCount > 0;

    // we infer the cw/ccw ordering depending on the number of indices
    // this is screwed up by the fact that we insert -1s to denote changing strips
    // this is to account for that
    int accountForNegatives = 0;

    for (int i = 0; i < nStripCount; i++) {
      final NvStripInfo strip = allStrips.get(i);
      final int nStripFaceCount = strip._faces.size();
      assert nStripFaceCount > 0;

      // Handle the first face in the strip
      {
        final NvFaceInfo tFirstFace =
            new NvFaceInfo(strip._faces.get(0)._v0, strip._faces.get(0)._v1, strip._faces.get(0)._v2);

        // If there is a second face, reorder vertices such that the
        // unique vertex is first
        if (nStripFaceCount > 1) {
          final int nUnique = NvStripifier.getUniqueVertexInB(strip._faces.get(1), tFirstFace);
          if (nUnique == tFirstFace._v1) {
            final int store = tFirstFace._v1;
            tFirstFace._v1 = tFirstFace._v0;
            tFirstFace._v0 = store;
          } else if (nUnique == tFirstFace._v2) {
            final int store = tFirstFace._v2;
            tFirstFace._v2 = tFirstFace._v0;
            tFirstFace._v0 = store;
          }

          // If there is a third face, reorder vertices such that the
          // shared vertex is last
          if (nStripFaceCount > 2) {
            if (NvStripifier.isDegenerate(strip._faces.get(1))) {
              final int pivot = strip._faces.get(1)._v1;
              if (tFirstFace._v1 == pivot) {
                final int store = tFirstFace._v2;
                tFirstFace._v2 = tFirstFace._v1;
                tFirstFace._v1 = store;
              }
            } else {
              final int[] nShared = NvStripifier.getSharedVertices(strip._faces.get(2), tFirstFace);
              if (nShared[0] == tFirstFace._v1 && nShared[1] == -1) {
                final int store = tFirstFace._v2;
                tFirstFace._v2 = tFirstFace._v1;
                tFirstFace._v1 = store;
              }
            }
          }
        }

        if (i == 0 || !bStitchStrips || bRestart) {
          if (!NvStripifier.isCW(strip._faces.get(0), tFirstFace._v0, tFirstFace._v1)) {
            stripIndices.add(tFirstFace._v0);
          }
        } else {
          // Double tap the first in the new strip
          stripIndices.add(tFirstFace._v0);

          // Check CW/CCW ordering
          if (NvStripifier.nextIsCW(stripIndices.size() - accountForNegatives) != NvStripifier.isCW(strip._faces.get(0),
              tFirstFace._v0, tFirstFace._v1)) {
            stripIndices.add(tFirstFace._v0);
          }
        }

        stripIndices.add(tFirstFace._v0);
        stripIndices.add(tFirstFace._v1);
        stripIndices.add(tFirstFace._v2);

        // Update last face info
        tLastFace = tFirstFace;
      }

      for (int j = 1; j < nStripFaceCount; j++) {
        final int nUnique = NvStripifier.getUniqueVertexInB(tLastFace, strip._faces.get(j));
        if (nUnique != -1) {
          stripIndices.add(nUnique);

          // Update last face info
          tLastFace._v0 = tLastFace._v1;
          tLastFace._v1 = tLastFace._v2;
          tLastFace._v2 = nUnique;
        } else {
          // we've hit a degenerate
          stripIndices.add(strip._faces.get(j)._v2);
          tLastFace._v0 = strip._faces.get(j)._v0;// tLastFace.m_v1;
          tLastFace._v1 = strip._faces.get(j)._v1;// tLastFace.m_v2;
          tLastFace._v2 = strip._faces.get(j)._v2;// tLastFace.m_v1;

        }
      }

      // Double tap between strips.
      if (bStitchStrips && !bRestart) {
        if (i != nStripCount - 1) {
          stripIndices.add(tLastFace._v2);
        }
      } else if (bRestart) {
        stripIndices.add(restartVal);
      } else {
        // -1 index indicates next strip
        stripIndices.add(-1);
        accountForNegatives++;
        numSeparateStrips++;
      }

      // Update last face info
      tLastFace._v0 = tLastFace._v1;
      tLastFace._v1 = tLastFace._v2;
      // tLastFace._v2 = tLastFace._v2; // for info purposes.
    }

    if (bStitchStrips || bRestart) {
      numSeparateStrips = 1;
    }

    return numSeparateStrips;
  }

  /**
   * @param faceA
   * @param faceB
   * @return the first vertex unique to faceB
   */
  static int getUniqueVertexInB(final NvFaceInfo faceA, final NvFaceInfo faceB) {
    final int facev0 = faceB._v0;
    if (facev0 != faceA._v0 && facev0 != faceA._v1 && facev0 != faceA._v2) {
      return facev0;
    }

    final int facev1 = faceB._v1;
    if (facev1 != faceA._v0 && facev1 != faceA._v1 && facev1 != faceA._v2) {
      return facev1;
    }

    final int facev2 = faceB._v2;
    if (facev2 != faceA._v0 && facev2 != faceA._v1 && facev2 != faceA._v2) {
      return facev2;
    }

    // nothing is different
    return -1;
  }

  /**
   * @param faceA
   * @param faceB
   * @return the (at most) two vertices shared between the two faces
   */
  static int[] getSharedVertices(final NvFaceInfo faceA, final NvFaceInfo faceB) {
    final int[] vertexStore = new int[2];
    vertexStore[0] = vertexStore[1] = -1;

    final int facev0 = faceB._v0;
    if (facev0 == faceA._v0 || facev0 == faceA._v1 || facev0 == faceA._v2) {
      if (vertexStore[0] == -1) {
        vertexStore[0] = facev0;
      } else {
        vertexStore[1] = facev0;
        return vertexStore;
      }
    }

    final int facev1 = faceB._v1;
    if (facev1 == faceA._v0 || facev1 == faceA._v1 || facev1 == faceA._v2) {
      if (vertexStore[0] == -1) {
        vertexStore[0] = facev1;
      } else {
        vertexStore[1] = facev1;
        return vertexStore;
      }
    }

    final int facev2 = faceB._v2;
    if (facev2 == faceA._v0 || facev2 == faceA._v1 || facev2 == faceA._v2) {
      if (vertexStore[0] == -1) {
        vertexStore[0] = facev2;
      } else {
        vertexStore[1] = facev2;
        return vertexStore;
      }
    }

    return vertexStore;
  }

  static boolean isDegenerate(final NvFaceInfo face) {
    if (face._v0 == face._v1) {
      return true;
    } else if (face._v0 == face._v2) {
      return true;
    } else if (face._v1 == face._v2) {
      return true;
    } else {
      return false;
    }
  }

  static boolean isDegenerate(final int v0, final int v1, final int v2) {
    if (v0 == v1) {
      return true;
    } else if (v0 == v2) {
      return true;
    } else if (v1 == v2) {
      return true;
    } else {
      return false;
    }
  }

  // ///////////////////////////////////////////////////////////////////////////////
  //
  // Big mess of functions called during stripification
  // Note: I removed some that were orphans - JES
  //
  // ///////////////////////////////////////////////////////////////////////////////

  /**
   * @param faceInfo
   * @param v0
   * @param v1
   * @return true if the face is ordered in CW fashion
   */
  static boolean isCW(final NvFaceInfo faceInfo, final int v0, final int v1) {
    if (faceInfo._v0 == v0) {
      return faceInfo._v1 == v1;
    } else if (faceInfo._v1 == v0) {
      return faceInfo._v2 == v1;
    } else {
      return faceInfo._v0 == v1;
    }
  }

  /**
   *
   * @param numIndices
   * @return true if the next face should be ordered in CW fashion
   */
  static boolean nextIsCW(final int numIndices) {
    return numIndices % 2 == 0;
  }

  /**
   * @param indices
   * @param face
   * @return vertex of the input face which is "next" in the input index list
   */
  static int getNextIndex(final List<Integer> indices, final NvFaceInfo face) {
    final int numIndices = indices.size();
    assert numIndices >= 2;

    final int v0 = indices.get(numIndices - 2);
    final int v1 = indices.get(numIndices - 1);

    final int fv0 = face._v0;
    final int fv1 = face._v1;
    final int fv2 = face._v2;

    if (fv0 != v0 && fv0 != v1) {
      if (fv1 != v0 && fv1 != v1 || fv2 != v0 && fv2 != v1) {
        NvStripifier.logger.warning("getNextIndex: Triangle doesn't have all of its vertices\n");
        NvStripifier.logger.warning("getNextIndex: Duplicate triangle probably got us derailed\n");
      }
      return fv0;
    }
    if (fv1 != v0 && fv1 != v1) {
      if (fv0 != v0 && fv0 != v1 || fv2 != v0 && fv2 != v1) {
        NvStripifier.logger.warning("getNextIndex: Triangle doesn't have all of its vertices\n");
        NvStripifier.logger.warning("getNextIndex: Duplicate triangle probably got us derailed\n");
      }
      return fv1;
    }
    if (fv2 != v0 && fv2 != v1) {
      if (fv0 != v0 && fv0 != v1 || fv1 != v0 && fv1 != v1) {
        NvStripifier.logger.warning("getNextIndex: Triangle doesn't have all of its vertices\n");
        NvStripifier.logger.warning("getNextIndex: Duplicate triangle probably got us derailed\n");
      }
      return fv2;
    }

    // shouldn't get here, but let's try and fail gracefully
    if (fv0 == fv1 || fv0 == fv2) {
      return fv0;
    } else if (fv1 == fv0 || fv1 == fv2) {
      return fv1;
    } else if (fv2 == fv0 || fv2 == fv1) {
      return fv2;
    } else {
      return -1;
    }
  }

  /**
   * find the edge info for these two indices
   *
   * @param edgeInfos
   * @param v0
   * @param v1
   * @return
   */
  static NvEdgeInfo findEdgeInfo(final List<NvEdgeInfo> edgeInfos, final int v0, final int v1) {
    // we can get to it through either array because the edge infos have a v0 and v1 and there is no
    // order except
    // how it was first created.
    NvEdgeInfo infoIter = edgeInfos.get(v0);
    while (infoIter != null) {
      if (infoIter._v0 == v0) {
        if (infoIter._v1 == v1) {
          return infoIter;
        } else {
          infoIter = infoIter._nextV0;
        }
      } else {
        assert infoIter._v1 == v0;
        if (infoIter._v0 == v1) {
          return infoIter;
        } else {
          infoIter = infoIter._nextV1;
        }
      }
    }
    return null;
  }

  /**
   * find the other face sharing these vertices
   *
   * @param edgeInfos
   * @param v0
   * @param v1
   * @param faceInfo
   * @return
   */
  static NvFaceInfo findOtherFace(final List<NvEdgeInfo> edgeInfos, final int v0, final int v1,
      final NvFaceInfo faceInfo) {
    final NvEdgeInfo edgeInfo = NvStripifier.findEdgeInfo(edgeInfos, v0, v1);

    if (edgeInfo == null && v0 == v1) {
      // we've hit a degenerate
      return null;
    }

    assert edgeInfo != null;
    return edgeInfo._face0 == faceInfo ? edgeInfo._face1 : edgeInfo._face0;
  }

  /**
   * A good reset point is one near other committed areas so that we know that when we've made the
   * longest strips its because we're stripifying in the same general orientation.
   *
   * @param faceInfos
   * @param edgeInfos
   * @return
   */
  NvFaceInfo findGoodResetPoint(final List<NvFaceInfo> faceInfos, final List<NvEdgeInfo> edgeInfos) {
    // we hop into different areas of the mesh to try to get other large open spans done. Areas of small
    // strips can
    // just be left to triangle lists added at the end.
    NvFaceInfo result = null;
    final int numFaces = faceInfos.size();
    int startPoint;
    if (_firstTimeResetPoint) {
      // first time, find a face with few neighbors (look for an edge of the mesh)
      startPoint = findStartPoint(faceInfos, edgeInfos);
      _firstTimeResetPoint = false;
    } else {
      startPoint = (int) (((float) numFaces - 1) * _meshJump);
    }

    if (startPoint == -1) {
      startPoint = (int) (((float) numFaces - 1) * _meshJump);

      // meshJump += 0.1f;
      // if (meshJump > 1.0f)
      // meshJump = .05f;
    }

    int i = startPoint;
    do {

      // if this guy isn't visited, try him
      if (faceInfos.get(i)._stripId < 0) {
        result = faceInfos.get(i);
        break;
      }

      // update the index and clamp to 0-(numFaces-1)
      if (++i >= numFaces) {
        i = 0;
      }

    } while (i != startPoint);

    // update the meshJump
    _meshJump += 0.1f;
    if (_meshJump > 1.0f) {
      _meshJump = .05f;
    }

    // return the best face we found
    return result;
  }

  /**
   * Does the stripification, puts output strips into vector allStrips
   *
   * Works by setting running a number of experiments in different areas of the mesh, and accepting
   * the one which results in the longest strips. It then accepts this, and moves on to a different
   * area of the mesh. We try to jump around the mesh some, to ensure that large open spans of strips
   * get generated.
   *
   * @param allStrips
   * @param allFaceInfos
   * @param allEdgeInfos
   * @param numSamples
   */
  @SuppressWarnings("unchecked")
  void findAllStrips(final List<NvStripInfo> allStrips, final List<NvFaceInfo> allFaceInfos,
      final List<NvEdgeInfo> allEdgeInfos, final int numSamples) {
    // the experiments
    int experimentId = 0;
    int stripId = 0;
    boolean done = false;

    while (!done) {

      //
      // PHASE 1: Set up numSamples * numEdges experiments
      //
      final List<NvStripInfo>[] experiments = new List[numSamples * 6];
      for (int i = 0; i < experiments.length; i++) {
        experiments[i] = new ArrayList<>();
      }

      int experimentIndex = 0;
      final Set<NvFaceInfo> resetPoints = new HashSet<>();
      for (int i = 0; i < numSamples; i++) {

        // Try to find another good reset point.
        // If there are none to be found, we are done
        final NvFaceInfo nextFace = findGoodResetPoint(allFaceInfos, allEdgeInfos);
        if (nextFace == null) {
          done = true;
          break;
        }
        // If we have already evaluated starting at this face in this slew
        // of experiments, then skip going any further
        else if (resetPoints.contains(nextFace)) {
          continue;
        }

        // trying it now...
        resetPoints.add(nextFace);

        // otherwise, we shall now try experiments for starting on the 01,12, and 20 edges
        assert nextFace._stripId < 0;

        // build the strip off of this face's 0-1 edge
        final NvEdgeInfo edge01 = NvStripifier.findEdgeInfo(allEdgeInfos, nextFace._v0, nextFace._v1);
        final NvStripInfo strip01 =
            new NvStripInfo(new NvStripStartInfo(nextFace, edge01, true), stripId++, experimentId++);
        experiments[experimentIndex++].add(strip01);

        // build the strip off of this face's 1-0 edge
        final NvEdgeInfo edge10 = NvStripifier.findEdgeInfo(allEdgeInfos, nextFace._v0, nextFace._v1);
        final NvStripInfo strip10 =
            new NvStripInfo(new NvStripStartInfo(nextFace, edge10, false), stripId++, experimentId++);
        experiments[experimentIndex++].add(strip10);

        // build the strip off of this face's 1-2 edge
        final NvEdgeInfo edge12 = NvStripifier.findEdgeInfo(allEdgeInfos, nextFace._v1, nextFace._v2);
        final NvStripInfo strip12 =
            new NvStripInfo(new NvStripStartInfo(nextFace, edge12, true), stripId++, experimentId++);
        experiments[experimentIndex++].add(strip12);

        // build the strip off of this face's 2-1 edge
        final NvEdgeInfo edge21 = NvStripifier.findEdgeInfo(allEdgeInfos, nextFace._v1, nextFace._v2);
        final NvStripInfo strip21 =
            new NvStripInfo(new NvStripStartInfo(nextFace, edge21, false), stripId++, experimentId++);
        experiments[experimentIndex++].add(strip21);

        // build the strip off of this face's 2-0 edge
        final NvEdgeInfo edge20 = NvStripifier.findEdgeInfo(allEdgeInfos, nextFace._v2, nextFace._v0);
        final NvStripInfo strip20 =
            new NvStripInfo(new NvStripStartInfo(nextFace, edge20, true), stripId++, experimentId++);
        experiments[experimentIndex++].add(strip20);

        // build the strip off of this face's 0-2 edge
        final NvEdgeInfo edge02 = NvStripifier.findEdgeInfo(allEdgeInfos, nextFace._v2, nextFace._v0);
        final NvStripInfo strip02 =
            new NvStripInfo(new NvStripStartInfo(nextFace, edge02, false), stripId++, experimentId++);
        experiments[experimentIndex++].add(strip02);
      }

      //
      // PHASE 2: Iterate through that we setup in the last phase
      // and really build each of the strips and strips that follow to see how
      // far we get
      //
      final int numExperiments = experimentIndex;
      for (int i = 0; i < numExperiments; i++) {
        // get the strip set
        NvStripInfo stripIter = experiments[i].get(0);

        // build the first strip of the list
        stripIter.build(allEdgeInfos, allFaceInfos);
        final int currExperimentId = stripIter._experimentId;

        final NvStripStartInfo startInfo = new NvStripStartInfo(null, null, false);
        while (findTraversal(allFaceInfos, allEdgeInfos, stripIter, startInfo)) {

          // create the new strip info
          stripIter = new NvStripInfo(startInfo, stripId++, currExperimentId);

          // build the next strip
          stripIter.build(allEdgeInfos, allFaceInfos);

          // add it to the list
          experiments[i].add(stripIter);
        }
      }

      //
      // Phase 3: Find the experiment that has the most promise
      //
      int bestIndex = 0;
      double bestValue = 0;
      for (int i = 0; i < numExperiments; i++) {
        final float avgStripSizeWeight = 1.0f;
        // final float numTrisWeight = 0.0f; // unused
        final float numStripsWeight = 0.0f;
        final float avgStripSize = avgStripSize(experiments[i]);
        final float numStrips = experiments[i].size();
        final float value = avgStripSize * avgStripSizeWeight + numStrips * numStripsWeight;
        // float value = 1.f / numStrips;
        // float value = numStrips * avgStripSize;

        if (value > bestValue) {
          bestValue = value;
          bestIndex = i;
        }
      }

      //
      // Phase 4: commit the best experiment of the bunch
      //
      commitStrips(allStrips, experiments[bestIndex]);
    }
  }

  /**
   * Splits the input vector of strips (allBigStrips) into smaller, cache friendly pieces, then
   * reorders these pieces to maximize cache hits. The final strips are stored in outStrips
   *
   * @param allStrips
   * @param outStrips
   * @param edgeInfos
   * @param outFaceList
   */
  void splitUpStripsAndOptimize(final List<NvStripInfo> allStrips, final List<NvStripInfo> outStrips,
      final List<NvEdgeInfo> edgeInfos, final List<NvFaceInfo> outFaceList) {
    final int threshold = _cacheSize;
    final List<NvStripInfo> tempStrips = new ArrayList<>();

    // split up strips into threshold-sized pieces
    for (int i = 0; i < allStrips.size(); i++) {
      final NvStripInfo allStripI = allStrips.get(i);
      NvStripInfo currentStrip;
      final NvStripStartInfo startInfo = new NvStripStartInfo(null, null, false);

      int actualStripSize = 0;
      for (final NvFaceInfo face : allStripI._faces) {
        if (!NvStripifier.isDegenerate(face)) {
          actualStripSize++;
        }
      }

      if (actualStripSize > threshold) {

        final int numTimes = actualStripSize / threshold;
        int numLeftover = actualStripSize % threshold;

        int degenerateCount = 0, j = 0;
        for (; j < numTimes; j++) {
          currentStrip = new NvStripInfo(startInfo, 0, -1);

          int faceCtr = j * threshold + degenerateCount;
          boolean bFirstTime = true;
          while (faceCtr < threshold + j * threshold + degenerateCount) {
            if (NvStripifier.isDegenerate(allStripI._faces.get(faceCtr))) {
              degenerateCount++;

              // last time or first time through, no need for a degenerate
              if ((faceCtr + 1 != threshold + j * threshold + degenerateCount
                  || j == numTimes - 1 && numLeftover < 4 && numLeftover > 0) && !bFirstTime) {
                currentStrip._faces.add(allStripI._faces.get(faceCtr++));
              } else {
                ++faceCtr;
              }
            } else {
              currentStrip._faces.add(allStripI._faces.get(faceCtr++));
              bFirstTime = false;
            }
          }
          if (j == numTimes - 1) // last time through
          {
            if (numLeftover < 4 && numLeftover > 0) // way too small
            {
              // just add to last strip
              int ctr = 0;
              while (ctr < numLeftover) {
                if (NvStripifier.isDegenerate(allStripI._faces.get(faceCtr))) {
                  ++degenerateCount;
                } else {
                  ++ctr;
                }
                currentStrip._faces.add(allStripI._faces.get(faceCtr++));
              }
              numLeftover = 0;
            }
          }
          tempStrips.add(currentStrip);
        }
        int leftOff = j * threshold + degenerateCount;

        if (numLeftover != 0) {
          currentStrip = new NvStripInfo(startInfo, 0, -1);

          int ctr = 0;
          boolean bFirstTime = true;
          while (ctr < numLeftover) {
            if (!NvStripifier.isDegenerate(allStripI._faces.get(leftOff))) {
              ctr++;
              bFirstTime = false;
              currentStrip._faces.add(allStripI._faces.get(leftOff++));
            } else if (!bFirstTime) {
              currentStrip._faces.add(allStripI._faces.get(leftOff++));
            } else {
              leftOff++;
            }
          }

          tempStrips.add(currentStrip);
        }
      } else {
        // we're not just doing a tempStrips.add(allBigStrips.get(i)) because
        // this way we can delete allBigStrips later to free the memory
        currentStrip = new NvStripInfo(startInfo, 0, -1);

        for (int j = 0; j < allStripI._faces.size(); j++) {
          currentStrip._faces.add(allStripI._faces.get(j));
        }

        tempStrips.add(currentStrip);
      }
    }

    // add small strips to face list
    final List<NvStripInfo> tempStrips2 = new ArrayList<>();
    removeSmallStrips(tempStrips, tempStrips2, outFaceList);

    outStrips.clear();
    // screw optimization for now
    // for(i = 0; i < tempStrips.size(); ++i)
    // outStrips.add(tempStrips.get(i));

    if (tempStrips2.size() != 0) {
      // Optimize for the vertex cache
      final VertexCache vcache = new VertexCache(_cacheSize);

      float bestNumHits = -1.0f;
      float numHits;
      int bestIndex = 0;
      int firstIndex = 0;
      float minCost = 10000.0f;

      for (int i = 0; i < tempStrips2.size(); i++) {
        final NvStripInfo tempStrips2I = tempStrips2.get(i);
        int numNeighbors = 0;

        // find strip with least number of neighbors per face
        for (int j = 0; j < tempStrips2I._faces.size(); j++) {
          numNeighbors += numNeighbors(tempStrips2I._faces.get(j), edgeInfos);
        }

        final float currCost = numNeighbors / (float) tempStrips2I._faces.size();
        if (currCost < minCost) {
          minCost = currCost;
          firstIndex = i;
        }
      }

      final NvStripInfo tempStrips2FirstIndex = tempStrips2.get(firstIndex);
      updateCacheStrip(vcache, tempStrips2FirstIndex);
      outStrips.add(tempStrips2FirstIndex);

      tempStrips2FirstIndex._visited = true;

      boolean bWantsCW = tempStrips2FirstIndex._faces.size() % 2 == 0;

      // XXX: this n^2 algo is what slows down stripification so much.... needs to be improved
      while (true) {
        bestNumHits = -1.0f;

        // find best strip to add next, given the current cache
        for (int i = 0; i < tempStrips2.size(); i++) {
          final NvStripInfo tempStrips2I = tempStrips2.get(i);
          if (tempStrips2I._visited) {
            continue;
          }

          numHits = calcNumHitsStrip(vcache, tempStrips2I);
          if (numHits > bestNumHits) {
            bestNumHits = numHits;
            bestIndex = i;
          } else if (numHits >= bestNumHits) {
            // check previous strip to see if this one requires it to switch polarity
            final NvStripInfo strip = tempStrips2I;
            final int nStripFaceCount = strip._faces.size();

            final NvFaceInfo tFirstFace = new NvFaceInfo(strip._faces.get(0));

            // If there is a second face, reorder vertices such that the
            // unique vertex is first
            if (nStripFaceCount > 1) {
              final int nUnique = NvStripifier.getUniqueVertexInB(strip._faces.get(1), tFirstFace);
              if (nUnique == tFirstFace._v1) {
                final int store = tFirstFace._v1;
                tFirstFace._v1 = tFirstFace._v0;
                tFirstFace._v0 = store;
              } else if (nUnique == tFirstFace._v2) {
                final int store = tFirstFace._v2;
                tFirstFace._v2 = tFirstFace._v0;
                tFirstFace._v0 = store;
              }

              // If there is a third face, reorder vertices such that the
              // shared vertex is last
              if (nStripFaceCount > 2) {
                final int[] nShared = NvStripifier.getSharedVertices(strip._faces.get(2), tFirstFace);
                if (nShared[0] == tFirstFace._v1 && nShared[1] == -1) {
                  final int store = tFirstFace._v2;
                  tFirstFace._v2 = tFirstFace._v1;
                  tFirstFace._v1 = store;
                }
              }
            }

            // Check CW/CCW ordering
            if (bWantsCW == NvStripifier.isCW(strip._faces.get(0), tFirstFace._v0, tFirstFace._v1)) {
              // I like this one!
              bestIndex = i;
            }
          }
        }

        if (bestNumHits == -1.0f) {
          break;
        }
        tempStrips2.get(bestIndex)._visited = true;
        updateCacheStrip(vcache, tempStrips2.get(bestIndex));
        outStrips.add(tempStrips2.get(bestIndex));
        bWantsCW = tempStrips2.get(bestIndex)._faces.size() % 2 == 0 ? bWantsCW : !bWantsCW;
      }
    }
  }

  /**
   * @param allStrips
   *          the whole strip vector...all small strips will be deleted from this list, to avoid
   *          leaking mem
   * @param allBigStrips
   *          an out parameter which will contain all strips above minStripLength
   * @param faceList
   *          an out parameter which will contain all faces which were removed from the striplist
   */
  void removeSmallStrips(final List<NvStripInfo> allStrips, final List<NvStripInfo> allBigStrips,
      final List<NvFaceInfo> faceList) {
    faceList.clear();
    allBigStrips.clear(); // make sure these are empty
    final List<NvFaceInfo> tempFaceList = new ArrayList<>();

    for (int i = 0; i < allStrips.size(); i++) {
      final NvStripInfo allStripI = allStrips.get(i);
      if (allStripI._faces.size() < _minStripLength) {
        // strip is too small, add faces to faceList
        for (int j = 0; j < allStripI._faces.size(); j++) {
          tempFaceList.add(allStripI._faces.get(j));
        }
      } else {
        allBigStrips.add(allStripI);
      }
    }

    if (!tempFaceList.isEmpty()) {
      final boolean[] bVisitedList = new boolean[tempFaceList.size()];
      final VertexCache vcache = new VertexCache(_cacheSize);

      int bestNumHits = -1;
      int numHits = 0;
      int bestIndex = 0;

      while (true) {
        bestNumHits = -1;

        // find best face to add next, given the current cache
        for (int i = 0; i < tempFaceList.size(); i++) {
          if (bVisitedList[i]) {
            continue;
          }

          numHits = calcNumHitsFace(vcache, tempFaceList.get(i));
          if (numHits > bestNumHits) {
            bestNumHits = numHits;
            bestIndex = i;
          }
        }

        if (bestNumHits == -1.0f) {
          break;
        }
        bVisitedList[bestIndex] = true;
        updateCacheFace(vcache, tempFaceList.get(bestIndex));
        faceList.add(tempFaceList.get(bestIndex));
      }
    }
  }

  /**
   * Finds the next face to start the next strip on.
   *
   * @param faceInfos
   * @param edgeInfos
   * @param strip
   * @param startInfo
   * @return
   */
  boolean findTraversal(final List<NvFaceInfo> faceInfos, final List<NvEdgeInfo> edgeInfos, final NvStripInfo strip,
      final NvStripStartInfo startInfo) {
    // if the strip was v0.v1 on the edge, then v1 will be a vertex in the next edge.
    final int v = strip._startInfo._toV1 ? strip._startInfo._startEdge._v1 : strip._startInfo._startEdge._v0;

    NvFaceInfo untouchedFace = null;
    NvEdgeInfo edgeIter = edgeInfos.get(v);
    while (edgeIter != null) {
      final NvFaceInfo face0 = edgeIter._face0;
      final NvFaceInfo face1 = edgeIter._face1;
      if (face0 != null && !strip.IsInStrip(face0) && face1 != null && !strip.isMarked(face1)) {
        untouchedFace = face1;
        break;
      }
      if (face1 != null && !strip.IsInStrip(face1) && face0 != null && !strip.isMarked(face0)) {
        untouchedFace = face0;
        break;
      }

      // find the next edgeIter
      edgeIter = edgeIter._v0 == v ? edgeIter._nextV0 : edgeIter._nextV1;
    }

    startInfo._startFace = untouchedFace;
    startInfo._startEdge = edgeIter;
    if (edgeIter != null) {
      if (strip.sharesEdge(startInfo._startFace, edgeInfos)) {
        startInfo._toV1 = edgeIter._v0 == v; // note! used to be m_v1
      } else {
        startInfo._toV1 = edgeIter._v1 == v;
      }
    }
    return startInfo._startFace != null;
  }

  /**
   * "Commits" the input strips by setting their m_experimentId to -1 and adding to the allStrips
   * vector
   *
   * @param allStrips
   * @param strips
   */
  void commitStrips(final List<NvStripInfo> allStrips, final List<NvStripInfo> strips) {
    // Iterate through strips
    for (final NvStripInfo strip : strips) {
      // Tell the strip that it is now real
      strip._experimentId = -1;

      // add to the list of real strips
      allStrips.add(strip);

      // Iterate through the faces of the strip
      // Tell the faces of the strip that they belong to a real strip now
      final List<NvFaceInfo> faces = strip._faces;
      for (final NvFaceInfo face : faces) {
        strip.markTriangle(face);
      }
    }
  }

  /**
   *
   * @param strips
   * @return the average strip size of the input vector of strips
   */
  float avgStripSize(final List<NvStripInfo> strips) {
    int sizeAccum = 0;
    for (final NvStripInfo strip : strips) {
      sizeAccum += strip._faces.size();
      sizeAccum -= strip._numDegenerates;
    }
    return (float) sizeAccum / (float) strips.size();
  }

  /**
   * Finds a good starting point, namely one which has only one neighbor
   *
   * @param faceInfos
   * @param edgeInfos
   * @return
   */
  int findStartPoint(final List<NvFaceInfo> faceInfos, final List<NvEdgeInfo> edgeInfos) {
    int bestCtr = -1;
    int bestIndex = -1;

    int i = 0;
    for (final NvFaceInfo faceInfo : faceInfos) {
      int ctr = 0;

      if (NvStripifier.findOtherFace(edgeInfos, faceInfo._v0, faceInfo._v1, faceInfo) == null) {
        ctr++;
      }
      if (NvStripifier.findOtherFace(edgeInfos, faceInfo._v1, faceInfo._v2, faceInfo) == null) {
        ctr++;
      }
      if (NvStripifier.findOtherFace(edgeInfos, faceInfo._v2, faceInfo._v0, faceInfo) == null) {
        ctr++;
      }
      if (ctr > bestCtr) {
        bestCtr = ctr;
        bestIndex = i;
      }
      i++;
    }

    if (bestCtr == 0) {
      return -1;
    } else {
      return bestIndex;
    }
  }

  /**
   * Updates the input vertex cache with this strip's vertices
   *
   * @param vcache
   * @param strip
   */
  void updateCacheStrip(final VertexCache vcache, final NvStripInfo strip) {
    for (final NvFaceInfo face : strip._faces) {
      updateCacheFace(vcache, face);
    }
  }

  /**
   * Updates the input vertex cache with this face's vertices
   *
   * @param vcache
   * @param face
   */
  void updateCacheFace(final VertexCache vcache, final NvFaceInfo face) {
    if (!vcache.inCache(face._v0)) {
      vcache.addEntry(face._v0);
    }

    if (!vcache.inCache(face._v1)) {
      vcache.addEntry(face._v1);
    }

    if (!vcache.inCache(face._v2)) {
      vcache.addEntry(face._v2);
    }
  }

  /**
   * @param vcache
   * @param strip
   * @return the number of cache hits per face in the strip
   */
  float calcNumHitsStrip(final VertexCache vcache, final NvStripInfo strip) {
    int numHits = 0;
    int numFaces = 0;

    for (final NvFaceInfo face : strip._faces) {
      if (vcache.inCache(face._v0)) {
        ++numHits;
      }

      if (vcache.inCache(face._v1)) {
        ++numHits;
      }

      if (vcache.inCache(face._v2)) {
        ++numHits;
      }

      numFaces++;
    }

    return (float) numHits / (float) numFaces;
  }

  /**
   * @param vcache
   * @param face
   * @return the number of cache hits in the face
   */
  int calcNumHitsFace(final VertexCache vcache, final NvFaceInfo face) {
    int numHits = 0;

    if (vcache.inCache(face._v0)) {
      numHits++;
    }

    if (vcache.inCache(face._v1)) {
      numHits++;
    }

    if (vcache.inCache(face._v2)) {
      numHits++;
    }

    return numHits;
  }

  /**
   *
   * @param face
   * @param edgeInfoVec
   * @return the number of neighbors that this face has
   */
  int numNeighbors(final NvFaceInfo face, final List<NvEdgeInfo> edgeInfoVec) {
    int numNeighbors = 0;

    if (NvStripifier.findOtherFace(edgeInfoVec, face._v0, face._v1, face) != null) {
      numNeighbors++;
    }

    if (NvStripifier.findOtherFace(edgeInfoVec, face._v1, face._v2, face) != null) {
      numNeighbors++;
    }

    if (NvStripifier.findOtherFace(edgeInfoVec, face._v2, face._v0, face) != null) {
      numNeighbors++;
    }

    return numNeighbors;
  }

  /**
   * Builds the list of all face and edge infos
   *
   * @param faceInfos
   * @param edgeInfos
   * @param maxIndex
   */
  void buildStripifyInfo(final List<NvFaceInfo> faceInfos, final List<NvEdgeInfo> edgeInfos, final int maxIndex) {
    // reserve space for the face infos, but do not resize them.
    final int numIndices = _indices.size();

    // we actually resize the edge infos, so we must initialize to null
    for (int i = 0; i <= maxIndex; i++) {
      edgeInfos.add(null);
    }

    // iterate through the triangles of the triangle list
    final int numTriangles = numIndices / 3;
    int index = 0;
    final boolean[] bFaceUpdated = new boolean[3];

    for (int i = 0; i < numTriangles; i++) {
      boolean bMightAlreadyExist = true;
      bFaceUpdated[0] = false;
      bFaceUpdated[1] = false;
      bFaceUpdated[2] = false;

      // grab the indices
      final int v0 = _indices.get(index++);
      final int v1 = _indices.get(index++);
      final int v2 = _indices.get(index++);

      // we disregard degenerates
      if (NvStripifier.isDegenerate(v0, v1, v2)) {
        continue;
      }

      // create the face info and add it to the list of faces, but only if this exact face doesn't already
      // exist in the list
      final NvFaceInfo faceInfo = new NvFaceInfo(v0, v1, v2);

      // grab the edge infos, creating them if they do not already exist
      NvEdgeInfo edgeInfo01 = NvStripifier.findEdgeInfo(edgeInfos, v0, v1);
      if (edgeInfo01 == null) {
        // since one of it's edges isn't in the edge data structure, it can't already exist in the face
        // structure
        bMightAlreadyExist = false;

        // create the info
        edgeInfo01 = new NvEdgeInfo(v0, v1);

        // update the linked list on both
        edgeInfo01._nextV0 = edgeInfos.get(v0);
        edgeInfo01._nextV1 = edgeInfos.get(v1);
        edgeInfos.set(v0, edgeInfo01);
        edgeInfos.set(v1, edgeInfo01);

        // set face 0
        edgeInfo01._face0 = faceInfo;
      } else {
        if (edgeInfo01._face1 != null) {
          NvStripifier.logger.warning("BuildStripifyInfo: > 2 triangles on an edge... uncertain consequences\n");
        } else {
          edgeInfo01._face1 = faceInfo;
          bFaceUpdated[0] = true;
        }
      }

      // grab the edge infos, creating them if they do not already exist
      NvEdgeInfo edgeInfo12 = NvStripifier.findEdgeInfo(edgeInfos, v1, v2);
      if (edgeInfo12 == null) {
        bMightAlreadyExist = false;

        // create the info
        edgeInfo12 = new NvEdgeInfo(v1, v2);

        // update the linked list on both
        edgeInfo12._nextV0 = edgeInfos.get(v1);
        edgeInfo12._nextV1 = edgeInfos.get(v2);
        edgeInfos.set(v1, edgeInfo12);
        edgeInfos.set(v2, edgeInfo12);

        // set face 0
        edgeInfo12._face0 = faceInfo;
      } else {
        if (edgeInfo12._face1 != null) {
          NvStripifier.logger.warning("BuildStripifyInfo: > 2 triangles on an edge... uncertain consequences\n");
        } else {
          edgeInfo12._face1 = faceInfo;
          bFaceUpdated[1] = true;
        }
      }

      // grab the edge infos, creating them if they do not already exist
      NvEdgeInfo edgeInfo20 = NvStripifier.findEdgeInfo(edgeInfos, v2, v0);
      if (edgeInfo20 == null) {
        bMightAlreadyExist = false;

        // create the info
        edgeInfo20 = new NvEdgeInfo(v2, v0);

        // update the linked list on both
        edgeInfo20._nextV0 = edgeInfos.get(v2);
        edgeInfo20._nextV1 = edgeInfos.get(v0);
        edgeInfos.set(v2, edgeInfo20);
        edgeInfos.set(v0, edgeInfo20);

        // set face 0
        edgeInfo20._face0 = faceInfo;
      } else {
        if (edgeInfo20._face1 != null) {
          NvStripifier.logger.warning("BuildStripifyInfo: > 2 triangles on an edge... uncertain consequences\n");
        } else {
          edgeInfo20._face1 = faceInfo;
          bFaceUpdated[2] = true;
        }
      }

      if (bMightAlreadyExist) {
        if (!alreadyExists(faceInfo, faceInfos)) {
          faceInfos.add(faceInfo);
        } else {
          // cleanup pointers that point to this deleted face
          if (bFaceUpdated[0]) {
            edgeInfo01._face1 = null;
          }
          if (bFaceUpdated[1]) {
            edgeInfo12._face1 = null;
          }
          if (bFaceUpdated[2]) {
            edgeInfo20._face1 = null;
          }
        }
      } else {
        faceInfos.add(faceInfo);
      }
    }
  }

  boolean alreadyExists(final NvFaceInfo toFind, final List<NvFaceInfo> faceInfos) {
    for (final NvFaceInfo face : faceInfos) {
      if (face._v0 == toFind._v0 && face._v1 == toFind._v1 && face._v2 == toFind._v2) {
        return true;
      }
    }

    return false;
  }
}
