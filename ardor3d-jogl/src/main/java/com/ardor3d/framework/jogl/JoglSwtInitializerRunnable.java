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

public class JoglSwtInitializerRunnable implements Runnable {

    private final JoglSwtCanvas _joglSwtCanvas;

    private final DisplaySettings _settings;

    public JoglSwtInitializerRunnable(final JoglSwtCanvas joglSwtCanvas, final DisplaySettings settings) {
        _joglSwtCanvas = joglSwtCanvas;
        _settings = settings;
    }

    @Override
    public void run() {
        // Make the window visible to realize the OpenGL surface.
        _joglSwtCanvas.setVisible(true);
        // Force the realization
        _joglSwtCanvas.display();
        if (_joglSwtCanvas.getDelegatedDrawable().isRealized()) {
            // Request the focus here as it cannot work when the window is not visible
            _joglSwtCanvas.setFocus();
            // The OpenGL context has been created after the realization of the surface
            _joglSwtCanvas.getCanvasRenderer().setContext(_joglSwtCanvas.getContext());
            // As the canvas renderer knows the OpenGL context, it can be initialized
            _joglSwtCanvas.getCanvasRenderer().init(_settings, true);
        }
    }

}
