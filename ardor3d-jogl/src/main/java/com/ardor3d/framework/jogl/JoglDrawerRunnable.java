package com.ardor3d.framework.jogl;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLRunnable;

public class JoglDrawerRunnable implements GLRunnable {
	
	private final JoglCanvasRenderer _canvasRenderer;
	
	public JoglDrawerRunnable(final JoglCanvasRenderer canvasRenderer) {
		_canvasRenderer = canvasRenderer;
	}
	
	@Override
    public boolean run(GLAutoDrawable glAutoDrawable) {
        _canvasRenderer.draw();
        return true;
    }
}