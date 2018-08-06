/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.visitor.Visitor;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Utility for combining multiple Meshes into a single Mesh. Note that you generally will want to combine Mesh objects
 * that have the same render states.
 *
 * XXX: should add in a way to combine only meshes with similar renderstates<br>
 * XXX: Might be able to reduce memory usage in the singular case where all sources do not have indices defined
 * (arrays).<br>
 * XXX: combining of triangle strips may not work properly? <br>
 * XXX: Could be smarter about texcoords and have a tuple size per channel.<br>
 */
public class MeshCombiner {
    public static final float[] DEFAULT_COLOR = { 1f, 1f, 1f, 1f };
    public static final float[] DEFAULT_NORMAL = { 0f, 1f, 0f };
    public static final float[] DEFAULT_TEXCOORD = { 0 };

    /**
     * <p>
     * Combine all mesh objects that fall under the scene graph the given source node. All Mesh objects must have
     * vertices and texcoords that have the same tuple width. It is possible to merge Mesh objects together that have
     * mismatched normals/colors/etc. (eg. one with colors and one without.)
     * </p>
     *
     * @param source
     *            our source node
     * @return the combined Mesh.
     */
    public final static Mesh combine(final Node source) {
        return combine(source, new MeshCombineLogic());
    }

    public final static Mesh combine(final Spatial source, final MeshCombineLogic logic) {
        final List<Mesh> sources = Lists.newArrayList();
        source.acceptVisitor(new Visitor() {
            @Override
            public void visit(final Spatial spatial) {
                if (spatial instanceof Mesh) {
                    sources.add((Mesh) spatial);
                }
            }
        }, true);

        return combine(sources, logic);
    }

    /**
     * Combine the given array of Mesh objects into a single Mesh. All Mesh objects must have vertices and texcoords
     * that have the same tuple width. It is possible to merge Mesh objects together that have mismatched
     * normals/colors/etc. (eg. one with colors and one without.)
     *
     * @param sources
     *            our Mesh objects to combine.
     * @return the combined Mesh.
     */
    public final static Mesh combine(final Mesh... sources) {
        return combine(Lists.newArrayList(sources));
    }

    /**
     * Combine the given collection of Mesh objects into a single Mesh. All Mesh objects must have vertices and
     * texcoords that have the same tuple width. It is possible to merge Mesh objects together that have mismatched
     * normals/colors/etc. (eg. one with colors and one without.)
     *
     * @param sources
     *            our collection of Mesh objects to combine.
     * @return the combined Mesh.
     */
    public final static Mesh combine(final Collection<Mesh> sources) {
        return combine(sources, new MeshCombineLogic());
    }

