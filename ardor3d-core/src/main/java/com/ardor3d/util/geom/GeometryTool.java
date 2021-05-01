/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.geom;

import java.nio.FloatBuffer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.FloatBufferData;
import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

/**
 * This tool assists in reducing geometry information.<br>
 *
 * Note: Does not work with geometry using texcoords other than 2d coords. <br>
 * TODO: Consider adding an option for "close enough" vertex matches... ie, smaller than X distance
 * apart.<br>
 */
public final class GeometryTool {
  private static final Logger logger = Logger.getLogger(GeometryTool.class.getName());

  /**
   * Condition options for determining if one vertex is "equal" to another.
   */
  public enum MatchCondition {
    /** Vertices must have identical normals. */
    Normal,
    /** Vertices must have identical texture coords on all channels. */
    UVs,
    /** Vertices must have identical vertex coloring. */
    Color,
    /** Vertices must be in same group. */
    Group;
  }

  private GeometryTool() {
    super();
  }

  /**
   * Attempt to collapse duplicate vertex data in a given mesh. Vertices are considered duplicate if
   * they occupy the same place in space and match the supplied conditions. All vertices in the mesh
   * are considered part of the same vertex "group".
   *
   * @param mesh
   *          the mesh to reduce
   * @param conditions
   *          our match conditions.
   * @return a mapping of old vertex positions to their new positions.
   */
  public static VertMap minimizeVerts(final Mesh mesh, final EnumSet<MatchCondition> conditions) {
    final VertGroupData groupData = new VertGroupData();
    groupData.setGroupConditions(VertGroupData.DEFAULT_GROUP, conditions);
    return minimizeVerts(mesh, groupData);
  }

