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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.ICanvasListener;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.input.focus.FocusWrapper;
import com.ardor3d.input.mouse.MouseManager;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;

public class GLFWCanvas implements NativeCanvas, FocusWrapper {
  private static final Logger logger = Logger.getLogger(GLFWCanvas.class.getName());

  protected final Lwjgl3CanvasRenderer _canvasRenderer;

  protected final DisplaySettings _settings;
  protected boolean _inited = false;

  protected long _windowId;
  protected int glMajorVersion = 3;
  protected int glMinorVersion = 3;

  protected volatile boolean _focusLost = false;

  protected GLFWWindowFocusCallback _focusCallback;
  protected GLFWErrorCallback _errorCallback;
  protected GLFWWindowSizeCallbackI _resizeCallback;

  protected int _contentWidth, _contentHeight;
  protected float _dpiScale = 1f;
  protected boolean _doSwap = true;

  protected List<ICanvasListener> _listeners = new ArrayList<>();

  public GLFWCanvas(final DisplaySettings settings, final Lwjgl3CanvasRenderer canvasRenderer) {
    _canvasRenderer = canvasRenderer;
    _settings = settings;
  }

  @Override
  public void init() {
    if (_inited) {
      return;
    }

    GLFW.glfwSetErrorCallback(_errorCallback = GLFWErrorCallback.createPrint(System.err));

    if (!GLFW.glfwInit()) {
      throw new IllegalStateException("Unable to initialize GLFW");
    }

    try {
      setWindowHints();

      if (_settings.isFullScreen()) {
        // TODO: allow choice of monitor
        final long primary = GLFW.glfwGetPrimaryMonitor();
        _windowId = GLFW.glfwCreateWindow(_settings.getWidth(), _settings.getHeight(), "Ardor3D", primary, 0);
      } else {
        _windowId = GLFW.glfwCreateWindow(_settings.getWidth(), _settings.getHeight(), "Ardor3D", 0, 0);
      }

      if (_windowId == 0) {
        throw new RuntimeException("Failed to create the GLFW window");
      }

      GLFW.glfwMakeContextCurrent(_windowId);
      _canvasRenderer.setCanvasCallback(new GLFWCanvasCallback(this::getWindowId));

      updateContentSize();

      GLFW.glfwSetWindowFocusCallback(_windowId, _focusCallback = new GLFWWindowFocusCallback() {
        @Override
        public void invoke(final long window, final boolean focused) {
          if (!focused) {
            _focusLost = true;
          }
        }
      });

      GLFW.glfwSetWindowSizeCallback(_windowId, _resizeCallback = (window, width, height) -> updateContentSize());

      GLFW.glfwShowWindow(_windowId);

    } catch (final Exception e) {
      logger.severe("Cannot create window");
      logger.logp(Level.SEVERE, this.getClass().toString(), "initDisplay()", "Exception", e);
      throw new Ardor3dException("Cannot create window: " + e.getMessage());
    }

    _canvasRenderer.init(this, _settings, _doSwap);
    _inited = true;
  }

