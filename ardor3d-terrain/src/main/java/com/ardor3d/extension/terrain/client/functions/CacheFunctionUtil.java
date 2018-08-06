
package com.ardor3d.extension.terrain.client.functions;

import java.nio.ByteBuffer;

import com.ardor3d.image.TextureStoreFormat;

public class CacheFunctionUtil {
    public static void applyFunction(final boolean targetAlpha, final SourceCacheFunction function,
            final ByteBuffer sourceData, final byte[] store, final int destX, final int destY,
            final TextureStoreFormat format, final int tileSize, final int dataSize) {
        if (function == null) {
            switch (format) {
                case R8:
                    R8ToRGBFunction.getInstance().doConversion(sourceData, store, destX, destY, dataSize, tileSize);
                    return;
                case RG8:
                    RG8ToRGBAFunction.getInstance().doConversion(sourceData, store, destX, destY, dataSize, tileSize);
                    return;
                case RGB8:
                    if (!targetAlpha) {
                        RGB8ToRGBFunction.getInstance().doConversion(sourceData, store, destX, destY, dataSize,
                                tileSize);
                        return;
                    } else {
                        RGB8ToRGBAFunction.getInstance().doConversion(sourceData, store, destX, destY, dataSize,
                                tileSize);
                        return;
                    }
                case RGBA8:
                    if (!targetAlpha) {
                        RGBA8ToRGBFunction.getInstance().doConversion(sourceData, store, destX, destY, dataSize,
                                tileSize);
                        return;
                    } else {
                        RGBA8ToRGBAFunction.getInstance().doConversion(sourceData, store, destX, destY, dataSize,
                                tileSize);
                        return;
                    }
                default:
                    System.err.println("CacheFunctionUtil.applyFunction: Unhandled format: " + format);
                    break;
            }
        } else {
            function.doConversion(sourceData, store, destX, destY, dataSize, tileSize);
        }
    }
}
