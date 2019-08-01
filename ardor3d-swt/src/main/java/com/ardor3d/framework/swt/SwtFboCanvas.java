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
import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.ICanvasListener;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.input.mouse.MouseManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.ContextGarbageCollector;

public class SwtFboCanvas extends GLCanvas implements com.ardor3d.framework.Canvas {
    // Canvas items
    protected CanvasRenderer _canvasRenderer;
    protected MouseManager _manager;

    // Lwjgl FBO related fields
    protected boolean _inited = false;
    protected final DisplaySettings _settings;
    protected TextureRenderer _rtt;

    protected int _oldWidth, _oldHeight;
    private Texture2D _fboTexture;
    private final List<Texture> _texList = new ArrayList<>();
    private Quad _quad;

    protected List<ICanvasListener> _listeners = new ArrayList<>();

    public SwtFboCanvas(final Composite composite, final int style, final DisplaySettings settings) {
        super(composite, style, toGLData(settings));
        _settings = settings;

        addListener(SWT.Resize, event -> {
            final Rectangle clientArea = getClientArea();
            for (final ICanvasListener l : _listeners) {
                l.onResize(clientArea.width, clientArea.height);
            }
        });
    }

    private static GLData toGLData(final DisplaySettings settings) {
        final GLData rVal = new GLData();
        rVal.doubleBuffer = true;
        return rVal;
    }

    public CanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }

    public void setCanvasRenderer(final CanvasRenderer renderer) {
        _canvasRenderer = renderer;
    }

    int oldRectW = 0, oldRectH = 0;

    public void draw(final CountDownLatch latch) {
        if (!_inited) {
            init();
        }

        if (!isDisposed() && isVisible()) {
            // draw our scene to FBO
            checkRTT();

            _rtt.setBackgroundColor(_canvasRenderer.getRenderer().getBackgroundColor());
            _rtt.render(_canvasRenderer.getScene(), _texList, Renderer.BUFFER_COLOR_AND_DEPTH);

            // now render our quad
            final Renderer renderer = _canvasRenderer.getRenderer();

            // clear color buffer
            renderer.clearBuffers(Renderer.BUFFER_COLOR);

            // draw ortho quad, textured with our actual scene
            _quad.draw(renderer);

            // flush render buckets
            renderer.flushFrame(false);

            // Clean up card garbage such as textures, vbos, etc.
            ContextGarbageCollector.doRuntimeCleanup(renderer);

            // swap our swt managed back buffer
            swapBuffers();
        }

        latch.countDown();
    }

    @MainThread
    public void init() {
        // tell our parent to lay us out so we have the right starting size.
        getParent().layout();

        _quad = new Quad("fsq", 2, 2);
        _quad.setRenderMaterial("unlit/textured/fsq.yaml");

        _quad.setModelBound(new BoundingBox());
        _quad.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        _quad.getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
        _quad.getSceneHints().setRenderBucketType(RenderBucketType.Skip);

        checkRTT();

        _canvasRenderer.getCamera().apply(_canvasRenderer.getRenderer());
        _inited = true;
    }

    private void checkRTT() {
        final Rectangle size = getClientArea();
        int width = Math.max(1, size.width), height = Math.max(1, size.height);

        if (width == _oldWidth && height == _oldHeight) {
            return;
        }

        _oldWidth = width;
        _oldHeight = height;

        // On Linux, HDPI scaling makes a mess of the canvas.
        width = org.eclipse.swt.internal.DPIUtil.autoScaleUp(width);
        height = org.eclipse.swt.internal.DPIUtil.autoScaleUp(height);

        final DisplaySettings settings = _settings.resizedCopy(width, height);
        _canvasRenderer.init(settings, false);

        if (_fboTexture != null && _fboTexture.getTextureIdForContext(_canvasRenderer.getRenderContext()) != 0) {
            _canvasRenderer.getRenderer().getTextureUtils().deleteTexture(_fboTexture);
        }

        _rtt = _canvasRenderer.getRenderer().createTextureRenderer(width, height, _settings.getDepthBits(),
                _settings.getSamples());

        // copy our camera settings from the canvas renderer camera.
        _rtt.getCamera().set(_canvasRenderer.getCamera());

        // now, merge our cameras
        _canvasRenderer.setCamera(_rtt.getCamera());

        _fboTexture = new Texture2D();
        _rtt.setupTexture(_fboTexture);
        _texList.clear();
        _texList.add(_fboTexture);

        final TextureState screen = new TextureState();
        screen.setTexture(_fboTexture);
        screen.setEnabled(true);

        _quad.setRenderState(screen);

        _quad.updateGeometricState(0);
    }

    @Override
    public MouseManager getMouseManager() {
        return _manager;
    }

    @Override
    public void setMouseManager(final MouseManager manager) {
        _manager = manager;
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

    @Override
    public void addListener(final ICanvasListener listener) {
        _listeners.add(listener);
    }

    @Override
    public boolean removeListener(final ICanvasListener listener) {
        return _listeners.remove(listener);
    }
}