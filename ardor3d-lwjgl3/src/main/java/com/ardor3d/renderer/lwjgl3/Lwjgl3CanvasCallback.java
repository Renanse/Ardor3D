
package com.ardor3d.renderer.lwjgl3;

public interface Lwjgl3CanvasCallback {

    /**
     * Request this canvas as the current opengl owner.
     */
    void makeCurrent(boolean force);

    /**
     * Release this canvas as the current opengl owner.
     */
    void releaseContext(boolean force);

    void doSwap();

}
