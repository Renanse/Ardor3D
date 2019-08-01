/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.framework.lwjgl3;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.system.MemoryStack;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.ICanvasListener;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.input.Focus.FocusWrapper;
import com.ardor3d.input.mouse.MouseManager;
import com.ardor3d.renderer.lwjgl3.Lwjgl3CanvasCallback;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

public class GLFWCanvas implements NativeCanvas, FocusWrapper {
    private static final Logger logger = Logger.getLogger(GLFWCanvas.class.getName());

    protected final Lwjgl3CanvasRenderer _canvasRenderer;

    protected final DisplaySettings _settings;
    protected boolean _inited = false;

    protected long _windowId;

    protected volatile boolean _focusLost = false;

    protected GLFWWindowFocusCallback _focusCallback;

    protected GLFWErrorCallback _errorCallback;

    protected GLFWWindowSizeCallbackI _resizeCallback;

    protected int _contentWidth, _contentHeight;

    protected List<ICanvasListener> _listeners = new ArrayList<>();

    /**
     * If true, we will not try to drop and reclaim the context on each frame.
     */
    public static boolean SINGLE_THREADED_MODE = true;

    public GLFWCanvas(final DisplaySettings settings, final Lwjgl3CanvasRenderer canvasRenderer) {
        _canvasRenderer = canvasRenderer;
        _canvasRenderer.setCanvasCallback(new Lwjgl3CanvasCallback() {
            @Override
            public void makeCurrent(final boolean force) {
                if (force || !SINGLE_THREADED_MODE) {
                    GLFW.glfwMakeContextCurrent(_windowId);
                }
            }

            @Override
            public void releaseContext(final boolean force) {
                if (force || !SINGLE_THREADED_MODE) {
                    GLFW.glfwMakeContextCurrent(0);
                }
            }

            @Override
            public void doSwap() {
                if (Constants.stats) {
                    StatCollector.startStat(StatType.STAT_DISPLAYSWAP_TIMER);
                }
                GLFW.glfwSwapBuffers(_windowId);
                GLFW.glfwPollEvents();
                if (Constants.stats) {
                    StatCollector.endStat(StatType.STAT_DISPLAYSWAP_TIMER);
                }
            }
        });
        _settings = settings;
    }

    @Override
    public void init() {
        if (_inited) {
            return;
        }

        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        try {
            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL11C.GL_TRUE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
            GLFW.glfwSetErrorCallback(_errorCallback = GLFWErrorCallback.createPrint(System.err));
            if (_settings.isFullScreen()) {
                // TODO: allow choice of monitor
                final long primary = GLFW.glfwGetPrimaryMonitor();
                _windowId = GLFW.glfwCreateWindow(_settings.getWidth(), _settings.getHeight(), "Ardor3D", primary, 0);
            } else {
                _windowId = GLFW.glfwCreateWindow(_settings.getWidth(), _settings.getHeight(), "Ardor3D", 0, 0);
            }
            updateContentSize();

            GLFW.glfwSetWindowFocusCallback(_windowId, _focusCallback = new GLFWWindowFocusCallback() {
                @Override
                public void invoke(final long window, final boolean focused) {
                    if (!focused) {
                        _focusLost = true;
                    }
                }
            });

            GLFW.glfwSetWindowSizeCallback(_windowId, _resizeCallback = new GLFWWindowSizeCallbackI() {
                @Override
                public void invoke(final long window, final int width, final int height) {
                    updateContentSize();
                }
            });
        } catch (final Exception e) {
            logger.severe("Cannot create window");
            logger.logp(Level.SEVERE, this.getClass().toString(), "initDisplay()", "Exception", e);
            throw new Ardor3dException("Cannot create window: " + e.getMessage());
        }

        _canvasRenderer.init(_settings, true); // true - do swap in renderer.
        _inited = true;
    }

    private void updateContentSize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer width = stack.mallocInt(1);
            final IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(_windowId, width, height);
            _contentWidth = width.get();
            _contentHeight = height.get();
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

    public CanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }

    protected MouseManager _manager;

    @Override
    public MouseManager getMouseManager() {
        return _manager;
    }

    @Override
    public void setMouseManager(final MouseManager manager) {
        _manager = manager;
    }

    @Override
    public void close() {
        GLFW.glfwDestroyWindow(_windowId);
        GLFW.glfwTerminate();
    }

    @Override
    public boolean isActive() {
        // XXX: Needs more investigation
        return GLFW.glfwGetWindowAttrib(_windowId, GLFW.GLFW_FOCUSED) != 0;
    }

    @Override
    public boolean isClosing() {
        return GLFW.glfwWindowShouldClose(_windowId);
    }

    @Override
    public void setVSyncEnabled(final boolean enabled) {
        GLFW.glfwSwapInterval(enabled ? 1 : 0);
    }

    @Override
    public void setTitle(final String title) {
        GLFW.glfwSetWindowTitle(_windowId, title);
    }

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
                throw new Ardor3dException(
                        "Your icon is in a format that could not be converted to UnsignedByte - RGBA");
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

    private static Image _RGB888_to_RGBA8888(final Image rgb888) {
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
        return new Image(ImageDataFormat.RGBA, PixelDataType.UnsignedByte, rgb888.getWidth(), rgb888.getHeight(),
                rgba8888, null);
    }

    @Override
    public void moveWindowTo(final int locX, final int locY) {
        GLFW.glfwSetWindowPos(_windowId, locX, locY);
    }

    public boolean getAndClearFocusLost() {
        final boolean result = _focusLost;

        _focusLost = false;

        return result;
    }

    public long getWindowId() {
        return _windowId;
    }

    @Override
    public int getContentHeight() {
        return _contentHeight;
    }

    @Override
    public int getContentWidth() {
        return _contentWidth;
    }

    @Override
    public void addListener(final ICanvasListener listener) {
        _listeners.add(listener);
    }

    @Override
    public boolean removeListener(final ICanvasListener listener) {
        return _listeners.remove(listener);
    }
}
