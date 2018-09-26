/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state.record;

import java.nio.DoubleBuffer;
import java.util.Collection;
import java.util.HashMap;

import com.ardor3d.image.Texture;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Maps;

public class TextureStateRecord extends StateRecord {

    public HashMap<Integer, TextureRecord> textures;
    public TextureUnitRecord[] units;
    public int currentUnit = -1;

    /**
     * temporary matrix buffer to flatline memory usage.
     */
    public final DoubleBuffer tmp_matrixBuffer = BufferUtils.createDoubleBuffer(16);

    public TextureStateRecord() {
        textures = Maps.newHashMap();
        units = new TextureUnitRecord[TextureState.MAX_TEXTURES];
        for (int i = 0; i < units.length; i++) {
            units[i] = new TextureUnitRecord();
        }
    }

    public TextureRecord getTextureRecord(final Integer textureId, final Texture.Type type) {
        TextureRecord tr = textures.get(textureId);
        if (tr == null) {
            tr = new TextureRecord();
            textures.put(textureId, tr);
        }
        return tr;
    }

    public void removeTextureRecord(final Integer textureId) {
        if (textureId == null) {
            return;
        }
        textures.remove(textureId);
        for (int i = 0; i < units.length; i++) {
            if (units[i].boundTexture == textureId.intValue()) {
                units[i].boundTexture = -1;
            }
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        currentUnit = -1;
        final Collection<TextureRecord> texs = textures.values();
        for (final TextureRecord tr : texs) {
            tr.invalidate();
        }
        for (int i = 0; i < units.length; i++) {
            units[i].invalidate();
        }
    }

    @Override
    public void validate() {
        super.validate();
        final Collection<TextureRecord> texs = textures.values();
        for (final TextureRecord tr : texs) {
            tr.validate();
        }
        for (int i = 0; i < units.length; i++) {
            units[i].validate();
        }
    }
}
