/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

/**
 * TODO: document this class!
 *
 */
public enum JoglLibraryPaths {
    MACOSX("Mac OS X", null, new String[] {
            "/macosx/libgluegen-rt.jnilib",
            "/macosx/libjogl.jnilib",
            "/macosx/libjogl_awt.jnilib",
            "/macosx/libjogl_cg.jnilib",
    }),
    WINDOWS_XP("Windows XP", null, new String[] {
            "/win32/gluegen-rt.dll",
            "/win32/jogl.dll",
            "/win32/jogl_awt.dll",
            "/win32/jogl_cg.dll",
    });

    private final String _operatingSystem;
    private final String _architecture;
    private final String[] _libraryPaths;


    JoglLibraryPaths(String operatingSystem, String architecture, String[] libraryPaths) {
        _operatingSystem = operatingSystem;
        _architecture = architecture;
        _libraryPaths = libraryPaths;
    }

    public static String[] getLibraryPaths(String operatingSystem, String architecture) {
        for (JoglLibraryPaths libraryPath : JoglLibraryPaths.values()) {
            if (operatingSystem.equals(libraryPath._operatingSystem) &&
                    (libraryPath._architecture == null || architecture.equals(libraryPath._architecture))) {
                return libraryPath._libraryPaths;
            }
        }

        throw new IllegalStateException("No matching set of library paths found for " + operatingSystem + ", " + architecture);
    }

    public static void main(String[] args) {
        System.out.println(System.getProperty("os.name"));
        System.out.println(System.getProperty("os.arch"));

        System.getProperties();
    }
}