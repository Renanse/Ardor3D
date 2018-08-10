/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.md2;

import java.io.InputStream;
import java.util.List;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.extension.model.util.KeyframeController;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.LittleEndianRandomAccessDataInput;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.google.common.collect.Lists;

public class Md2Importer {

    private final Vector3 calcVert = new Vector3();

    private boolean _loadTextures = true;
    private ResourceLocator _textureLocator;
    private ResourceLocator _modelLocator;

    // texture defaults
    private MinificationFilter _minificationFilter = MinificationFilter.Trilinear;
    private boolean _useCompression = true;
    private boolean _flipTextureVertically = false;

    public boolean isLoadTextures() {
        return _loadTextures;
    }

    public Md2Importer setLoadTextures(final boolean loadTextures) {
        _loadTextures = loadTextures;
        return this;
    }

    public Md2Importer setTextureLocator(final ResourceLocator locator) {
        _textureLocator = locator;
        return this;
    }

    public Md2Importer setModelLocator(final ResourceLocator locator) {
        _modelLocator = locator;
        return this;
    }

    public Md2Importer setFlipTextureVertically(final boolean flipTextureVertically) {
        _flipTextureVertically = flipTextureVertically;
        return this;
    }

    public boolean isFlipTextureVertically() {
        return _flipTextureVertically;
    }

    public Md2Importer setUseCompression(final boolean useCompression) {
        _useCompression = useCompression;
        return this;
    }

    public boolean isUseCompression() {
        return _useCompression;
    }

    public Md2Importer setMinificationFilter(final MinificationFilter minificationFilter) {
        _minificationFilter = minificationFilter;
        return this;
    }

    public MinificationFilter getMinificationFilter() {
        return _minificationFilter;
    }

