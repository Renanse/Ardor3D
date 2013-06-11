/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.media.nativewindow.util.Dimension;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLRunnable;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.image.Image;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.ScreenMode;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.util.ScreenModeUtil;

public class JoglNewtWindow implements NativeCanvas, NewtWindowContainer {

    private final JoglCanvasRenderer _canvasRenderer;
    private boolean _inited = false;
    private boolean _isClosing = false;

    private final DisplaySettings _settings;

    private final JoglDrawerRunnable _drawerGLRunnable;

    private final GLWindow _newtWindow;

    public JoglNewtWindow(final JoglCanvasRenderer canvasRenderer, final DisplaySettings settings) {
        this(canvasRenderer, settings, true, false, false, false);
    }

    public JoglNewtWindow(final JoglCanvasRenderer canvasRenderer, final DisplaySettings settings,
            final boolean onscreen, final boolean bitmapRequested, final boolean pbufferRequested,
            final boolean fboRequested) {
        this(canvasRenderer, settings, onscreen, bitmapRequested, pbufferRequested, fboRequested, null);
    }

    /**
     * @param displayConnection
     *            e.g. :0.0 on Linux, or null for default display
     */
    public JoglNewtWindow(final JoglCanvasRenderer canvasRenderer, final DisplaySettings settings,
            final boolean onscreen, final boolean bitmapRequested, final boolean pbufferRequested,
            final boolean fboRequested, final String displayConnection) {

        final Display display = NewtFactory.createDisplay(displayConnection);
        final Screen screen = NewtFactory.createScreen(display, 0);

        canvasRenderer.setDisplay(display);

        _newtWindow = GLWindow.create(screen, CapsUtil.getCapsForSettings(display, settings, onscreen, bitmapRequested,
                pbufferRequested, fboRequested));
        _drawerGLRunnable = new JoglDrawerRunnable(canvasRenderer);
        _settings = settings;
        _canvasRenderer = canvasRenderer;
        setAutoSwapBufferMode(false);
    }

    /**
     * Applies all settings not related to OpenGL (screen resolution, screen size, etc...)
     * */
    private void applySettings() {
        _newtWindow.setUndecorated(_settings.isFullScreen());
        _newtWindow.setFullscreen(_settings.isFullScreen());
        // FIXME Ardor3D does not allow to change the resolution
        /**
         * uses the filtering relying on resolution with the size to fetch only the screen mode matching with the
         * current resolution
         */
        if (_settings.isFullScreen()) {
            final Screen screen = _newtWindow.getScreen();
            List<ScreenMode> screenModes = screen.getScreenModes();
            // the resolution is provided by the user
            final Dimension dimension = new Dimension(_settings.getWidth(), _settings.getHeight());
            screenModes = ScreenModeUtil.filterByResolution(screenModes, dimension);
            screenModes = ScreenModeUtil.getHighestAvailableBpp(screenModes);
            if (_settings.getFrequency() > 0) {
                screenModes = ScreenModeUtil.filterByRate(screenModes, _settings.getFrequency());
            } else {
                screenModes = ScreenModeUtil.getHighestAvailableRate(screenModes);
            }
            screen.setCurrentScreenMode(screenModes.get(0));
        }
    }

    public void addKeyListener(final KeyListener keyListener) {
        _newtWindow.addKeyListener(keyListener);
    }

    public void addMouseListener(final MouseListener mouseListener) {
        _newtWindow.addMouseListener(mouseListener);
    }

    public void addWindowListener(final WindowListener windowListener) {
        _newtWindow.addWindowListener(windowListener);
    }

    public GLContext getContext() {
        return _newtWindow.getContext();
    }

    public int getWidth() {
        return _newtWindow.getWidth();
    }

    public int getHeight() {
        return _newtWindow.getHeight();
    }

    public int getX() {
        return _newtWindow.getX();
    }

    public int getY() {
        return _newtWindow.getY();
    }

    public boolean isVisible() {
        return _newtWindow.isVisible();
    }

    public void setSize(final int width, final int height) {
        _newtWindow.setTopLevelSize(width, height);
    }

    public void setVisible(final boolean visible) {
        _newtWindow.setVisible(visible);
    }

    public void setAutoSwapBufferMode(final boolean autoSwapBufferModeEnabled) {
        _newtWindow.setAutoSwapBufferMode(autoSwapBufferModeEnabled);
    }

    @MainThread
    public void init() {
        if (_inited) {
            return;
        }

        // Set the size very early to prevent the default one from being used (typically when exiting full screen mode)
        setSize(_settings.getWidth(), _settings.getHeight());
        // Make the window visible to realize the OpenGL surface.
        setVisible(true);
        if (_newtWindow.isRealized()) {
            _newtWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowDestroyNotify(final WindowEvent e) {
                    _isClosing = true;
                }

                // public void windowResized(final WindowEvent e) {
                // _newtWindow.invoke(true, new GLRunnable() {
                //
                // @Override
                // public boolean run(GLAutoDrawable glAutoDrawable) {
                // _canvasRenderer._camera.resize(_newtWindow.getWidth(), _newtWindow.getHeight());
                // _canvasRenderer._camera.setFrustumPerspective(_canvasRenderer._camera.getFovY(),
                // (float) _newtWindow.getWidth() / (float) _newtWindow.getHeight(),
                // _canvasRenderer._camera.getFrustumNear(),
                // _canvasRenderer._camera.getFrustumFar());
                // return true;
                // }
                // });
                // }
            });

            // Request the focus here as it cannot work when the window is not visible
            _newtWindow.requestFocus();
            applySettings();

            _canvasRenderer.setContext(getContext());

            _newtWindow.invoke(true, new GLRunnable() {
                @Override
                public boolean run(final GLAutoDrawable glAutoDrawable) {
                    _canvasRenderer.init(_settings, true);// true - do swap in renderer.
                    return true;
                }
            });
            _inited = true;
        }
    }

    public void draw(final CountDownLatch latch) {
        if (!_inited) {
            init();
        }

        if (/* isShowing() */isVisible()) {
            _newtWindow.invoke(true, _drawerGLRunnable);
        }
        if (latch != null) {
            latch.countDown();
        }
    }

    public JoglCanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }

    @Override
    public void close() {
        _newtWindow.destroy();
    }

    @Override
    public boolean isActive() {
        return _newtWindow.hasFocus();
    }

    @Override
    public boolean isClosing() {
        return _isClosing;
    }

    @Override
    public void setVSyncEnabled(final boolean enabled) {
        _newtWindow.invoke(true, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable glAutoDrawable) {
                _newtWindow.getGL().setSwapInterval(enabled ? 1 : 0);
                return false;
            }
        });
    }

    @Override
    public void setTitle(final String title) {
        _newtWindow.setTitle(title);
    }

    @Override
    public void setIcon(final Image[] iconImages) {
        // FIXME not supported by NEWT
    }

    @Override
    public void moveWindowTo(final int locX, final int locY) {
        _newtWindow.setPosition(locX, locY);
    }

    @Override
    public GLWindow getNewtWindow() {
        return _newtWindow;
    }
}
