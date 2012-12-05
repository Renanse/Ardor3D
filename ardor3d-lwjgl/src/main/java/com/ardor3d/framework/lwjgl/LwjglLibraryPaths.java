/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */


package com.ardor3d.framework.lwjgl;

/**
 * TODO: document this class!
 *
 */
public enum LwjglLibraryPaths {
    MACOSX("Mac OS X", null, new String[] {
            "/macosx/libjinput-osx.jnilib",
            "/macosx/liblwjgl.jnilib",
            "/macosx/openal.dylib",
    }),
    LINUX_AMD64("Linux", "amd64", new String[] {
            "/linux/libjinput-linux64.so",
            "/linux/liblwjgl64.so",
            "/linux/libopenal64.so",
    }),
    LINUX_I386("Linux", "i386", new String[] {
            "/linux/libjinput-linux.so",
            "/linux/liblwjgl.so",
            "/linux/libopenal.so",
    }),
    // NOTE: the order of the elements in this array is significant, so the catchall has to come last,
    // or it will shadow more specific alternatives
    LINUX_CATCHALL("Linux", null, new String[] {
            "/linux/libjinput-linux.so",
            "/linux/liblwjgl.so",
            "/linux/libopenal.so",
    }),
    SOLARIS("TODO", null, new String[] {
            "/solaris/liblwjgl.so",
            "/solaris/libopenal.so",
    }),
    WINDOWS_XP("Windows XP", null, new String[] {
            "/windows/jinput-dx8.dll",
            "/windows/jinput-raw.dll",
            "/windows/lwjgl.dll",
            "/windows/OpenAL32.dll",
    });
    
    private final String _operatingSystem;
    private final String _architecture;
    private final String[] _libraryPaths;


    LwjglLibraryPaths(String operatingSystem, String architecture, String[] libraryPaths) {
        _operatingSystem = operatingSystem;
        _architecture = architecture;
        _libraryPaths = libraryPaths;
    }

    public static String[] getLibraryPaths(String operatingSystem, String architecture) {
        for (LwjglLibraryPaths libraryPath : values()) {
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
