/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

/**
 * Just a simple flag holder for runtime stripping of various ardor3d logging and debugging features.
 */
public class Constants {

    public static boolean updateGraphs = false;

    public static final boolean strictVertexAttributes;

    public static final boolean useStatePools;

    public static final boolean stats;

    public static final boolean trackDirectMemory;

    public static final boolean useMultipleContexts;

    public static final boolean storeSavableImages;

    public static final int maxStatePoolSize;

    public static final boolean useValidatingTransform;

    public static final boolean enableInstancedGeometrySupport;

    static {
        boolean hasPropertyAccess = true;
        try {
            if (System.getSecurityManager() != null) {
                System.getSecurityManager().checkPropertiesAccess();
            }
        } catch (final SecurityException e) {
            hasPropertyAccess = false;
        }

        if (hasPropertyAccess) {
            stats = (System.getProperty("ardor3d.stats") != null);
            trackDirectMemory = (System.getProperty("ardor3d.trackDirect") != null);
            useMultipleContexts = (System.getProperty("ardor3d.useMultipleContexts") != null);
            useStatePools = (System.getProperty("ardor3d.noStatePools") == null);
            storeSavableImages = (System.getProperty("ardor3d.storeSavableImages") != null);
            maxStatePoolSize = (System.getProperty("ardor3d.maxStatePoolSize") != null
                    ? Integer.parseInt(System.getProperty("ardor3d.maxStatePoolSize"))
                    : 11);

            useValidatingTransform = (System.getProperty("ardor3d.disableValidatingTransform") == null);
            enableInstancedGeometrySupport = (System.getProperty("ardor3d.enableInstancedGeometrySupport") != null);
            strictVertexAttributes = (System.getProperty("ardor3d.strictVertexAttributes") != null);
        } else {
            stats = false;
            trackDirectMemory = false;
            useMultipleContexts = false;
            useStatePools = true;
            storeSavableImages = false;
            maxStatePoolSize = 11;
            useValidatingTransform = true;
            enableInstancedGeometrySupport = false;
            strictVertexAttributes = false;
        }
    }
}
