/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * ResourceSource that pulls its content from a String. This source type does not support relative sources.
 */
public class StringResourceSource implements ResourceSource {

    /** Our class logger */
    private static final Logger logger = Logger.getLogger(StringResourceSource.class.getName());

    /** The data this source returns. */
    private String _data;

    /** An optional type value for the source. */
    private String _type;

    /**
     * Construct a new StringResourceSource.
     * 
     * @param data
     *            the data this source should return.
     */
    public StringResourceSource(final String data) {
        this(data, null);
    }

    /**
     * Construct a new StringResourceSource.
     * 
     * @param data
     *            the data this source should return.
     * @param type
     *            the type for this source. Usually a file extension such as .txt or .js. Required for generic loading
     *            when multiple resource handlers could be used.
     */
    public StringResourceSource(final String data, final String type) {
        _data = data;
        _type = type;
    }

    /**
     * Returns "string resource" as strings have no name.
     */
    public String getName() {
        return "string resource";
    }

    /**
     * Returns null and logs a warning as this is not supported.
     */
    public ResourceSource getRelativeSource(final String name) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.logp(Level.WARNING, getClass().getName(), "getRelativeSource(String)",
                    "StringResourceSource does not support this method.");
        }
        return null;
    }

    public String getType() {
        return _type;
    }

    /**
     * Grabs our data as a UTF8 byte array and returns it in a ByteArrayInputStream.
     */
    public InputStream openStream() throws IOException {
        return new ByteArrayInputStream(_data.getBytes("UTF8"));
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<?> getClassTag() {
        return StringResourceSource.class;
    }

    public void read(final InputCapsule capsule) throws IOException {
        _data = capsule.readString("data", null);
        _type = capsule.readString("type", null);
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_data, "data", null);
        capsule.write(_type, "type", null);
    }
}
