/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.resource;

/**
 * Interface for locating resources from resource names.
 */
public interface ResourceLocator {

    /**
     * Locates a resource according to the strategy of the resource locator implementation (subclass).
     * 
     * @param resourceName
     *            the name of the resource to locate; it this is a path it must be slash separated (no backslashes)
     * @see SimpleResourceLocator
     * @see MultiFormatResourceLocator
     * @return a source for the resource, null if the resource was not found
     */
    public ResourceSource locateResource(String resourceName);

}
