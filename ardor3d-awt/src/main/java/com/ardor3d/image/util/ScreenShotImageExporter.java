/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.util.screen.ScreenExportable;

public class ScreenShotImageExporter implements ScreenExportable {
    private static final Logger logger = Logger.getLogger(ScreenShotImageExporter.class.getName());

    protected File _directory;
    protected String _prepend;
    protected String _fileFormat;
    protected boolean _useAlpha;

    protected File _lastFile;

    /**
     * Make a new exporter with the default settings:
     * 
     * <pre>
     * directory: local working directory
     * prepend: &quot;capture_&quot;
     * format: &quot;png&quot;
     * useAlpha: false
     * </pre>
     */
    public ScreenShotImageExporter() {
        this(new File(System.getProperty("user.dir")), "capture_", "png", false);
    }

    /**
     * Construct a new exporter.
     * 
     * @param directory
     *            the directory to save the screen shots in.
     * @param prepend
     *            a value to prepend onto the generated file name. This must be at least 3 characters long.
     * @param format
     *            the format to use for saving the image. ImageIO is used for this, so safe values are likely: "png",
     *            "jpg", "gif" and "bmp"
     * @param useAlpha
     *            true for alpha values to be stored in image (as applicable, depending on the given format)
     */
    public ScreenShotImageExporter(final File directory, final String prepend, final String format,
            final boolean useAlpha) {
        _directory = directory;
        _prepend = prepend;
        _fileFormat = format;
        _useAlpha = useAlpha;
    }

    public void export(final ByteBuffer data, final int width, final int height) {
        final BufferedImage img = new BufferedImage(width, height, _useAlpha ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB);

        int index, r, g, b, a;
        int argb;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                index = (_useAlpha ? 4 : 3) * ((height - y - 1) * width + x);
                r = ((data.get(index + 0)));
                g = ((data.get(index + 1)));
                b = ((data.get(index + 2)));

                argb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);

                if (_useAlpha) {
                    a = ((data.get(index + 3)));
                    argb |= (a & 0xFF) << 24;
                }

                img.setRGB(x, y, argb);
            }
        }

        try {
            final File out = new File(_directory, _prepend + System.currentTimeMillis() + "." + _fileFormat);
            logger.fine("Taking screenshot: " + out.getAbsolutePath());

            // write out the screen shot image to a file.
            ImageIO.write(img, _fileFormat, out);

            // save our successful file to be accessed as desired.
            _lastFile = out;
        } catch (final IOException e) {
            logger
                    .logp(Level.WARNING, getClass().getName(), "export(ByteBuffer, int, int)", e.getLocalizedMessage(),
                            e);
        }
    }

    public ImageDataFormat getFormat() {
        if (_useAlpha) {
            return ImageDataFormat.RGBA;
        } else {
            return ImageDataFormat.RGB;
        }
    }

    /**
     * @return the last File written by this exporter, or null if none were written.
     */
    public File getLastFile() {
        return _lastFile;
    }

    public File getDirectory() {
        return _directory;
    }

    public void setDirectory(final File directory) {
        _directory = directory;
    }

    public String getPrepend() {
        return _prepend;
    }

    public void setPrepend(final String prepend) {
        _prepend = prepend;
    }

    public boolean isUseAlpha() {
        return _useAlpha;
    }

    public void setUseAlpha(final boolean useAlpha) {
        _useAlpha = useAlpha;
    }

    public String getFileFormat() {
        return _fileFormat;
    }

    public void setFileFormat(final String format) {
        _fileFormat = format;
    }
}
