/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.client.functions;

import java.nio.ByteBuffer;

public class RGBA8ToRGBAFunction implements SourceCacheFunction {

    private static RGBA8ToRGBAFunction INSTANCE;

    public static RGBA8ToRGBAFunction getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RGBA8ToRGBAFunction();
        }
        return INSTANCE;
    }

    public void doConversion(final ByteBuffer sourceData, final byte[] store, final int destX, final int destY,
            final int dataSize, final int tileSize) {
        try {
            int destIndex = (destY * tileSize * dataSize + destX * tileSize) * 4;
            int sourceIndex = 0;
            final int width = tileSize * 4;

            for (int y = 0; y < tileSize; y++) {
                sourceData.position(sourceIndex);
                sourceData.get(store, destIndex, width);

                sourceIndex += width;
                destIndex += dataSize * 4;
            }
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

}
