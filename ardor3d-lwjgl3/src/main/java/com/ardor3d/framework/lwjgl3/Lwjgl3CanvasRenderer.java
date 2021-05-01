/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.framework.lwjgl3;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.lwjgl3.Lwjgl3CanvasAdapter;
import com.ardor3d.renderer.lwjgl3.Lwjgl3CanvasCallback;
import com.ardor3d.renderer.lwjgl3.Lwjgl3ContextCapabilities;
import com.ardor3d.renderer.lwjgl3.Lwjgl3Renderer;
import com.ardor3d.util.Ardor3dException;

public class Lwjgl3CanvasRenderer implements CanvasRenderer {
  protected Scene _scene;
  protected Camera _camera;
  protected boolean _doSwap;
  protected Lwjgl3Renderer _renderer;

  // protected Object _context = new Object();
  protected int _frameClear = Renderer.BUFFER_COLOR_AND_DEPTH;

  private RenderContext _currentContext;
  private Lwjgl3CanvasCallback _canvasCallback;

  public Lwjgl3CanvasRenderer(final Scene scene) {
    _scene = scene;
    _canvasCallback = new Lwjgl3CanvasAdapter();
  }

  @MainThread
  protected ContextCapabilities createContextCapabilities() {
    return new Lwjgl3ContextCapabilities(GL.createCapabilities());
  }

  @Override
  public Lwjgl3Renderer createRenderer() {
    return new Lwjgl3Renderer();
  }

  /**
   * Note: this requires the OpenGL context is already current.
   */
  @Override
  public void init(final Canvas canvas, final DisplaySettings settings, final boolean doSwap) {
    _doSwap = doSwap;

    // Grab a shared context, if one is given.
    RenderContext sharedContext = null;
    if (settings.getShareContext() != null) {
      sharedContext = ContextManager.getContextForKey(settings.getShareContext().getRenderContext().getContextKey());
    }

    _canvasCallback.makeCurrent(true);

    final ContextCapabilities caps = createContextCapabilities();
    _currentContext = new RenderContext(this, caps, sharedContext);

    ContextManager.addContext(this, _currentContext);
    ContextManager.switchContext(this);
    ContextManager.getCurrentContext().setCurrentCanvasRenderer(this);

    _renderer = createRenderer();

    if (settings.getSamples() != 0) {
      GL11C.glEnable(GL13C.GL_MULTISAMPLE);
    }

    _renderer.setBackgroundColor(ColorRGBA.BLACK);

    final float aspectRatio = (float) canvas.getContentWidth() / (float) canvas.getContentHeight();

    if (_camera == null) {
      /** Set up how our camera sees. */
      _camera = new Camera(canvas.getContentWidth(), canvas.getContentHeight());
      _camera.setFrustumPerspective(45.0f, aspectRatio, 1, 1000);

      final Vector3 loc = new Vector3(0.0f, 0.0f, 10.0f);
      final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
      final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
      final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
      /** Move our camera to a correct place and orientation. */
      _camera.setFrame(loc, left, up, dir);
    } else {
      // use new width and height to set ratio.
      _camera.setFrustumPerspective(_camera.getFovY(), aspectRatio, _camera.getFrustumNear(), _camera.getFrustumFar());
    }
  }

  @Override
  public boolean draw() {
    // set up context for rendering this canvas
    makeCurrentContext();

    // render stuff, first apply our camera if we have one
    if (_camera != null) {
      _camera.apply(_renderer);
    }
    _renderer.clearBuffers(_frameClear);

    final boolean drew = _scene.render(_renderer);
    _renderer.flushFrame(drew && _doSwap);
    if (drew && _doSwap) {
      _canvasCallback.doSwap();
    }

    // release the context if we're done (swapped and all)
    if (_doSwap) {
      releaseCurrentContext();
    }

    return drew;
  }

  @Override
  public Camera getCamera() { return _camera; }

  @Override
  public Scene getScene() { return _scene; }

  @Override
  public void setScene(final Scene scene) { _scene = scene; }

  public Lwjgl3CanvasCallback getCanvasCallback() { return _canvasCallback; }

  public void setCanvasCallback(final Lwjgl3CanvasCallback canvasCallback) { _canvasCallback = canvasCallback; }

  @Override
  public Renderer getRenderer() { return _renderer; }

  @Override
  public void makeCurrentContext() throws Ardor3dException {
    _canvasCallback.makeCurrent(false);
    ContextManager.switchContext(this);
    ContextManager.getCurrentContext().setCurrentCanvasRenderer(this);
  }

  @Override
  public void releaseCurrentContext() {
    _canvasCallback.releaseContext(false);
  }

  @Override
  public void setCamera(final Camera camera) { _camera = camera; }

  @Override
  public RenderContext getRenderContext() { return _currentContext; }

  @Override
  public int getFrameClear() { return _frameClear; }

  @Override
  public void setFrameClear(final int buffers) { _frameClear = buffers; }
}
