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
import java.io.InputStream;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class InputStreamResourceSource implements ResourceSource {
    private InputStream _is;
    private String _type;

    /**
     * Construct a new InputStreamResourceSource. Must set stream separately.
     */
    public InputStreamResourceSource() {}

    /**
     * Construct a new InputStreamResourceSource from a specific stream and type.
     *
     * @param stream
     *            The stream to load the resource from. Must not be null.
     * @param type
     *            our type. Usually a file extension such as .png. Required for generic loading when multiple resource
     *            handlers could be used.
     */
    public InputStreamResourceSource(final InputStream stream, final String type) {
        assert (stream != null) : "stream must not be null";
        setStream(stream);

        _is = stream;
    }

    public ResourceSource getRelativeSource(final String name) {
        throw new UnsupportedOperationException("Relative resources are not supported by this ResourceSource.");
    }

    public void setStream(final InputStream stream) {
        _is = stream;
    }

    public InputStream getStream() {
        return _is;
    }

    public String getName() {
        return "InputStream";
    }

    public String getType() {
        return _type;
    }

    public void setType(final String type) {
        _type = type;
    }

    public InputStream openStream() throws IOException {
        return _is;
    }

    /**
     * @return the string representation of this InputStreamResourceSource.
     */
    @Override
    public String toString() {
        return "InputStreamResourceSource [type=" + _type + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_type == null) ? 0 : _type.hashCode());
        result = prime * result + ((_is == null) ? 0 : _is.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof InputStreamResourceSource)) {
            return false;
        }
        final InputStreamResourceSource other = (InputStreamResourceSource) obj;
        if (_type == null) {
            if (other._type != null) {
                return false;
            }
        } else if (!_type.equals(other._type)) {
            return false;
        }
        if (_is == null) {
            if (other._is != null) {
                return false;
            }
        } else if (!_is.equals(other._is)) {
            return false;
        }
        return true;
    }

    public Class<?> getClassTag() {
        return InputStreamResourceSource.class;
    }

    public void read(final InputCapsule capsule) throws IOException {
        _type = capsule.readString("type", null);
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_type, "type", null);
    }
}
