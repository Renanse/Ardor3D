/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.lwjgl3.swt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.widgets.Composite;
import org.lwjgl.opengl.swt.GLCanvas;
import org.lwjgl.opengl.swt.GLData;
import org.lwjgl.system.Platform;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.ICanvasListener;
import com.ardor3d.input.MouseManager;

/**
 * A canvas for embedding into SWT applications.
 */
public class Lwjgl3SwtCanvas extends GLCanvas implements Canvas {
	protected CanvasRenderer _canvasRenderer;
	protected boolean _inited = false;
	protected final GLData _passedGLData;

	protected List<ICanvasListener> _listeners = new ArrayList<>();

	public Lwjgl3SwtCanvas(final Composite composite, final int style, final GLData glData) {
		super(composite, style, glData);
		_passedGLData = getGLData();
		setCurrent();

		addListener(SWT.Resize, event -> {
			if (_listeners.isEmpty()) {
				return;
			}

			final int width = getContentWidth();
			final int height = getContentHeight();

			for (final ICanvasListener l : _listeners) {
				l.onResize(width, height);
			}
		});
	}

	@Override
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
		setCurrent();
		final int w = getContentWidth();
		final int h = getContentHeight();

		final DisplaySettings settings = new DisplaySettings(w, h, 0, 0, _passedGLData.alphaSize,
				_passedGLData.depthSize, _passedGLData.stencilSize, _passedGLData.samples, false, _passedGLData.stereo);

		_canvasRenderer.init(settings, false); // false - do not do back buffer swap, swt will do that.
		_inited = true;
	}

	@Override
	@MainThread
	public void init() {
		privateInit();
	}

	@Override
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
		final int height = getSize().y;
		int zoom = DPIUtil.getDeviceZoom(); // zoom is 100 if no scaling, 200 for 2x scaling, etc.
		if (Platform.get() == Platform.LINUX) zoom *= 2;
		return Math.round(zoom * height * 0.01f);
	}

	@Override
	public int getContentWidth() {
		final int width = getSize().x;
		int zoom = DPIUtil.getDeviceZoom(); // zoom is 100 if no scaling, 200 for 2x scaling, etc.
		if (Platform.get() == Platform.LINUX) zoom *= 2;
		return Math.round(zoom * width * 0.01f);
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
