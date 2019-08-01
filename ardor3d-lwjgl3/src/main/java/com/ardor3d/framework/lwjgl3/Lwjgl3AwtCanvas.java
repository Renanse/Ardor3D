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

import static org.lwjgl.system.jawt.JAWTFunctions.JAWT_VERSION_1_4;

import java.awt.Canvas;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.lwjgl.system.jawt.JAWT;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.ICanvasListener;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.image.Image;
import com.ardor3d.input.Focus.FocusWrapper;
import com.ardor3d.input.mouse.MouseManager;

public class Lwjgl3AwtCanvas extends Canvas implements NativeCanvas, FocusWrapper {

    private static final long serialVersionUID = 1L;

    private final JAWT awt;

    private final Lwjgl3CanvasRenderer _canvasRenderer;

    private final DisplaySettings _settings;
    private final boolean _inited = false;

    protected int _contentWidth, _contentHeight;

    protected List<ICanvasListener> _listeners = new ArrayList<>();

    /**
     * If true, we will not try to drop and reclaim the context on each frame.
     */
    public static boolean SINGLE_THREADED_MODE = true;

    public Lwjgl3AwtCanvas(final DisplaySettings settings, final Lwjgl3CanvasRenderer canvasRenderer) {

        awt = JAWT.calloc();
        awt.version(JAWT_VERSION_1_4);
        _canvasRenderer = canvasRenderer;
        // _canvasRenderer.setCanvasCallback(new Lwjgl3CanvasCallback() {
        // @Override
        // public void makeCurrent(final boolean force) {
        // if (force || !SINGLE_THREADED_MODE) {
        // GLFW.glfwMakeContextCurrent(windowId);
        // }
        // }
        //
        // @Override
        // public void releaseContext(final boolean force) {
        // if (force || !SINGLE_THREADED_MODE) {
        // GLFW.glfwMakeContextCurrent(0);
        // }
        // }
        //
        // @Override
        // public void doSwap() {
        // if (Constants.stats) {
        // StatCollector.startStat(StatType.STAT_DISPLAYSWAP_TIMER);
        // }
        // GLFW.glfwSwapBuffers(windowId);
        // GLFW.glfwPollEvents();
        // if (Constants.stats) {
        // StatCollector.endStat(StatType.STAT_DISPLAYSWAP_TIMER);
        // }
        // }
        // });
        _settings = settings;

    }

    @Override
    public void init() {
        // TODO Auto-generated method stub

    }

    @Override
    public void draw(final CountDownLatch latch) {
        // TODO Auto-generated method stub

    }

    @Override
    public CanvasRenderer getCanvasRenderer() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MouseManager getMouseManager() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setMouseManager(final MouseManager manager) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean getAndClearFocusLost() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isActive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isClosing() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setVSyncEnabled(final boolean enabled) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setTitle(final String title) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setIcon(final Image[] iconImages) {
        // TODO Auto-generated method stub

    }

    @Override
    public void moveWindowTo(final int locX, final int locY) {
        // TODO Auto-generated method stub

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
