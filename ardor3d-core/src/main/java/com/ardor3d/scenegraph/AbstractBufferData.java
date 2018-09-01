/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.nio.Buffer;
import java.util.Map;
import java.util.Set;

import com.ardor3d.renderer.ContextCleanListener;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.renderer.material.IShaderUtils;
import com.ardor3d.util.Constants;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.gc.ContextValueReference;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;

public abstract class AbstractBufferData<T extends Buffer> implements Savable {

    /** Specifies the number of coordinates per vertex. Must be 1 - 4. */
    protected int _valuesPerTuple;

    private static Map<AbstractBufferData<?>, Object> _identityCache = new MapMaker().weakKeys().makeMap();
    private static final Object STATIC_REF = new Object();

    private static ReferenceQueue<AbstractBufferData<?>> _vboRefQueue = new ReferenceQueue<AbstractBufferData<?>>();

    static {
        ContextManager.addContextCleanListener(new ContextCleanListener() {
            public void cleanForContext(final RenderContext renderContext) {
                AbstractBufferData.cleanAllBuffers(null, renderContext);
            }
        });
    }

    protected transient ContextValueReference<AbstractBufferData<T>, Integer> _bufferIdCache;
    protected transient ContextValueReference<AbstractBufferData<T>, Boolean> _bufferCleanCache;

    /** Buffer holding the data. */
    protected T _buffer;

    /** Access mode of the buffer when using Vertex Buffer Objects. */
    public enum VBOAccessMode {
        StaticDraw, StaticCopy, StaticRead, StreamDraw, StreamCopy, StreamRead, DynamicDraw, DynamicCopy, DynamicRead
    }

    /** VBO Access mode for this buffer. */
    protected VBOAccessMode _vboAccessMode = VBOAccessMode.StaticDraw;

    AbstractBufferData() {
        _identityCache.put(this, STATIC_REF);
    }

    /**
     * @return the number of bytes per entry in the buffer. For example, an IntBuffer would return 4.
     */
    public abstract int getByteCount();

    public int getTupleCount() {
        if (_valuesPerTuple == 0) {
            return 0;
        }
        return getBufferLimit() / _valuesPerTuple;
    }

    /**
     * @return number of values per tuple
     */
    public int getValuesPerTuple() {
        return _valuesPerTuple;
    }

    /**
     * Gets the count.
     *
     * @return the count
     */
    public int getBufferLimit() {
        if (_buffer != null) {
            return _buffer.limit();
        }

        return 0;
    }

    /**
     * Gets the count.
     *
     * @return the count
     */
    public int getBufferCapacity() {
        if (_buffer != null) {
            return _buffer.capacity();
        }

        return 0;
    }

    /**
     * Get the buffer holding the data.
     *
     * @return the buffer
     */
    public T getBuffer() {
        return _buffer;
    }

    /**
     * Set the buffer holding the data.
     *
     * @param buffer
     *            the buffer to set
     */
    public void setBuffer(final T buffer) {
        _buffer = buffer;
    }

    /**
     * @param glContext
     *            the object representing the OpenGL context a buffer belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @return the buffer id of a buffer in the given context. If the buffer is not found in the given context, 0 is
     *         returned.
     */
    public int getBufferId(final Object glContext) {
        if (_bufferIdCache != null) {
            final Integer id = _bufferIdCache.getValue(glContext);
            if (id != null) {
                return id.intValue();
            }
        }
        return 0;
    }

    /**
     * Removes any buffer id from this object for the given OpenGL context.
     *
     * @param glContext
     *            the object representing the OpenGL context a buffer would belong to. See
     *            {@link RenderContext#getGlContextRep()}
     * @return the id removed or 0 if not found.
     */
    public int removeBufferId(final Object glContext) {
        if (_bufferCleanCache != null) {
            _bufferCleanCache.removeValue(glContext);
        }
        if (_bufferIdCache != null) {
            return _bufferIdCache.removeValue(glContext);
        }

        return 0;
    }

    /**
     * Sets the buffer id representing this data in regards to the given OpenGL context.
     *
     * @param glContext
     *            the object representing the OpenGL context a buffer belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @param id
     *            the buffer id. To be valid, this must greater than 0.
     * @throws IllegalArgumentException
     *             if id is less than or equal to 0.
     */
    public void setBufferId(final Object glContext, final int id) {
        if (id == 0) {
            throw new IllegalArgumentException("id must != 0");
        }

        if (_bufferIdCache == null) {
            _bufferIdCache = ContextValueReference.newReference(this, _vboRefQueue);
        }
        _bufferIdCache.put(glContext, id);
    }

    /**
     * @param glContext
     *            the object representing the OpenGL context a buffer belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @return false if the buffer is dirty for the given context or we don't have the given object in memory.
     */
    public boolean isBufferClean(final Object glContext) {
        if (_bufferCleanCache != null) {
            final Boolean value = _bufferCleanCache.getValue(glContext);
            if (value != null) {
                return value.booleanValue();
            }
        }

        return false;
    }

    /**
     * Mark this buffer dirty on all contexts.
     */
    public void markDirty() {
        if (_bufferCleanCache == null) {
            return;
        }
        // we assume an entry is to mark us clean
        _bufferCleanCache.clear();
    }

