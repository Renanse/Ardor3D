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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;

/**
 * Manager class for locator utility classes used to find various assets. (XXX: Needs more documentation)
 */
public class ResourceLocatorTool {
    private static final Logger logger = Logger.getLogger(ResourceLocatorTool.class.getName());

    public static final String TYPE_TEXTURE = "texture";
    public static final String TYPE_MODEL = "model";
    public static final String TYPE_PARTICLE = "particle";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_SHADER = "shader";

    private static final Map<String, List<ResourceLocator>> _locatorMap = new HashMap<String, List<ResourceLocator>>();

    public static ResourceSource locateResource(final String resourceType, String resourceName) {
        if (resourceName == null) {
            return null;
        }

        try {
            resourceName = URLDecoder.decode(resourceName, "UTF-8");
        } catch (final UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        synchronized (_locatorMap) {
            final List<ResourceLocator> bases = _locatorMap.get(resourceType);
            if (bases != null) {
                for (int i = bases.size(); --i >= 0;) {
                    final ResourceLocator loc = bases.get(i);
                    final ResourceSource rVal = loc.locateResource(resourceName);
                    if (rVal != null) {
                        return rVal;
                    }
                }
            }
            // last resort...
            try {
                final URL u = ResourceLocatorTool.getClassPathResource(ResourceLocatorTool.class, resourceName);
                if (u != null) {
                    return new URLResourceSource(u);
                }
            } catch (final Exception e) {
                logger.logp(Level.WARNING, ResourceLocatorTool.class.getName(), "locateResource(String, String)",
                        e.getMessage(), e);
            }

            logger.warning("Unable to locate: " + resourceName);
            return null;
        }
    }

    public static void addResourceLocator(final String resourceType, final ResourceLocator locator) {
        if (locator == null) {
            return;
        }
        synchronized (_locatorMap) {
            List<ResourceLocator> bases = _locatorMap.get(resourceType);
            if (bases == null) {
                bases = new ArrayList<ResourceLocator>();
                _locatorMap.put(resourceType, bases);
            }

            if (!bases.contains(locator)) {
                bases.add(locator);
            }
        }
    }

    public static boolean removeResourceLocator(final String resourceType, final ResourceLocator locator) {
        synchronized (_locatorMap) {
            final List<ResourceLocator> bases = _locatorMap.get(resourceType);
            if (bases == null) {
                return false;
            }
            return bases.remove(locator);
        }
    }

    /**
     * Locate a resource using various classloaders.
     *
     * <ul>
     * <li>First it tries the Thread.currentThread().getContextClassLoader().</li>
     * <li>Then it tries the ClassLoader.getSystemClassLoader() (if not same as context class loader).</li>
     * <li>Finally it tries the clazz.getClassLoader()</li>
     * </ul>
     *
     * @param clazz
     *            a class to use as a local reference.
     * @param name
     *            the name and path of the resource.
     * @return the URL of the resource, or null if none found.
     */
    public static URL getClassPathResource(final Class<?> clazz, final String name) {
        URL result = Thread.currentThread().getContextClassLoader().getResource(name);
        if (result == null
                && !Thread.currentThread().getContextClassLoader().equals(ClassLoader.getSystemClassLoader())) {
            result = ClassLoader.getSystemResource(name);
        }
        if (result == null) {
            result = clazz.getClassLoader().getResource(name);
        }
        return result;
    }

    /**
     * Locate a resource using various classloaders and open a stream to it.
     *
     * @param clazz
     *            a class to use as a local reference.
     * @param name
     *            the name and path of the resource.
     * @return the input stream if resource is found, or null if not.
     */
    public static InputStream getClassPathResourceAsStream(final Class<?> clazz, final String name) {
        InputStream result = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        if (result == null
                && !Thread.currentThread().getContextClassLoader().equals(ClassLoader.getSystemClassLoader())) {
            result = ClassLoader.getSystemResourceAsStream(name);
        }
        if (result == null) {
            result = clazz.getClassLoader().getResourceAsStream(name);
        }
        return result;
    }

    /**
     * Locate a resource using various classloaders and open a stream to it.
     *
     * @param clazz
     *            a class to use as a local reference.
     * @param name
     *            the name and path of the resource.
     * @return the input stream if resource is found, or null if not.
     * @throws IOException
     */
    public static String getClassPathResourceAsString(final Class<?> clazz, final String name) throws IOException {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        if (stream == null
                && !Thread.currentThread().getContextClassLoader().equals(ClassLoader.getSystemClassLoader())) {
            stream = ClassLoader.getSystemResourceAsStream(name);
        }
        if (stream == null) {
            stream = clazz.getClassLoader().getResourceAsStream(name);
        }

        if (stream == null) {
            return null;
        }
        String text = null;
        try (final Reader reader = new InputStreamReader(stream)) {
            text = CharStreams.toString(reader);
        }
        return text;
    }

    /**
     * Locate all instances of a resource using various classloaders.
     *
     * @param clazz
     *            a class to use as a local reference.
     * @param name
     *            the name and path of the resource.
     * @return a set containing the located URLs of the named resource.
     */
    public static Set<URL> getClassPathResources(final Class<?> clazz, final String name) {
        final Set<URL> results = Sets.newHashSet();
        Enumeration<URL> urls = null;
        try {
            urls = Thread.currentThread().getContextClassLoader().getResources(name);
            for (; urls.hasMoreElements();) {
                results.add(urls.nextElement());
            }
        } catch (final IOException ioe) {
        }
        if (!Thread.currentThread().getContextClassLoader().equals(ClassLoader.getSystemClassLoader())) {
            try {
                urls = ClassLoader.getSystemResources(name);
                for (; urls.hasMoreElements();) {
                    results.add(urls.nextElement());
                }
            } catch (final IOException ioe) {
            }
        }
        try {
            urls = clazz.getClassLoader().getResources(name);
            for (; urls.hasMoreElements();) {
                results.add(urls.nextElement());
            }
        } catch (final IOException ioe) {
        }
        return results;
    }
}
