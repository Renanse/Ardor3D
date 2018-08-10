/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface Ardor3dImporter {

    /**
     * Load a Savable object from the given stream.
     * 
     * @param is
     * @return the savable object.
     * @throws IOException
     */
    Savable load(InputStream is) throws IOException;

    /**
     * Load a Savable object from the given URL.
     * 
     * @param url
     * @return the savable object.
     * @throws IOException
     */
    Savable load(URL url) throws IOException;

    /**
     * Load a Savable object from the given file.
     * 
     * @param file
     * @return the savable object.
     * @throws IOException
     */
    Savable load(File file) throws IOException;

    /**
     * Load a Savable object from the given byte array, starting at the first index.
     * 
     * @param data
     * @return the savable object.
     * @throws IOException
     */
    Savable load(byte[] data) throws IOException;
}
