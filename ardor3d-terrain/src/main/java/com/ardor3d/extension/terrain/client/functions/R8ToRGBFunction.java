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

public class R8ToRGBFunction implements SourceCacheFunction {

    private static R8ToRGBFunction INSTANCE;

    public static R8ToRGBFunction getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new R8ToRGBFunction();
        }
        return INSTANCE;
    }

    public void doConversion(final ByteBuffer sourceData, final byte[] store, final int destX, final int destY,
            final int dataSize, final int tileSize) {
        final int offset = (destY * tileSize * dataSize + destX * tileSize) * 3;
        try {
            int destIndex = offset;
            int sourceIndex = 0;
            for (int y = 0; y < tileSize; y++) {
                for (int x = 0; x < tileSize; x++) {
                    final byte sourceValue = sourceData.get(sourceIndex++);
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