    public final static Mesh combine(final Collection<Mesh> sources, final MeshCombineLogic logic) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }

        // go through each MeshData to see what buffers we need and validate sizes.
        for (final Mesh mesh : sources) {
            logic.addSource(mesh);
        }

        // initialize return buffers
        logic.initDataBuffers();

        // combine sources into buffers
        logic.combineSources();

        // get and return our combined mesh
        return logic.getCombinedMesh();
    }

    public static class MeshCombineLogic {
        protected boolean useIndices = false, useNormals = false, useTextures = false, useColors = false, first = true;
        protected int maxTextures = 0, totalVertices = 0, totalIndices = 0, texCoords = 2, vertCoords = 3;
        protected IndexMode mode = null;
        protected EnumMap<StateType, RenderState> states = null;
        protected MeshData data = new MeshData();
        protected BoundingVolume volumeType = null;
        protected List<Mesh> sources = Lists.newArrayList();
        private FloatBufferData vertices;
        private FloatBufferData colors;
        private FloatBufferData normals;
        private List<FloatBufferData> texCoordsList;

        public Mesh getMesh() {
            final Mesh mesh = new Mesh("combined");
            mesh.setMeshData(data);
            return mesh;
        }

        public Mesh getCombinedMesh() {
            final Mesh mesh = getMesh();

            // set our bounding volume using the volume type of our first source found above.
            mesh.setModelBound(volumeType);

            // set the render states from the first mesh
            for (final RenderState state : states.values()) {
                mesh.setRenderState(state);
            }

            return mesh;
        }

        public void combineSources() {
            final IndexCombiner iCombiner = new IndexCombiner();

            // Walk through our source meshes and populate return MeshData buffers.
            int vertexOffset = 0;
            for (final Mesh mesh : sources) {

                final MeshData md = mesh.getMeshData();

                // Vertices
                md.getVertexBuffer().rewind();
                vertices.getBuffer().put(mesh.getWorldVectors(null));

                // Normals
                if (useNormals) {
                    final FloatBuffer nb = md.getNormalBuffer();
                    if (nb != null) {
                        nb.rewind();
                        normals.getBuffer().put(mesh.getWorldNormals(null));
                    } else {
                        for (int i = 0; i < md.getVertexCount(); i++) {
                            normals.getBuffer().put(DEFAULT_NORMAL);
                        }
                    }
                }

                // Colors
                if (useColors) {
                    final FloatBuffer cb = md.getColorBuffer();
                    if (cb != null) {
                        cb.rewind();
                        colors.getBuffer().put(cb);
                    } else {
                        for (int i = 0; i < md.getVertexCount(); i++) {
                            colors.getBuffer().put(DEFAULT_COLOR);
                        }
                    }
                }

                // Tex Coords
                if (useTextures) {
                    for (int i = 0; i < maxTextures; i++) {
                        final FloatBuffer dest = texCoordsList.get(i).getBuffer();
                        final FloatBuffer tb = md.getTextureBuffer(i);
                        if (tb != null) {
                            tb.rewind();
                            dest.put(tb);
                        } else {
                            for (int j = 0; j < md.getVertexCount() * texCoords; j++) {
                                dest.put(DEFAULT_TEXCOORD);
                            }
                        }
                    }
                }

                // Indices
                if (useIndices) {
                    iCombiner.addEntry(md, vertexOffset);
                    vertexOffset += md.getVertexCount();
                }
            }

            // Apply our index combiner to the mesh
            if (useIndices) {
                iCombiner.saveTo(data);
            } else {
                data.setIndexLengths(null);
                data.setIndexMode(mode);
            }
        }

        public void initDataBuffers() {
            // Generate our buffers based on the information collected above and populate MeshData
            vertices = new FloatBufferData(totalVertices * vertCoords, vertCoords);
            data.setVertexCoords(vertices);

            colors = useColors ? new FloatBufferData(totalVertices * 4, 4) : null;
            data.setColorCoords(colors);

            normals = useNormals ? new FloatBufferData(totalVertices * 3, 3) : null;
            data.setNormalCoords(normals);

            texCoordsList = Lists.newArrayListWithCapacity(maxTextures);
            for (int i = 0; i < maxTextures; i++) {
                final FloatBufferData uvs = new FloatBufferData(totalVertices * texCoords, texCoords);
                texCoordsList.add(uvs);
                data.setTextureCoords(uvs, i);
            }
        }

        public void addSource(final Mesh mesh) {
            sources.add(mesh);

            // update world transforms
            mesh.updateWorldTransform(false);

            final MeshData md = mesh.getMeshData();
            if (first) {
                // copy info from first mesh
                vertCoords = md.getVertexCoords().getValuesPerTuple();
                volumeType = mesh.getModelBound(null);
                states = mesh.getLocalRenderStates();
                first = false;
            } else if (vertCoords != md.getVertexCoords().getValuesPerTuple()) {
                throw new IllegalArgumentException("all MeshData vertex coords must use same tuple size.");
            }

            // update total vertices
            totalVertices += md.getVertexCount();

            // check for indices
            if (useIndices || md.getIndices() != null) {
                useIndices = true;
                if (md.getIndices() != null) {
                    totalIndices += md.getIndices().capacity();
                } else {
                    totalIndices += md.getVertexCount();
                }
            } else {
                mode = md.getIndexMode(0);
            }

            // check for normals
            if (!useNormals && md.getNormalBuffer() != null) {
                useNormals = true;
            }

            // check for colors
            if (!useColors && md.getColorBuffer() != null) {
                useColors = true;
            }

            // check for texcoord usage
            if (md.getMaxTextureUnitUsed() >= 0) {
                if (!useTextures) {
                    useTextures = true;
                    texCoords = md.getTextureCoords(0).getValuesPerTuple();
                } else if (md.getTextureCoords(0) != null && texCoords != md.getTextureCoords(0).getValuesPerTuple()) {
                    throw new IllegalArgumentException("all MeshData objects with texcoords must use same tuple size.");
                }
                maxTextures = Math.max(maxTextures, md.getMaxTextureUnitUsed() + 1);
            }
        }
    }
}

class IndexCombiner {
    Multimap<IndexMode, int[]> sectionMap = ArrayListMultimap.create();

