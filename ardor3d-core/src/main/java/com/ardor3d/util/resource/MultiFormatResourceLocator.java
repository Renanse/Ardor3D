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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

/**
 * This class extends the behavior of the {@link SimpleResourceLocator} by replacing the resource's file extension with
 * different various provided extensions. If none of these work, it will try the original resource name as-is. You can
 * choose to have the original file searched for first, or last using {@link #setTrySpecifiedFormatFirst(boolean)}.
 */
public class MultiFormatResourceLocator extends SimpleResourceLocator {

    private final String[] _extensions;
    private boolean _trySpecifiedFormatFirst = false;

    /**
     * Construct a new MultiFormatResourceLocator using the given URI as our context and the list of possible extensions
     * as extensions to try during file search.
     * 
     * @param baseDir
     *            our base context. This is meant to be a "directory" wherein we will search for resources. Therefore,
     *            if it does not end in /, a / will be added to ensure we are talking about children of the given
     *            baseDir.
     * @param extensions
     *            an array of extensions (eg. ".png", ".dds", ".tga", etc.) to try while searching for a resource with
     *            this locator. This is done by replacing any existing extension in the resource name with each of the
     *            given extensions.
     * @throws URISyntaxException
     *             if the given URI does not end in / and we can not make a new URI with a trailing / from it.
     */
    public MultiFormatResourceLocator(final URI baseDir, final String... extensions) throws URISyntaxException {
        super(baseDir);

        if (extensions == null) {
            throw new NullPointerException("extensions can not be null.");
        }
        _extensions = extensions;
    }

    /**
     * Construct a new MultiFormatResourceLocator using the given URL as our context and the list of possible extensions
     * as extensions to try during file search.
     * 
     * @param baseDir
     *            our base context. This is converted to a URI. This is meant to be a "directory" wherein we will search
     *            for resources. Therefore, if it does not end in /, a / will be added to ensure we are talking about
     *            children of the given baseDir.
     * @param extensions
     *            an array of extensions (eg. ".png", ".dds", ".tga", etc.) to try while searching for a resource with
     *            this locator. This is done by replacing any existing extension in the resource name with each of the
     *            given extensions.
     * @throws URISyntaxException
     *             if this URL can not be converted to a URI, or if the converted URI does not end in / and we can not
     *             make a new URI with a trailing / from it.
     */
    public MultiFormatResourceLocator(final URL baseDir, final String... extensions) throws URISyntaxException {
        this(baseDir.toURI(), extensions);
    }

    @Override
    public ResourceSource locateResource(String resourceName) {
        resourceName = cleanup(resourceName);

        if (_trySpecifiedFormatFirst) {
            final ResourceSource src = doRecursiveLocate(resourceName);
            if (src != null) {
                return src;
            }
        }

        final String baseFileName = getBaseFileName(resourceName);
        for (final String extension : _extensions) {
            final ResourceSource src = doRecursiveLocate(baseFileName + extension);
            if (src != null) {
                return src;
            }
        }

        if (!_trySpecifiedFormatFirst) {
            // If all else fails, just try the original name.
            return doRecursiveLocate(resourceName);
        } else {
            return null;
        }
    }

    private String getBaseFileName(final String resourceName) {
        final File f = new File(resourceName);
        final String name = f.getPath();
        final int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return name;
        } else {
            return name.substring(0, dot);
        }
    }

    public boolean isTrySpecifiedFormatFirst() {
        return _trySpecifiedFormatFirst;
    }

    public void setTrySpecifiedFormatFirst(final boolean trySpecifiedFormatFirst) {
        _trySpecifiedFormatFirst = trySpecifiedFormatFirst;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MultiFormatResourceLocator) {
            return getBaseDir().equals(((MultiFormatResourceLocator) obj).getBaseDir())
                    && Arrays.equals(_extensions, ((MultiFormatResourceLocator) obj)._extensions);
        }
        return super.equals(obj);
    }
}
