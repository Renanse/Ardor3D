/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.ui.text.BMFont;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

/**
 * Simple hack singleton that loads some font textures and provides easy access.
 */
public class BMFontLoader {
    static Logger logger = Logger.getLogger(BMFontLoader.class.getName());

    static BMFontLoader s_instance = null;
    final ArrayList<BMFont> _fontList = new ArrayList<BMFont>();

    public static List<BMFont> allFonts() {
        return instance()._fontList;
    }

    public static BMFont defaultFont() {
        return instance()._fontList.get(0);
    }

    private static BMFontLoader instance() {
        if (s_instance == null) {
            s_instance = new BMFontLoader();
        }
        return s_instance;
    }

    private BMFontLoader() {
        final FontLoad[] fontNames = new FontLoad[] { new FontLoad("DejaVuSansCondensed-20-bold-regular", true),
                new FontLoad("DroidSans-15-bold-regular", false), // --------------------
                new FontLoad("LiberationMono-15-bold-regular", false), // ---------------
                new FontLoad("FreebooterScript-60-medium-regular", true), // ------------
                new FontLoad("Bandy-35-medium-regular", true), // -----------------------
                new FontLoad("OkasaSansSerif-35-medium-regular", true),// ---------------
                new FontLoad("Chinyen-30-medium-regular", true), // ---------------------
                new FontLoad("Computerfont-35-medium-regular", true) };

        for (final FontLoad fl : fontNames) {
            try {
                final String file = "fonts/" + fl.fontName + ".fnt";
                final ResourceSource url = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, file);
                final BMFont bmFont = new BMFont(url, fl.useMipMaps);
                _fontList.add(bmFont);
            } catch (final Throwable t) {
                logger.warning(t.getMessage());
            }
        }
        logger.info("defaultFont = " + _fontList.get(0).getStyleName());
    }

    private static class FontLoad {
        String fontName;
        boolean useMipMaps;

        FontLoad(final String name, final boolean mipMap) {
            fontName = name;
            useMipMaps = mipMap;
        }
    }

}
