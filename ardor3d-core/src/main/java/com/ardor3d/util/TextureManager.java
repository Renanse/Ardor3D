/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.util.ImageLoaderUtil;
import com.ardor3d.image.util.ImageUtils;
import com.ardor3d.renderer.ContextCleanListener;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.texture.ITextureUtils;
import com.ardor3d.util.gc.ContextValueReference;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;

/**
 * <code>TextureManager</code> provides static methods for building or retrieving a <code>Texture</code> object from
 * cache.
 */
final public class TextureManager {
    private static final Logger logger = Logger.getLogger(TextureManager.class.getName());

    private static Map<TextureKey, Texture> _tCache = new MapMaker().weakKeys().weakValues().makeMap();

    private static ReferenceQueue<TextureKey> _textureRefQueue = new ReferenceQueue<TextureKey>();

    static {
        ContextManager.addContextCleanListener(new ContextCleanListener() {
            public void cleanForContext(final RenderContext renderContext) {
                TextureManager.cleanAllTextures(null, renderContext, null);
            }
        });
    }

    private TextureManager() {}

    /**
     * Loads a texture by attempting to locate the given name using ResourceLocatorTool.
     *
     * @param name
     *            the name of the texture image.
     * @param minFilter
     *            the filter for the near values. Used to determine if we should generate mipmaps.
     * @param flipVertically
     *            If true, the image is flipped vertically during image loading.
     * @return the loaded texture.
     */
    public static Texture load(final String name, final Texture.MinificationFilter minFilter,
            final boolean flipVertically) {
        return load(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, name), minFilter,
                TextureStoreFormat.GuessNoCompressedFormat, flipVertically);
    }

    /**
     * Loads a texture by attempting to locate the given name using ResourceLocatorTool.
     *
     * @param name
     *            the name of the texture image.
     * @param minFilter
     *            the filter for the near values. Used to determine if we should generate mipmaps.
     * @param format
     *            the specific format to use when storing this texture on the card.
     * @param flipVertically
     *            If true, the image is flipped vertically during image loading.
     * @return the loaded texture.
     */
    public static Texture load(final String name, final Texture.MinificationFilter minFilter,
            final TextureStoreFormat format, final boolean flipVertically) {
        return load(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, name), minFilter, format,
                flipVertically);
    }

    /**
     * Loads a texture from the given source.
     *
     * @param source
     *            the source of the texture image.
     * @param minFilter
     *            the filter for the near values. Used to determine if we should generate mipmaps.
     * @param flipVertically
     *            If true, the image is flipped vertically during image loading.
     * @return the loaded texture.
     */
    public static Texture load(final ResourceSource source, final Texture.MinificationFilter minFilter,
            final boolean flipVertically) {
        return load(source, minFilter, TextureStoreFormat.GuessNoCompressedFormat, flipVertically);
    }

    /**
     * Loads a texture from the given source.
     *
     * @param source
     *            the source of the texture image.
     * @param minFilter
     *            the filter for the near values. Used to determine if we should generate mipmaps.
     * @param format
     *            the specific format to use when storing this texture on the card.
     * @param flipVertically
     *            If true, the image is flipped vertically during image loading.
     * @return the loaded texture.
     */
    public static Texture load(final ResourceSource source, final Texture.MinificationFilter minFilter,
            final TextureStoreFormat format, final boolean flipVertically) {

        if (null == source) {
            logger.warning("Could not load image...  source was null. defaultTexture used.");
            return TextureState.getDefaultTexture();
        }

        final TextureKey tkey = TextureKey.getKey(source, flipVertically, format, minFilter);

        return loadFromKey(tkey, null, null);
    }

    /**
     * Creates a texture from a given Ardor3D Image object.
     *
     * @param image
     *            the Ardor3D image.
     * @param minFilter
     *            the filter for the near values. Used to determine if we should generate mipmaps.
     * @return the loaded texture.
     */
    public static Texture loadFromImage(final Image image, final Texture.MinificationFilter minFilter) {
        return loadFromImage(image, minFilter, TextureStoreFormat.GuessNoCompressedFormat);
    }

    /**
     * Creates a texture from a given Ardor3D Image object.
     *
     * @param image
     *            the Ardor3D image.
     * @param minFilter
     *            the filter for the near values. Used to determine if we should generate mipmaps.
     * @param format
     *            the specific format to use when storing this texture on the card.
     * @return the loaded texture.
     */
    public static Texture loadFromImage(final Image image, final Texture.MinificationFilter minFilter,
            final TextureStoreFormat format) {
        final TextureKey key = TextureKey.getKey(null, false, format, "img_" + image.hashCode(), minFilter);
        return loadFromKey(key, image, null);
    }

    /**
     * Load a texture from the given TextureKey. If imageData is given, use that, otherwise load it using the key's
     * source information. If store is given, populate and return that Texture object.
     *
     * @param tkey
     *            our texture key. Must not be null.
     * @param imageData
     *            optional Image data. If present, this is used instead of loading from source.
     * @param store
     *            if not null, this Texture object is populated and returned instead of a new Texture object.
     * @return the resulting texture.
     */
    public static Texture loadFromKey(final TextureKey tkey, final Image imageData, final Texture store) {
        if (tkey == null) {
            logger.warning("TextureKey is null, cannot load");
            return TextureState.getDefaultTexture();
        }

        Texture result = store;

        // First look for the texture using the supplied key
        final Texture cache = findCachedTexture(tkey);

        if (cache != null) {
            // look into cache.
            if (result == null) {
                result = cache.createSimpleClone();
                if (result.getTextureKey() == null) {
                    result.setTextureKey(tkey);
                }
                return result;
            }
            cache.createSimpleClone(result);
            return result;
        }

        Image img = imageData;
        if (img == null) {
            img = ImageLoaderUtil.loadImage(tkey.getSource(), tkey.isFlipped());
        }

        if (null == img) {
            logger.warning("(image null) Could not load: " + tkey.getSource());
            return TextureState.getDefaultTexture();
        }

        // Default to Texture2D
        if (result == null) {
            if (img.getDataSize() == 6) {
                result = new TextureCubeMap();
            } else if (img.getDataSize() > 1) {
                result = new Texture3D();
            } else {
                result = new Texture2D();
            }
        }

        result.setTextureKey(tkey);
        result.setImage(img);
        result.setMinificationFilter(tkey.getMinificationFilter());
        result.setTextureStoreFormat(ImageUtils.getTextureStoreFormat(tkey.getFormat(), result.getImage()));

        // Cache the no-context version
        addToCache(result);
        return result;
    }

    /**
     * Add a given texture to the cache
     *
     * @param texture
     *            our texture
     */
    public static void addToCache(final Texture texture) {
        if (TextureState.getDefaultTexture() == null || (texture != TextureState.getDefaultTexture()
                && texture.getImage() != TextureState.getDefaultTextureImage())) {
            _tCache.put(texture.getTextureKey(), texture);
        }
    }

    /**
     * Locate a texture in the cache by key
     *
     * @param textureKey
     *            our key
     * @return the texture, or null if not found.
     */
    public static Texture findCachedTexture(final TextureKey textureKey) {
        return _tCache.get(textureKey);
    }

    public static Texture removeFromCache(final TextureKey tk) {
        return _tCache.remove(tk);
    }

    /**
     * Delete all textures from card. This will gather all texture ids believed to be on the card and try to delete
     * them. If a deleter is passed in, textures that are part of the currently active context (if one is active) will
     * be deleted immediately. If a deleter is not passed in, we do not have an active context, or we encounter textures
     * that are not part of the current context, then we will queue those textures to be deleted later using the
     * GameTaskQueueManager.
     *
     * @param utils
     *            if not null, this will be used to immediately delete any textures in the currently active context. All
     *            other textures will be queued to delete in their own contexts.
     */
    public static void cleanAllTextures(final ITextureUtils utils) {
        cleanAllTextures(utils, null);
    }

    /**
     * Delete all textures from card. This will gather all texture ids believed to be on the card and try to delete
     * them. If a deleter is passed in, textures that are part of the currently active context (if one is active) will
     * be deleted immediately. If a deleter is not passed in, we do not have an active context, or we encounter textures
     * that are not part of the current context, then we will queue those textures to be deleted later using the
     * GameTaskQueueManager.
     *
     * If a non null map is passed into futureStore, it will be populated with Future objects for each queued context.
     * These objects may be used to discover when the deletion tasks have all completed.
     *
     * @param utils
     *            if not null, this will be used to immediately delete any textures in the currently active context. All
     *            other textures will be queued to delete in their own contexts.
     * @param futureStore
     *            if not null, this map will be populated with any Future task handles created during cleanup.
     */
    public static void cleanAllTextures(final ITextureUtils utils, final Map<Object, Future<Void>> futureStore) {
        // gather up expired textures... these don't exist in our cache
        Multimap<Object, Integer> idMap = gatherGCdIds();

        // Walk through the cached items and gather those too.
        for (final TextureKey key : _tCache.keySet()) {
            // possibly lazy init
            if (idMap == null) {
                idMap = ArrayListMultimap.create();
            }

            if (Constants.useMultipleContexts) {
                final Set<Object> contextObjects = key.getContextObjects();
                for (final Object o : contextObjects) {
                    // Add id to map
                    idMap.put(o, key.getTextureIdForContext(o));
                }
            } else {
                idMap.put(ContextManager.getCurrentContext().getGlContextRep(), key.getTextureIdForContext(null));
            }
            key.removeFromIdCache();
        }

        // delete the ids
        if (idMap != null && !idMap.isEmpty()) {
            handleTextureDelete(utils, idMap, futureStore);
        }
    }

    /**
     * Deletes all textures from card for a specific gl context. This will gather all texture ids believed to be on the
     * card for the given context and try to delete them. If a deleter is passed in, textures that are part of the
     * currently active context (if one is active) will be deleted immediately. If a deleter is not passed in, we do not
     * have an active context, or we encounter textures that are not part of the current context, then we will queue
     * those textures to be deleted later using the GameTaskQueueManager.
     *
     * If a non null map is passed into futureStore, it will be populated with Future objects for each queued context.
     * These objects may be used to discover when the deletion tasks have all completed.
     *
     * @param utils
     *            if not null, this will be used to immediately delete any textures in the currently active context. All
     *            other textures will be queued to delete in their own contexts.
     * @param context
     *            the context to delete for.
     * @param futureStore
     *            if not null, this map will be populated with any Future task handles created during cleanup.
     */
    public static void cleanAllTextures(final ITextureUtils utils, final RenderContext context,
            final Map<Object, Future<Void>> futureStore) {
        // gather up expired textures... these don't exist in our cache
        Multimap<Object, Integer> idMap = gatherGCdIds();

        final Object glRep = context.getGlContextRep();
        // Walk through the cached items and gather those too.
        for (final TextureKey key : _tCache.keySet()) {
            // possibly lazy init
            if (idMap == null) {
                idMap = ArrayListMultimap.create();
            }

            final Integer id = key.getTextureIdForContext(glRep);
            if (id != 0) {
                idMap.put(context.getGlContextRep(), id);
                key.removeFromIdCache(glRep);
            }
        }

        // delete the ids
        if (!idMap.isEmpty()) {
            handleTextureDelete(utils, idMap, futureStore);
        }
    }

    /**
     * Delete any textures from the card that have been recently garbage collected in Java. If a deleter is passed in,
     * gc'd textures that are part of the currently active context (if one is active) will be deleted immediately. If a
     * deleter is not passed in, we do not have an active context, or we encounter gc'd textures that are not part of
     * the current context, then we will queue those textures to be deleted later using the GameTaskQueueManager.
     *
     * @param utils
     *            if not null, this will be used to immediately delete any gc'd textures in the currently active
     *            context. All other gc'd textures will be queued to delete in their own contexts.
     */
    public static void cleanExpiredTextures(final ITextureUtils utils) {
        cleanExpiredTextures(utils, null);
    }

    /**
     * Delete any textures from the card that have been recently garbage collected in Java. If a deleter is passed in,
     * gc'd textures that are part of the currently active context (if one is active) will be deleted immediately. If a
     * deleter is not passed in, we do not have an active context, or we encounter gc'd textures that are not part of
     * the current context, then we will queue those textures to be deleted later using the GameTaskQueueManager.
     *
     * If a non null map is passed into futureStore, it will be populated with Future objects for each queued context.
     * These objects may be used to discover when the deletion tasks have all completed.
     *
     * @param utils
     *            if not null, this will be used to immediately delete any gc'd textures in the currently active
     *            context. All other gc'd textures will be queued to delete in their own contexts.
     * @param futureStore
     *            if not null, this map will be populated with any Future task handles created during cleanup.
     */
    public static void cleanExpiredTextures(final ITextureUtils utils, final Map<Object, Future<Void>> futureStore) {
        // gather up expired textures...
        final Multimap<Object, Integer> idMap = gatherGCdIds();

        // send to be deleted on next render.
        if (idMap != null) {
            handleTextureDelete(utils, idMap, futureStore);
        }
    }

    @SuppressWarnings("unchecked")
    private static Multimap<Object, Integer> gatherGCdIds() {
        Multimap<Object, Integer> idMap = null;
        // Pull all expired textures from ref queue and add to an id multimap.
        ContextValueReference<TextureKey, Integer> ref;
        Integer id;
        while ((ref = (ContextValueReference<TextureKey, Integer>) _textureRefQueue.poll()) != null) {
            // lazy init
            if (idMap == null) {
                idMap = ArrayListMultimap.create();
            }
            if (Constants.useMultipleContexts) {
                final Set<Object> contextObjects = ref.getContextObjects();
                for (final Object o : contextObjects) {
                    id = ref.getValue(o);
                    if (id != null && id.intValue() != 0) {
                        // Add id to map
                        idMap.put(o, id);
                    }
                }
            } else {
                id = ref.getValue(null);
                if (id != null && id.intValue() != 0) {
                    idMap.put(ContextManager.getCurrentContext().getGlContextRep(), id);
                }
            }
            ref.clear();
        }
        return idMap;
    }

    private static void handleTextureDelete(final ITextureUtils utils, final Multimap<Object, Integer> idMap,
            final Map<Object, Future<Void>> futureStore) {
        Object currentGLRef = null;
        // Grab the current context, if any.
        if (utils != null && ContextManager.getCurrentContext() != null) {
            currentGLRef = ContextManager.getCurrentContext().getGlContextRep();
        }
        // For each affected context...
        for (final Object glref : idMap.keySet()) {
            // If we have a deleter and the context is current, immediately delete
            if (currentGLRef != null && (!Constants.useMultipleContexts || glref.equals(currentGLRef))) {
                utils.deleteTextureIds(idMap.get(glref));
            }
            // Otherwise, add a delete request to that context's render task queue.
            else {
                final Future<Void> future = GameTaskQueueManager.getManager(ContextManager.getContextForRef(glref))
                        .render(new RendererCallable<Void>() {
                            public Void call() throws Exception {
                                getRenderer().getTextureUtils().deleteTextureIds(idMap.get(glref));
                                return null;
                            }
                        });
                if (futureStore != null) {
                    futureStore.put(glref, future);
                }
            }
        }
    }

    @MainThread
    public static void preloadCache(final ITextureUtils util) {
        for (final Texture t : _tCache.values()) {
            if (t == null) {
                continue;
            }
            if (t.getTextureKey().getSource() != null) {
                util.loadTexture(t, 0);
            }
        }
    }

    static ReferenceQueue<TextureKey> getRefQueue() {
        return _textureRefQueue;
    }
}
