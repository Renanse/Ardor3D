/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export.binary;

public class BinaryIdContentPair {
    private int _id;
    private BinaryOutputCapsule _content;

    public BinaryIdContentPair(final int id, final BinaryOutputCapsule content) {
        _id = id;
        _content = content;
    }

    public BinaryOutputCapsule getContent() {
        return _content;
    }

    public void setContent(final BinaryOutputCapsule content) {
        _content = content;
    }

    public int getId() {
        return _id;
    }

    public void setId(final int id) {
        _id = id;
    }
}
