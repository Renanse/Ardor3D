/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.screen;

import java.nio.ByteBuffer;

import com.ardor3d.image.ImageDataFormat;

public interface ScreenExportable {

    /**
     * Export the given image data (byte buffer) in a manner of our choosing. Note that this byte buffer should be
     * treated by the implementing class as immutable and temporary. If you need access to it after returning from the
     * method, make a copy.
     * 
     * @param data
     *            the data from the screen. Please respect the data's limit() value.
     * @param width
     * @param height
     */
    public void export(ByteBuffer data, int width, int height);

    /**
     * 
     * @return the image data format we'd like to pull in.
     */
    public ImageDataFormat getFormat();

}
