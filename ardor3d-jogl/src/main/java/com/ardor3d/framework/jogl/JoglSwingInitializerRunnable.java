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

public class JoglSwingInitializerRunnable implements Runnable {

    private final JoglSwingCanvas _joglSwingCanvas;

    private final DisplaySettings _settings;

    public JoglSwingInitializerRunnable(final JoglSwingCanvas joglSwingCanvas, final DisplaySettings settings) {
        _joglSwingCanvas = joglSwingCanvas;
        _settings = settings;
    }

    @Override
    public void run() {
        // Make the window visible to realize the OpenGL surface.
        _joglSwingCanvas.setVisible(true);
        // Force the realization
        _joglSwingCanvas.display();
        if (_joglSwingCanvas.getDelegatedDrawable().isRealized()) {
            // Request the focus here as it cannot work when the window is not visible
            _joglSwingCanvas.requestFocus();
            // The OpenGL context has been created after the realization of the surface
            _joglSwingCanvas.getCanvasRenderer().setContext(_joglSwingCanvas.getContext());
            // As the canvas renderer knows the OpenGL context, it can be initialized
            _joglSwingCanvas.getCanvasRenderer().init(_settings, true);
        }
    }
}