    /**
     * Reads an MD2 file from the given resource
     * 
     * @param resource
     *            a resource pointing to the model we wish to load.
     * @return an Md2DataStore data object containing the scene and other useful elements.
     */
    public Md2DataStore load(final ResourceSource resource) {
        if (resource == null) {
            throw new NullPointerException("Unable to load null resource");
        }

        try {
            final InputStream md2Stream = resource.openStream();
            if (md2Stream == null) {
                throw new NullPointerException("Unable to load null streams");
            }
            // final Md2DataStore store = new Md2DataStore();
            final LittleEndianRandomAccessDataInput bis = new LittleEndianRandomAccessDataInput(md2Stream);

            // parse the header
            final Md2Header header = new Md2Header(bis.readInt(), bis.readInt(), bis.readInt(), bis.readInt(), bis
                    .readInt(), bis.readInt(), bis.readInt(), bis.readInt(), bis.readInt(), bis.readInt(), bis
                    .readInt(), bis.readInt(), bis.readInt(), bis.readInt(), bis.readInt(), bis.readInt(), bis
                    .readInt());

            // Check magic word and version
            if (header.magic != ('2' << 24) + ('P' << 16) + ('D' << 8) + 'I') {
                throw new Ardor3dException("Not an MD2 file.");
            }
            if (header.version != 8) {
                throw new Ardor3dException("Invalid file version (Version not 8)!");
            }

            // Parse out texture names
            final String[] texNames = new String[header.numSkins];
            bis.seek(header.offsetSkins);
            for (int i = 0; i < header.numSkins; i++) {
                texNames[i] = bis.readString(64);
            }

            // Parse out tex coords
            final float[] texCoords = new float[2 * header.numTexCoords];
            bis.seek(header.offsetTexCoords);
            final float inverseWidth = 1f / header.skinWidth;
            final float inverseHeight = 1f / header.skinHeight;
            for (int i = 0; i < header.numTexCoords; i++) {
                texCoords[i * 2 + 0] = bis.readShort() * inverseWidth;
                texCoords[i * 2 + 1] = bis.readShort() * inverseHeight;
            }

            // Parse out triangles
            final short[] triangles = new short[header.numTriangles * 6];
            bis.seek(header.offsetTriangles);
            for (int i = 0; i < header.numTriangles; i++) {
                triangles[i * 6 + 0] = bis.readShort(); // vert index 0
                triangles[i * 6 + 1] = bis.readShort(); // vert index 1
                triangles[i * 6 + 2] = bis.readShort(); // vert index 2
                triangles[i * 6 + 3] = bis.readShort(); // texcoord index 0
                triangles[i * 6 + 4] = bis.readShort(); // texcoord index 1
                triangles[i * 6 + 5] = bis.readShort(); // texcoord index 2
            }

            // Parse out gl commands
            final Md2GlCommand[] commands = new Md2GlCommand[header.numGlCommands];
            bis.seek(header.offsetGlCommands);
            int length, absLength;
            Md2GlCommand cmd;
            final List<Integer> fanIndices = Lists.newArrayList();
            final List<Integer> stripIndices = Lists.newArrayList();
            for (int i = 0; i < header.numGlCommands; i++) {
                length = bis.readInt();
                if (length == 0) {
                    break;
                }
                absLength = Math.abs(length);
                commands[i] = cmd = new Md2GlCommand(length >= 0 ? IndexMode.TriangleStrip : IndexMode.TriangleFan,
                        absLength);
                if (cmd.mode == IndexMode.TriangleFan) {
                    fanIndices.add(i);
                } else {
                    stripIndices.add(i);
                }
                for (int j = 0; j < absLength; j++) {
                    cmd.texCoords[j * 2 + 0] = bis.readFloat();
                    cmd.texCoords[j * 2 + 1] = bis.readFloat();
                    cmd.vertIndices[j] = bis.readInt();
                }
            }

            // Parse out frames
            final Md2Frame[] frames = new Md2Frame[header.numFrames];
            bis.seek(header.offsetFrames);
            final Vector3 scale = new Vector3();
            final Vector3 translate = new Vector3();
            for (int i = 0; i < header.numFrames; i++) {
                scale.set(bis.readFloat(), bis.readFloat(), bis.readFloat());
                translate.set(bis.readFloat(), bis.readFloat(), bis.readFloat());
                final String name = bis.readString(16);
                final byte[] vertData = new byte[header.numVertices * 4];
                bis.readFully(vertData);
                frames[i] = new Md2Frame(vertData, name, scale, translate);
            }

            // make index modes/counts to be used throughout meshes
            int vertexCount = 0;
            int fanIndex = stripIndices.size() != 0 ? 1 : 0;
            final IndexMode[] modes = new IndexMode[fanIndices.size() + fanIndex];
            final int[] counts = new int[modes.length];
            for (final Integer index : fanIndices) {
                counts[fanIndex] = commands[index].vertIndices.length;
                modes[fanIndex] = IndexMode.TriangleFan;
                vertexCount += counts[fanIndex];
                fanIndex++;
            }
            if (stripIndices.size() != 0) {
                int triCounts = 0;
                int vertCount;
                int extra = 0;
                for (final Integer index : stripIndices) {
                    vertCount = commands[index].vertIndices.length;
                    extra = vertCount % 2 == 1 ? 3 : 2;
                    triCounts += vertCount + extra;
                }
                counts[0] = triCounts - extra + 1;
                modes[0] = IndexMode.TriangleStrip;
                vertexCount += counts[0];
            }

            vertexCount++;

            // Create each frame as a Mesh using glcommands if given
            final Mesh[] meshes = new Mesh[header.numFrames];
            MeshData mData;
            for (int i = 0; i < header.numFrames; i++) {
                final Md2Frame frame = frames[i];

                meshes[i] = new Mesh(frames[i].name);
                mData = meshes[i].getMeshData();
                mData.setIndexLengths(counts);
                mData.setIndexModes(modes);

                final FloatBufferData verts = new FloatBufferData(vertexCount * 3, 3);
                final FloatBufferData norms = new FloatBufferData(vertexCount * 3, 3);
                final FloatBufferData texs = new FloatBufferData(vertexCount * 3, 2);
                mData.setVertexCoords(verts);
                mData.setNormalCoords(norms);
                mData.setTextureCoords(texs, 0);

                // go through the triangle strips/fans and add them in
                // first the strips
                if (stripIndices.size() != 0) {
                    for (int maxJ = stripIndices.size(), j = 0; j < maxJ; j++) {
                        cmd = commands[stripIndices.get(j)];
                        if (cmd.vertIndices.length < 3) {
                            continue;
                        }

                        addVert(cmd, frame, 0, verts);
                        norms.getBuffer().put(0).put(0).put(0);
                        texs.getBuffer().put(0).put(0);

                        // add strip verts / normals
                        for (int k = 0; k < cmd.vertIndices.length; k++) {
                            addVert(cmd, frame, k, verts);
                            addNormal(cmd, frame, k, norms);
                        }

                        // add strip tex coords
                        texs.getBuffer().put(cmd.texCoords);

                        // if we're not the last strip, add a vert or two for degenerate triangle connector
                        if (j != maxJ - 1) {
                            addVert(cmd, frame, cmd.vertIndices.length - 1, verts);
                            norms.getBuffer().put(0).put(0).put(0);
                            texs.getBuffer().put(0).put(0);
                            if (cmd.vertIndices.length % 2 == 1) {
                                // extra vert to maintain wind order
                                addVert(cmd, frame, cmd.vertIndices.length - 1, verts);
                                norms.getBuffer().put(0).put(0).put(0);
                                texs.getBuffer().put(0).put(0);
                            }
                        }
                    }
                }
                // Now the fans
                // XXX: could add these to the strip instead
                for (final int j : fanIndices) {
                    cmd = commands[j];
                    texs.getBuffer().put(cmd.texCoords[0]).put(cmd.texCoords[1]);
                    addNormal(cmd, frame, 0, norms);
                    addVert(cmd, frame, 0, verts);
                    for (int k = cmd.vertIndices.length; --k >= 1;) {
                        texs.getBuffer().put(cmd.texCoords[k * 2]).put(cmd.texCoords[k * 2 + 1]);
                        addNormal(cmd, frame, k, norms);
                        addVert(cmd, frame, k, verts);
                    }
                }
            }

            // Clone frame 0 as mesh for initial mesh
            final Mesh mesh = meshes[0].makeCopy(false);
            mesh.setModelBound(new BoundingBox());

            // Use resource name for mesh
            mesh.setName(resource.getName());

            // Add controller
            final KeyframeController<Mesh> controller = new KeyframeController<Mesh>();
            mesh.addController(controller);
            controller.setMorphingMesh(mesh);
            controller.setInterpTex(false);
            int i = 0;
            for (final Mesh meshX : meshes) {
                controller.setKeyframe(i, meshX);
                i++;
            }

            // Make a store object to return
            final Md2DataStore store = new Md2DataStore(mesh, controller);

            // store names
            for (final Md2Frame frame : frames) {
                store.getFrameNames().add(frame.name);
            }

            // store skin names
            for (final String name : texNames) {
                store.getSkinNames().add(name);
            }

            // Apply our texture
            if (isLoadTextures()) {
                Texture tex = null;
                for (final String name : texNames) {
                    tex = loadTexture(name);
                    if (tex != null) {
                        break;
                    }
                }

                // try using model name
                if (tex == null) {
                    tex = loadTexture(resource.getName());
                }

                if (tex != null) {
                    final TextureState ts = new TextureState();
                    ts.setTexture(tex);
                    mesh.setRenderState(ts);
                }
            }

            return store;
        } catch (final Exception e) {
            throw new Error("Unable to load md2 resource from URL: " + resource, e);
        }
    }

