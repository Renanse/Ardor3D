/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.material;

import java.io.IOException;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

public class VertexAttributeRef implements Savable {

    protected String _shaderVariableName;
    protected int _location;
    protected String _meshDataKey;

    public VertexAttributeRef() {}

    public VertexAttributeRef(final String shaderVariableName, final String meshDataKey) {
        _shaderVariableName = shaderVariableName;
        _meshDataKey = meshDataKey;

        _location = -1;
    }

    public VertexAttributeRef(final String nameAndKey) {
        this(nameAndKey, nameAndKey);
    }

    public VertexAttributeRef(final int location, final String meshDataKey) {
        _location = location;
        _meshDataKey = meshDataKey;

        _shaderVariableName = null;
    }

    public int getLocation() {
        return _location;
    }

    public String getShaderVariableName() {
        return _shaderVariableName;
    }

    public String getMeshDataKey() {
        return _meshDataKey;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends VertexAttributeRef> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_shaderVariableName, "name", null);
        capsule.write(_location, "location", -1);
        capsule.write(_meshDataKey, "key", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        _shaderVariableName = capsule.readString("name", null);
        _location = capsule.readInt("location", -1);
        _meshDataKey = capsule.readString("key", null);
    }
}