    /**
     * Mark this buffer clean on the given context.
     *
     * @param glContext
     *            the object representing the OpenGL context a buffer belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     */
    public void markClean(final Object glContext) {
        if (_bufferCleanCache == null) {
            _bufferCleanCache = ContextValueReference.newReference(this, null);
        }
        _bufferCleanCache.put(glContext, true);
    }

    public VBOAccessMode getVboAccessMode() {
        return _vboAccessMode;
    }

    public void setVboAccessMode(final VBOAccessMode vboAccessMode) {
        this._vboAccessMode = vboAccessMode;
    }

    public static void cleanAllBuffers(final IShaderUtils utils) {
        final Multimap<Object, Integer> idMap = ArrayListMultimap.create();

        // gather up expired vbos... these don't exist in our cache
        gatherGCdIds(idMap);

        // Walk through the cached items and delete those too.
        for (final AbstractBufferData<?> buf : _identityCache.keySet()) {
            if (buf._bufferIdCache != null) {
                if (Constants.useMultipleContexts) {
                    final Set<Object> contextObjects = buf._bufferIdCache.getContextObjects();
                    for (final Object o : contextObjects) {
                        // Add id to map
                        idMap.put(o, buf.getBufferId(o));
                    }
                } else {
                    idMap.put(ContextManager.getCurrentContext().getGlContextRep(), buf.getBufferId(null));
                }
                buf._bufferIdCache.clear();
            }
        }

        handleVBODelete(utils, idMap);
    }

    public static void cleanAllBuffers(final IShaderUtils utils, final RenderContext context) {
        final Multimap<Object, Integer> idMap = ArrayListMultimap.create();

        // gather up expired vbos... these don't exist in our cache
        gatherGCdIds(idMap);

        final Object glRep = context.getGlContextRep();
        // Walk through the cached items and delete those too.
        for (final AbstractBufferData<?> buf : _identityCache.keySet()) {
            // only worry about buffers that have received ids.
            if (buf._bufferIdCache != null) {
                final Integer id = buf._bufferIdCache.removeValue(glRep);
                if (id != null && id.intValue() != 0) {
                    idMap.put(context.getGlContextRep(), id);
                }
            }
        }

        handleVBODelete(utils, idMap);
    }

    /**
     * Clean any VBO ids from the hardware, using the given utility object to do the work immediately, if given. If not,
     * we will delete in the next execution of the appropriate context's game task render queue.
     *
     * @param utils
     *            the util class to use. If null, execution will not occur immediately.
     */
    public static void cleanExpiredVBOs(final IShaderUtils utils) {
        // gather up expired vbos...
        final Multimap<Object, Integer> idMap = gatherGCdIds(null);

        if (idMap != null) {
            // send to be deleted (perhaps on next render.)
            handleVBODelete(utils, idMap);
        }
    }

    /**
     * @return a deep copy of this buffer data object
     */
    public abstract AbstractBufferData<T> makeCopy();

    @SuppressWarnings("unchecked")
    private static final Multimap<Object, Integer> gatherGCdIds(Multimap<Object, Integer> store) {
        // Pull all expired vbos from ref queue and add to an id multimap.
        ContextValueReference<AbstractBufferData<?>, Integer> ref;
        while ((ref = (ContextValueReference<AbstractBufferData<?>, Integer>) _vboRefQueue.poll()) != null) {
            if (Constants.useMultipleContexts) {
                final Set<Object> contextObjects = ref.getContextObjects();
                for (final Object o : contextObjects) {
                    // Add id to map
                    final Integer id = ref.getValue(o);
                    if (id != null) {
                        if (store == null) { // lazy init
                            store = ArrayListMultimap.create();
                        }
                        store.put(o, id);
                    }
                }
            } else {
                final Integer id = ref.getValue(null);
                if (id != null) {
                    if (store == null) { // lazy init
                        store = ArrayListMultimap.create();
                    }
                    store.put(ContextManager.getCurrentContext().getGlContextRep(), id);
                }
            }
            ref.clear();
        }

        return store;
    }

    private static void handleVBODelete(final IShaderUtils utils, final Multimap<Object, Integer> idMap) {
        Object currentGLRef = null;
        // Grab the current context, if any.
        if (utils != null && ContextManager.getCurrentContext() != null) {
            currentGLRef = ContextManager.getCurrentContext().getGlContextRep();
        }
        // For each affected context...
        for (final Object glref : idMap.keySet()) {
            // If we have a deleter and the context is current, immediately delete
            if (utils != null && glref.equals(currentGLRef)) {
                utils.deleteBuffers(idMap.get(glref));
            }
            // Otherwise, add a delete request to that context's render task queue.
            else {
                GameTaskQueueManager.getManager(ContextManager.getContextForRef(glref))
                        .render(new RendererCallable<Void>() {
                            public Void call() throws Exception {
                                getRenderer().getShaderUtils().deleteBuffers(idMap.get(glref));
                                return null;
                            }
                        });
            }
        }
    }

    public void read(final InputCapsule capsule) throws IOException {
        _vboAccessMode = capsule.readEnum("vboAccessMode", VBOAccessMode.class, VBOAccessMode.StaticDraw);
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_vboAccessMode, "vboAccessMode", VBOAccessMode.StaticDraw);
    }
}
