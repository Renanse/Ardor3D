/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.RenderContext.RenderContextRef;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.gc.ContextValueReference;
import com.ardor3d.util.resource.ResourceSource;
import com.google.common.collect.Lists;

/**
 * <code>TextureKey</code> provides a way for the TextureManager to cache and retrieve <code>Texture</code> objects.
 */
@SavableFactory(factoryMethod = "initSavable")
final public class TextureKey implements Savable {

    /** The source of the image used in this Texture. */
    protected ResourceSource _source = null;

    /** Whether we had asked the loader of the image to flip vertically. */
    protected boolean _flipped;

    /** The stored format of our image. */
    protected TextureStoreFormat _format = TextureStoreFormat.GuessCompressedFormat;

    /**
     * An optional id, used to differentiate keys where the rest of the values are the same. RTT operations are a good
     * example.
     */
    protected String _id;

    /**
     * The minification filter used on our texture (generally determines what mipmaps are created - if any - and how.)
     */
    protected Texture.MinificationFilter _minFilter = MinificationFilter.Trilinear;

    /**
     * list of OpenGL contexts we believe have up to date values from all buffers and indices. For use in multi-context
     * mode.
     */
    protected transient Set<WeakReference<RenderContextRef>> _uploadedContexts;

    /**
     * In single-context rendering this is used to track if this texture is in need of updating.
     */
    private boolean _uploaded;

    /** cache of OpenGL context specific texture ids for the associated texture. */
    protected final transient ContextValueReference<TextureKey, Integer> _idCache = ContextValueReference
            .newReference(this, TextureManager.getRefQueue());

    /** cached hashcode value. */
    protected transient int _code = Integer.MAX_VALUE;

    /** cache of texturekey objects allowing us to find an existing texture key. */
    protected static final List<WeakReference<TextureKey>> _keyCache = Lists.newLinkedList();

    private static final Integer ZERO = new Integer(0);

    /** RTT code use */
    private static AtomicInteger _uniqueTK = new AtomicInteger(Integer.MIN_VALUE);

    /** DO NOT USE. FOR INTERNAL USE ONLY */
    protected TextureKey() {
        if (Constants.useMultipleContexts) {
            _uploadedContexts = new HashSet<>();
        }
    }

    /** DO NOT USE. FOR INTERNAL USE ONLY */
    public static TextureKey initSavable() {
        return new TextureKey();
    }

    public void markDirty() {
        if (Constants.useMultipleContexts) {
            synchronized (_uploadedContexts) {
                _uploadedContexts.clear();
            }
        } else {
            _uploaded = false;
        }
    }

    public boolean isDirty(final RenderContext context) {
        if (Constants.useMultipleContexts) {
            synchronized (_uploadedContexts) {
                // check if we are empty...
                if (_uploadedContexts.isEmpty()) {
                    return true;
                }

                WeakReference<RenderContextRef> ref;
                RenderContextRef check;
                // look for a matching reference and clean out all weak references that have expired
                boolean uploaded = false;
                for (final Iterator<WeakReference<RenderContextRef>> it = _uploadedContexts.iterator(); it.hasNext();) {
                    ref = it.next();
                    check = ref.get();
                    if (check == null) {
                        // found empty, clean up
                        it.remove();
                        continue;
                    }

                    if (!uploaded && check.equals(context.getGlContextRef())) {
                        // found match, return false
                        uploaded = true;
                    }
                }
                return !uploaded;
            }
        } else {
            return !_uploaded;
        }
    }

    public void markClean(final RenderContext context) {
        if (Constants.useMultipleContexts) {
            synchronized (_uploadedContexts) {
                _uploadedContexts.add(new WeakReference<>(context.getGlContextRef()));
            }
        } else {
            _uploaded = true;
        }
    }

    /**
     * Get a new unique TextureKey. This is meant for use by RTT and other situations where we know we are making a
     * unique texture.
     *
     * @param minFilter
     *            our minification filter value.
     * @return the new TextureKey
     */
    public static synchronized TextureKey getRTTKey(final MinificationFilter minFilter) {
        int val = _uniqueTK.addAndGet(1);
        if (val == Integer.MAX_VALUE) {
            _uniqueTK.set(Integer.MIN_VALUE);
            val = Integer.MIN_VALUE;
        }
        return getKey(null, false, TextureStoreFormat.GuessCompressedFormat, "RTT_" + val, minFilter);
    }

    public static synchronized TextureKey getKey(final ResourceSource source, final boolean flipped,
            final TextureStoreFormat storeFormat, final Texture.MinificationFilter minFilter) {
        return getKey(source, flipped, storeFormat, null, minFilter);
    }

    public static synchronized TextureKey getKey(final ResourceSource source, final boolean flipped,
            final TextureStoreFormat storeFormat, final String id, final Texture.MinificationFilter minFilter) {
        final TextureKey key = new TextureKey();

        key._source = source;
        key._flipped = flipped;
        key._minFilter = minFilter;
        key._format = storeFormat;
        key._id = id;
        key._code = Integer.MAX_VALUE;

        {
            WeakReference<TextureKey> ref;
            TextureKey check;
            for (final Iterator<WeakReference<TextureKey>> it = _keyCache.iterator(); it.hasNext();) {
                ref = it.next();
                check = ref.get();
                if (check == null) {
                    // found empty, clean up
                    it.remove();
                    continue;
                }

                if (check.equals(key)) {
                    // found match, return
                    return check;
                }
            }
        }

        // not found
        _keyCache.add(new WeakReference<TextureKey>(key));
        return key;
    }

