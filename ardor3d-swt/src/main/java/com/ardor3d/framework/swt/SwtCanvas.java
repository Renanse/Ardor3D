/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.framework.swt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.ICanvasListener;
import com.ardor3d.input.mouse.MouseManager;

/**
 * A canvas for embedding into SWT applications.
 */
public class SwtCanvas extends GLCanvas implements Canvas {
    protected CanvasRenderer _canvasRenderer;
    protected boolean _inited = false;
    protected final GLData _passedGLData;

    protected List<ICanvasListener> _listeners = new ArrayList<>();

    public SwtCanvas(final Composite composite, final int style, final GLData glData) {
        super(composite, style, glData);
        _passedGLData = getGLData();
        setCurrent();

        addListener(SWT.Resize, event -> {
            final Rectangle clientArea = getClientArea();
            for (final ICanvasListener l : _listeners) {
                l.onResize(clientArea.width, clientArea.height);
            }
        });
    }

    public CanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }

    public void setCanvasRenderer(final CanvasRenderer renderer) {
        _canvasRenderer = renderer;
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

    @MainThread
    private void privateInit() {
        // tell our parent to lay us out so we have the right starting size.
        getParent().layout();
        final Rectangle size = getClientArea();

        setCurrent();

        final DisplaySettings settings = new DisplaySettings(Math.max(size.width, 1), Math.max(size.height, 1), 0, 0,
                _passedGLData.alphaSize, _passedGLData.depthSize, _passedGLData.stencilSize, _passedGLData.samples,
                false, _passedGLData.stereo);

        _canvasRenderer.init(settings, false); // false - do not do back buffer swap, swt will do that.
        _inited = true;
    }

    @MainThread
    public void init() {
        privateInit();
    }

    @MainThread
    public void draw(final CountDownLatch latch) {
        if (!_inited) {
            privateInit();
        }

        if (!isDisposed() && isVisible()) {
            setCurrent();

            if (_canvasRenderer.draw()) {
                swapBuffers();
                _canvasRenderer.releaseCurrentContext();
            }
        }

        latch.countDown();
    }

    @Override
    public int getContentHeight() {
        return scaleToHiDpi(getSize().y);
    }

    @Override
    public int getContentWidth() {
        return scaleToHiDpi(getSize().x);
    }

    @Override
    public void addListener(final ICanvasListener listener) {
        _listeners.add(listener);
    }

    @Override
    public boolean removeListener(final ICanvasListener listener) {
        return _listeners.remove(listener);
    }

    @Override
    public int scaleToHiDpi(final int size) {
        return ApplyScale ? org.eclipse.swt.internal.DPIUtil.autoScaleUp(size) : size;
    }

    @Override
    public int scaleFromHiDpi(final int size) {
        return ApplyScale ? org.eclipse.swt.internal.DPIUtil.autoScaleDown(size) : size;
    }

    public static boolean ApplyScale = true;
    static {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("mac os x")) {
            ApplyScale = false;
        }
    }
}