  /**
   * Attempt to collapse duplicate vertex data in a given mesh. Vertices are consider duplicate if
   * they occupy the same place in space and match the supplied conditions. The conditions are
   * supplied per vertex group.
   *
   * @param mesh
   *          the mesh to reduce
   * @param groupData
   *          grouping data for the vertices in this mesh.
   * @return a mapping of old vertex positions to their new positions.
   */
  public static VertMap minimizeVerts(final Mesh mesh, final VertGroupData groupData) {
    final long start = System.currentTimeMillis();

    int vertCount = -1;
    final int oldCount = mesh.getMeshData().getVertexCount();
    int newCount = 0;

    final VertMap result = new VertMap(mesh);

    // while we have not run through this optimization and ended up the same...
    // XXX: could optimize this to run all in arrays, then write to buffer after while loop.
    while (vertCount != newCount) {
      vertCount = mesh.getMeshData().getVertexCount();
      // go through each vert...
      final Vector3[] verts = BufferUtils.getVector3Array(mesh.getMeshData().getVertexCoords(), Vector3.ZERO);
      Vector3[] norms = null;
      if (mesh.getMeshData().getNormalBuffer() != null) {
        norms = BufferUtils.getVector3Array(mesh.getMeshData().getNormalCoords(), Vector3.UNIT_Y);
      }

      // see if we have vertex colors
      ColorRGBA[] colors = null;
      if (mesh.getMeshData().getColorBuffer() != null) {
        colors = BufferUtils.getColorArray(mesh.getMeshData().getColorCoords(), ColorRGBA.WHITE);
      }

      // see if we have uv coords
      final int maxUVUnit = mesh.getMeshData().getMaxTextureUnitUsed();
      final Vector2[][] tex;
      if (maxUVUnit >= 0) {
        tex = new Vector2[maxUVUnit + 1][];
        for (int x = 0; x < tex.length; x++) {
          if (mesh.getMeshData().getTextureCoords(x) != null) {
            tex[x] = BufferUtils.getVector2Array(mesh.getMeshData().getTextureCoords(x), Vector2.ZERO);
          }
        }
      } else {
        tex = new Vector2[0][];
      }

      // Use a map of maps - vert has to be equal, so we can reduce the comparisons being made drastically
      // by
      // reducing down to just same vertices.
      final Map<Vector3, Map<VertKey, Integer>> vertMap = new HashMap<>();
      final Map<Integer, Integer> indexRemap = new HashMap<>();
      Map<VertKey, Integer> store;
      int good = 0;
      long group;
      for (int x = 0, max = verts.length; x < max; x++) {
        group = groupData.getGroupForVertex(x);
        final VertKey vkey = new VertKey(verts[x], norms != null ? norms[x] : null, colors != null ? colors[x] : null,
            getTexs(tex, x), groupData.getGroupConditions(group), group);
        // Store if we have not already seen it
        store = vertMap.get(verts[x]);
        if (store == null) {
          store = new HashMap<>();
          vertMap.put(verts[x], store);
        }
        if (store.putIfAbsent(vkey, x) == null) {
          good++;
        }

        // if we've already seen it, swap it for the max, and decrease max.
        else {
          final int newInd = store.get(vkey);
          if (indexRemap.containsKey(x)) {
            indexRemap.put(max, newInd);
          } else {
            indexRemap.put(x, newInd);
          }
          max--;
          if (x != max) {
            indexRemap.put(max, x);
            verts[x] = verts[max];
            verts[max] = null;
            if (norms != null) {
              norms[newInd].addLocal(norms[x].normalizeLocal());
              norms[x] = norms[max];
            }
            if (colors != null) {
              colors[x] = colors[max];
            }
            for (int y = 0; y < tex.length; y++) {
              if (mesh.getMeshData().getTextureCoords(y) != null) {
                tex[y][x] = tex[y][max];
              }
            }
            x--;
          } else {
            verts[max] = null;
          }
        }
      }

      if (norms != null) {
        for (final Vector3 norm : norms) {
          norm.normalizeLocal();
        }
      }

      mesh.getMeshData().setVertexBuffer(BufferUtils.createFloatBuffer(0, good, verts));
      if (norms != null) {
        mesh.getMeshData().setNormalBuffer(BufferUtils.createFloatBuffer(0, good, norms));
      }
      if (colors != null) {
        mesh.getMeshData().setColorBuffer(BufferUtils.createFloatBuffer(0, good, colors));
      }

      for (int x = 0; x < tex.length; x++) {
        if (tex[x] != null) {
          mesh.getMeshData().setTextureBuffer(BufferUtils.createFloatBuffer(0, good, tex[x]), x);
        }
      }

      if (mesh.getMeshData().getIndices() == null || mesh.getMeshData().getIndices().getBufferCapacity() == 0) {
        final IndexBufferData<?> indexBuffer = BufferUtils.createIndexBufferData(oldCount, oldCount);
        mesh.getMeshData().setIndices(indexBuffer);
        for (int i = 0; i < oldCount; i++) {
          if (indexRemap.containsKey(i)) {
            indexBuffer.put(indexRemap.get(i));
          } else {
            indexBuffer.put(i);
          }
        }
      } else {
        final IndexBufferData<?> indexBuffer = mesh.getMeshData().getIndices();
        final int[] inds = BufferUtils.getIntArray(indexBuffer);
        indexBuffer.rewind();
        for (final int i : inds) {
          if (indexRemap.containsKey(i)) {
            indexBuffer.put(indexRemap.get(i));
          } else {
            indexBuffer.put(i);
          }
        }
      }
      result.applyRemapping(indexRemap);
      newCount = mesh.getMeshData().getVertexCount();
    }

    logger.info("Vertex reduction complete on: " + mesh + "  old vertex count: " + oldCount + " new vertex count: "
        + newCount + " (in " + (System.currentTimeMillis() - start) + " ms)");

    return result;
  }

  private static Vector2[] getTexs(final Vector2[][] tex, final int i) {
    final Vector2[] res = new Vector2[tex.length];
    for (int x = 0; x < tex.length; x++) {
      if (tex[x] != null) {
        res[x] = tex[x][i];
      }
    }
    return res;
  }

  public static void trimEmptyBranches(final Spatial spatial) {
    if (spatial instanceof Node) {
      final Node node = (Node) spatial;
      for (int i = node.getNumberOfChildren(); --i >= 0;) {
        trimEmptyBranches(node.getChild(i));
      }
      if (node.getNumberOfChildren() <= 0) {
        spatial.removeFromParent();
      }
    }
  }

