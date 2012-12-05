/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.resource;

import java.io.IOException;
import java.io.InputStream;

import com.ardor3d.util.export.Savable;

/**
 * Represents a named resource
 */
public interface ResourceSource extends Savable {

    public static final String UNKNOWN_TYPE = "-unknown-";

    /**
     * @return the name of this resource.
     */
    String getName();

    /**
     * @return the "type" of resource we are pointing to. For example ".jpg", ".dae", etc.
     */
    String getType();

    /**
     * Generate and return a new ResourceSource pointing to a named resource that is relative to this object's resource.
     * 
     * @param name
     *            the name of the resource we want. eg. "./mypic.jpg" etc.
     * @return the relative resource, or null if none is found. Will also return null if this ResourceSource type does
     *         not support relative source.
     */
    ResourceSource getRelativeSource(String name);

    /**
     * @return an InputStream to this resource's contents.
     * @throws IOException
     */
    InputStream openStream() throws IOException;

}
