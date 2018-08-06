/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
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
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.util.Constants;
import com.ardor3d.util.ContextIdReference;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;

public abstract class AbstractBufferData<T extends Buffer> implements Savable {

    private static Map<AbstractBufferData<?>, Object> _identityCache = new MapMaker().weakKeys().makeMap();
    private static final Object STATIC_REF = new Object();

    private static ReferenceQueue<AbstractBufferData<?>> _vboRefQueue = new ReferenceQueue<AbstractBufferData<?>>();

    static {
        ContextManager.addContextCleanListener(new ContextCleanListener() {
            public void cleanForContext(final RenderContext renderContext) {
                AbstractBufferData.cleanAllVBOs(null, renderContext);
            }
        });
    }

    protected transient ContextIdReference<AbstractBufferData<T>> _vboIdCache;

    /** Buffer holding the data. */
    protected T _buffer;

    /** Access mode of the buffer when using Vertex Buffer Objects. */
    public enum VBOAccessMode {
        StaticDraw, StaticCopy, StaticRead, StreamDraw, StreamCopy, StreamRead, DynamicDraw, DynamicCopy, DynamicRead
    }

    /** VBO Access mode for this buffer. */
    protected VBOAccessMode _vboAccessMode = VBOAccessMode.StaticDraw;

    /** Flag for notifying the renderer that the VBO buffer needs to be updated. */
    protected boolean _needsRefresh = false;

    AbstractBufferData() {
        _identityCache.put(this, STATIC_REF);
    }

    /**
     * @return the number of bytes per entry in the buffer. For example, an IntBuffer would return 4.
     */
    public abstract int getByteCount();

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
     *            the object representing the OpenGL context a vbo belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @return the vbo id of a vbo in the given context. If the vbo is not found in the given context, 0 is returned.
     */
    public int getVBOID(final Object glContext) {
        if (_vboIdCache != null) {
            final Integer id = _vboIdCache.getValue(glContext);
            if (id != null) {
                return id.intValue();
            }
        }
        return 0;
    }

    /**
     * Removes any vbo id from this buffer's data for the given OpenGL context.
     *
     * @param glContext
     *            the object representing the OpenGL context a vbo would belong to. See
     *            {@link RenderContext#getGlContextRep()}
     * @return the id removed or 0 if not found.
     */
    public int removeVBOID(final Object glContext) {
        if (_vboIdCache != null) {
            return _vboIdCache.removeValue(glContext);
        } else {
            return 0;
        }
    }

    /**
     * Sets the id for a vbo based on this buffer's data in regards to the given OpenGL context.
     *
     * @param glContextRep
     *            the object representing the OpenGL context a vbo belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @param vboId
     *            the vbo id of a vbo. To be valid, this must be not equals to 0.
     * @throws IllegalArgumentException
     *             if vboId is less than or equal to 0.
     */
    public void setVBOID(final Object glContextRep, final int vboId) {
        if (vboId == 0) {
            throw new IllegalArgumentException("vboId must != 0");
        }

        if (_vboIdCache == null) {
            _vboIdCache = new ContextIdReference<AbstractBufferData<T>>(this, _vboRefQueue);
        }
        _vboIdCache.put(glContextRep, vboId);
    }

    public VBOAccessMode getVboAccessMode() {
        return _vboAccessMode;
    }

    public void setVboAccessMode(final VBOAccessMode vboAccessMode) {
        this._vboAccessMode = vboAccessMode;
    }

    public boolean isNeedsRefresh() {
        return _needsRefresh;
    }

    public void setNeedsRefresh(final boolean needsRefresh) {
        this._needsRefresh = needsRefresh;
    }

    public static void cleanAllVBOs(final Renderer deleter) {
        final Multimap<Object, Integer> idMap = ArrayListMultimap.create();

        // gather up expired vbos... these don't exist in our cache
        gatherGCdIds(idMap);

        // Walk through the cached items and delete those too.
        for (final AbstractBufferData<?> buf : _identityCache.keySet()) {
            if (buf._vboIdCache != null) {
                if (Constants.useMultipleContexts) {
                    final Set<Object> contextObjects = buf._vboIdCache.getContextObjects();
                    for (final Object o : contextObjects) {
                        // Add id to map
                        idMap.put(o, buf.getVBOID(o));
                    }
                } else {
                    idMap.put(ContextManager.getCurrentContext().getGlContextRep(), buf.getVBOID(null));
                }
                buf._vboIdCache.clear();
            }
        }

        handleVBODelete(deleter, idMap);
    }

    public static void cleanAllVBOs(final Renderer deleter, final RenderContext context) {
        final Multimap<Object, Integer> idMap = ArrayListMultimap.create();

        // gather up expired vbos... these don't exist in our cache
        gatherGCdIds(idMap);

        final Object glRep = context.getGlContextRep();
        // Walk through the cached items and delete those too.
        for (final AbstractBufferData<?> buf : _identityCache.keySet()) {
            // only worry about buffers that have received ids.
            if (buf._vboIdCache != null) {
                final Integer id = buf._vboIdCache.removeValue(glRep);
                if (id != null && id.intValue() != 0) {
                    idMap.put(context.getGlContextRep(), id);
                }
            }
        }

        handleVBODelete(deleter, idMap);
    }

    /**
     * Clean any VBO ids from the hardware, using the given Renderer object to do the work immediately, if given. If
     * not, we will delete in the next execution of the appropriate context's game task render queue.
     *
     * @param deleter
     *            the Renderer to use. If null, execution will not occur immediately.
     */
    public static void cleanExpiredVBOs(final Renderer deleter) {
        // gather up expired vbos...
        final Multimap<Object, Integer> idMap = gatherGCdIds(null);

        if (idMap != null) {
            // send to be deleted (perhaps on next render.)
            handleVBODelete(deleter, idMap);
        }
    }

    /**
     * @return a deep copy of this buffer data object
     */
    public abstract AbstractBufferData<T> makeCopy();

    @SuppressWarnings("unchecked")
    private static final Multimap<Object, Integer> gatherGCdIds(Multimap<Object, Integer> store) {
        // Pull all expired vbos from ref queue and add to an id multimap.
        ContextIdReference<AbstractBufferData<?>> ref;
        while ((ref = (ContextIdReference<AbstractBufferData<?>>) _vboRefQueue.poll()) != null) {
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

    private static void handleVBODelete(final Renderer deleter, final Multimap<Object, Integer> idMap) {
        Object currentGLRef = null;
        // Grab the current context, if any.
        if (deleter != null && ContextManager.getCurrentContext() != null) {
            currentGLRef = ContextManager.getCurrentContext().getGlContextRep();
        }
        // For each affected context...
        for (final Object glref : idMap.keySet()) {
            // If we have a deleter and the context is current, immediately delete
            if (deleter != null && glref.equals(currentGLRef)) {
                deleter.deleteVBOs(idMap.get(glref));
            }
            // Otherwise, add a delete request to that context's render task queue.
            else {
                GameTaskQueueManager.getManager(ContextManager.getContextForRef(glref)).render(
                        new RendererCallable<Void>() {
                            public Void call() throws Exception {
                                getRenderer().deleteVBOs(idMap.get(glref));
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
