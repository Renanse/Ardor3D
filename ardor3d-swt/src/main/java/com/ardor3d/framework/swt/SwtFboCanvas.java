/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.swt;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.input.MouseManager;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.ContextGarbageCollector;
import com.google.common.collect.Lists;

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
    private final List<Texture> _texList = Lists.newArrayList();
    private Quad _quad;

    public SwtFboCanvas(final Composite composite, final int style, final DisplaySettings settings) {
        super(composite, style, toGLData(settings));
        _settings = settings;
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

        _quad = new Quad("Quad", 111f, 111f);
        _quad.setModelBound(new BoundingBox());
        _quad.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        _quad.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        _quad.setSolidColor(ColorRGBA.WHITE);

        checkRTT();

        _canvasRenderer.getCamera().apply(_canvasRenderer.getRenderer());
        _inited = true;
    }

    private void checkRTT() {
        final Rectangle size = getClientArea();
        final int width = Math.max(1, size.width), height = Math.max(1, size.height);

        if (width == _oldWidth && height == _oldHeight) {
            return;
        }

        _oldWidth = width;
        _oldHeight = height;

        final DisplaySettings settings = _settings.resizedCopy(width, height);
        _canvasRenderer.init(settings, false);

        if (_fboTexture != null && _fboTexture.getTextureIdForContext(_canvasRenderer.getRenderContext()) != 0) {
            _canvasRenderer.getRenderer().deleteTexture(_fboTexture);
        }

        _rtt = TextureRendererFactory.INSTANCE.createTextureRenderer(settings, false, _canvasRenderer.getRenderer(),
                _canvasRenderer.getRenderContext().getCapabilities());

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
        _quad.updateWorldRenderStates(false);

        _quad.resize(width, height);
        _quad.setTranslation(width / 2f, height / 2f, 0);
        _quad.updateWorldTransform(false);
    }

    @Override
    public MouseManager getMouseManager() {
        return _manager;
    }

    @Override
    public void setMouseManager(final MouseManager manager) {
        _manager = manager;
    }
}