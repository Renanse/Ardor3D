/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text.font;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.text.CharacterDescriptor;
import com.ardor3d.extension.ui.text.StyleConstants;
import com.ardor3d.framework.IDpiScaleProvider;
import com.ardor3d.image.Texture2D;
import com.ardor3d.ui.text.BMFont;
import com.ardor3d.ui.text.BMFont.Char;
import com.ardor3d.util.export.binary.BinaryImporter;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.URLResourceSource;

/**
 * Provides BMFonts for use in UIFont.
 */
public class BMFontProvider implements FontProvider {

  private static Logger logger = Logger.getLogger(BMFontProvider.class.getName());

  protected Map<UIFont, Integer> _scoreMap = new HashMap<>();

  protected final Set<FontInfo> _fonts = new HashSet<>();

  public void addFont(final String source, final String family, final int size, final boolean bold,
      final boolean italic) {
    final FontInfo fontInfo = new FontInfo();
    fontInfo.source = source;
    fontInfo.family = family;
    fontInfo.size = size;
    fontInfo.bold = bold;
    fontInfo.italic = italic;
    _fonts.add(fontInfo);
  }

  @Override
  public UIFont getClosestMatchingFont(final Map<String, Object> currentStyles, final IDpiScaleProvider scaler,
      final AtomicReference<Double> scale) {
    final boolean isBold =
        currentStyles.containsKey(StyleConstants.KEY_BOLD) ? (Boolean) currentStyles.get(StyleConstants.KEY_BOLD)
            : false;
    final boolean isItalic =
        currentStyles.containsKey(StyleConstants.KEY_ITALICS) ? (Boolean) currentStyles.get(StyleConstants.KEY_ITALICS)
            : false;
    final String family =
        currentStyles.containsKey(StyleConstants.KEY_FAMILY) ? currentStyles.get(StyleConstants.KEY_FAMILY).toString()
            : UIComponent.getDefaultFontFamily();

    int size = currentStyles.containsKey(StyleConstants.KEY_SIZE) ? (Integer) currentStyles.get(StyleConstants.KEY_SIZE)
        : UIComponent.getDefaultFontSize();
    if (scaler != null) {
      size = (int) Math.round(scaler.scaleToScreenDpi(size));
    }

    FontInfo closest = null;
    int score, bestScore = Integer.MIN_VALUE;

    for (final FontInfo info : _fonts) {
      score = 0;

      if (family.equalsIgnoreCase(info.family)) {
        score += 200;
      }

      score -= 5 * Math.abs(size - info.size);

      score += isBold == info.bold ? 10 : -10;
      score += isItalic == info.italic ? 10 : -10;

      if (score > bestScore) {
        closest = info;
        bestScore = score;
      }
    }

    if (closest == null) {
      return null;
    }

    scale.set((double) size / closest.size);

    // now build the requested font if necessary
    if (closest.uiFont == null) {
      // load from file if necessary
      if (closest.bmFont == null) {
        // First try to load from a3d file
        String resource = closest.source + ".a3d";
        URL url = ResourceLocatorTool.getClassPathResource(BMFontProvider.class, resource);
        try {
          if (url != null) {
            final BinaryImporter binaryImporter = new BinaryImporter();
            closest.bmFont = (BMFont) binaryImporter.load(url);
          } else {
            // Not found, load from .fnt
            resource = closest.source + ".fnt";
            url = ResourceLocatorTool.getClassPathResource(BMFontProvider.class, resource);
            closest.bmFont = new BMFont(new URLResourceSource(url), false);
          }
        } catch (final IOException e) {
          BMFontProvider.logger.severe("Unable to load font: " + closest.source);
          return null;
        }
      }

      final Map<Character, CharacterDescriptor> descriptors = new HashMap<>();
      for (final int val : closest.bmFont.getMappedChars()) {
        final Char c = closest.bmFont.getChar(val);
        final CharacterDescriptor desc = new CharacterDescriptor((char) c.id, c.x, c.y, c.width, c.height, c.xadvance,
            c.xoffset, c.yoffset, 1.0, null);
        descriptors.put((char) val, desc);
      }

      closest.uiFont = new UIFont((Texture2D) closest.bmFont.getPageTexture(), descriptors,
          closest.bmFont.getLineHeight(), closest.bmFont.getSize());
      final Map<Integer, Map<Integer, Integer>> kernings = closest.bmFont.getKerningMap();
      for (final int valA : kernings.keySet()) {
        final Map<Integer, Integer> kerns = kernings.get(valA);
        for (final int valB : kerns.keySet()) {
          closest.uiFont.addKerning((char) valA, (char) valB, kerns.get(valB));
        }
      }
    }

    return closest.uiFont;
  }

  class FontInfo {
    String source;
    String family;
    int size;
    boolean bold;
    boolean italic;
    BMFont bmFont;
    UIFont uiFont;
  }
}