    public static synchronized boolean clearKey(final TextureKey key) {
        WeakReference<TextureKey> ref;
        TextureKey check;
        for (final Iterator<WeakReference<TextureKey>> it = _keyCache.iterator(); it.hasNext();) {
            ref = it.next();
            check = ref.get();
            if (check != null && check.equals(key)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * @param context
     *            the OpenGL context a texture belongs to.
     * @return the texture id of a texture in the given context. If the texture is not found in the given context, 0 is
     *         returned.
     */
    public Integer getTextureIdForContext(final RenderContext context) {
        return getTextureIdForContextRef(context.getGlContextRef());
    }

    /**
     * @param context
     *            the reference to a shared OpenGL context that a texture belongs to.
     * @return the texture id of a texture in the given context. If the texture is not found in the given context, 0 is
     *         returned.
     */
    public Integer getTextureIdForContextRef(final RenderContextRef contextRef) {
        final Integer id = _idCache.getValue(contextRef);
        if (id != null) {
            return id;
        }
        return ZERO;
    }

    /**
     * @return a Set of context objects that currently reference this texture.
     */
    public Set<RenderContextRef> getContextRefs() {
        return _idCache.getContextRefs();
    }

    /**
     * <p>
     * Removes any texture id for this texture for the given OpenGL context.
     * </p>
     * <p>
     * Note: This does not remove the texture from the card and is provided for use by code that does remove textures
     * from the card.
     * </p>
     *
     * @param context
     *            the OpenGL context this texture belongs to.
     * @return the id removed, or 0 if not found.
     */
    public int removeFromIdCache(final RenderContext context) {
        final Integer id = _idCache.removeValue(context.getGlContextRef());
        if (Constants.useMultipleContexts) {
            synchronized (_uploadedContexts) {
                WeakReference<RenderContextRef> ref;
                RenderContextRef check;
                for (final Iterator<WeakReference<RenderContextRef>> it = _uploadedContexts.iterator(); it.hasNext();) {
                    ref = it.next();
                    check = ref.get();
                    if (check == null || check.equals(context.getGlContextRef())) {
                        it.remove();
                        continue;
                    }
                }
            }
        } else {
            _uploaded = false;
        }
        return id != null ? id.intValue() : 0;
    }

    /**
     * <p>
     * Clears all currently associated texture ids for this texture.
     * </p>
     * <p>
     * Note: This does not remove the texture from the card and is provided for use by code that does remove textures
     * from the card.
     * </p>
     */
    public void clearIdCache() {
        _idCache.clear();
        markDirty();
    }

    /**
     * Sets the id for a texture in regards to the given OpenGL context.
     *
     * @param context
     *            the OpenGL context a texture belongs to.
     * @param textureId
     *            the texture id of a texture. To be valid, this must not be 0.
     * @throws IllegalArgumentException
     *             if textureId is equal to 0.
     */
    public void setTextureIdForContext(final RenderContext context, final int textureId) {
        if (textureId == 0) {
            throw new IllegalArgumentException("textureId must != 0");
        }

        _idCache.put(context.getGlContextRef(), textureId);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof TextureKey)) {
            return false;
        }

        final TextureKey that = (TextureKey) other;
        if (_source == null) {
            if (that._source != null) {
                return false;
            }
        } else if (!_source.equals(that._source)) {
            return false;
        }

        if (_id == null && that._id != null) {
            return false;
        } else if (_id != null && !_id.equals(that._id)) {
            return false;
        }

        if (_minFilter != that._minFilter) {
            return false;
        }

        if (_format != that._format) {
            return false;
        }

        if (_flipped != that._flipped) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        if (_code == Integer.MAX_VALUE) {
            _code = 17;

            _code += 31 * _code + (_source != null ? _source.hashCode() : 0);
            _code += 31 * _code + (_id != null ? _id.hashCode() : 0);
            _code += 31 * _code + _minFilter.hashCode();
            _code += 31 * _code + _format.hashCode();
            _code += 31 * _code + (_flipped ? 1 : 0);
        }
        return _code;
    }

    public Texture.MinificationFilter getMinificationFilter() {
        return _minFilter;
    }

    public TextureStoreFormat getFormat() {
        return _format;
    }

    /**
     * @return Returns the flipped.
     */
    public boolean isFlipped() {
        return _flipped;
    }

    /**
     * @return Returns the source.
     */
    public ResourceSource getSource() {
        return _source;
    }

    public String getId() {
        return _id;
    }

    @Override
    public String toString() {
        final String x = "tkey: src:" + _source + " flip: " + _flipped + " code: " + hashCode() + " imageType: "
                + _format + " id: " + _id;
        return x;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends TextureKey> getClassTag() {
        return this.getClass();
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_source, "source", null);
        capsule.write(_flipped, "flipped", false);
        capsule.write(_format, "format", TextureStoreFormat.GuessCompressedFormat);
        capsule.write(_minFilter, "minFilter", MinificationFilter.Trilinear);
        capsule.write(_id, "id", null);
    }

    public void read(final InputCapsule capsule) throws IOException {
        _source = (ResourceSource) capsule.readSavable("source", null);
        _flipped = capsule.readBoolean("flipped", false);
        _format = capsule.readEnum("format", TextureStoreFormat.class, TextureStoreFormat.GuessCompressedFormat);
        _minFilter = capsule.readEnum("minFilter", MinificationFilter.class, MinificationFilter.Trilinear);
        _id = capsule.readString("id", null);
        _code = Integer.MAX_VALUE;
    }
}