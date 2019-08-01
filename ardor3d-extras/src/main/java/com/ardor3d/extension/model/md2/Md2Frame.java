/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.md2;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

final class Md2Frame {

    /** frame name */
    String name; // char [16]

    /** scale factor */
    final Vector3 scale = new Vector3(1, 1, 1);

    /** translation vector */
    final Vector3 translate = new Vector3();

    /** vertex data */
    byte[] vertData;

    Md2Frame(final byte[] vertData, final String name, final ReadOnlyVector3 scale, final ReadOnlyVector3 translate) {
        this.vertData = vertData;
        this.scale.set(scale);
        this.translate.set(translate);
        this.name = name;
    }
}
