/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.stb;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_UNSIGNED_INT_8_8_8_8_REV;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.MemoryStack;

import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.ardor3d.util.resource.ResourceUtils;

public class StbTtfReader {
    public static StbTtfInfo createFont(final String fontFile, final int fontHeight) {
        return createFont(fontFile, fontHeight, 1024, 1024, 32, 95);
    }

    public static StbTtfInfo createFont(final String fontFile, final int fontHeight, final int texWidth,
            final int texHeight, final int startUnicodeChar, final int charLength) {

        final StbTtfInfo font = new StbTtfInfo();

        final ResourceSource rsrc = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_FONT, fontFile);
        font.ttf = ResourceUtils.loadResourceAsByteBuffer(rsrc, 4096);
        font.info = STBTTFontinfo.create();
        font.charData = STBTTPackedchar.create(charLength);
        font.textureId = glGenTextures();

        try (MemoryStack stack = stackPush()) {
            stbtt_InitFont(font.info, font.ttf);
            font.scale = stbtt_ScaleForPixelHeight(font.info, fontHeight);

            final IntBuffer d = stack.mallocInt(1);
            stbtt_GetFontVMetrics(font.info, null, d, null);
            font.descent = d.get(0) * font.scale;

            final ByteBuffer bitmap = memAlloc(texWidth * texHeight);

            final STBTTPackContext pc = STBTTPackContext.mallocStack(stack);
            stbtt_PackBegin(pc, bitmap, texWidth, texHeight, 0, 1, NULL);
            stbtt_PackSetOversampling(pc, 4, 4);
            stbtt_PackFontRange(pc, font.ttf, 0, fontHeight, startUnicodeChar, font.charData);
            stbtt_PackEnd(pc);

            // Convert R8 to RGBA8
            final ByteBuffer texture = memAlloc(texWidth * texHeight * 4);
            for (int i = 0; i < bitmap.capacity(); i++) {
                texture.putInt(bitmap.get(i) << 24 | 0x00FFFFFF);
            }
            texture.flip();

            glBindTexture(GL_TEXTURE_2D, font.textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, texWidth, texHeight, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV,
                    texture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

            memFree(texture);
            memFree(bitmap);
        }

        return font;
    }
}
