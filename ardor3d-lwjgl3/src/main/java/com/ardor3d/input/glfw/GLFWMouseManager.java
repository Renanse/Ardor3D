/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.glfw;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;

import com.ardor3d.framework.lwjgl3.GLFWCanvas;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.MouseManager;

public class GLFWMouseManager implements MouseManager {

    private final GLFWCanvas _canvas;

    private GrabbedState _grabbedState;

    public GLFWMouseManager(final GLFWCanvas canvas) {
        _canvas = canvas;
    }

    @Override
    public void setCursor(final MouseCursor cursor) {
        if (cursor == MouseCursor.SYSTEM_DEFAULT || cursor == null) {
            GLFW.glfwSetCursor(_canvas.getWindowId(), 0);
            return;
        }

        final GLFWImage glfwImage = GLFWImage.create();
        glfwImage.set(cursor.getWidth(), cursor.getHeight(), cursor.getImage().getData(0));

        final long cptr = GLFW.glfwCreateCursor(glfwImage, cursor.getHotspotX(), cursor.getHotspotY());
        GLFW.glfwSetCursor(_canvas.getWindowId(), cptr);
    }

    @Override
    public void setPosition(final int x, final int y) {
        GLFW.glfwSetCursorPos(_canvas.getWindowId(), x, y);
    }

    @Override
    public boolean isSetPositionSupported() {
        return true;
    }

    @Override
    public void setGrabbed(final GrabbedState grabbedState) {
        _grabbedState = grabbedState;
        GLFW.glfwSetInputMode(_canvas.getWindowId(), GLFW.GLFW_CURSOR,
                grabbedState == GrabbedState.GRABBED ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
    }

    @Override
    public GrabbedState getGrabbed() {
        return _grabbedState;
    }

    @Override
    public boolean isSetGrabbedSupported() {
        return true;
    }

}
