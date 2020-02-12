/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.resource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import com.ardor3d.util.geom.BufferUtils;

public final class ResourceUtils {

    public static ByteBuffer loadResourceAsByteBuffer(final ResourceSource rsrc, final int initialSize) {
        ByteBuffer buffer;
        try (final ReadableByteChannel channel = Channels.newChannel(rsrc.openStream())) {
            buffer = BufferUtils.createByteBuffer(initialSize);

            while (true) {
                final int bytes = channel.read(buffer);
                if (bytes == -1) {
                    break;
                }
                if (buffer.remaining() == 0) {
                    final int newSize = buffer.capacity() * 2;
                    final ByteBuffer newBuffer = BufferUtils.createByteBuffer(newSize);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            }
            buffer.flip();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return buffer;
    }

}
