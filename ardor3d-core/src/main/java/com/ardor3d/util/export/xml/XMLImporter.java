/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.ardor3d.util.export.Ardor3dImporter;
import com.ardor3d.util.export.Savable;

/**
 * Part of the ardor3d XML IO system
 */
public class XMLImporter implements Ardor3dImporter {

    public XMLImporter() {}

    public Savable load(final InputStream is) throws IOException {
        try {
            final DOMInputCapsule _domIn = new DOMInputCapsule(DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(is));
            return _domIn.readSavable(null, null);
        } catch (final SAXException e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        } catch (final ParserConfigurationException e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
    }

    public Savable load(final URL url) throws IOException {
        return load(url.openStream());
    }

    public Savable load(final File f) throws IOException {
        return load(new FileInputStream(f));
    }

    public Savable load(final byte[] data) throws IOException {
        return load(new ByteArrayInputStream(data));
    }
}
