/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.util;

import java.nio.ByteBuffer;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.math.Vector3;

public class NormalMapUtil {

    /**
     * Generate an image from the given terrain height data to be used as a source for terrain normal maps.
     * 
     * @param heightmap
     *            the base height data. Generally this is the most detailed height data available. It must be a square
     *            heightmap, with a side of "side" as passed below.
     * @param side
     *            the number of samples on a side of the heightmap. This could be calculated by taking the squareroot of
     *            heightmap.length, but generally this number is well known by the caller.
     * @param heightScale
     *            the scaling factor applied to the heightMap values to get real world height.
     * @param xGridSpacing
     *            real world spacing between grid in the x direction
     * @param zGridSpacing
     *            real world spacing between grid in the z direction
     * @return the normal image.
     */
    public static Image constructNormalMap(final float[] heightmap, final int side, final double heightScale,
            final double xGridSpacing, final double zGridSpacing) {
        int x, z;
        final Vector3 n = new Vector3();
        final Vector3 n2 = new Vector3();
        final ByteBuffer data = ByteBuffer.allocateDirect(side * side * 3);
        final Image normalMap = new Image(ImageDataFormat.RGB, PixelDataType.UnsignedByte, side, side, data, null);
        for (z = 0; z < side; ++z) {
            for (x = 0; x < side; ++x) {
                if (x == 0 || z == 0 || x == side - 1 || z == side - 1) {
                    n.set(0, 0, 1);
                } else {
                    // change across "x" from point to our "left" to point on our "right"
                    double dXh = heightmap[z * side + x - 1] - heightmap[z * side + x + 1];
                    if (dXh != 0) {
                        // alter by our height scale
                        dXh *= heightScale;
                        // determine slope of perpendicular line
                        final double slopeX = 2.0 * xGridSpacing / dXh;
                        // now plug into cos(arctan(x)) to get unit length vector
                        n.setX(Math.copySign(1.0 / Math.sqrt(1 + slopeX * slopeX), dXh));
                        n.setY(0);
                        n.setZ(Math.abs(slopeX * n.getX()));
                    } else {
                        n.set(0, 0, 1);
                    }

                    // change across "z" from point "above" us to point "below" us
                    double dZh = heightmap[(z - 1) * side + x] - heightmap[(z + 1) * side + x];
                    if (dZh != 0) {
                        // alter by our height scale
                        dZh *= heightScale;
                        // determine slope of perpendicular line
                        final double slopeZ = 2.0 * zGridSpacing / dZh;
                        // now plug into cos(arctan(x)) to get unit length vector
                        n2.setX(0);
                        n2.setY(Math.copySign(1.0 / Math.sqrt(1 + slopeZ * slopeZ), dZh));
                        n2.setZ(Math.abs(slopeZ * n2.getY()));
                    } else {
                        n2.set(0, 0, 1);
                    }

                    // add together the vectors across X and Z and normalize to get final normal
                    n.addLocal(n2).normalizeLocal();
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
