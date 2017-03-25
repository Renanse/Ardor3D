/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.md3;

import java.io.InputStream;

import com.ardor3d.extension.model.util.KeyframeController;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.LittleEndianRandomAccessDataInput;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

/**
 * http://education.mit.edu/starlogo-tng/shapes-tutorial/shapetutorial.html
 */
public class Md3Importer {

    private static final float XYZ_SCALE = 1.0f / 64;

    private ResourceLocator _modelLocator;

    public void setModelLocator(final ResourceLocator locator) {
        _modelLocator = locator;
    }

    /**
     * Reads a MD3 file from the given resource
     * 
     * @param resource
     *            the name of the resource to find.
     * @return an ObjGeometryStore data object containing the scene and other useful elements.
     */
    public Md3DataStore load(final String resource) {
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

    /**
     * Reads an MD3 file from the given resource
     * 
     * @param resource
     *            a resource pointing to the model we wish to load.
     * @return an Md3DataStore data object containing the scene and other useful elements.
     */
    public Md3DataStore load(final ResourceSource resource) {
        if (resource == null) {
            throw new NullPointerException("Unable to load null resource");
        }

        try {
            final InputStream md3Stream = resource.openStream();
            if (md3Stream == null) {
                throw new NullPointerException("Unable to load null streams");
            }
            final LittleEndianRandomAccessDataInput bis = new LittleEndianRandomAccessDataInput(md3Stream);

            // parse the header
            final Md3Header header = new Md3Header(bis.readInt(), bis.readInt(), bis.readString(64), bis.readInt(),
                    bis.readInt(), bis.readInt(), bis.readInt(), bis.readInt(), bis.readInt(), bis.readInt(),
                    bis.readInt(), bis.readInt());

            // Check magic word and version
            if (header._magic != ('3' << 24) + ('P' << 16) + ('D' << 8) + 'I') {
                throw new Ardor3dException("Not an MD3 file.");
            }
            if (header._version != 15) {
                throw new Ardor3dException("Invalid file version (Version not 15)!");
            }

            // Parse out frames
            final Md3Frame[] frames = new Md3Frame[header._numFrames];
            bis.seek(header._offsetFrame);
            final Vector3 minBounds = new Vector3();
            final Vector3 maxBounds = new Vector3();
            final Vector3 localOrigin = new Vector3();
            for (int i = 0; i < header._numFrames; i++) {
                minBounds.set(bis.readFloat(), bis.readFloat(), bis.readFloat());
                maxBounds.set(bis.readFloat(), bis.readFloat(), bis.readFloat());
                localOrigin.set(bis.readFloat(), bis.readFloat(), bis.readFloat());
                frames[i] = new Md3Frame(minBounds, maxBounds, localOrigin, bis.readFloat(), bis.readString(16));
            }

            // Parse out tags
            final Md3Tag[] tags = new Md3Tag[header._numTags];
            bis.seek(header._offsetTag);
            final Vector3 origin = new Vector3();
            final Matrix3 axis = new Matrix3();
            for (int i = 0; i < header._numTags; i++) {
                final String name = bis.readString(64);
                origin.set(bis.readFloat(), bis.readFloat(), bis.readFloat());
                axis.set(bis.readFloat(), bis.readFloat(), bis.readFloat(), bis.readFloat(), bis.readFloat(),
                        bis.readFloat(), bis.readFloat(), bis.readFloat(), bis.readFloat());
                tags[i] = new Md3Tag(name, origin, axis);
            }

            // Parse out surfaces
            final Md3Surface[] surfaces = new Md3Surface[header._numSurfaces];
            bis.seek(header._offsetSurface);
            for (int i = 0; i < header._numSurfaces; i++) {
                final int surfaceStart = bis.position();
                final int magic = bis.readInt();
                if (magic != ('3' << 24) + ('P' << 16) + ('D' << 8) + 'I') {
                    throw new Ardor3dException("Not an MD3 surface.");
                }
                surfaces[i] = new Md3Surface(magic, bis.readString(64), bis.readInt(), bis.readInt(), bis.readInt(),
                        bis.readInt(), bis.readInt(), bis.readInt(), bis.readInt(), bis.readInt(), bis.readInt(),
                        bis.readInt());
                // Parse out shaders
                bis.seek(surfaceStart + surfaces[i]._offsetShaders);
                for (int j = 0; j < surfaces[i]._numShaders; j++) {
                    // final String name = bis.readString(64);
                    // final int index = bis.readInt();
                    // unused yet
                }
                // Parse out triangles
                bis.seek(surfaceStart + surfaces[i]._offsetTriangles);
                for (int j = 0; j < surfaces[i]._triIndexes.length; j++) {
                    surfaces[i]._triIndexes[j] = bis.readInt();
                }
                // Parse out texture coordinates
                bis.seek(surfaceStart + surfaces[i]._offsetTexCoords);
                for (int j = 0; j < surfaces[i]._texCoords.length; j++) {
                    surfaces[i]._texCoords[j] = new Vector2(bis.readFloat(), bis.readFloat());
                }
                // Parse out vertices
                bis.seek(surfaceStart + surfaces[i]._offsetXyzNormals);
                for (int j = 0; j < surfaces[i]._numFrames; j++) {
                    for (int k = 0; k < surfaces[i]._numVerts; k++) {
                        surfaces[i]._verts[j][k] = new Vector3(bis.readShort(), bis.readShort(), bis.readShort())
                                .multiplyLocal(Md3Importer.XYZ_SCALE);
                        final int zenith = bis.readByte();
                        final int azimuth = bis.readByte();
                        final float lat = (float) (zenith * 2 * Math.PI / 255);
                        final float lng = (float) (azimuth * 2 * Math.PI / 255);
                        surfaces[i]._norms[j][k] = new Vector3(Math.cos(lat) * Math.sin(lng), Math.sin(lat)
                                * Math.sin(lng), Math.cos(lng));
                    }
                }
            }

            final Node node = new Node(header._name);
            for (int i = 0; i < header._numSurfaces; i++) {
                final Md3Surface surface = surfaces[i];
                final KeyframeController<Mesh> controller = new KeyframeController<Mesh>();
                final Mesh morphingMesh = new Mesh(surface._name);
                morphingMesh.getMeshData().setIndexBuffer(BufferUtils.createIntBuffer(surface._triIndexes));
                morphingMesh.getMeshData().setVertexBuffer(BufferUtils.createFloatBuffer(surface._verts[0]));
                morphingMesh.getMeshData().setNormalBuffer(BufferUtils.createFloatBuffer(surface._norms[0]));
                morphingMesh.getMeshData().setTextureBuffer(BufferUtils.createFloatBuffer(surface._texCoords), 0);
                node.attachChild(morphingMesh);
                controller.setMorphingMesh(morphingMesh);
                for (int j = 0; j < surface._numFrames; j++) {
                    final Md3Frame frame = frames[j];
                    final Mesh mesh = new Mesh(frame._name);
                    mesh.getMeshData().setVertexBuffer(BufferUtils.createFloatBuffer(surface._verts[j]));
                    mesh.getMeshData().setNormalBuffer(BufferUtils.createFloatBuffer(surface._norms[j]));
                    controller.setKeyframe(j, mesh);
                }
                morphingMesh.addController(controller);
                // TODO should I add a controller into the node?
            }

            // Make a store object to return
            final Md3DataStore store = new Md3DataStore(node);

            // store names
            for (final Md3Frame frame : frames) {
                store.getFrameNames().add(frame._name);
            }

            // TODO load the animation configuration file (animation.cfg): [sex f/m][first frame, num frames, looping
            // frames, frames per second]

            /**
             * TODO there is one .skin file per MD3 file, it contains at most one line per surface (?) with the name and
             * the texture filename (.jpg or .tga) and a tag per attachment to another MD3 file
             */

            return store;
        } catch (final Exception e) {
            throw new Error("Unable to load md3 resource from URL: " + resource, e);
        }
    }
}
