/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import com.ardor3d.framework.DisplaySettings;

public class JoglNewtAwtInitializerRunnable implements Runnable {

    private final JoglNewtAwtCanvas _joglNewtAwtCanvas;

    private final DisplaySettings _settings;

    public JoglNewtAwtInitializerRunnable(final JoglNewtAwtCanvas joglAwtCanvas, final DisplaySettings settings) {
        _joglNewtAwtCanvas = joglAwtCanvas;
        _settings = settings;
    }

    @Override
    public void run() {
        // Make the window visible to realize the OpenGL surface.
        _joglNewtAwtCanvas.setVisible(true);
        // Force the realization
        _joglNewtAwtCanvas.getNewtWindow().display();
        if (!_joglNewtAwtCanvas.getNewtWindow().getDelegatedDrawable().isRealized()) {
            throw new RuntimeException("The heavyweight AWT drawable cannot be realized");
        }
        // Request the focus here as it cannot work when the window is not visible
        _joglNewtAwtCanvas.requestFocus();
        // The OpenGL context has been created after the realization of the surface
        _joglNewtAwtCanvas.getCanvasRenderer().setContext(_joglNewtAwtCanvas.getNewtWindow().getContext());
        // As the canvas renderer knows the OpenGL context, it can be initialized
        _joglNewtAwtCanvas.getCanvasRenderer().init(_settings, true);
    }
}