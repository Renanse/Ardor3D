/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.collada.jdom;

import java.nio.FloatBuffer;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.jdom2.Element;

import com.ardor3d.extension.model.collada.jdom.ColladaInputPipe.Type;
import com.ardor3d.extension.model.collada.jdom.data.DataCache;
import com.ardor3d.extension.model.collada.jdom.data.MeshVertPairs;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.geom.GeometryTool;
import com.ardor3d.util.geom.GeometryTool.MatchCondition;
import com.ardor3d.util.geom.VertMap;

/**
 * Methods for parsing Collada data related to meshes.
 */
public class ColladaMeshUtils {
    private static final Logger logger = Logger.getLogger(ColladaMeshUtils.class.getName());

    private final DataCache _dataCache;
    private final ColladaDOMUtil _colladaDOMUtil;
    private final ColladaMaterialUtils _colladaMaterialUtils;
    private final boolean _optimizeMeshes;
    private final EnumSet<MatchCondition> _optimizeSettings;

    public ColladaMeshUtils(final DataCache dataCache, final ColladaDOMUtil colladaDOMUtil,
            final ColladaMaterialUtils colladaMaterialUtils, final boolean optimizeMeshes,
            final EnumSet<MatchCondition> optimizeSettings) {
        _dataCache = dataCache;
        _colladaDOMUtil = colladaDOMUtil;
        _colladaMaterialUtils = colladaMaterialUtils;
        _optimizeMeshes = optimizeMeshes;
        _optimizeSettings = EnumSet.copyOf(optimizeSettings);
    }

    /**
     * Builds geometry from an instance_geometry element.
     *
     * @param instanceGeometry
     * @return our Spatial
     */
    public Spatial getGeometryMesh(final Element instanceGeometry) {
        final Element geometry = _colladaDOMUtil.findTargetWithId(instanceGeometry.getAttributeValue("url"));

        if (geometry != null) {
            return buildMesh(geometry);
        }
        return null;
    }

    /**
     * Builds a mesh from a Collada geometry element. Currently supported mesh types: mesh, polygons, polylist,
     * triangles, lines. Not supported yet: linestrips, trifans, tristrips. If no meshtype is found, a pointcloud is
     * built.
     *
     * @param colladaGeometry
     * @return a Node containing all of the Ardor3D meshes we've parsed from this geometry element.
     */
    public Node buildMesh(final Element colladaGeometry) {
        if (colladaGeometry.getChild("mesh") != null) {
            final Element cMesh = colladaGeometry.getChild("mesh");
            final Node meshNode = new Node(colladaGeometry.getAttributeValue("name", colladaGeometry.getName()));

            // Grab all mesh types (polygons, triangles, etc.)
            // Create each as an Ardor3D Mesh, and attach to node
            boolean hasChild = false;
            if (cMesh.getChild("polygons") != null) {
                for (final Element p : cMesh.getChildren("polygons")) {
                    final Mesh child = buildMeshPolygons(colladaGeometry, p);
                    if (child != null) {
                        if (child.getName() == null) {
                            child.setName(meshNode.getName() + "_polygons");
                        }
                        meshNode.attachChild(child);
                        hasChild = true;
                    }
                }
            }
            if (cMesh.getChild("polylist") != null) {
                for (final Element p : cMesh.getChildren("polylist")) {
                    final Mesh child = buildMeshPolylist(colladaGeometry, p);
                    if (child != null) {
                        if (child.getName() == null) {
                            child.setName(meshNode.getName() + "_polylist");
                        }
                        meshNode.attachChild(child);
                        hasChild = true;
                    }
                }
            }
            if (cMesh.getChild("triangles") != null) {
                for (final Element t : cMesh.getChildren("triangles")) {
                    final Mesh child = buildMeshTriangles(colladaGeometry, t);
                    if (child != null) {
                        if (child.getName() == null) {
                            child.setName(meshNode.getName() + "_triangles");
                        }
                        meshNode.attachChild(child);
                        hasChild = true;
                    }
                }
            }
            if (cMesh.getChild("lines") != null) {
                for (final Element l : cMesh.getChildren("lines")) {
                    final Line child = buildMeshLines(colladaGeometry, l);
                    if (child != null) {
                        if (child.getName() == null) {
                            child.setName(meshNode.getName() + "_lines");
                        }
                        meshNode.attachChild(child);
                        hasChild = true;
                    }
                }
            }
            if (cMesh.getChild("linestrips") != null) {
                logger.warning("<linestrips> not currently supported.");
                hasChild = true;
                // TODO: Add support
            }
            if (cMesh.getChild("trifans") != null) {
                logger.warning("<trifan> not currently supported.");
                hasChild = true;
                // TODO: Add support
            }
            if (cMesh.getChild("tristrips") != null) {
                logger.warning("<tristrip> not currently supported.");
                hasChild = true;
                // TODO: Add support
            }

            // If we did not find a valid child, the spec says to add verts as a "cloud of points"
            if (!hasChild) {
                logger.warning("No valid child found, creating 'cloud of points'");
                final Point points = buildPoints(colladaGeometry, cMesh);
                if (points != null) {
                    if (points.getName() == null) {
                        points.setName(meshNode.getName() + "_points");
                    }
                    meshNode.attachChild(points);
                }
            }

            return meshNode;
        }
        return null;
    }

