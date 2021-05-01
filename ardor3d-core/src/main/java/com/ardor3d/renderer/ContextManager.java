/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ardor3d.renderer.RenderContext.RenderContextRef;
import com.google.common.collect.MapMaker;

public class ContextManager {

  protected static RenderContext currentContext = null;

  private static List<ContextCleanListener> _cleanListeners = new ArrayList<>();

  protected static final Map<Object, RenderContext> contextStore = new MapMaker().weakKeys().makeMap();

  /**
   * @return a RenderContext object representing the current OpenGL context.
   */
  public static RenderContext getCurrentContext() { return currentContext; }

  public static RenderContext switchContext(final Object contextKey) {
    currentContext = contextStore.get(contextKey);
    if (currentContext == null) {
      throw new IllegalArgumentException("contextKey not found in context store.");
    }
    return currentContext;
  }

  public static void removeContext(final Object contextKey) {
    contextStore.remove(contextKey);
  }

  public static void addContext(final Object contextKey, final RenderContext context) {
    contextStore.put(contextKey, context);
  }

  public static RenderContext getContextForKey(final Object key) {
    return contextStore.get(key);
  }

  /**
   * Find the first context we manage that uses the given shared opengl context.
   *
   * @param reference
   * @return
   */
  public static RenderContext getContextForSharableRef(final RenderContextRef reference) {
    if (reference == null) {
      return null;
    }
    for (final RenderContext context : contextStore.values()) {
      if (reference.equals(context.getSharableContextRef())) {
        return context;
      }
    }
    return null;
  }

  /**
   * Find the first context we manage that uses the given unique opengl context.
   *
   * @param reference
   * @return
   */
  public static RenderContext getContextForUniqueRef(final RenderContextRef reference) {
    if (reference == null) {
      return null;
    }
    for (final RenderContext context : contextStore.values()) {
      if (reference.equals(context.getUniqueContextRef())) {
        return context;
      }
    }
    return null;
  }

  public static void fireCleanContextEvent(final RenderContext renderContext) {
    for (final ContextCleanListener listener : _cleanListeners) {
      listener.cleanForContext(renderContext);
    }
  }

  public static void addContextCleanListener(final ContextCleanListener listener) {
    _cleanListeners.add(listener);
  }
}
