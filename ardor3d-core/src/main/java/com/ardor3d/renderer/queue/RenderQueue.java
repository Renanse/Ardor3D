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

import java.util.LinkedHashMap;
import java.util.Map;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Ardor3dException;

public class RenderQueue {

  private final Map<RenderBucketType, RenderBucket> renderBuckets = new LinkedHashMap<>();

  public RenderQueue() {
    setupDefaultBuckets();
  }

  public void setupBuckets(final RenderBucketType[] renderBucketTypes, final RenderBucket[] buckets) {
    if (renderBucketTypes.length != buckets.length) {
      throw new Ardor3dException("Can't setup buckets, RenderBucketType and RenderBucket counts don't match.");
    }
    removeRenderBuckets();
    for (int i = 0; i < renderBucketTypes.length; i++) {
      setRenderBucket(renderBucketTypes[i], buckets[i]);
    }
  }

  private void setupDefaultBuckets() {
    setRenderBucket(RenderBucketType.PreBucket, new OpaqueRenderBucket());
    setRenderBucket(RenderBucketType.Shadow, new OpaqueRenderBucket());
    setRenderBucket(RenderBucketType.Opaque, new OpaqueRenderBucket());
    setRenderBucket(RenderBucketType.Transparent, new TransparentRenderBucket());
    setRenderBucket(RenderBucketType.OrthoOrder, new OrthoOrderRenderBucket());
    setRenderBucket(RenderBucketType.PostBucket, new OpaqueRenderBucket());
  }

  public void removeRenderBuckets() {
    renderBuckets.clear();
  }

  public void removeRenderBucket(final RenderBucketType type) {
    renderBuckets.remove(type);
  }

  public void setRenderBucket(final RenderBucketType type, final RenderBucket renderBucket) {
    renderBuckets.put(type, renderBucket);
  }

  public RenderBucket getRenderBucket(final RenderBucketType type) {
    return renderBuckets.get(type);
  }

  public void addToQueue(final Spatial spatial, final RenderBucketType type) {
    if (type == RenderBucketType.Inherit || type == RenderBucketType.Skip) {
      throw new Ardor3dException("Can't add spatial to bucket of type: " + type);
    }

    final RenderBucket renderBucket = getRenderBucket(type);
    if (renderBucket != null) {
      renderBucket.add(spatial);
    } else {
      throw new Ardor3dException("No bucket exists of type: " + type);
    }
  }

  public void removeFromQueue(final Spatial spatial, final RenderBucketType type) {
    if (type == RenderBucketType.Inherit || type == RenderBucketType.Skip) {
      throw new Ardor3dException("Can't remove spatial from bucket of type: " + type);
    }

    final RenderBucket renderBucket = getRenderBucket(type);
    if (renderBucket != null) {
      renderBucket.remove(spatial);
    } else {
      throw new Ardor3dException("No bucket exists of type: " + type);
    }
  }

  public void clearBuckets() {
    for (final RenderBucket renderBucket : renderBuckets.values()) {
      renderBucket.clear();
    }
  }

  public void sortBuckets() {
    for (final RenderBucket renderBucket : renderBuckets.values()) {
      renderBucket.sort();
    }
  }

  public void renderOnly(final Renderer renderer) {
    for (final RenderBucket renderBucket : renderBuckets.values()) {
      renderBucket.render(renderer);
    }
  }

  public void renderBuckets(final Renderer renderer) {
    for (final RenderBucket renderBucket : renderBuckets.values()) {
      renderBucket.sort();
      renderBucket.render(renderer);
      renderBucket.clear();
    }
  }

  public void pushBuckets() {
    for (final RenderBucket renderBucket : renderBuckets.values()) {
      renderBucket.pushBucket();
    }
  }

  public void popBuckets() {
    for (final RenderBucket renderBucket : renderBuckets.values()) {
      renderBucket.popBucket();
    }
  }
}
