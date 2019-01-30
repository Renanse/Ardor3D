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

public class VertexAttributeRef {

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
}
