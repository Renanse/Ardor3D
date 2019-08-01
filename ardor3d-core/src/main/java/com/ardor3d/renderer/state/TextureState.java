/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.state;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.ardor3d.util.resource.URLResourceSource;

/**
 * <code>TextureState</code> maintains a texture state for a given node and it's children. The number of states that a
 * TextureState can maintain at one time is equal to the number of texture units available on the GPU. It is not within
 * the scope of this class to generate the texture, and is recommended that <code>TextureManager</code> be used to
 * create the Texture objects.
 *
 * @see com.ardor3d.util.TextureManager
 */
public class TextureState extends RenderState {
    private static final Logger logger = Logger.getLogger(TextureState.class.getName());

    public static final int MAX_TEXTURES = 32;

    protected static Texture _defaultTexture = null;
    protected static boolean defaultTextureLoaded = false;

    /** The texture(s). */
    protected List<Texture> _texture = new ArrayList<Texture>(1);

    public transient TextureKey[] _keyCache = new TextureKey[MAX_TEXTURES];

    public static ResourceSource DEFAULT_TEXTURE_SOURCE;
    static {
        try {
            DEFAULT_TEXTURE_SOURCE = new URLResourceSource(ResourceLocatorTool.getClassPathResource(TextureState.class,
                    "com/ardor3d/renderer/state/notloaded.tga"));
        } catch (final Exception e) {
            // ignore.
            DEFAULT_TEXTURE_SOURCE = null;
        }
    }

    /**
     * Constructor instantiates a new <code>TextureState</code> object.
     */
    public TextureState() {
        if (!defaultTextureLoaded) {
            loadDefaultTexture();
        }
    }

    @Override
    public StateType getType() {
        return StateType.Texture;
    }

    /**
     * <code>setTexture</code> sets a single texture to the first texture unit.
     *
     * @param texture
     *            the texture to set.
     */
    public void setTexture(final Texture texture) {
        if (_texture.size() == 0) {
            _texture.add(texture);
        } else {
            _texture.set(0, texture);
        }
        setNeedsRefresh(true);
    }

    /**
     * <code>getTexture</code> gets the texture that is assigned to the first texture unit.
     *
     * @return the texture in the first texture unit.
     */
    public Texture getTexture() {
        if (_texture.size() > 0) {
            return _texture.get(0);
        } else {
            return null;
        }
    }

    /**
     * <code>setTexture</code> sets the texture object to be used by the state. The texture unit that this texture uses
     * is set, if the unit is not valid, i.e. less than zero or greater than the number of texture units supported by
     * the graphics card, it is ignored.
     *
     * @param texture
     *            the texture to be used by the state.
     * @param textureUnit
     *            the texture unit this texture will fill.
     */
    public void setTexture(final Texture texture, final int textureUnit) {
        if (textureUnit >= 0 && textureUnit < MAX_TEXTURES) {
            while (textureUnit >= _texture.size()) {
                _texture.add(null);
            }
            _texture.set(textureUnit, texture);
        }
        setNeedsRefresh(true);
    }

    /**
     * <code>getTexture</code> retrieves the texture being used by the state in a particular texture unit.
     *
     * @param textureUnit
     *            the texture unit to retrieve the texture from.
     * @return the texture being used by the state. If the texture unit is invalid, null is returned.
     */
    public Texture getTexture(final int textureUnit) {
        if (textureUnit < _texture.size() && textureUnit >= 0) {
            return _texture.get(textureUnit);
        }

        return null;
    }

    public boolean removeTexture(final Texture tex) {

        final int index = _texture.indexOf(tex);
        if (index == -1) {
            return false;
        }

        _texture.set(index, null);
        _keyCache[index] = null;
        return true;
    }

    public boolean removeTexture(final int textureUnit) {
        if (textureUnit < 0 || textureUnit >= MAX_TEXTURES || textureUnit >= _texture.size()) {
            return false;
        }

        final Texture t = _texture.get(textureUnit);
        if (t == null) {
            return false;
        }

        _texture.set(textureUnit, null);
        _keyCache[textureUnit] = null;
        return true;

    }

    /**
     * Removes all textures in this texture state. Does not delete them from the graphics card.
     */
    public void clearTextures() {
        for (int i = _texture.size(); --i >= 0;) {
            removeTexture(i);
        }
    }

