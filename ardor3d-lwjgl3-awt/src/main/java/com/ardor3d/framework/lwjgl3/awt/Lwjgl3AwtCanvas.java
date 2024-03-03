/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.framework.lwjgl3.awt;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.ICanvasListener;
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer;
import com.ardor3d.input.mouse.MouseManager;

/**
 * A canvas for embedding into AWT applications.
 */
public class Lwjgl3AwtCanvas extends AWTGLCanvas implements Canvas {

  @Serial
  private static final long serialVersionUID = 1L;

  protected CanvasRenderer _canvasRenderer;
  protected boolean _inited = false;

  protected List<ICanvasListener> _listeners = new ArrayList<>();

  public Lwjgl3AwtCanvas(final GLData data) {
    super(data);

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        if (_listeners.isEmpty()) {
          return;
        }

        final int width = getContentWidth();
        final int height = getContentHeight();

        for (final ICanvasListener l : _listeners) {
          l.onResize(width, height);
        }
      }
    });
  }

  public Lwjgl3AwtCanvas(final GLData data, final Lwjgl3CanvasRenderer renderer) {
    this(data);
    setCanvasRenderer(renderer);
  }

  @Override
  @MainThread
  public void init() {}

  @Override
  public void initGL() {
    GL.createCapabilities();
    privateInit();
  }

  @Override
  public void paintGL() {
    if (!_inited) {
      privateInit();
    }

    if (isVisible()) {
      if (_canvasRenderer.draw()) {
        swapBuffers();
        // no need to release context - AwtGLCanvas does that already
      }
    }
  }

  @Override
  public CanvasRenderer getCanvasRenderer() { return _canvasRenderer; }

  public void setCanvasRenderer(final CanvasRenderer renderer) { _canvasRenderer = renderer; }

  protected MouseManager _manager;

  @Override
  public MouseManager getMouseManager() { return _manager; }

  @Override
  public void setMouseManager(final MouseManager manager) { _manager = manager; }

  @MainThread
  private void privateInit() {
    // tell our parent to lay us out so we have the right starting size.
    getParent().doLayout();
    final int w = getContentWidth();
    final int h = getContentHeight();

    final DisplaySettings settings = new DisplaySettings(w, h, 0, 0, data.alphaSize, data.depthSize, data.stencilSize,
        data.samples, false, data.stereo);

    _canvasRenderer.init(this, settings, false);
    _inited = true;
  }

  @Override
  @MainThread
  public void draw(final CountDownLatch latch) {
    try {
      if (!isValid()) {
        return;
      }

      render();
    } finally {
      if (latch != null) {
        latch.countDown();
      }
    }
  }

  @Override
  public int getContentHeight() { return (int) Math.round(scaleToScreenDpi(getSize().getHeight())); }

  @Override
  public int getContentWidth() { return (int) Math.round(scaleToScreenDpi(getSize().getWidth())); }

  @Override
  public void addListener(final ICanvasListener listener) {
    _listeners.add(listener);
  }

  @Override
  public boolean removeListener(final ICanvasListener listener) {
    return _listeners.remove(listener);
  }

  public boolean isInited() { return _inited; }
}
