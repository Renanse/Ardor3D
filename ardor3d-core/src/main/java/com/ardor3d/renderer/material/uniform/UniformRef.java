/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.material.uniform;

import java.io.IOException;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

public class UniformRef implements Savable {

    protected String _shaderVariableName;
    protected int _location = -1;
    protected UniformType _type = UniformType.Float1;
    protected UniformSource _source = UniformSource.Value;
    protected Object _value = null;

    public UniformRef() {}

    public UniformRef(final String shaderVariableName, final UniformType type, final UniformSource source,
            final Object value) {
        _shaderVariableName = shaderVariableName;
        _type = type;
        _source = source;
        _value = value;
    }

    public UniformRef(final int location, final UniformType type, final UniformSource source, final Object value) {
        _location = location;
        _type = type;
        _source = source;
        _value = value;

        _shaderVariableName = null;
    }

    public int getLocation() {
        return _location;
    }

    public String getShaderVariableName() {
        return _shaderVariableName;
    }

    public UniformType getType() {
        return _type;
    }

    public UniformSource getSource() {
        return _source;
    }

    public Object getValue() {
        return _value;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends UniformRef> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_shaderVariableName, "name", null);
        capsule.write(_location, "location", -1);
        capsule.write(_type, "type", UniformType.Float1);
        capsule.write(_source, "source", UniformSource.Value);

        // TODO: Store value
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        _shaderVariableName = capsule.readString("name", null);
        _location = capsule.readInt("location", -1);
        _type = capsule.readEnum("type", UniformType.class, UniformType.Float1);
        _source = capsule.readEnum("source", UniformSource.class, UniformSource.Value);
    }
}
