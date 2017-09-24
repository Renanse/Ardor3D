
package com.ardor3d.scene.state.lwjgl.util;

/**
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** 
 * Loads shared libraries from JAR files. Call {@link SharedLibraryLoader#load() to load the
 * required LWJGL native shared libraries. (Used and relicensed for Ardor3D by written permission of mzechner.)
 *
 * @author mzechner
 * @author Nathan Sweet
 * @author Joshua Slack (slight modifications to work with lwjgl2)
 */
public class SharedLibraryLoader {
    static public boolean isWindows = System.getProperty("os.name").contains("Windows");
    static public boolean isLinux = System.getProperty("os.name").contains("Linux");
    static public boolean isMac = System.getProperty("os.name").contains("Mac");
    static public boolean isIos = false;
    static public boolean isAndroid = false;
    static public boolean isARM = System.getProperty("os.arch").startsWith("arm");
    static public boolean is64Bit = System.getProperty("os.arch").equals("amd64")
            || System.getProperty("os.arch").equals("x86_64");

    // JDK 8 only.
    static public String abi = (System.getProperty("sun.arch.abi") != null ? System.getProperty("sun.arch.abi") : "");

    static {
        final String vm = System.getProperty("java.runtime.name");
        if (vm != null && vm.contains("Android Runtime")) {
            isAndroid = true;
            isWindows = false;
            isLinux = false;
            isMac = false;
            is64Bit = false;
        }
        if (!isAndroid && !isWindows && !isLinux && !isMac) {
            isIos = true;
            is64Bit = false;
        }
    }

    static boolean load = true;

    static {
        // Don't extract natives if using JWS.
        try {
            final Method method = Class.forName("javax.jnlp.ServiceManager").getDeclaredMethod("lookup",
                    new Class[] { String.class });
            method.invoke(null, "javax.jnlp.PersistenceService");
            load = false;
        } catch (final Throwable ex) {
            load = true;
        }
    }

    /** Extracts the LWJGL native libraries from the classpath and sets the "org.lwjgl.librarypath" system property. */
    static public synchronized void load() {
        load(false);
    }

    /** Extracts the LWJGL native libraries from the classpath and sets the "org.lwjgl.librarypath" system property. */
    static public synchronized void load(final boolean disableOpenAL) {
        if (!load) {
            return;
        }

        final SharedLibraryLoader loader = new SharedLibraryLoader();
        File nativesDir = null;
        try {
            if (SharedLibraryLoader.isWindows) {
                nativesDir = loader.extractFile(SharedLibraryLoader.is64Bit ? "lwjgl64.dll" : "lwjgl.dll", null)
                        .getParentFile();
                if (!disableOpenAL) {
                    loader.extractFile(SharedLibraryLoader.is64Bit ? "OpenAL64.dll" : "OpenAL32.dll",
                            nativesDir.getName());
                }
            } else if (SharedLibraryLoader.isMac) {
                nativesDir = loader.extractFile("liblwjgl.dylib", null).getParentFile();
                if (!disableOpenAL) {
                    loader.extractFile("libopenal.dylib", nativesDir.getName());
                }
            } else if (SharedLibraryLoader.isLinux) {
                nativesDir = loader.extractFile(SharedLibraryLoader.is64Bit ? "liblwjgl64.so" : "liblwjgl.so", null)
                        .getParentFile();
                if (!disableOpenAL) {
                    loader.extractFile(SharedLibraryLoader.is64Bit ? "libopenal64.so" : "libopenal.so",
                            nativesDir.getName());
                }
            }
        } catch (final Throwable ex) {
            throw new RuntimeException("Unable to extract LWJGL natives.", ex);
        }
        System.setProperty("org.lwjgl.librarypath", nativesDir.getAbsolutePath());
        load = false;
    }

    static private final HashSet<String> loadedLibraries = new HashSet<String>();

    private String nativesJar;

    public SharedLibraryLoader() {}

    /**
     * Fetches the natives from the given natives jar file. Used for testing a shared lib on the fly.
     *
     * @param nativesJar
     */
    public SharedLibraryLoader(final String nativesJar) {
        this.nativesJar = nativesJar;
    }

