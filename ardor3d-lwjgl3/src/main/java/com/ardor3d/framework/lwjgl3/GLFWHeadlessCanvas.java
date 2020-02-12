/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.framework.lwjgl3;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Image;
import com.ardor3d.util.Ardor3dException;

public class GLFWHeadlessCanvas extends GLFWCanvas {
    private static final Logger logger = Logger.getLogger(GLFWCanvas.class.getName());

    public GLFWHeadlessCanvas(final DisplaySettings settings, final Lwjgl3CanvasRenderer canvasRenderer) {
    	super(settings, canvasRenderer);
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
            _windowId = GLFW.glfwCreateWindow(_settings.getWidth(), _settings.getHeight(), "Ardor3D", 0, 0);

            if (_windowId == 0) {
                throw new RuntimeException("Failed to create the GLFW window");
            }

            GLFW.glfwMakeContextCurrent(_windowId);
            _canvasRenderer.setCanvasCallback(new GLFWCanvasCallback(this::getWindowId));

            updateContentSize();

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

        _canvasRenderer.init(this, _settings, _doSwap); // true - do swap in renderer.
        _inited = true;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void setTitle(final String title) {}

    @Override
    public void setIcon(final Image[] iconImages) {}

    @Override
    public void moveWindowTo(final int locX, final int locY) {}

}
