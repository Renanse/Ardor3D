/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import com.ardor3d.framework.DisplaySettings;

public class JoglAwtInitializerRunnable implements Runnable {

    private final JoglAwtCanvas _joglAwtCanvas;

    private final DisplaySettings _settings;

    public JoglAwtInitializerRunnable(final JoglAwtCanvas joglAwtCanvas, final DisplaySettings settings) {
        _joglAwtCanvas = joglAwtCanvas;
        _settings = settings;
    }

    @Override
    public void run() {
        // Make the window visible to realize the OpenGL surface.
        _joglAwtCanvas.setVisible(true);
        // Force the realization
        _joglAwtCanvas.display();
        if (_joglAwtCanvas.getDelegatedDrawable().isRealized()) {
            // Request the focus here as it cannot work when the window is not visible
            _joglAwtCanvas.requestFocus();
            // The OpenGL context has been created after the realization of the surface
            _joglAwtCanvas.getCanvasRenderer().setContext(_joglAwtCanvas.getContext());
            // As the canvas renderer knows the OpenGL context, it can be initialized
            _joglAwtCanvas.getCanvasRenderer().init(_settings, true);
        }
    }
}