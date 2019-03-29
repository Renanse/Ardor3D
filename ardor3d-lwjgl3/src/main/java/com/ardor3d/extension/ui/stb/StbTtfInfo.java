/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://github.com/Renanse/Ardor3D/blob/master/LICENSE>.
 */

package com.ardor3d.extension.ui.stb;

import java.nio.ByteBuffer;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackedchar;

public class StbTtfInfo {

    public STBTTFontinfo info;
    public STBTTPackedchar.Buffer charData;
    public float scale;
    public float descent;
    public int textureId;
    public ByteBuffer ttf;

}
