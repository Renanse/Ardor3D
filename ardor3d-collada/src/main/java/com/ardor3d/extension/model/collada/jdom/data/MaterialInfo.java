/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import java.util.Map;

import com.ardor3d.image.Texture;
import com.ardor3d.renderer.state.MaterialState;
import com.google.common.collect.Maps;

public class MaterialInfo {

    private String _materialName;
    private String _profile;
    private String _technique;
    private final Map<String, String> _textureReferences = Maps.newHashMap();
    private final Map<String, Texture> _textures = Maps.newHashMap();
    private final Map<String, String> _textureFileNames = Maps.newHashMap();
    private boolean _useTransparency;
    private float _transparency = 1.0f;

    private MaterialState _materialState;

    public void setMaterialName(final String materialName) {
        _materialName = materialName;
    }

    public String getMaterialName() {
        return _materialName;
    }

    public void setProfile(final String profile) {
        _profile = profile;
    }

    public String getProfile() {
        return _profile;
    }

    public void setTechnique(final String technique) {
        _technique = technique;
    }

    public String getTechnique() {
        return _technique;
    }

    public void setTextureSlot(final String textureSlot, final String textureReference, final Texture texture,
            final String fileName) {
        _textureReferences.put(textureSlot, textureReference);
        _textures.put(textureSlot, texture);
        _textureFileNames.put(textureSlot, fileName);
    }

    public Map<String, String> getTextureReferences() {
        return _textureReferences;
    }

    public Map<String, Texture> getTextures() {
        return _textures;
    }

    public Map<String, String> getTextureFileNames() {
        return _textureFileNames;
    }

    public void setUseTransparency(final boolean useTransparency) {
        _useTransparency = useTransparency;
    }

    public boolean isUseTransparency() {
        return _useTransparency;
    }

    public void setMaterialState(final MaterialState state) {
        _materialState = state;
    }

    public MaterialState getMaterialState() {
        return _materialState;
    }

    public float getTransparency() {
        return _transparency;
    }

    public void setTransparency(final float transparency) {
        _transparency = transparency;
    }
}
