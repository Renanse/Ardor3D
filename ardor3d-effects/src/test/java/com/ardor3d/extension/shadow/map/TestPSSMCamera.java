/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.shadow.map;

import junit.framework.Assert;

import org.junit.Test;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.math.Vector3;

public class TestPSSMCamera {
    @Test
    public void testBoxSphereCameraPack() {
        MockPSSMCamera camera = new MockPSSMCamera();
        camera.setLocation(0, 0, -10);
        camera.setFrustumPerspective(50, 1, 1, 100);

        final BoundingBox boundingBox = new BoundingBox();
        boundingBox.setCenter(new Vector3(0, 0, 10));
        boundingBox.setXExtent(2);
        boundingBox.setYExtent(2);
        boundingBox.setZExtent(2);

        camera.pack(boundingBox);

        final double boxNear1 = camera.getFrustumNear();
        final double boxFar1 = camera.getFrustumFar();

        Assert.assertEquals(new Vector3(2, 2, 2), camera.getExtents());

        camera = new MockPSSMCamera();
        camera.setLocation(0, 0, -10);
        camera.setFrustumPerspective(50, 1, 1, 100);

        final BoundingSphere boundingSphere = new BoundingSphere();
        boundingSphere.setCenter(new Vector3(0, 0, 10));
        boundingSphere.setRadius(2);

        camera.pack(boundingSphere);

        final double boxNear2 = camera.getFrustumNear();
        final double boxFar2 = camera.getFrustumFar();

        Assert.assertEquals(new Vector3(2, 2, 2), camera.getExtents());

        Assert.assertEquals(boxNear1, boxNear2);
        Assert.assertEquals(boxFar1, boxFar2);
    }
}
