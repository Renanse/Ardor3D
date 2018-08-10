/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom;

import java.util.EnumSet;
import java.util.Map;
import java.util.logging.Logger;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.google.common.collect.Maps;

/**
 * This tool assists in reducing geometry information.<br>
 *
 * Note: Does not work with geometry using texcoords other than 2d coords. <br>
 * TODO: Consider adding an option for "close enough" vertex matches... ie, smaller than X distance apart.<br>
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
     * Attempt to collapse duplicate vertex data in a given mesh. Vertices are considered duplicate if they occupy the
     * same place in space and match the supplied conditions. All vertices in the mesh are considered part of the same
     * vertex "group".
     *
     * @param mesh
     *            the mesh to reduce
     * @param conditions
     *            our match conditions.
     * @return a mapping of old vertex positions to their new positions.
     */
    public static VertMap minimizeVerts(final Mesh mesh, final EnumSet<MatchCondition> conditions) {
        final VertGroupData groupData = new VertGroupData();
        groupData.setGroupConditions(VertGroupData.DEFAULT_GROUP, conditions);
        return minimizeVerts(mesh, groupData);
    }

    /**
     * Attempt to collapse duplicate vertex data in a given mesh. Vertices are consider duplicate if they occupy the
     * same place in space and match the supplied conditions. The conditions are supplied per vertex group.
     *
     * @param mesh
     *            the mesh to reduce
     * @param groupData
     *            grouping data for the vertices in this mesh.
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

            final Map<VertKey, Integer> store = Maps.newHashMap();
            final Map<Integer, Integer> indexRemap = Maps.newHashMap();
            int good = 0;
            long group;
            for (int x = 0, max = verts.length; x < max; x++) {
                group = groupData.getGroupForVertex(x);
                final VertKey vkey = new VertKey(verts[x], norms != null ? norms[x] : null, colors != null ? colors[x]
                        : null, getTexs(tex, x), groupData.getGroupConditions(group), group);
                // if we've already seen it, swap it for the max, and decrease max.
                if (store.containsKey(vkey)) {
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

                // otherwise just store it
                else {
                    store.put(vkey, x);
                    good++;
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
}
