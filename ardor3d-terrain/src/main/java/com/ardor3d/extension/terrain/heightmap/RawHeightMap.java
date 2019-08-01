/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.heightmap;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import com.ardor3d.util.LittleEndianDataInput;

public class RawHeightMap {
    private static final Logger logger = Logger.getLogger(RawHeightMap.class.getName());

    public enum HeightMapFormat {
        Byte, Short, UnsignedByte, UnsignedShort, Integer, Float
    }

    private HeightMapFormat format;
    private boolean isLittleEndian;

    private int size;
    private InputStream stream;
    private float heightData[];

    private boolean swapXY;
    private boolean flipX;
    private boolean flipY;

    private boolean loaded;

    public RawHeightMap(final String filename, final int size) throws Exception {
        // varify that filename and size are valid.
        if (null == filename || size <= 0) {
            throw new Exception("Must supply valid filename and " + "size (> 0)");
        }
        try {
            setup(new FileInputStream(filename), size);
        } catch (final FileNotFoundException e) {
            throw new Exception("height file not found: " + filename);
        }
    }

    public RawHeightMap(final InputStream stream, final int size) throws Exception {
        setup(stream, size);
    }

    public RawHeightMap(final URL resource, final int size) throws Exception {
        // varify that resource and size are valid.
        if (null == resource || size <= 0) {
            throw new Exception("Must supply valid resource and " + "size (> 0)");
        }

        try {
            setup(resource.openStream(), size);
        } catch (final IOException e) {
            throw new Exception("Unable to open height url: " + resource);
        }
    }

    private void setup(final InputStream stream, final int size) throws Exception {
        // varify that filename and size are valid.
        if (null == stream || size <= 0) {
            throw new Exception("Must supply valid stream and " + "size (> 0)");
        }

        this.stream = stream;
        this.size = size;
    }

    /**
     * <code>load</code> fills the height data array with the appropriate data from the set RAW image stream or file.
     * 
     * @return true if the load is successful, false otherwise.
     */
    public boolean loadHeightmap() {
        // initialize the height data attributes
        heightData = new float[size * size];

        // attempt to connect to the supplied file.
        BufferedInputStream bis = null;

        try {
            bis = new BufferedInputStream(stream);
            final DataInputStream dis = new DataInputStream(bis);
            DataInput di = dis;
            if (isLittleEndian) {
                di = new LittleEndianDataInput(dis);
            }

            // read the raw file
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int index;
                    int xIndex = x;
                    int yIndex = y;

                    if (flipX) {
                        xIndex = size - x - 1;
                    }
                    if (flipY) {
                        yIndex = size - y - 1;
                    }

                    if (swapXY) {
                        index = xIndex * size + yIndex;
                    } else {
                        index = yIndex * size + xIndex;
                    }

                    switch (format) {
                        case Byte:
                            heightData[index] = di.readByte() * 0.5f / Byte.MAX_VALUE + 0.5f;
                            break;
                        case Short:
                            heightData[index] = di.readShort() * 0.5f / Short.MAX_VALUE + 0.5f;
                            break;
                        case UnsignedByte:
                            heightData[index] = di.readUnsignedByte() * 0.5f / Byte.MAX_VALUE;
                            break;
                        case UnsignedShort:
                            heightData[index] = di.readUnsignedShort() * 0.5f / Short.MAX_VALUE;
                            break;
                        case Integer:
                            heightData[index] = di.readInt() * 0.5f / Integer.MAX_VALUE + 0.5f;
                            break;
                        case Float:
                            heightData[index] = di.readFloat() / Float.MAX_VALUE;
                            break;
                    }
                }
            }
            dis.close();
        } catch (final IOException e1) {
            logger.warning("Error reading height data from stream.");
            return false;
        }
        loaded = true;
        return true;
    }

    /**
     * @return the heightData
     */
    public float[] getHeightData() {
        if (!loaded) {
            loadHeightmap();
        }
        return heightData;
    }

    /**
     * @return the format
     */
    public HeightMapFormat getFormat() {
        return format;
    }

    /**
     * @param format
     *            the format to set
     */
    public void setFormat(final HeightMapFormat format) {
        this.format = format;
    }

    /**
     * @return the isLittleEndian
     */
    public boolean isLittleEndian() {
        return isLittleEndian;
    }

    /**
     * @param isLittleEndian
     *            the isLittleEndian to set
     */
    public void setLittleEndian(final boolean isLittleEndian) {
        this.isLittleEndian = isLittleEndian;
    }

    /**
     * @return the swapXY
     */
    public boolean isSwapXY() {
        return swapXY;
    }

    /**
     * @param swapXY
     *            the swapXY to set
     */
    public void setSwapXY(final boolean swapXY) {
        this.swapXY = swapXY;
    }

    /**
     * @return the flipX
     */
    public boolean isFlipX() {
        return flipX;
    }

    /**
     * @param flipX
     *            the flipX to set
     */
    public void setFlipX(final boolean flipX) {
        this.flipX = flipX;
    }

    /**
     * @return the flipY
     */
    public boolean isFlipY() {
        return flipY;
    }

    /**
     * @param flipY
     *            the flipY to set
     */
    public void setFlipY(final boolean flipY) {
        this.flipY = flipY;
    }

    /**
     * @return the loaded
     */
    public boolean isLoaded() {
        return loaded;
    }
}
