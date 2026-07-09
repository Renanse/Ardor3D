/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.glfw;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;

import com.ardor3d.framework.lwjgl3.GLFWCanvas;
import com.ardor3d.input.mouse.GrabbedState;
import com.ardor3d.input.mouse.MouseCursor;
import com.ardor3d.input.mouse.MouseManager;

public class GLFWMouseManager implements MouseManager {

  private static final Logger logger = Logger.getLogger(GLFWMouseManager.class.getName());

  private final GLFWCanvas _canvas;

  private GrabbedState _grabbedState;

  /**
   * Native GLFW cursor handle per {@link MouseCursor}, so a repeated setCursor reuses the handle
   * instead of creating (and leaking) a fresh one every call. GLFW cursor objects are
   * process-global, not window-bound, so they survive resizes; they are dropped if the window they
   * were built under is gone (see the window-change guard in {@link #setCursor}) and destroyed by
   * {@link #cleanup()}.
   */
  private final Map<MouseCursor, Long> _cursorCache = new HashMap<>();
  /** The window id the cached handles were created under; a change means the old GLFW context, and
   * with it those handles, are gone. */
  private long _cacheWindowId = 0;
  /** The cursor currently applied to the window, to skip redundant native calls. */
  private MouseCursor _currentCursor;

  public GLFWMouseManager(final GLFWCanvas canvas) {
    _canvas = canvas;
  }

  @Override
  public void setCursor(final MouseCursor cursor) {
    final long windowId = _canvas.getWindowId();
    if (windowId == 0) {
      // No window (not created yet, or already closed) - nothing to apply a cursor to.
      return;
    }

    // If the window was recreated, GLFW was torn down and rebuilt, so any handles we cached were
    // freed with it. Drop them WITHOUT destroying (that would double-free) and rebuild under the
    // new window on demand below.
    if (windowId != _cacheWindowId) {
      _cursorCache.clear();
      _currentCursor = null;
      _cacheWindowId = windowId;
    }

    if (cursor == _currentCursor) {
      return;
    }

    if (cursor == null || cursor == MouseCursor.SYSTEM_DEFAULT) {
      GLFW.glfwSetCursor(windowId, 0);
      _currentCursor = cursor;
      return;
    }

    Long cached = _cursorCache.get(cursor);
    if (cached == null) {
      final long created = createCursor(cursor);
      if (created == 0) {
        // Creation failed - leave the current cursor in place rather than blanking it, and do not
        // cache the failure so a later call can retry.
        return;
      }
      cached = created;
      _cursorCache.put(cursor, cached);
    }

    GLFW.glfwSetCursor(windowId, cached);
    _currentCursor = cursor;
  }

  /** Create a native GLFW cursor from the given cursor's image, or 0 if GLFW could not create one. */
  private long createCursor(final MouseCursor cursor) {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      final GLFWImage image = GLFWImage.malloc(stack);
      image.set(cursor.getWidth(), cursor.getHeight(), cursor.getImage().getData(0));
      final long cptr = GLFW.glfwCreateCursor(image, cursor.getHotspotX(), cursor.getHotspotY());
      if (cptr == 0) {
        logger.warning("glfwCreateCursor failed for cursor '" + cursor.getName() + "'");
      }
      return cptr;
    }
  }

  /**
   * Destroy every cached native cursor and forget them. Call before the window and GLFW are torn
   * down ({@link GLFWCanvas#close()} does). Safe to call more than once; a no-op once the window is
   * already gone, so it never touches native state after GLFW has terminated.
   */
  public void cleanup() {
    if (_canvas.getWindowId() == 0) {
      // Window (and possibly GLFW) already gone - the handles, if any, went with it. Just forget.
      _cursorCache.clear();
      _cacheWindowId = 0;
      _currentCursor = null;
      return;
    }
    for (final long cptr : _cursorCache.values()) {
      if (cptr != 0) {
        GLFW.glfwDestroyCursor(cptr);
      }
    }
    _cursorCache.clear();
    _cacheWindowId = 0;
    _currentCursor = null;
  }

  @Override
  public void setPosition(final int x, final int y) {
    GLFW.glfwSetCursorPos(_canvas.getWindowId(), x, _canvas.getContentHeight() - y);
  }

  @Override
  public boolean isSetPositionSupported() { return true; }

  @Override
  public void setGrabbed(final GrabbedState grabbedState) {
    _grabbedState = grabbedState;
    GLFW.glfwSetInputMode(_canvas.getWindowId(), GLFW.GLFW_CURSOR,
        grabbedState == GrabbedState.GRABBED ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
  }

  @Override
  public GrabbedState getGrabbed() { return _grabbedState; }

  @Override
  public boolean isSetGrabbedSupported() { return true; }

}
