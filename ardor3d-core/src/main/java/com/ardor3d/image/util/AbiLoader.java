/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util;

import java.io.IOException;
import java.io.InputStream;

import com.ardor3d.image.Image;
import com.ardor3d.util.export.binary.BinaryImporter;

/**
 * Loads Image objects from binary Ardor3D format.
 */
public final class AbiLoader implements ImageLoader {
    public AbiLoader() {}

    /**
     * Load an image from Ardor3D binary format.
     * 
     * @param is
     *            the input stream delivering the binary data.
     * @param flip
     *            ignored... for now.
     * @return the new loaded Image.
     * @throws IOException
     *             if an error occurs during read.
     */
    public Image load(final InputStream is, final boolean flip) throws IOException {
        return (Image) new BinaryImporter().load(is);
    }
}
