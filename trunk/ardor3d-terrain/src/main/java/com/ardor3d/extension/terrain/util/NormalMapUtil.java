/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.util;

import java.nio.ByteBuffer;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.math.Vector3;

public class NormalMapUtil {

    public static Image constructNormalMap(final float[] heightmap, final double spacing, final double zScale,
            final double mapScale, final int side) {
        int x, z;
        final Vector3 n = new Vector3();
        final ByteBuffer data = ByteBuffer.allocateDirect(side * side * 3);
        final Image normalMap = new Image(ImageDataFormat.RGB, PixelDataType.UnsignedByte, side, side, data, null);
        for (z = 0; z < side; ++z) {
            for (x = 0; x < side; ++x) {
                n.setZ(1);
                if (x == 0 || z == 0 || x == side - 1 || z == side - 1) {
                    n.setX(0);
                    n.setY(0);
                } else {
                    n.setX(zScale * (heightmap[z * side + x - 1] - heightmap[z * side + x + 1]) / 2
                            / (mapScale * spacing));
                    n.setY(zScale * (heightmap[(z - 1) * side + x] - heightmap[(z + 1) * side + x]) / 2
                            / (mapScale * spacing));
                    n.normalizeLocal();
                }
                // System.err.println(n);
                data.put(3 * (z * side + x) + 0, (byte) ((int) (127 * n.getX()) + 128));
                data.put(3 * (z * side + x) + 1, (byte) ((int) (127 * n.getY()) + 128));
                data.put(3 * (z * side + x) + 2, (byte) ((int) (127 * n.getZ()) + 128));
            }
        }

        return normalMap;
    }
}
