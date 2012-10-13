
package com.ardor3d.extension.terrain.client.functions;

import java.nio.ByteBuffer;

import com.ardor3d.image.TextureStoreFormat;

public class CacheFunctionUtil {
    public static void applyFunction(final boolean targetAlpha, final SourceCacheFunction function,
            final ByteBuffer sourceData, final byte[] store, final int destX, final int destY,
            final TextureStoreFormat format, final int tileSize, final int dataSize) {
        if (function == null) {
            switch (format) {
                case Luminance8:
                    Luminance8ToRGBFunction.getInstance().doConversion(sourceData, store, destX, destY, dataSize,
                            tileSize);
                    return;
                case Luminance8Alpha8:
                    Luminance8Alpha8ToRGBAFunction.getInstance().doConversion(sourceData, store, destX, destY, dataSize, tileSize);
                    return;
                case Luminance12:
                    Luminance12ToRGBFunction.getInstance().doConversion(sourceData, store, destX, destY, dataSize,
                            tileSize);
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
            }
        } else {
            function.doConversion(sourceData, store, destX, destY, dataSize, tileSize);
        }
    }
}
