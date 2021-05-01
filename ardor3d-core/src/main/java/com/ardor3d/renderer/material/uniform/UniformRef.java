/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.material.uniform;

import com.ardor3d.renderer.material.IUniformSupplier;

public class UniformRef {

  protected String _shaderVariableName;
  protected int _location = -1;
  protected UniformType _type = UniformType.Float1;
  protected UniformSource _source = UniformSource.Value;
  protected Object _value = null;
  protected Object _extra = null;
  protected Object _defaultValue = null;

  public transient IUniformSupplier _cachedDefaultSupplier;

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

  public int getLocation() { return _location; }

  public String getShaderVariableName() { return _shaderVariableName; }

  public UniformType getType() { return _type; }

  public UniformSource getSource() { return _source; }

  public Object getValue() { return _value; }

  public void setValue(final Object value) { _value = value; }

  public Object getExtra() { return _extra; }

  public Object getDefaultValue() { return _defaultValue; }
}
