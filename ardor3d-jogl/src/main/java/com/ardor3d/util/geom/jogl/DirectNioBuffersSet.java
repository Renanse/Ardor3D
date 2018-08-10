/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom.jogl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.ardor3d.util.geom.BufferUtils;
import com.jogamp.common.nio.Buffers;

/**
 * <code>DirectNioBuffersSet</code> is a helper class for generating reusable long-lived direct nio buffers mainly for
 * the renderer based on JOGL and its helpers. Those buffers replace many short-lived ones that had no chance to be
 * garbage collected when the VM runs out of memory on its native heap but only when it runs out of memory on its Java
 * heap. This class is not thread-safe.
 */
public final class DirectNioBuffersSet {

    private static final int slicedByteBufferMaxSize = 4096;

    private static final int transformBufferMaxSize = 16;

    private static final int fboColorAttachmentBufferMaxSize = 64;

    private static final int infoLogBufferMaxSize = 1024;// 1 KB

    private static final int textureIdsBufferMaxSize = 256;// 1KB

    private static final int vboIdsBufferMaxSize = 256;// 1KB

    private static final int vaoIdsBufferMaxSize = 256;// 1KB

    /** buffer containing all others, sliced to occupy only a single memory page */
    private final ByteBuffer slicedByteBuffer;

    private final IntBuffer singleIntBuffer;

    private final FloatBuffer singleFloatBuffer;

    private final FloatBuffer transformBuffer;

    private final IntBuffer fboColorAttachmentBuffer;

    private final ByteBuffer infoLogBuffer;

    private final IntBuffer textureIdsBuffer;

    private final IntBuffer vboIdsBuffer;

    private final IntBuffer vaoIdsBuffer;

    public DirectNioBuffersSet() {
        slicedByteBuffer = BufferUtils.createByteBuffer(slicedByteBufferMaxSize);
        slicedByteBuffer.position(0).limit(slicedByteBuffer.position() + Buffers.SIZEOF_INT);
        singleIntBuffer = slicedByteBuffer.slice().order(ByteOrder.nativeOrder()).asIntBuffer();
        slicedByteBuffer.position(slicedByteBuffer.limit()).limit(slicedByteBuffer.position() + Buffers.SIZEOF_FLOAT);
        singleFloatBuffer = slicedByteBuffer.slice().order(ByteOrder.nativeOrder()).asFloatBuffer();
        slicedByteBuffer.position(slicedByteBuffer.limit()).limit(
                slicedByteBuffer.position() + (Buffers.SIZEOF_FLOAT * transformBufferMaxSize));
        transformBuffer = slicedByteBuffer.slice().order(ByteOrder.nativeOrder()).asFloatBuffer();
        slicedByteBuffer.position(slicedByteBuffer.limit()).limit(
                slicedByteBuffer.position() + (Buffers.SIZEOF_INT * fboColorAttachmentBufferMaxSize));
        fboColorAttachmentBuffer = slicedByteBuffer.slice().order(ByteOrder.nativeOrder()).asIntBuffer();
        slicedByteBuffer.position(slicedByteBuffer.limit()).limit(
                slicedByteBuffer.position() + (Buffers.SIZEOF_BYTE * infoLogBufferMaxSize));
        infoLogBuffer = slicedByteBuffer.slice().order(ByteOrder.nativeOrder());
        slicedByteBuffer.position(slicedByteBuffer.limit()).limit(
                slicedByteBuffer.position() + (Buffers.SIZEOF_INT * textureIdsBufferMaxSize));
        textureIdsBuffer = slicedByteBuffer.slice().order(ByteOrder.nativeOrder()).asIntBuffer();
        slicedByteBuffer.position(slicedByteBuffer.limit()).limit(
                slicedByteBuffer.position() + (Buffers.SIZEOF_INT * vboIdsBufferMaxSize));
        vboIdsBuffer = slicedByteBuffer.slice().order(ByteOrder.nativeOrder()).asIntBuffer();
        slicedByteBuffer.position(slicedByteBuffer.limit()).limit(
                slicedByteBuffer.position() + (Buffers.SIZEOF_INT * vaoIdsBufferMaxSize));
        vaoIdsBuffer = slicedByteBuffer.slice().order(ByteOrder.nativeOrder()).asIntBuffer();
        slicedByteBuffer.clear();
    }

    public IntBuffer getSingleIntBuffer() {
        return singleIntBuffer;
    }

    public FloatBuffer getSingleFloatBuffer() {
        return singleFloatBuffer;
    }

    public FloatBuffer getTransformBuffer() {
        return transformBuffer;
    }

    public IntBuffer getFboColorAttachmentBuffer() {
        return fboColorAttachmentBuffer;
    }

    public ByteBuffer getInfoLogBuffer() {
        return infoLogBuffer;
    }

    public IntBuffer getTextureIdsBuffer() {
        return textureIdsBuffer;
    }

    public IntBuffer getVboIdsBuffer() {
        return vboIdsBuffer;
    }

    public IntBuffer getVaoIdsBuffer() {
        return vaoIdsBuffer;
    }

}