/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
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
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.util.ImageUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.util.geom.BufferUtils;

public class ScreenExporter {

    static ByteBuffer _scratch = BufferUtils.createByteBuffer(1);

    public synchronized static void exportCurrentScreen(final Renderer renderer, final ScreenExportable exportable) {
        final ImageDataFormat format = exportable.getFormat();
        final Camera camera = Camera.getCurrentCamera();
        final int width = camera.getWidth(), height = camera.getHeight();

        // prepare our data buffer
        final int size = width * height * ImageUtils.getPixelByteSize(format, PixelDataType.UnsignedByte);
        if (_scratch.capacity() < size) {
            _scratch = BufferUtils.createByteBuffer(size);
        } else {
            _scratch.limit(size);
            _scratch.rewind();
        }

        // Ask the renderer for the current scene to be stored in the buffer
        renderer.grabScreenContents(_scratch, format, 0, 0, width, height);

        // send the buffer to the exportable object for processing.
        exportable.export(_scratch, width, height);
    }
}
