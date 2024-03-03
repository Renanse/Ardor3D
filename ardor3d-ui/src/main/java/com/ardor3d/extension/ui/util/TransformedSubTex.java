/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.util;

import com.ardor3d.image.Texture;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.type.ReadOnlyTransform;

public class TransformedSubTex extends SubTex implements Comparable<TransformedSubTex> {

  final Transform _transform = new Transform();
  final Vector2 _pivot = new Vector2(.5, .5);
  private int _priority = 0;
  private Alignment _alignment = Alignment.BOTTOM_LEFT;

  public TransformedSubTex(final SubTex source) {
    this(source.getTexture(), source.getX(), source.getY(), source.getWidth(), source.getHeight());
  }

  public TransformedSubTex(final Texture texture) {
    super(texture);
  }

  public TransformedSubTex(final Texture texture, final int x, final int y, final int width, final int height) {
    super(texture, x, y, width, height);
  }

  public void setTransform(final ReadOnlyTransform transform) {
    _transform.set(transform);
  }

  public Transform getTransform() { return _transform; }

  public void setPivot(final Vector2 pivot) {
    _pivot.set(pivot);
  }

  public Vector2 getPivot() { return _pivot; }

  public void setAlignment(final Alignment alignment) { _alignment = alignment; }

  public Alignment getAlignment() { return _alignment; }

  public void setPriority(final int priority) { _priority = priority; }

  public int getPriority() { return _priority; }

  @Override
  public int compareTo(final TransformedSubTex other) {
    return _priority - other._priority;
  }
}