    private Point buildPoints(final Element colladaGeometry, final Element mesh) {
        if (mesh == null || mesh.getChild("vertices") == null || mesh.getChild("vertices").getChild("input") == null) {
            return null;
        }
        final Point points = new Point();
        points.setName(mesh.getAttributeValue("name", mesh.getName()));

        // Find POSITION vertices source
        final Element source = _colladaDOMUtil.getPositionSource(mesh.getChild("vertices"));
        if (source == null) {
            return null;
        }

        if (source.getChild("float_array") != null) {
            // Turn into Floatbuffer if we have float array data
            final Element floatArray = source.getChild("float_array");
            if ("0".equals(floatArray.getAttributeValue("count"))) {
                return null;
            }
            final FloatBuffer vertices = BufferUtils.createFloatBuffer(_colladaDOMUtil.parseFloatArray(floatArray));
            // Add to points
            points.getMeshData().setVertexBuffer(vertices);
        } else if (source.getChild("int_array") != null) {
            // Turn into Floatbuffer if we have int array data
            final Element intArray = source.getChild("int_array");
            if ("0".equals(intArray.getAttributeValue("count"))) {
                return null;
            }
            final int[] data = _colladaDOMUtil.parseIntArray(intArray);
            final FloatBuffer vertices = BufferUtils.createFloatBuffer(data.length);
            for (final int i : data) {
                vertices.put(i);
            }
            // Add to points
            points.getMeshData().setVertexBuffer(vertices);
        }

        // Add to vert mapping
        final int[] indices = new int[points.getMeshData().getVertexCount()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        final MeshVertPairs mvp = new MeshVertPairs(points, indices);
        _dataCache.getVertMappings().put(colladaGeometry, mvp);

        if (_optimizeMeshes) {
            final VertMap map = GeometryTool.minimizeVerts(points, _optimizeSettings);
            _dataCache.setMeshVertMap(points, map);
        }

        // Update bound
        points.updateModelBound();

        // return
        return points;
    }

    public Mesh buildMeshPolygons(final Element colladaGeometry, final Element polys) {
        if (polys == null || polys.getChild("input") == null) {
            return null;
        }
        final Mesh polyMesh = new Mesh(extractName(colladaGeometry, polys));
        polyMesh.getMeshData().setIndexMode(IndexMode.Triangles);

        // Build and set RenderStates for our material
        _colladaMaterialUtils.applyMaterial(polys.getAttributeValue("material"), polyMesh);

        final LinkedList<ColladaInputPipe> pipes = new LinkedList<ColladaInputPipe>();
        final int maxOffset = extractPipes(polys, pipes);
        final int interval = maxOffset + 1;

        // use interval & sum of sizes of p entries to determine buffer sizes.
        int numEntries = 0;
        int numIndices = 0;
        for (final Element vals : polys.getChildren("p")) {
            final int length = _colladaDOMUtil.parseIntArray(vals).length;
            numEntries += length;
            numIndices += (length / interval - 2) * 3;
        }
        numEntries /= interval;

        // Construct nio buffers for specified inputs.
        for (final ColladaInputPipe pipe : pipes) {
            pipe.setupBuffer(numEntries, polyMesh.getMeshData(), _dataCache);
        }

        // Add to vert mapping
        final int[] indices = new int[numEntries];
        final MeshVertPairs mvp = new MeshVertPairs(polyMesh, indices);
        _dataCache.getVertMappings().put(colladaGeometry, mvp);

        // Prepare indices buffer
        final IndexBufferData<?> meshIndices = BufferUtils.createIndexBufferData(numIndices, polyMesh.getMeshData()
                .getVertexCount() - 1);
        polyMesh.getMeshData().setIndices(meshIndices);

        // go through the polygon entries
        int firstIndex = 0, vecIndex;
        final int[] currentVal = new int[interval];
        for (final Element dia : polys.getChildren("p")) {
            // for each p, iterate using max offset
            final int[] vals = _colladaDOMUtil.parseIntArray(dia);

            final int first = firstIndex + 0;
            System.arraycopy(vals, 0, currentVal, 0, interval);
            vecIndex = processPipes(pipes, currentVal);
            if (vecIndex != Integer.MIN_VALUE) {
                indices[firstIndex + 0] = vecIndex;
            }

            int prev = firstIndex + 1;
            System.arraycopy(vals, interval, currentVal, 0, interval);
            vecIndex = processPipes(pipes, currentVal);
            if (vecIndex != Integer.MIN_VALUE) {
                indices[firstIndex + 1] = vecIndex;
            }

            // first add the first two entries to the buffers.

            // Now go through remaining entries and create a polygon as a triangle fan.
            for (int j = 2, max = vals.length / interval; j < max; j++) {
                // add first as index
                meshIndices.put(first);
                // add prev as index
                meshIndices.put(prev);

                // set prev to current
                prev = firstIndex + j;
                // add current to buffers
                System.arraycopy(vals, j * interval, currentVal, 0, interval);
                vecIndex = processPipes(pipes, currentVal);
                if (vecIndex != Integer.MIN_VALUE) {
                    indices[firstIndex + j] = vecIndex;
                }
                // add current as index
                meshIndices.put(prev);
            }
            firstIndex += vals.length / interval;
        }

        if (_optimizeMeshes) {
            final VertMap map = GeometryTool.minimizeVerts(polyMesh, _optimizeSettings);
            _dataCache.setMeshVertMap(polyMesh, map);
        }

        // update bounds
        polyMesh.updateModelBound();

        // return
        return polyMesh;
    }

    public Mesh buildMeshPolylist(final Element colladaGeometry, final Element polys) {
        if (polys == null || polys.getChild("input") == null) {
            return null;
        }
        final Mesh polyMesh = new Mesh(extractName(colladaGeometry, polys));
        polyMesh.getMeshData().setIndexMode(IndexMode.Triangles);

        // Build and set RenderStates for our material
        _colladaMaterialUtils.applyMaterial(polys.getAttributeValue("material"), polyMesh);

        final LinkedList<ColladaInputPipe> pipes = new LinkedList<ColladaInputPipe>();
        final int maxOffset = extractPipes(polys, pipes);
        final int interval = maxOffset + 1;

        // use interval & sum of sizes of vcount to determine buffer sizes.
        int numEntries = 0;
        int numIndices = 0;
        for (final int length : _colladaDOMUtil.parseIntArray(polys.getChild("vcount"))) {
            numEntries += length;
            numIndices += (length - 2) * 3;
        }

        // Construct nio buffers for specified inputs.
        for (final ColladaInputPipe pipe : pipes) {
            pipe.setupBuffer(numEntries, polyMesh.getMeshData(), _dataCache);
        }

        // Add to vert mapping
        final int[] indices = new int[numEntries];
        final MeshVertPairs mvp = new MeshVertPairs(polyMesh, indices);
        _dataCache.getVertMappings().put(colladaGeometry, mvp);

        // Prepare indices buffer
        final IndexBufferData<?> meshIndices = BufferUtils.createIndexBufferData(numIndices, polyMesh.getMeshData()
                .getVertexCount() - 1);
        polyMesh.getMeshData().setIndices(meshIndices);

        // go through the polygon entries
        int firstIndex = 0;
        int vecIndex;
        final int[] vals = _colladaDOMUtil.parseIntArray(polys.getChild("p"));
        for (final int length : _colladaDOMUtil.parseIntArray(polys.getChild("vcount"))) {
            final int[] currentVal = new int[interval];

            // first add the first two entries to the buffers.
            final int first = firstIndex + 0;
            System.arraycopy(vals, (first * interval), currentVal, 0, interval);
            vecIndex = processPipes(pipes, currentVal);
            if (vecIndex != Integer.MIN_VALUE) {
                indices[firstIndex + 0] = vecIndex;
            }

            int prev = firstIndex + 1;
            System.arraycopy(vals, (prev * interval), currentVal, 0, interval);
            vecIndex = processPipes(pipes, currentVal);
            if (vecIndex != Integer.MIN_VALUE) {
                indices[firstIndex + 1] = vecIndex;
            }

            // Now go through remaining entries and create a polygon as a triangle fan.
            for (int j = 2, max = length; j < max; j++) {
                // add first as index
                meshIndices.put(first);
                // add prev as index
                meshIndices.put(prev);

                // set prev to current
                prev = firstIndex + j;
                // add current to buffers
                System.arraycopy(vals, (prev * interval), currentVal, 0, interval);
                vecIndex = processPipes(pipes, currentVal);
                if (vecIndex != Integer.MIN_VALUE) {
                    indices[firstIndex + j] = vecIndex;
                }
                // add current as index
                meshIndices.put(prev);
            }
            firstIndex += length;
        }

        if (_optimizeMeshes) {
            final VertMap map = GeometryTool.minimizeVerts(polyMesh, _optimizeSettings);
            _dataCache.setMeshVertMap(polyMesh, map);
        }

        // update bounds
        polyMesh.updateModelBound();

        // return
        return polyMesh;
    }

    public Mesh buildMeshTriangles(final Element colladaGeometry, final Element tris) {
        if (tris == null || tris.getChild("input") == null || tris.getChild("p") == null) {
            return null;
        }
        final Mesh triMesh = new Mesh(extractName(colladaGeometry, tris));
        triMesh.getMeshData().setIndexMode(IndexMode.Triangles);

        // Build and set RenderStates for our material
        _colladaMaterialUtils.applyMaterial(tris.getAttributeValue("material"), triMesh);

        final LinkedList<ColladaInputPipe> pipes = new LinkedList<ColladaInputPipe>();
        final int maxOffset = extractPipes(tris, pipes);
        final int interval = maxOffset + 1;

        // use interval & size of p array to determine buffer sizes.
        final int[] vals = _colladaDOMUtil.parseIntArray(tris.getChild("p"));
        final int numEntries = vals.length / interval;

        // Construct nio buffers for specified inputs.
        for (final ColladaInputPipe pipe : pipes) {
            pipe.setupBuffer(numEntries, triMesh.getMeshData(), _dataCache);
        }

        // Add to vert mapping
        final int[] indices = new int[numEntries];
        final MeshVertPairs mvp = new MeshVertPairs(triMesh, indices);
        _dataCache.getVertMappings().put(colladaGeometry, mvp);

        // go through the p entry
        // for each p, iterate using max offset
        final int[] currentVal = new int[interval];

        // Go through entries and add to buffers.
        for (int j = 0, max = numEntries; j < max; j++) {
            // add entry to buffers
            System.arraycopy(vals, j * interval, currentVal, 0, interval);
            final int rVal = processPipes(pipes, currentVal);
            if (rVal != Integer.MIN_VALUE) {
                indices[j] = rVal;
            }
        }

        if (_optimizeMeshes) {
            final VertMap map = GeometryTool.minimizeVerts(triMesh, _optimizeSettings);
            _dataCache.setMeshVertMap(triMesh, map);
        }

        triMesh.updateModelBound();

        return triMesh;
    }

    public Line buildMeshLines(final Element colladaGeometry, final Element lines) {
        if (lines == null || lines.getChild("input") == null || lines.getChild("p") == null) {
            return null;
        }
        final Line lineMesh = new Line(extractName(colladaGeometry, lines));

        // Build and set RenderStates for our material
        _colladaMaterialUtils.applyMaterial(lines.getAttributeValue("material"), lineMesh);

        final LinkedList<ColladaInputPipe> pipes = new LinkedList<ColladaInputPipe>();
        final int maxOffset = extractPipes(lines, pipes);
        final int interval = maxOffset + 1;

        // use interval & size of p array to determine buffer sizes.
        final int[] vals = _colladaDOMUtil.parseIntArray(lines.getChild("p"));
        final int numEntries = vals.length / interval;

        // Construct nio buffers for specified inputs.
        for (final ColladaInputPipe pipe : pipes) {
            pipe.setupBuffer(numEntries, lineMesh.getMeshData(), _dataCache);
        }

        // Add to vert mapping
        final int[] indices = new int[numEntries];
        final MeshVertPairs mvp = new MeshVertPairs(lineMesh, indices);
        _dataCache.getVertMappings().put(colladaGeometry, mvp);

        // go through the p entry
        // for each p, iterate using max offset
        final int[] currentVal = new int[interval];

        // Go through entries and add to buffers.
        for (int j = 0, max = numEntries; j < max; j++) {
            // add entry to buffers
            System.arraycopy(vals, j * interval, currentVal, 0, interval);
            final int rVal = processPipes(pipes, currentVal);
            if (rVal != Integer.MIN_VALUE) {
                indices[j] = rVal;
            }
        }

        if (_optimizeMeshes) {
            final VertMap map = GeometryTool.minimizeVerts(lineMesh, _optimizeSettings);
            _dataCache.setMeshVertMap(lineMesh, map);
        }

        lineMesh.updateModelBound();

        return lineMesh;
    }

    /**
     * Extract our pipes from the given parent element.
     *
     * @param inputsParent
     * @param pipesStore
     *            the store for our pipes
     * @return the max offset of our pipes.
     */
    private int extractPipes(final Element inputsParent, final LinkedList<ColladaInputPipe> pipesStore) {
        int maxOffset = 0;
        int texCount = 0;
        for (final Element input : inputsParent.getChildren("input")) {
            maxOffset = Math.max(maxOffset, _colladaDOMUtil.getAttributeIntValue(input, "offset", 0));
            try {
                final Type type = Type.valueOf(input.getAttributeValue("semantic"));
                if (type == Type.VERTEX) {
                    final Element vertexElement = _colladaDOMUtil.findTargetWithId(input.getAttributeValue("source"));
                    for (final Element vertexInput : vertexElement.getChildren("input")) {
                        vertexInput.setAttribute("offset", input.getAttributeValue("offset"));
                        vertexInput.setAttribute("isVertexDefined", "true");
                        final ColladaInputPipe pipe = new ColladaInputPipe(_colladaDOMUtil, vertexInput);
                        if (pipe.getType() == Type.TEXCOORD) {
                            pipe.setTexCoord(texCount++);
                        }
                        pipesStore.add(pipe);
                    }
                } else {
                    final ColladaInputPipe pipe = new ColladaInputPipe(_colladaDOMUtil, input);
                    if (pipe.getType() == Type.TEXCOORD) {
                        pipe.setTexCoord(texCount++);
                    }
                    pipesStore.add(pipe);
                }
            } catch (final Exception ex) {
                logger.warning("Unknown input type: " + input.getAttributeValue("semantic"));
                continue;
            }
        }
        return maxOffset;
    }

    /**
     * Push the values at the given indices of currentVal onto the buffers defined in pipes.
     *
     * @param pipes
     * @param currentVal
     * @return the vertex index referenced in the given indices based on the pipes. Integer.MIN_VALUE is returned if no
     *         vertex pipe is found.
     */
    private int processPipes(final LinkedList<ColladaInputPipe> pipes, final int[] currentVal) {
        // go through our pipes. use the indices in currentVal to pull the correct float val
        // from our source and set into our buffer.
        int rVal = Integer.MIN_VALUE;
        for (final ColladaInputPipe pipe : pipes) {
            pipe.pushValues(currentVal[pipe.getOffset()]);
            if (pipe.getType() == Type.POSITION) {
                rVal = currentVal[pipe.getOffset()];
            }
        }
        return rVal;
    }

    /**
     * Extract name from xml element, some exporters don't support 'name' attribute, so we better use the material
     * instead of a generic name.
     *
     * @param element
     * @return value from 'name' or 'material' attribute
     */
    private String extractName(final Element colladaGeometry, final Element element) {
        // Try to get mesh name
        String name = element.getAttributeValue("name");
        if (name == null || name.isEmpty()) {
            // No mesh name found, try to get mesh id
            name = element.getAttributeValue("id");
        }
        if (name == null || name.isEmpty()) {
            // No mesh name or id found, try to get parent geometry name
            name = colladaGeometry.getAttributeValue("name");
            if (name == null || name.isEmpty()) {
                // No parent geometry name found, try to get geometry id (mandatory according to spec)
                name = colladaGeometry.getAttributeValue("id");
            }
            if (name == null) {
                name = "";
            }

            // Since we have retrieved the parent geometry name/id, we append the material(if any),
            // to make identification unique.
            final String materialName = element.getAttributeValue("material");
            if (materialName != null && !materialName.isEmpty()) {
                name += "[" + materialName + "]";
            }
        }

        return name;
    }
}
