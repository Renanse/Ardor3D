/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework;

import com.ardor3d.image.Image;
import java.nio.ByteBuffer;

public interface NativeCanvas extends Canvas {

    /**
     * <code>close</code> shutdowns and destroys any window contexts.
     */
    void close();

    /**
     * <code>isActive</code> returns true if the display is active.
     * 
     * @return whether the display system is active.
     */
    boolean isActive();

    /**
     * <code>isClosing</code> notifies if the window is currently closing. This could be caused via the application
     * itself or external interrupts such as alt-f4 etc.
     * 
     * @return true if the window is closing, false otherwise.
     */
    boolean isClosing();

    /**
     * <code>setVSyncEnabled</code> attempts to enable or disable monitor vertical synchronization. The method is a
     * "best attempt" to change the monitor vertical refresh synchronization, and is <b>not </b> guaranteed to be
     * successful. This is dependent on OS.
     * 
     * @param enabled
     *            <code>true</code> to synchronize, <code>false</code> to ignore synchronization
     */
    void setVSyncEnabled(boolean enabled);

    /**
     * Sets the title of the display system. This is usually reflected by the renderer as text in the menu bar.
     * 
     * @param title
     *            The new display title.
     */
    void setTitle(String title);

    /**
     * Sets one or more icons for the Canvas.
     * <p>
     * As a reference for usual platforms on number of icons and their sizes:
     * <ul>
     * <li>On Windows you should supply at least one 16x16 image and one 32x32.</li>
     * <li>Linux (and similar platforms) expect one 32x32 image.</li>
     * <li>Mac OS X should be supplied one 128x128 image.</li>
     * </ul>
     * </p>
     * <p>
     * Images should be in format RGBA8888. If they are not ardor3d will try to convert them using ImageUtils. If that
     * fails a <code>Ardor3dException</code> could be thrown.
     * </p>
     * 
     * @param iconImages
     *            Array of Images to be used as icons.
     */
    void setIcon(Image[] iconImages);

    void setIcon(final ByteBuffer[] icon);

    /**
     * If running in windowed mode, move the window's position to the given display coordinates.
     * 
     * @param locX
     * @param locY
     */
    void moveWindowTo(int locX, int locY);

    void setDisplayMode(DisplaySettings settings);

    void toggleFullScreen();
    
}
