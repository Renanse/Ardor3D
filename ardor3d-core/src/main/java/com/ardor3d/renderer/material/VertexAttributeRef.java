/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.material;

public class VertexAttributeRef {

  protected String _shaderVariableName;
  protected int _location = -1;
  protected int _stride = 0;
  protected int _offset = 0;
  protected int _divisor = 0;
  protected int _span = 1;
  protected boolean _normalized = false;
  protected String _meshDataKey;

  public VertexAttributeRef() {}

  public VertexAttributeRef(final String shaderVariableName, final String meshDataKey) {
    _shaderVariableName = shaderVariableName;
    _meshDataKey = meshDataKey;
  }

  public VertexAttributeRef(final String nameAndKey) {
    this(nameAndKey, nameAndKey);
  }

  public VertexAttributeRef(final int location, final String meshDataKey) {
    _location = location;
    _meshDataKey = meshDataKey;

    _shaderVariableName = null;
  }

  public int getLocation() { return _location; }

  public void setLocation(final int location) { _location = location; }

  public String getShaderVariableName() { return _shaderVariableName; }

  public String getMeshDataKey() { return _meshDataKey; }

  public int getDivisor() { return _divisor; }

  public void setDivisor(final int divisor) { _divisor = divisor; }

  public int getStride() { return _stride; }

  public void setStride(final int stride) { _stride = stride; }

  public int getOffset() { return _offset; }

  public void setOffset(final int offset) { _offset = offset; }

  public int getSpan() { return _span; }

  public void setSpan(final int span) { _span = span; }

  public boolean isNormalized() { return _normalized; }

  public void setNormalized(final boolean normalized) { _normalized = normalized; }
}