    /** Returns a CRC of the remaining bytes in the stream. */
    public String crc(final InputStream input) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null.");
        }
        final CRC32 crc = new CRC32();
        final byte[] buffer = new byte[4096];
        try {
            while (true) {
                final int length = input.read(buffer);
                if (length == -1) {
                    break;
                }
                crc.update(buffer, 0, length);
            }
        } catch (final Exception ex) {
            if (input != null) {
                try {
                    input.close();
                } catch (final IOException e) {
                }
            }
        }
        return Long.toString(crc.getValue(), 16);
    }

    /** Maps a platform independent library name to a platform dependent name. */
    public String mapLibraryName(final String libraryName) {
        if (isWindows) {
            return libraryName + (is64Bit ? "64.dll" : ".dll");
        }
        if (isLinux) {
            return "lib" + libraryName + (isARM ? "arm" + abi : "") + (is64Bit ? "64.so" : ".so");
        }
        if (isMac) {
            return "lib" + libraryName + (is64Bit ? "64.dylib" : ".dylib");
        }
        return libraryName;
    }

    /**
     * Loads a shared library for the platform the application is running on.
     *
     * @param libraryName
     *            The platform independent library name. If not contain a prefix (eg lib) or suffix (eg .dll).
     */
    public synchronized void load(String libraryName) {
        // in case of iOS, things have been linked statically to the executable, bail out.
        if (isIos) {
            return;
        }

        libraryName = mapLibraryName(libraryName);
        if (loadedLibraries.contains(libraryName)) {
            return;
        }

        try {
            if (isAndroid) {
                System.loadLibrary(libraryName);
            } else {
                loadFile(libraryName);
            }
        } catch (final Throwable ex) {
            throw new RuntimeException("Couldn't load shared library '" + libraryName + "' for target: "
                    + System.getProperty("os.name") + (is64Bit ? ", 64-bit" : ", 32-bit"), ex);
        }
        loadedLibraries.add(libraryName);
    }

    private InputStream readFile(final String path) {
        if (nativesJar == null) {
            final InputStream input = SharedLibraryLoader.class.getResourceAsStream("/" + path);
            if (input == null) {
                throw new RuntimeException("Unable to read file for extraction: " + path);
            }
            return input;
        }

        // Read from JAR.
        ZipFile file = null;
        try {
            file = new ZipFile(nativesJar);
            final ZipEntry entry = file.getEntry(path);
            if (entry == null) {
                throw new RuntimeException("Couldn't find '" + path + "' in JAR: " + nativesJar);
            }
            return file.getInputStream(entry);
        } catch (final IOException ex) {
            throw new RuntimeException("Error reading '" + path + "' in JAR: " + nativesJar, ex);
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (final IOException e) {
                }
            }
        }
    }

    /**
     * Extracts the specified file into the temp directory if it does not already exist or the CRC does not match. If
     * file extraction fails and the file exists at java.library.path, that file is returned.
     *
     * @param sourcePath
     *            The file to extract from the classpath or JAR.
     * @param dirName
     *            The name of the subdirectory where the file will be extracted. If null, the file's CRC will be used.
     * @return The extracted file.
     */
    public File extractFile(final String sourcePath, String dirName) throws IOException {
        try {
            final String sourceCrc = crc(readFile(sourcePath));
            if (dirName == null) {
                dirName = sourceCrc;
            }

            final File extractedFile = getExtractedFile(dirName, new File(sourcePath).getName());
            return extractFile(sourcePath, sourceCrc, extractedFile);
        } catch (final RuntimeException ex) {
            // Fallback to file at java.library.path location, eg for applets.
            final File file = new File(System.getProperty("java.library.path"), sourcePath);
            if (file.exists()) {
                return file;
            }
            throw ex;
        }
    }

    /** Returns a path to a file that can be written. Tries multiple locations and verifies writing succeeds. */
    private File getExtractedFile(final String dirName, final String fileName) {
        // Temp directory with username in path.
        final File idealFile = new File(System.getProperty("java.io.tmpdir") + "/libgdx"
                + System.getProperty("user.name") + "/" + dirName, fileName);
        if (canWrite(idealFile)) {
            return idealFile;
        }

        // System provided temp directory.
        try {
            File file = File.createTempFile(dirName, null);
            if (file.delete()) {
                file = new File(file, fileName);
                if (canWrite(file)) {
                    return file;
                }
            }
        } catch (final IOException ignored) {
        }

        // User home.
        File file = new File(System.getProperty("user.home") + "/.libgdx/" + dirName, fileName);
        if (canWrite(file)) {
            return file;
        }

        // Relative directory.
        file = new File(".temp/" + dirName, fileName);
        if (canWrite(file)) {
            return file;
        }

        return idealFile; // Will likely fail, but we did our best.
    }

    /** Returns true if the parent directories of the file can be created and the file can be written. */
    private boolean canWrite(final File file) {
        final File parent = file.getParentFile();
        File testFile;
        if (file.exists()) {
            if (!file.canWrite() || !canExecute(file)) {
                return false;
            }
            // Don't overwrite existing file just to check if we can write to directory.
            testFile = new File(parent, UUID.randomUUID().toString());
        } else {
            parent.mkdirs();
            if (!parent.isDirectory()) {
                return false;
            }
            testFile = file;
        }
        try {
            new FileOutputStream(testFile).close();
            if (!canExecute(testFile)) {
                return false;
            }
            return true;
        } catch (final Throwable ex) {
            return false;
        } finally {
            testFile.delete();
        }
    }

    private boolean canExecute(final File file) {
        try {
            final Method canExecute = File.class.getMethod("canExecute");
            if ((Boolean) canExecute.invoke(file)) {
                return true;
            }

            final Method setExecutable = File.class.getMethod("setExecutable", boolean.class, boolean.class);
            setExecutable.invoke(file, true, false);

            return (Boolean) canExecute.invoke(file);
        } catch (final Exception ignored) {
        }
        return false;
    }

    private File extractFile(final String sourcePath, final String sourceCrc, final File extractedFile)
            throws IOException {
        String extractedCrc = null;
        if (extractedFile.exists()) {
            try {
                extractedCrc = crc(new FileInputStream(extractedFile));
            } catch (final FileNotFoundException ignored) {
            }
        }

        // If file doesn't exist or the CRC doesn't match, extract it to the temp dir.
        if (extractedCrc == null || !extractedCrc.equals(sourceCrc)) {
            try {
                final InputStream input = readFile(sourcePath);
                extractedFile.getParentFile().mkdirs();
                final FileOutputStream output = new FileOutputStream(extractedFile);
                final byte[] buffer = new byte[4096];
                while (true) {
                    final int length = input.read(buffer);
                    if (length == -1) {
                        break;
                    }
                    output.write(buffer, 0, length);
                }
                input.close();
                output.close();
            } catch (final IOException ex) {
                throw new RuntimeException("Error extracting file: " + sourcePath + "\nTo: "
                        + extractedFile.getAbsolutePath(), ex);
            }
        }

        return extractedFile;
    }

    /**
     * Extracts the source file and calls System.load. Attemps to extract and load from multiple locations. Throws
     * runtime exception if all fail.
     */
    private void loadFile(final String sourcePath) {
        final String sourceCrc = crc(readFile(sourcePath));

        final String fileName = new File(sourcePath).getName();

        // Temp directory with username in path.
        File file = new File(System.getProperty("java.io.tmpdir") + "/libgdx" + System.getProperty("user.name") + "/"
                + sourceCrc, fileName);
        final Throwable ex = loadFile(sourcePath, sourceCrc, file);
        if (ex == null) {
            return;
        }

        // System provided temp directory.
        try {
            file = File.createTempFile(sourceCrc, null);
            if (file.delete() && loadFile(sourcePath, sourceCrc, file) == null) {
                return;
            }
        } catch (final Throwable ignored) {
        }

        // User home.
        file = new File(System.getProperty("user.home") + "/.libgdx/" + sourceCrc, fileName);
        if (loadFile(sourcePath, sourceCrc, file) == null) {
            return;
        }

        // Relative directory.
        file = new File(".temp/" + sourceCrc, fileName);
        if (loadFile(sourcePath, sourceCrc, file) == null) {
            return;
        }

        // Fallback to java.library.path location, eg for applets.
        file = new File(System.getProperty("java.library.path"), sourcePath);
        if (file.exists()) {
            System.load(file.getAbsolutePath());
            return;
        }

        throw new RuntimeException(ex);
    }

    /** @return null if the file was extracted and loaded. */
    private Throwable loadFile(final String sourcePath, final String sourceCrc, final File extractedFile) {
        try {
            System.load(extractFile(sourcePath, sourceCrc, extractedFile).getAbsolutePath());
            return null;
        } catch (final Throwable ex) {
            ex.printStackTrace();
            return ex;
        }
    }
}