/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.queue;

import java.util.HashMap;
import java.util.Map;

public final class RenderBucketType {

  public static RenderBucketType getRenderBucketType(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("name must not be null!");
    }

    RenderBucketType bucketType = bucketTypeMap.get(name);
    if (bucketType == null) {
      bucketType = new RenderBucketType(name);
      bucketTypeMap.put(name, bucketType);
    }
    return bucketType;
  }

  private static final Map<String, RenderBucketType> bucketTypeMap = new HashMap<>();

  private final String name;

  private RenderBucketType(final String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return name();
  }

  /**
   * Use your parent's RenderBucketType. If you do not have a parent, {@link #Opaque} will be used
   * instead.
   */
  public static final RenderBucketType Inherit = getRenderBucketType("Inherit");

  /**
   * Used for objects that we want to guarantee will be rendered first.
   */
  public static final RenderBucketType PreBucket = getRenderBucketType("PreBucket");

  /**
   * TODO: Add definition.
   */
  public static final RenderBucketType Shadow = getRenderBucketType("Shadow");

  /**
   * Used for surfaces that are fully opaque - can not be seen through. Drawn from front to back.
   */
  public static final RenderBucketType Opaque = getRenderBucketType("Opaque");

  /**
   * Used for surfaces that are partially transparent or translucent - can be seen through. Drawn from
   * back to front. See also the flag
   * {@link com.ardor3d.renderer.queue.TransparentRenderBucket#setTwoPassTransparency(boolean)
   * TransparentRenderBucket.setTwoPassTransparency(boolean)} allowing you to enable two pass
   * transparency for more accurate results.
   */
  public static final RenderBucketType Transparent = getRenderBucketType("Transparent");

  /**
   * Uses {@link com.ardor3d.scenegraph.hint.SceneHints#getOrthoOrder() SceneHints.getOrthoOrder()} to
   * determine draw order.
   */
  public static final RenderBucketType OrthoOrder = getRenderBucketType("OrthoOrder");

  /**
   * Used for objects that we want to guarantee will be rendered last.
   */
  public static final RenderBucketType PostBucket = getRenderBucketType("PostBucket");

  /**
   * Do not use bucket system. Instead, draw the spatial immediately to the back buffer.
   */
  public static final RenderBucketType Skip = getRenderBucketType("Skip");
}
