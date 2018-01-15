/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.swt;

import java.util.concurrent.CountDownLatch;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.input.MouseManager;

/**
 * A canvas for embedding into SWT applications.
 */
public class SwtCanvas extends GLCanvas implements Canvas {
    private CanvasRenderer _canvasRenderer;
    private boolean _inited = false;
    private final GLData _passedGLData;

    public SwtCanvas(final Composite composite, final int style, final GLData glData) {
        super(composite, style, glData);
        _passedGLData = clone(glData);
        setCurrent();
    }

    private GLData clone(final GLData glData) {
        final GLData rVal = new GLData();
        rVal.accumAlphaSize = glData.accumAlphaSize;
        rVal.accumBlueSize = glData.accumBlueSize;
        rVal.accumGreenSize = glData.accumGreenSize;
        rVal.accumRedSize = glData.accumRedSize;
        rVal.alphaSize = glData.alphaSize;
        rVal.blueSize = glData.blueSize;
        rVal.depthSize = glData.depthSize;
        rVal.doubleBuffer = glData.doubleBuffer;
        rVal.greenSize = glData.greenSize;
        rVal.redSize = glData.redSize;
        rVal.sampleBuffers = glData.sampleBuffers;
        rVal.samples = glData.samples;
        rVal.stencilSize = glData.stencilSize;
        rVal.stereo = glData.stereo;
        return rVal;
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
        final Point size = getSize();

        setCurrent();

        final DisplaySettings settings = new DisplaySettings(Math.max(size.x, 1), Math.max(size.y, 1), 0, 0,
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
}
