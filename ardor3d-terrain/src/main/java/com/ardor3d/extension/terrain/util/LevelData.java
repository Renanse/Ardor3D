
package com.ardor3d.extension.terrain.util;

import java.nio.ByteBuffer;

public class LevelData {
    public int unit;
    public int x = Integer.MAX_VALUE, y = Integer.MAX_VALUE;
    public int offsetX, offsetY;
    public int textureOffsetX, textureOffsetY;
    public Region clipRegion;

    public ByteBuffer sliceData;

    public LevelData(final int unit, final int textureSize) {
        this.unit = unit;
        clipRegion = new Region(unit, 0, 0, textureSize, textureSize);
    }
}
