/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.blendtree;

/**
 * Abstract blend tree source that takes two child sources and a blend weight [0.0, 1.0]. The
 * subclass is responsible for implementing how these two sources are combined.
 */
public abstract class AbstractTwoPartSource implements BlendTreeSource {
  /** Our first source. */
  private BlendTreeSource _sourceA;

  /** Our second source. */
  private BlendTreeSource _sourceB;

  /**
   * A key into the related AnimationManager's values store for pulling blend weighting. What blend
   * weighting is used for is up to the subclass.
   */
  private String _blendKey;

  public BlendTreeSource getSourceA() { return _sourceA; }

  public BlendTreeSource getSourceB() { return _sourceB; }

  public String getBlendKey() { return _blendKey; }

  public void setBlendKey(final String blendKey) { _blendKey = blendKey; }

  public void setSourceA(final BlendTreeSource sourceA) { _sourceA = sourceA; }

  public void setSourceB(final BlendTreeSource sourceB) { _sourceB = sourceB; }
}
