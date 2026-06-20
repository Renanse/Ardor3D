/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import com.ardor3d.renderer.RenderContext.RenderContextRef;
import com.ardor3d.util.Constants;

public class ContextManager {

  private static final Logger logger = Logger.getLogger(ContextManager.class.getName());

  protected static RenderContext currentContext = null;

  private static List<ContextCleanListener> _cleanListeners = new ArrayList<>();

  protected static final Map<Object, RenderContext> contextStore = new WeakHashMap<>();

  /**
   * Latches true once we have warned about unsafe multi-context use, so the warning fires once.
   * {@code volatile} so a set on one registration thread is visible to another; a narrow check-then-set
   * race could still log twice, which is harmless for a one-time diagnostic.
   */
  protected static volatile boolean _warnedSingleContextMode = false;

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
    warnIfUnsafeMultiContext();
  }

  /**
   * Warn (once) if more than one distinct context is registered while
   * {@link Constants#useMultipleContexts} is off. In that mode {@link com.ardor3d.util.gc.ContextValueReference}
   * ignores the per-context key and keeps a single value, so non-sharable GL resources (VAOs, FBOs)
   * created in one context get handed back for another, corrupting rendering on the second context.
   * The fix is to launch with {@code -Dardor3d.useMultipleContexts}; this surfaces the silent
   * misconfiguration in one log line.
   */
  private static void warnIfUnsafeMultiContext() {
    if (_warnedSingleContextMode || Constants.useMultipleContexts) {
      return;
    }
    // Distinct RenderContext instances each carry a unique context ref, so >1 of them is exactly
    // the condition under which non-sharable GL resources would be wrongly reused across contexts.
    final Set<RenderContext> distinct = new HashSet<>(contextStore.values());
    if (distinct.size() > 1) {
      _warnedSingleContextMode = true;
      logger.warning("More than one OpenGL context has been registered, but Constants.useMultipleContexts"
          + " is false. Per-context GL resources (VAOs, FBOs, ...) are not tracked per context in this mode"
          + " and will be shared across all contexts, which can corrupt rendering on the second context."
          + " Launch with -Dardor3d.useMultipleContexts to enable per-context resource tracking.");
    }
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
