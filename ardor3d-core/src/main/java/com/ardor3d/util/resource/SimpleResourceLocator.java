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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * This locator takes a base location for finding resources specified with a relative path. If it cannot find the path
 * relative to the location, it successively omits the starting components of the relative path until it can find a
 * resources with such a trimmed path. If no resource is found with this method null is returned.
 */
public class SimpleResourceLocator implements ResourceLocator {

    private final URI _baseDir;

    /**
     * Construct a new SimpleResourceLocator using the given URI as our context.
     *
     * @param baseDir
     *            our base context. This is meant to be a "directory" wherein we will search for resources. Therefore,
     *            if it does not end in /, a / will be added to ensure we are talking about children of the given
     *            baseDir.
     * @throws NullPointerException
     *             if the given URI is null.
     * @throws URISyntaxException
     *             if the given URI does not end in / and we can not make a new URI with a trailing / from it.
     */
    public SimpleResourceLocator(final URI baseDir) throws URISyntaxException {
        if (baseDir == null) {
            throw new NullPointerException("baseDir can not be null.");
        }

        final String uri = baseDir.toString();
        if (!uri.endsWith("/")) {
            _baseDir = new URI(baseDir.toString() + "/");
        } else {
            _baseDir = baseDir;
        }
    }

    /**
     * Construct a new SimpleResourceLocator using the given URL as our context.
     *
     * @param baseDir
     *            our base context. This is converted to a URI. This is meant to be a "directory" wherein we will search
     *            for resources. Therefore, if it does not end in /, a / will be added to ensure we are talking about
     *            children of the given baseDir.
     * @throws NullPointerException
     *             if the given URL is null.
     * @throws URISyntaxException
     *             if this URL can not be converted to a URI, or if the converted URI does not end in / and we can not
     *             make a new URI with a trailing / from it.
     */
    public SimpleResourceLocator(final URL baseDir) throws URISyntaxException {
        this(baseDir.toURI());
    }

    public URI getBaseDir() {
        return _baseDir;
    }

    public ResourceSource locateResource(final String resourceName) {
        return doRecursiveLocate(cleanup(resourceName));
    }

    protected ResourceSource doRecursiveLocate(String resourceName) {
        // Trim off any prepended local dir.
        while (resourceName.startsWith("./") && resourceName.length() > 2) {
            resourceName = resourceName.substring(2);
        }
        while (resourceName.startsWith(".\\") && resourceName.length() > 2) {
            resourceName = resourceName.substring(2);
        }

        // Try to locate using resourceName as is.
        try {
            final URL rVal = new URL(_baseDir.toURL(), resourceName);
            // open a stream to see if this is a valid resource
            rVal.openStream().close();
            return new URLResourceSource(rVal);
        } catch (final IOException e) {
            // URL wasn't valid in some way, so try up a path.
        } catch (final IllegalArgumentException e) {
            // URL wasn't valid in some way, so try up a path.
        }

        // Now try url encoding
        try {
            String spec = URLEncoder.encode(resourceName, "UTF-8");
            // this fixes a bug in JRE1.5 (file handler does not decode "+" to spaces)
            spec = spec.replaceAll("\\+", "%20");

            final URL rVal = new URL(_baseDir.toURL(), spec);
            // open a stream to see if this is a valid resource
            // XXX: Perhaps this is wasteful? Also, what info will determine validity?
            rVal.openStream().close();
            return new URLResourceSource(rVal);
        } catch (final IOException e) {
            // URL wasn't valid in some way, so try up a path.
        } catch (final IllegalArgumentException e) {
            // URL wasn't valid in some way, so try up a path.
        }

        resourceName = trimResourceName(resourceName);
        if (resourceName == null) {
            return null;
        } else {
            return doRecursiveLocate(resourceName);
        }
    }

    protected String trimResourceName(String resourceName) {
        // it's possible this URL has back slashes, so replace them.
        resourceName = cleanup(resourceName);
        final int firstSlashIndex = resourceName.indexOf('/');
        if (firstSlashIndex >= 0 && firstSlashIndex < resourceName.length() - 1) {
            return resourceName.substring(firstSlashIndex + 1);
        } else {
            return null;
        }
    }

    protected String cleanup(String name) {
        // Replace any %2F (or %2f) with forward slashes
        name = name.replaceAll("\\%2[F,f]", "/");
        // replace back slashes with forward
        name = name.replace('\\', '/');
        return name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof SimpleResourceLocator) {
            return _baseDir.equals(((SimpleResourceLocator) obj)._baseDir);
        }
        return false;
    }
}
