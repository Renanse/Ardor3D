/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.text.font;

import java.util.Map;

import com.ardor3d.extension.ui.text.CharacterDescriptor;
import com.ardor3d.image.Texture2D;
import com.google.common.collect.Maps;

/**
 * Defines a texture based font for use in the UI system.
 */
public class UIFont {

    private final Map<Character, CharacterDescriptor> _charDescriptors = Maps.newHashMap();
    private final Map<Character, Map<Character, Integer>> _kernMap = Maps.newHashMap();
    private final Texture2D _fontTexture;
    private final int _fontHeight;
    private final int _fontSize;

    public UIFont(final Texture2D texture, final Map<Character, CharacterDescriptor> descriptors, final int height,
            final int size) {
        _charDescriptors.putAll(descriptors);
        _fontTexture = texture;
        _fontHeight = height;
        _fontSize = size;
    }

    public int getFontHeight() {
        return _fontHeight;
    }

    public int getFontSize() {
        return _fontSize;
    }

    public Texture2D getFontTexture() {
        return _fontTexture;
    }

    /**
     * @param c
     *            the char to retrieve descriptor for
     * @return a descriptor for the given character, or null if none exists.
     */
    public CharacterDescriptor getDescriptor(final char c) {
        return _charDescriptors.get(c);
    }

    public void addKerning(final char charA, final char charB, final int amount) {
        Map<Character, Integer> map = _kernMap.get(charA);
        if (map == null) {
            map = Maps.newHashMap();
            _kernMap.put(charA, map);
        }

        map.put(charB, amount);
    }

    public int getKerning(final char charA, final char charB) {
        final Map<Character, Integer> map = _kernMap.get(charA);
        if (map != null) {
            final Integer amt = map.get(charB);
            if (amt != null) {
                return amt;
            }
        }
        return 0;
    }
}
