/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.collision;

import java.nio.FloatBuffer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.CollisionTree;
import com.ardor3d.bounding.CollisionTreeManager;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.intersection.CollisionData;
import com.ardor3d.intersection.CollisionResults;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitiveCollisionResults;
import com.ardor3d.intersection.PrimitiveKey;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.PQTorus;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.geom.BufferUtils;

/**
 * A demonstration of finding and retrieving collisions between two nodes.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.collision.CollisionTreeExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/collision_CollisionTreeExample.jpg", //
maxHeapMemory = 64)
public class CollisionTreeExample extends ExampleBase {
    private final ReadOnlyColorRGBA[] colorSpread = { ColorRGBA.WHITE, ColorRGBA.GREEN, ColorRGBA.GRAY };

    private Mesh sphere, torus;
    private Node sphereNode, torusNode;

    private CollisionResults results;
    private CollisionData oldData;

    private int updateCounter = 0;

    public static void main(final String[] args) {
        start(CollisionTreeExample.class);
    }

    @Override
    protected void initExample() {
        _lightState.setEnabled(false);

        CollisionTreeManager.getInstance().setTreeType(CollisionTree.Type.AABB);
        CollisionTreeManager.getInstance().setDoSort(true);

        results = new PrimitiveCollisionResults();
        sphere = new Sphere("sphere", 10, 10, 2);

        sphere.setSolidColor(ColorRGBA.WHITE);
        sphere.setModelBound(new BoundingBox());

        sphereNode = new Node("sphere node");

        torus = new PQTorus("torus", 5, 4, 2f, .5f, 128, 16);
        torus.setTranslation(new Vector3(0, 0, 0));
        torus.setSolidColor(ColorRGBA.WHITE);
        torus.setModelBound(new BoundingBox());

        torusNode = new Node("torus node");

        torus.addController(new SpatialController<PQTorus>() {
            private double currentTime;

            public void update(final double time, final PQTorus caller) {
                currentTime += time * 0.2;
                final ReadOnlyVector3 t = caller.getTranslation();
                caller.setTranslation(Math.sin(currentTime) * 10.0, t.getY(), t.getZ());
            }
        });

        final FloatBuffer color1 = torus.getMeshData().getColorBuffer();
        color1.clear();
        for (int i = 0, bLength = color1.capacity(); i < bLength; i += 4) {
            final ReadOnlyColorRGBA c = colorSpread[i % 3];
            color1.put(c.getRed()).put(c.getGreen()).put(c.getBlue()).put(c.getAlpha());
        }
        color1.flip();
        final FloatBuffer color2 = sphere.getMeshData().getColorBuffer();
        color2.clear();
        for (int i = 0, bLength = color2.capacity(); i < bLength; i += 4) {
            final ReadOnlyColorRGBA c = colorSpread[i % 3];
            color2.put(c.getRed()).put(c.getGreen()).put(c.getBlue()).put(c.getAlpha());
        }
        color2.flip();

        sphereNode.attachChild(torus);
        torusNode.attachChild(sphere);

        _root.attachChild(sphereNode);
        _root.attachChild(torusNode);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        updateCounter++;
        if (updateCounter < 5) {
            return;
        }
        updateCounter = 0;

        final int[] indexBuffer = new int[3];
        final MeshData sphereMD = sphere.getMeshData();
        final MeshData torusMD = torus.getMeshData();
        final FloatBuffer color1 = sphere.getMeshData().getColorBuffer();
        final FloatBuffer color2 = torus.getMeshData().getColorBuffer();

        if (oldData != null) {
            for (int j = 0; j < oldData.getSourcePrimitives().size(); j++) {
                final PrimitiveKey key = oldData.getSourcePrimitives().get(j);
                sphereMD.getPrimitiveIndices(key.getPrimitiveIndex(), key.getSection(), indexBuffer);
                BufferUtils.setInBuffer(colorSpread[indexBuffer[0] % 3], color1, indexBuffer[0]);
                BufferUtils.setInBuffer(colorSpread[indexBuffer[1] % 3], color1, indexBuffer[1]);
                BufferUtils.setInBuffer(colorSpread[indexBuffer[2] % 3], color1, indexBuffer[2]);
            }

            for (int j = 0; j < oldData.getTargetPrimitives().size(); j++) {
                final PrimitiveKey key = oldData.getTargetPrimitives().get(j);
                torusMD.getPrimitiveIndices(key.getPrimitiveIndex(), key.getSection(), indexBuffer);
                BufferUtils.setInBuffer(colorSpread[indexBuffer[0] % 3], color2, indexBuffer[0]);
                BufferUtils.setInBuffer(colorSpread[indexBuffer[1] % 3], color2, indexBuffer[1]);
                BufferUtils.setInBuffer(colorSpread[indexBuffer[2] % 3], color2, indexBuffer[2]);
            }
        }

        results.clear();
        PickingUtil.findCollisions(torusNode, sphereNode, results);

        if (results.getNumber() > 0) {
            oldData = results.getCollisionData(0);
            for (int i = 0; i < oldData.getSourcePrimitives().size(); i++) {
                final PrimitiveKey key = oldData.getSourcePrimitives().get(i);
                sphereMD.getPrimitiveIndices(key.getPrimitiveIndex(), key.getSection(), indexBuffer);
                BufferUtils.setInBuffer(ColorRGBA.RED, color1, indexBuffer[0]);
                BufferUtils.setInBuffer(ColorRGBA.RED, color1, indexBuffer[1]);
                BufferUtils.setInBuffer(ColorRGBA.RED, color1, indexBuffer[2]);
            }

            for (int i = 0; i < oldData.getTargetPrimitives().size(); i++) {
                final PrimitiveKey key = oldData.getTargetPrimitives().get(i);
                torusMD.getPrimitiveIndices(key.getPrimitiveIndex(), key.getSection(), indexBuffer);
                BufferUtils.setInBuffer(ColorRGBA.BLUE, color2, indexBuffer[0]);
                BufferUtils.setInBuffer(ColorRGBA.BLUE, color2, indexBuffer[1]);
                BufferUtils.setInBuffer(ColorRGBA.BLUE, color2, indexBuffer[2]);
            }
        }
    }
}