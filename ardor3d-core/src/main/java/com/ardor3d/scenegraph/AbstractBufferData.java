/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
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
import java.lang.ref.WeakReference;
import java.nio.Buffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ardor3d.renderer.ContextCleanListener;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.RenderContext.RenderContextRef;
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

    /**
     * list of OpenGL contexts we believe have up to date values from this buffer. For use in multi-context mode.
     */
    protected transient Set<WeakReference<RenderContextRef>> _uploadedContexts;

    /** if true, we believe we are fully uploaded to OpenGL. For use in single-context mode. */
    protected transient boolean _uploaded;

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
        if (Constants.useMultipleContexts) {
            _uploadedContexts = new HashSet<>();
        }
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
     * @param context
     *            the OpenGL context to get our id for.
     * @return the buffer id of a buffer in the given context. If the buffer is not found in the given context, 0 is
     *         returned.
     */
    public int getBufferId(final RenderContext context) {
        return getBufferIdByRef(context.getGlContextRef());
    }

    /**
     * @param contextRef
     *            the reference to a shared GL context to get our id for.
     * @return the buffer id of a buffer in the given context. If the buffer is not found in the given context rep, 0 is
     *         returned.
     */
    public int getBufferIdByRef(final RenderContextRef contextRef) {
        if (_bufferIdCache != null) {
            final Integer id = _bufferIdCache.getValue(contextRef);
            if (id != null) {
                return id.intValue();
            }
        }
        return 0;
    }

    /**
     * Removes any buffer id from this object for the given OpenGL context.
     *
     * @param context
     *            the OpenGL context to remove our id for.
     * @return the id removed or 0 if not found.
     */
    public int removeBufferId(final RenderContext context) {
        final Integer id = _bufferIdCache.removeValue(context.getGlContextRef());
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
     * Sets the buffer id representing this data in regards to the given OpenGL context.
     *
     * @param context
     *            the OpenGL context to set our id for.
     * @param id
     *            the buffer id. To be valid, this must greater than 0.
     * @throws IllegalArgumentException
     *             if id is less than or equal to 0.
     */
    public void setBufferId(final RenderContext context, final int id) {
        if (id == 0) {
            throw new IllegalArgumentException("id must != 0");
        }

        if (_bufferIdCache == null) {
            _bufferIdCache = ContextValueReference.newReference(this, _vboRefQueue);
        }
        _bufferIdCache.put(context.getGlContextRef(), id);
    }

    /**
     * @param context
     *            the RenderContext to check our state for.
     * @return false if the buffer is dirty for the given context or we don't have the given object in memory.
     */
    public boolean isBufferClean(final RenderContext context) {
        if (Constants.useMultipleContexts) {
            synchronized (_uploadedContexts) {
                // check if we are empty...
                if (_uploadedContexts.isEmpty()) {
                    return false;
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
                return uploaded;
            }
        } else {
            return _uploaded;
        }
    }

    /**
     * Mark this buffer dirty on all contexts.
     */
    public void markDirty() {
        if (Constants.useMultipleContexts) {
            synchronized (_uploadedContexts) {
                _uploadedContexts.clear();
            }
        } else {
            _uploaded = false;
        }
    }

    /**
     * Mark this buffer clean on the given context.
     *
     * @param context
     *            the context to marks this buffer clean under.
     */
    public void markClean(final RenderContext context) {
        if (Constants.useMultipleContexts) {
            synchronized (_uploadedContexts) {
                _uploadedContexts.add(new WeakReference<>(context.getGlContextRef()));
            }
        } else {
            _uploaded = true;
        }
    }

    public VBOAccessMode getVboAccessMode() {
        return _vboAccessMode;
    }

    public void setVboAccessMode(final VBOAccessMode vboAccessMode) {
        this._vboAccessMode = vboAccessMode;
    }

    public static void cleanAllBuffers(final IShaderUtils utils) {
        final Multimap<RenderContextRef, Integer> idMap = ArrayListMultimap.create();

        // gather up expired vbos... these don't exist in our cache
        gatherGCdIds(idMap);

        // Walk through the cached items and delete those too.
        for (final AbstractBufferData<?> buf : _identityCache.keySet()) {
            if (buf._bufferIdCache != null) {
                if (Constants.useMultipleContexts) {
                    final Set<RenderContextRef> contextObjects = buf._bufferIdCache.getContextRefs();
                    for (final RenderContextRef o : contextObjects) {
                        // Add id to map
                        idMap.put(o, buf.getBufferIdByRef(o));
                    }
                } else {
                    idMap.put(ContextManager.getCurrentContext().getGlContextRef(), buf.getBufferIdByRef(null));
                }
                buf._bufferIdCache.clear();
                buf.markDirty();
            }
        }

        handleVBODelete(utils, idMap);
    }

    public static void cleanAllBuffers(final IShaderUtils utils, final RenderContext context) {
        final Multimap<RenderContextRef, Integer> idMap = ArrayListMultimap.create();

        // gather up expired vbos... these don't exist in our cache
        gatherGCdIds(idMap);

        final RenderContextRef glRef = context.getGlContextRef();
        // Walk through the cached items and delete those too.
        for (final AbstractBufferData<?> buf : _identityCache.keySet()) {
            // only worry about buffers that have received ids.
            if (buf._bufferIdCache != null) {
                final Integer id = buf._bufferIdCache.removeValue(glRef);
                if (id != null && id.intValue() != 0) {
                    idMap.put(context.getGlContextRef(), id);
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
        final Multimap<RenderContextRef, Integer> idMap = gatherGCdIds(null);

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
    private static final Multimap<RenderContextRef, Integer> gatherGCdIds(Multimap<RenderContextRef, Integer> store) {
        // Pull all expired vbos from ref queue and add to an id multimap.
        ContextValueReference<AbstractBufferData<?>, Integer> ref;
        while ((ref = (ContextValueReference<AbstractBufferData<?>, Integer>) _vboRefQueue.poll()) != null) {
            if (Constants.useMultipleContexts) {
                final Set<RenderContextRef> renderRefs = ref.getContextRefs();
                for (final RenderContextRef renderRef : renderRefs) {
                    // Add id to map
                    final Integer id = ref.getValue(renderRef);
                    if (id != null) {
                        if (store == null) { // lazy init
                            store = ArrayListMultimap.create();
                        }
                        store.put(renderRef, id);
                    }
                }
            } else {
                final Integer id = ref.getValue(null);
                if (id != null) {
                    if (store == null) { // lazy init
                        store = ArrayListMultimap.create();
                    }
                    store.put(ContextManager.getCurrentContext().getGlContextRef(), id);
                }
            }
            ref.clear();
        }

        return store;
    }

    private static void handleVBODelete(final IShaderUtils utils, final Multimap<RenderContextRef, Integer> idMap) {
        Object currentGLRef = null;
        // Grab the current context, if any.
        if (utils != null && ContextManager.getCurrentContext() != null) {
            currentGLRef = ContextManager.getCurrentContext().getGlContextRef();
        }
        // For each affected context...
        for (final RenderContextRef glref : idMap.keySet()) {
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
