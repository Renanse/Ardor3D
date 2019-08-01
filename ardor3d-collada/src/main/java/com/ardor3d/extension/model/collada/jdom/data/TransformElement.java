/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import java.util.Arrays;

public class TransformElement {
    private final double[] _array;
    private final TransformElementType _type;

    public enum TransformElementType {
        Translation, Rotation, Scale, Matrix, Lookat
    }

    public TransformElement(final double[] array, final TransformElementType type) {
        super();
        _array = array;
        _type = type;
    }

    @Override
    public String toString() {
        return "TransformElement [array=" + Arrays.toString(_array) + ", type=" + _type + "]";
    }

    public double[] getArray() {
        return _array;
    }

    public TransformElementType getType() {
        return _type;
    }
}
