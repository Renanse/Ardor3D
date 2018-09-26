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
    protected Object _extra = null;
    protected Object _defaultValue = null;

    protected transient int _cachedLocation = -1;

    public UniformRef() {}

    public UniformRef(final String shaderVariableName, final UniformType type, final UniformSource source,
            final Object value) {
        this(shaderVariableName, type, source, value, null, null);
    }

    public UniformRef(final String shaderVariableName, final UniformType type, final UniformSource source,
            final Object value, final Object extra, final Object defaultValue) {
        _shaderVariableName = shaderVariableName;
        _type = type;
        _source = source;
        _value = value;
        _extra = extra;
        _defaultValue = defaultValue;
    }

    public UniformRef(final int location, final UniformType type, final UniformSource source, final Object value) {
        this(location, type, source, value, null, null);
    }

    public UniformRef(final int location, final UniformType type, final UniformSource source, final Object value,
            final Object extra, final Object defaultValue) {
        _location = location;
        _type = type;
        _source = source;
        _value = value;
        _extra = extra;
        _defaultValue = defaultValue;

        _shaderVariableName = null;
    }

    public int getLocation() {
        return _location;
    }

    public int getCachedLocation() {
        return _cachedLocation;
    }

    public void setCachedLocation(final int location) {
        _cachedLocation = location;
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

    public Object getExtra() {
        return _extra;
    }

    public Object getDefaultValue() {
        return _defaultValue;
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

        // TODO: Store value and extra
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        _shaderVariableName = capsule.readString("name", null);
        _location = capsule.readInt("location", -1);
        _type = capsule.readEnum("type", UniformType.class, UniformType.Float1);
        _source = capsule.readEnum("source", UniformSource.class, UniformSource.Value);

        // TODO: Read value and extra
    }
}