  public static FloatBufferData convertQuadVerticesToTriangles(final FloatBufferData vertices) {
    final int dims = vertices.getValuesPerTuple();
    if (dims != 2 && dims != 3) {
      throw new IllegalArgumentException(
          "Only 2d and 3d quad data can be supported.  Vertices had tuple size of " + dims);
    }

    final FloatBuffer srcBuffer = vertices.getBuffer();
    final int qVerts = vertices.getTupleCount();

    // write our quads as 2 individual triangles
    if (qVerts < 4 || (qVerts % 4) != 0) {
      throw new IllegalArgumentException("Quad data should have 4*N verts where N=[1,R]");
    }
    final int quads = qVerts / 4;
    final int tris = 2 * quads;
    final int tVerts = tris * 3;

    final FloatBufferData rVal = new FloatBufferData(tVerts * dims, dims);
    final FloatBuffer dstBuffer = rVal.getBuffer();

    // write our new data by walking through our quads
    for (int q = 0; q < quads; q++) {
      final int offsetSrc = q * 4 * dims;
      final int offsetDst = q * 6 * dims;
      // 0 1 2
      BufferUtils.copy(srcBuffer, offsetSrc + 0 * dims, dstBuffer, offsetDst + 0 * dims, dims);
      BufferUtils.copy(srcBuffer, offsetSrc + 1 * dims, dstBuffer, offsetDst + 1 * dims, dims);
      BufferUtils.copy(srcBuffer, offsetSrc + 2 * dims, dstBuffer, offsetDst + 2 * dims, dims);

      // 2 1 3
      BufferUtils.copy(srcBuffer, offsetSrc + 2 * dims, dstBuffer, offsetDst + 3 * dims, dims);
      BufferUtils.copy(srcBuffer, offsetSrc + 1 * dims, dstBuffer, offsetDst + 4 * dims, dims);
      BufferUtils.copy(srcBuffer, offsetSrc + 3 * dims, dstBuffer, offsetDst + 5 * dims, dims);
    }

    return rVal;
  }

  public static IndexBufferData<?> convertQuadIndicesToTriangles(final IndexBufferData<?> quadIndices,
      final int vertexCount) {
    final int[] indices = new int[quadIndices.getBufferLimit()];
    for (int i = 0; i < indices.length; i++) {
      indices[i] = quadIndices.get(i);
    }

    return convertQuadIndicesToTriangles(indices, vertexCount);
  }

  public static IndexBufferData<?> convertQuadIndicesToTriangles(final int[] quadIndices, final int vertexCount) {
    final int qIndices = quadIndices.length;

    if (qIndices < 4 || (qIndices % 4) != 0) {
      throw new IllegalArgumentException("Quad data should have 4*N indices where N=[1,R]");
    }
    final int quads = qIndices / 4;
    final int tris = 2 * quads;
    final int tVerts = tris * 3;

    final IndexBufferData<?> rVal = BufferUtils.createIndexBufferData(tVerts, vertexCount - 1);

    // write our new data by walking through our quads
    for (int q = 0; q < quads; q++) {
      final int offsetSrc = q * 4;

      // 0 1 2
      rVal.put(quadIndices[offsetSrc + 0]);
      rVal.put(quadIndices[offsetSrc + 1]);
      rVal.put(quadIndices[offsetSrc + 2]);

      // 2 1 3
      rVal.put(quadIndices[offsetSrc + 2]);
      rVal.put(quadIndices[offsetSrc + 1]);
      rVal.put(quadIndices[offsetSrc + 3]);
    }

    return rVal;
  }

  public static IndexBufferData<?> generateAdjacencyIndices(final IndexMode mode, final int vertexCount) {
    switch (mode) {
      case LinesAdjacency: {
        if (vertexCount < 2) {
          return null;
        }
        final int lines = vertexCount / 2;
        final int indices = 4 * lines;
        final IndexBufferData<?> rVal = BufferUtils.createIndexBufferData(indices, vertexCount - 1);
        for (int i = 0; i < lines; i++) {
          rVal.put(i == 0 ? 1 : i * 2 - 1);
          rVal.put(i * 2);
          rVal.put(i * 2 + 1);
          rVal.put(i + 1 == lines ? i * 2 : i * 2 + 2);
        }
        return rVal;
      }
      case LineStripAdjacency: {
        if (vertexCount < 2) {
          return null;
        }
        final int indices = vertexCount + 2;
        final IndexBufferData<?> rVal = BufferUtils.createIndexBufferData(indices, vertexCount - 1);
        rVal.put(1);
        for (int i = 0; i < vertexCount; i++) {
          rVal.put(i);
        }
        rVal.put(vertexCount - 2);
        return rVal;
      }
      default:
        throw new IllegalArgumentException("Unhandled mode: " + mode);
    }
  }
}
