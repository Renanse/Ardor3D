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

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL11C;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.ICanvasListener;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.image.Image;
import com.ardor3d.input.mouse.MouseManager;
import com.ardor3d.renderer.lwjgl3.Lwjgl3CanvasCallback;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

public class GLFWHeadlessCanvas implements NativeCanvas {
    private static final Logger logger = Logger.getLogger(GLFWCanvas.class.getName());

    protected final Lwjgl3CanvasRenderer _canvasRenderer;

    protected final DisplaySettings _settings;
    protected boolean _inited = false;

    protected long _windowId;

    protected GLFWErrorCallback _errorCallback;

    /**
     * If true, we will not try to drop and reclaim the context on each frame.
     */
    public static boolean SINGLE_THREADED_MODE = true;

    public GLFWHeadlessCanvas(final DisplaySettings settings, final Lwjgl3CanvasRenderer canvasRenderer) {
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
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL11C.GL_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GL11C.GL_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
            GLFW.glfwSetErrorCallback(_errorCallback = GLFWErrorCallback.createPrint(System.err));
            _windowId = GLFW.glfwCreateWindow(_settings.getWidth(), _settings.getHeight(), "Ardor3D", 0, 0);

        } catch (final Exception e) {
            logger.severe("Cannot create window");
            logger.logp(Level.SEVERE, this.getClass().toString(), "initDisplay()", "Exception", e);
            throw new Ardor3dException("Cannot create window: " + e.getMessage());
        }

        _canvasRenderer.init(_settings, true); // true - do swap in renderer.
        _inited = true;
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
        return false;
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
    public void setTitle(final String title) {}

    public void setIcon(final Image[] iconImages) {}

    @Override
    public void moveWindowTo(final int locX, final int locY) {}

    public long getWindowId() {
        return _windowId;
    }

    @Override
    public int getContentHeight() {
        return 0;
    }

    @Override
    public int getContentWidth() {
        return 0;
    }

    @Override
    public void addListener(final ICanvasListener listener) {}

    @Override
    public boolean removeListener(final ICanvasListener listener) {
        return false;
    }
}