    /**
     * Returns the number of textures this texture manager is maintaining.
     *
     * @return the number of textures.
     */
    public int getNumberOfSetTextures() {
        int set = 0;
        for (int i = 0; i < _texture.size(); i++) {
            if (_texture.get(i) != null) {
                set++;
            }
        }
        return set;
    }

    /**
     * Returns the max index in this TextureState that contains a non-null Texture.
     *
     * @return the max index, or -1 if no textures are contained by this state.
     */
    public int getMaxTextureIndexUsed() {
        int max = _texture.size() - 1;
        while (max > 0 && _texture.get(max) == null) {
            max--;
        }
        return max;
    }

    /**
     * Fast access for retrieving a TextureKey. A return is guaranteed when <code>textureUnit</code> is any number under
     * or equal to the highest texture unit currently in use. This value can be retrieved with
     * <code>getNumberOfSetTextures</code>. A higher value might result in unexpected behavior such as an exception
     * being thrown.
     *
     * @param textureUnit
     *            The texture unit from which to retrieve the TextureKey.
     * @return the TextureKey, or null if there is none.
     */
    public final TextureKey getTextureKey(final int textureUnit) {
        if (textureUnit < _keyCache.length && textureUnit >= 0) {
            return _keyCache[textureUnit];
        }

        return null;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.writeSavableList(_texture, "texture", new ArrayList<Texture>(1));

    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _texture = capsule.readSavableList("texture", new ArrayList<Texture>(1));
    }

    public static Image getDefaultTextureImage() {
        return _defaultTexture != null ? _defaultTexture.getImage() : null;
    }

    public static Texture getDefaultTexture() {
        if (!defaultTextureLoaded) {
            loadDefaultTexture();
        }
        return _defaultTexture.createSimpleClone();
    }

    private static void loadDefaultTexture() {
        synchronized (logger) {
            if (!defaultTextureLoaded) {
                defaultTextureLoaded = true;
                _defaultTexture = new Texture2D();
                try {
                    _defaultTexture = TextureManager.load(DEFAULT_TEXTURE_SOURCE, Texture.MinificationFilter.Trilinear,
                            true);
                } catch (final Exception e) {
                    logger.log(Level.WARNING, "Failed to load default texture: notloaded.tga", e);
                }
            }
        }
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new TextureStateRecord();
    }

    @Override
    public RenderState extract(final Stack<? extends RenderState> stack, final Spatial spat) {
        if (spat == null) {
            return stack.peek();
        }

        final TextureCombineMode mode = spat.getSceneHints().getTextureCombineMode();
        if (mode == TextureCombineMode.Replace || (mode != TextureCombineMode.Off && stack.size() == 1)) {
            // todo: use dummy state if off?
            return stack.peek();
        }

        // accumulate the textures in the stack into a single TextureState object
        final TextureState newTState = new TextureState();
        boolean foundEnabled = false;
        final Object states[] = stack.toArray();
        switch (mode) {
            case CombineClosest:
            case CombineClosestEnabled:
                for (int iIndex = states.length - 1; iIndex >= 0; iIndex--) {
                    final TextureState pkTState = (TextureState) states[iIndex];
                    if (!pkTState.isEnabled()) {
                        if (mode == TextureCombineMode.CombineClosestEnabled) {
                            break;
                        }

                        continue;
                    }

                    foundEnabled = true;
                    for (int i = 0, max = pkTState.getMaxTextureIndexUsed(); i <= max; i++) {
                        final Texture pkText = pkTState.getTexture(i);
                        if (newTState.getTexture(i) == null) {
                            newTState.setTexture(pkText, i);
                        }
                    }
                }
                break;
            case CombineFirst:
                for (int iIndex = 0, max = states.length; iIndex < max; iIndex++) {
                    final TextureState pkTState = (TextureState) states[iIndex];
                    if (!pkTState.isEnabled()) {
                        continue;
                    }

                    foundEnabled = true;
                    for (int i = 0; i < TextureState.MAX_TEXTURES; i++) {
                        final Texture pkText = pkTState.getTexture(i);
                        if (newTState.getTexture(i) == null) {
                            newTState.setTexture(pkText, i);
                        }
                    }
                }
                break;
            default:
                break;
        }
        newTState.setEnabled(foundEnabled);
        return newTState;
    }
}
