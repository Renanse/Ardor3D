/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.client.functions;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class Luminance12ToRGBFunction implements SourceCacheFunction {

    private static Luminance12ToRGBFunction INSTANCE;

    public static Luminance12ToRGBFunction getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Luminance12ToRGBFunction();
        }
        return INSTANCE;
    }

    public void doConversion(final ByteBuffer sourceData, final byte[] store, final int destX, final int destY,
            final int dataSize, final int tileSize) {
        final int offset = (destY * tileSize * dataSize + destX * tileSize) * 3;
        final ShortBuffer source = sourceData.asShortBuffer();
        try {
            int destIndex = offset;
            int sourceIndex = 0;
            for (int y = 0; y < tileSize; y++) {
                for (int x = 0; x < tileSize; x++) {
                    final short sourceShort = source.get(sourceIndex++);
                    // Mark wants us to shift off bottom 4 bits and cast.
                    final byte sourceValue = (byte) (sourceShort >> 4 & 0xFF);
                    store[destIndex++] = sourceValue;
                    store[destIndex++] = sourceValue;
                    store[destIndex++] = sourceValue;
                }
                destIndex += (dataSize - tileSize) * 3;
            }
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

}
