/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import com.ardor3d.util.export.Ardor3dExporter;
import com.ardor3d.util.export.Savable;

/**
 * Part of the ardor3d XML IO system
 */
public class XMLExporter implements Ardor3dExporter {
    public static final String ELEMENT_MAPENTRY = "MapEntry";
    public static final String ELEMENT_KEY = "Key";
    public static final String ELEMENT_VALUE = "Value";
    public static final String ELEMENT_FLOATBUFFER = "FloatBuffer";
    public static final String ATTRIBUTE_SIZE = "size";

    public XMLExporter() {

    }

    public void save(final Savable object, final OutputStream os) throws IOException {
        try {
            // Initialize Document when saving so we don't retain state of previous exports
            final DOMOutputCapsule _domOut = new DOMOutputCapsule(DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument());
            _domOut.write(object, object.getClass().getName(), null);
            DOM_PrettyPrint.serialize(_domOut.getDoc(), os);
            os.flush();
        } catch (final Exception ex) {
            final IOException e = new IOException();
            e.initCause(ex);
            throw e;
        }
    }

    public void save(final Savable object, final File f) throws IOException {
        save(object, new FileOutputStream(f));
    }
}
