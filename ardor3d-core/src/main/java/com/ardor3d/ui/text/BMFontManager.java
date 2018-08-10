/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.ui.text;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.util.resource.URLResourceSource;

/**
 * Simple singleton that loads some font textures and provides easy access.
 */
public class BMFontManager {
    private static final Logger logger = Logger.getLogger(BMFontManager.class.getName());

    private static final BMFontManager INSTANCE = new BMFontManager();

    private final HashMap<String, BMFont> _loadedFonts = new HashMap<String, BMFont>();

    public enum FontStyle {
        ArialMedium("arial-24-bold-regular"), //
        SansExtraSmall("DroidSans-14-bold-regular"), //
        SansSmall("DroidSans-16-bold-regular"), //
        SansMedium("DroidSans-20-bold-regular"), //
        SansLarge("DroidSans-26-bold-regular"), //
        SansExtraLarge("DroidSans-40-bold-regular"), //
        MonoSmall("DroidSansMono-15-bold-regular"), //
        MonoMedium("DroidSansMono-20-bold-regular"), //
        MonoLarge("DroidSansMono-26-bold-regular"), //
        MonoExtraLarge("DroidSansMono-40-bold-regular");

        public final String fontName;

        FontStyle(final String name) {
            fontName = name;
        }
    }

    /**
     *
     * @param style
     * @return
     */
    public static BMFont getFont(final FontStyle style) {
        return getFont(style.fontName);
    }

    /**
     * Retrieves the font, loads it if need be. Font must exist in this bundle in images/fonts.
     *
     * @param fontName
     * @return
     */
    public static BMFont getFont(final String fontName) {
        BMFont found = INSTANCE._loadedFonts.get(fontName);
        if (found == null) {
            found = newFont(fontName);
            if (found != null) {
                INSTANCE._loadedFonts.put(fontName, found);
            }
        }
        return found;
    }

    /**
     * Creates the font. Font must exist in this bundle in images/fonts.
     *
     * @param fontName
     * @return
     */
    public static BMFont newFont(final String fontName) {
        BMFont font = null;
        final URLResourceSource rs = new URLResourceSource(BMFontManager.class.getResource(fontName + ".fnt"));
        try {
            font = new BMFont(rs, false);
        } catch (final IOException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.logp(Level.WARNING, "BMFontManager", "getFont(String)", "Unable to load font.",
                        new Object[] { fontName });
            }
        }
        return font;
    }

    public static BMFont defaultFont() {
        return getFont(FontStyle.SansMedium);
    }

    public static BMFont sansExtraSmall() {
        return getFont(FontStyle.SansExtraSmall);
    }

    public static BMFont sansSmall() {
        return getFont(FontStyle.SansSmall);
    }

    public static BMFont sansMedium() {
        return getFont(FontStyle.SansMedium);
    }

    public static BMFont sansLarge() {
        return getFont(FontStyle.SansLarge);
    }

    public static BMFont sansExtraLarge() {
        return getFont(FontStyle.SansExtraLarge);
    }

    public static BMFont monoSmall() {
        return getFont(FontStyle.MonoSmall);
    }

    public static BMFont monoMedium() {
        return getFont(FontStyle.MonoMedium);
    }

    public static BMFont monoLarge() {
        return getFont(FontStyle.MonoLarge);
    }

    public static BMFont monoExtraLarge() {
        return getFont(FontStyle.MonoExtraLarge);
    }
}