    public void addEntry(final MeshData source, final int vertexOffset) {
        // arrays or elements?
        if (source.getIndices() == null) {
            // arrays...
            int offset = 0;
            int indexModeCounter = 0;
            final IndexMode[] modes = source.getIndexModes();
            // walk through each section
            for (int i = 0, maxI = source.getSectionCount(); i < maxI; i++) {
                // make an int array and populate it.
                final int size = source.getIndexLengths() != null ? source.getIndexLengths()[i] : source
                        .getVertexCount();
                final int[] indices = new int[size];
                for (int j = 0; j < size; j++) {
                    indices[j] = j + vertexOffset + offset;
                }

                // add to map
                sectionMap.put(modes[indexModeCounter], indices);

                // move our offsets forward to the section
                offset += size;
                if (indexModeCounter < modes.length - 1) {
                    indexModeCounter++;
                }
            }
        } else {
            // elements...
            final IndexBufferData<?> ib = source.getIndices();
            ib.rewind();
            int offset = 0;
            int indexModeCounter = 0;
            final IndexMode[] modes = source.getIndexModes();
            // walk through each section
            for (int i = 0, maxI = source.getSectionCount(); i < maxI; i++) {
                // make an int array and populate it.
                final int size = source.getIndexLengths() != null ? source.getIndexLengths()[i] : source.getIndices()
                        .capacity();
                final int[] indices = new int[size];
                for (int j = 0; j < size; j++) {
                    indices[j] = ib.get(j + offset) + vertexOffset;
                }

                // add to map
                sectionMap.put(modes[indexModeCounter], indices);

                // move our offsets forward to the section
                offset += size;
                if (indexModeCounter < modes.length - 1) {
                    indexModeCounter++;
                }
            }
        }
    }

    public void saveTo(final MeshData data) {
        final List<IntBuffer> sections = Lists.newArrayList();
        final List<IndexMode> modes = Lists.newArrayList();
        int max = 0;
        // walk through index modes and combine those we can.
        for (final IndexMode mode : sectionMap.keySet()) {
            final Collection<int[]> sources = sectionMap.get(mode);
            switch (mode) {
                case Triangles:
                case Lines:
                case Points: {
                    // we can combine these as-is to our heart's content.
                    int size = 0;
                    for (final int[] indices : sources) {
                        size += indices.length;
                    }
                    max += size;
                    final IntBuffer newSection = BufferUtils.createIntBufferOnHeap(size);
                    for (final int[] indices : sources) {
                        newSection.put(indices);
                    }
                    // save
                    sections.add(newSection);
                    modes.add(mode);
                    break;
                }
                case TriangleFan:
                case LineLoop:
                case LineStrip: {
                    // these have to be kept, as is.
                    int size;
                    for (final int[] indices : sources) {
                        size = indices.length;
                        max += size;
                        final IntBuffer newSection = BufferUtils.createIntBufferOnHeap(size);
                        newSection.put(indices);

                        sections.add(newSection);
                        modes.add(mode);
                    }
                    break;
                }
                case TriangleStrip: {
                    // we CAN combine these, but we have to add degenerate triangles.
                    int size = 0;
                    for (final int[] indices : sources) {
                        size += indices.length + 2;
                    }
                    size -= 2;
                    max += size;
                    final IntBuffer newSection = BufferUtils.createIntBufferOnHeap(size);
                    int i = 0;
                    for (final int[] indices : sources) {
                        if (i != 0) {
                            newSection.put(indices[0]);
                        }
                        newSection.put(indices);
                        if (i < sources.size() - 1) {
                            newSection.put(indices[indices.length - 1]);
                        }
                        i++;
                    }
                    // save
                    sections.add(newSection);
                    modes.add(mode);
                    break;
                }
            }
        }

        // compile into data
        final IndexBufferData<?> finalIndices = BufferUtils.createIndexBufferData(max, data.getVertexCount() - 1);
        data.setIndices(finalIndices);
        final int[] sectionCounts = new int[sections.size()];
        for (int i = 0; i < sectionCounts.length; i++) {
            final IntBuffer ib = sections.get(i);
            ib.rewind();
            sectionCounts[i] = ib.remaining();
            while (ib.hasRemaining()) {
                finalIndices.put(ib.get());
            }
        }

        data.setIndexLengths(sectionCounts);
        data.setIndexModes(modes.toArray(new IndexMode[modes.size()]));
    }
}