    private Texture loadTexture(final String name) {
        Texture tex = null;
        if (_textureLocator == null) {
            tex = TextureManager.load(name, getMinificationFilter(),
                    isUseCompression() ? TextureStoreFormat.GuessCompressedFormat
                            : TextureStoreFormat.GuessNoCompressedFormat, isFlipTextureVertically());
        } else {
            final ResourceSource source = _textureLocator.locateResource(name);
            if (source != null) {
                tex = TextureManager.load(source, getMinificationFilter(),
                        isUseCompression() ? TextureStoreFormat.GuessCompressedFormat
                                : TextureStoreFormat.GuessNoCompressedFormat, isFlipTextureVertically());
            }
        }
        return tex;
    }

    private void addNormal(final Md2GlCommand cmd, final Md2Frame frame, final int normalIndex,
            final FloatBufferData norms) {
        final int index = cmd.vertIndices[normalIndex];
        final byte[] vertData = frame.vertData;
        Md2Normals.getNormalVector(vertData[index * 4 + 3], calcVert);
        norms.getBuffer().put(calcVert.getXf()).put(calcVert.getYf()).put(calcVert.getZf());
    }

    private void addVert(final Md2GlCommand cmd, final Md2Frame frame, final int vertIndex, final FloatBufferData verts) {
        final int index = cmd.vertIndices[vertIndex];
        final byte[] vertData = frame.vertData;
        calcVert.set((vertData[index * 4 + 0] & 0xFF), (vertData[index * 4 + 1] & 0xFF),
                (vertData[index * 4 + 2] & 0xFF));
        calcVert.multiplyLocal(frame.scale).addLocal(frame.translate);
        verts.getBuffer().put(calcVert.getXf()).put(calcVert.getYf()).put(calcVert.getZf());
    }

    /**
     * Reads a MD2 file from the given resource
     * 
     * @param resource
     *            the name of the resource to find.
     * @return an ObjGeometryStore data object containing the scene and other useful elements.
     */
    public Md2DataStore load(final String resource) {
        final ResourceSource source;
        if (_modelLocator == null) {
            source = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, resource);
        } else {
            source = _modelLocator.locateResource(resource);
        }

        if (source == null) {
            throw new Error("Unable to locate '" + resource + "'");
        }

        return load(source);
    }

    static class Md2GlCommand {
        IndexMode mode;
        float[] texCoords;
        int[] vertIndices;

        Md2GlCommand(final IndexMode indexMode, final int length) {
            mode = indexMode;
            texCoords = new float[length * 2];
            vertIndices = new int[length];
        }
    }
}