  protected void setWindowHints() {
    GLFW.glfwDefaultWindowHints();
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GL11C.GL_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL11C.GL_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, glMajorVersion);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, glMinorVersion);
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_SCALE_TO_MONITOR, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, _settings.getSamples());
    GLFW.glfwWindowHint(GLFW.GLFW_ALPHA_BITS, _settings.getAlphaBits());
    GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, _settings.getDepthBits());
    GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, _settings.getStencilBits());

    if (Platform.get() == Platform.MACOSX) {
      GLFW.glfwWindowHint(GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW.GLFW_FALSE);
    }
  }

  /**
   * Set the minimum OpenGL version to use. Default is 3.3
   *
   * NB: Must be called prior to {@link #init()}
   */
  public void setGlVersion(final int major, final int minor) {
    glMajorVersion = major;
    glMinorVersion = minor;
  }

  protected void updateContentSize() {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      final IntBuffer width = stack.mallocInt(1);
      final IntBuffer height = stack.mallocInt(1);
      GLFW.glfwGetFramebufferSize(_windowId, width, height);
      _contentWidth = width.get();
      _contentHeight = height.get();

      final FloatBuffer xscale = stack.mallocFloat(1);
      final FloatBuffer yscale = stack.mallocFloat(1);
      GLFW.glfwGetWindowContentScale(_windowId, xscale, yscale);
      _dpiScale = (xscale.get() + yscale.get()) / 2f;
    }
    for (final ICanvasListener l : _listeners) {
      l.onResize(_contentWidth, _contentHeight);
    }
  }

  @Override
  public void draw(final CountDownLatch latch) {
    if (!_inited) {
      init();
    }

    _canvasRenderer.draw();

    if (latch != null) {
      latch.countDown();
    }
  }

  @Override
  public CanvasRenderer getCanvasRenderer() { return _canvasRenderer; }

  protected MouseManager _manager;

  @Override
  public MouseManager getMouseManager() { return _manager; }

  @Override
  public void setMouseManager(final MouseManager manager) { _manager = manager; }

  @Override
  public void close() {
    if (_windowId != 0) {
      Callbacks.glfwFreeCallbacks(_windowId);
      GLFW.glfwDestroyWindow(_windowId);
      _windowId = 0;
    }
    if (!Constants.useMultipleContexts) {
      GLFW.glfwTerminate();
    }
  }

  @Override
  public boolean isActive() {
    // XXX: Needs more investigation
    return (_windowId != 0) ? GLFW.glfwGetWindowAttrib(_windowId, GLFW.GLFW_FOCUSED) != 0 : false;
  }

  @Override
  public boolean isClosing() { return (_windowId != 0) ? GLFW.glfwWindowShouldClose(_windowId) : true; }

  @Override
  public void setVSyncEnabled(final boolean enabled) {
    GLFW.glfwSwapInterval(enabled ? 1 : 0);
  }

  @Override
  public void setTitle(final String title) {
    if (_windowId != 0) {
      GLFW.glfwSetWindowTitle(_windowId, title);
    }
  }

  @Override
  public void setIcon(final Image[] iconImages) {
    if (iconImages.length == 0) {
      throw new IllegalArgumentException("Must have at least one icon image.  Only the first is used.");
    }

    Image image = iconImages[0];
    if (image.getDataType() != PixelDataType.UnsignedByte) {
      throw new Ardor3dException("Your icon is in a format that could not be converted to UnsignedByte - RGBA");
    }

    if (image.getDataFormat() != ImageDataFormat.RGBA) {
      if (image.getDataFormat() != ImageDataFormat.RGB) {
        throw new Ardor3dException("Your icon is in a format that could not be converted to UnsignedByte - RGBA");
      }
      image = _RGB888_to_RGBA8888(image);
    }

    final ByteBuffer iconData = image.getData(0);
    iconData.rewind();

    final GLFWImage img = GLFWImage.malloc();
    final GLFWImage.Buffer imagebf = GLFWImage.malloc(1);
    img.set(image.getWidth(), image.getHeight(), iconData);
    imagebf.put(0, img);
    GLFW.glfwSetWindowIcon(_windowId, imagebf);
  }

  protected static Image _RGB888_to_RGBA8888(final Image rgb888) {
    final int size = rgb888.getWidth() * rgb888.getHeight() * 4;

    final ByteBuffer rgb = rgb888.getData(0);

    final ByteBuffer rgba8888 = BufferUtils.createByteBuffer(size);
    rgb.rewind();
    for (int j = 0; j < size; j++) {
      if ((j + 1) % 4 == 0) {
        rgba8888.put((byte) 0xFF);
      } else {
        rgba8888.put(rgb.get());
      }
    }
    return new Image(ImageDataFormat.RGBA, PixelDataType.UnsignedByte, rgb888.getWidth(), rgb888.getHeight(), rgba8888,
        null);
  }

  @Override
  public void moveWindowTo(final int locX, final int locY) {
    if (_windowId != 0) {
      GLFW.glfwSetWindowPos(_windowId, locX, locY);
    }
  }

  public void resize(final int width, final int height) {
    if (_windowId != 0) {
      GLFW.glfwSetWindowSize(_windowId, width, height);
    }
  }

  @Override
  public boolean getAndClearFocusLost() {
    final boolean result = _focusLost;

    _focusLost = false;

    return result;
  }

  public long getWindowId() { return _windowId; }

  @Override
  public int getContentHeight() { return _contentHeight; }

  @Override
  public int getContentWidth() { return _contentWidth; }

  @Override
  public void addListener(final ICanvasListener listener) {
    _listeners.add(listener);
  }

  @Override
  public boolean removeListener(final ICanvasListener listener) {
    return _listeners.remove(listener);
  }

  @Override
  public double scaleToScreenDpi(final double size) {
    return size * _dpiScale;
  }

  @Override
  public double scaleFromScreenDpi(final double size) {
    return size / _dpiScale;
  }

  public boolean isDoSwap() { return _doSwap; }

  /**
   * @param doSwap
   *          true if the canvas renderer should auto swap and release context after draw. Setting
   *          this value only has an effect if done prior to init().
   */
  public void setDoSwap(final boolean doSwap) { _doSwap = doSwap; }
}
