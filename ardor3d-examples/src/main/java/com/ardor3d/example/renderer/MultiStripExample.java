/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Illustrates mesh with several primitives (i.e. strip, quad, triangle).
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.MultiStripExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_MultiStripExample.jpg", //
maxHeapMemory = 64)
public class MultiStripExample extends ExampleBase {

    public static void main(final String[] args) {
        start(MultiStripExample.class);
    }

    @Override
    protected void processPicks(final PrimitivePickResults pickResults) {
        int i = 0;
        while (pickResults.getNumber() > 0
                && pickResults.getPickData(i).getIntersectionRecord().getNumberOfIntersections() == 0
                && ++i < pickResults.getNumber()) {
        }
        if (pickResults.getNumber() > i) {
            final PickData pick = pickResults.getPickData(i);
            final int section = pick.getIntersectionRecord().getIntersectionPrimitive(0).getSection();
            if (pick.getTarget() instanceof Mesh) {
                final Mesh hit = (Mesh) pick.getTarget();
                System.err.println("picked: " + hit + " section: " + section + " (type = "
                        + hit.getMeshData().getIndexMode(section) + ") primitive: "
                        + pick.getIntersectionRecord().getIntersectionPrimitive(0).getPrimitiveIndex());
            }
        } else {
            System.err.println("picked: nothing");
        }
    }

    /**
     * Sets up a mesh with several sub primitives (strip, quad, triangle)
     */
    @Override
    protected void initExample() {
        _canvas.setTitle("TestSharedMesh");

        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 0, 90));
        final Mesh mesh = new Mesh();
        final MeshData meshData = mesh.getMeshData();

        final FloatBuffer vertexBuffer = BufferUtils.createVector3Buffer(16);

        vertexBuffer.put(-30).put(0).put(0);
        vertexBuffer.put(-40).put(0).put(0);
        vertexBuffer.put(-40).put(10).put(0);
        vertexBuffer.put(-30).put(10).put(0);

        vertexBuffer.put(-10).put(0).put(0);
        vertexBuffer.put(-20).put(0).put(0);
        vertexBuffer.put(-20).put(10).put(0);
        vertexBuffer.put(-10).put(10).put(0);

        vertexBuffer.put(10).put(0).put(0);
        vertexBuffer.put(20).put(0).put(0);
        vertexBuffer.put(20).put(10).put(0);
        vertexBuffer.put(10).put(10).put(0);

        vertexBuffer.put(30).put(0).put(0);
        vertexBuffer.put(40).put(0).put(0);
        vertexBuffer.put(40).put(10).put(0);
        vertexBuffer.put(30).put(10).put(0);

        meshData.setVertexBuffer(vertexBuffer);

        final IntBuffer indexBuffer = BufferUtils.createIntBuffer(18);

        // Strips
        indexBuffer.put(0).put(3).put(1).put(2);
        indexBuffer.put(4).put(7).put(5).put(6);

        // Quad
        indexBuffer.put(8).put(9).put(10).put(11);

        // Triangles
        indexBuffer.put(12).put(13).put(15);
        indexBuffer.put(13).put(14).put(15);

        meshData.setIndexBuffer(indexBuffer);

        // Setting sub primitive data
        final int[] indexLengths = new int[] { 4, 4, 4, 6 };
        meshData.setIndexLengths(indexLengths);

        final IndexMode[] indexModes = new IndexMode[] { IndexMode.TriangleStrip, IndexMode.TriangleStrip,
                IndexMode.Quads, IndexMode.Triangles };
        meshData.setIndexModes(indexModes);

        mesh.updateModelBound();

        _root.attachChild(mesh);
    }
}
