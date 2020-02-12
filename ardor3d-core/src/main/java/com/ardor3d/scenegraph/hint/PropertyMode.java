/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.hint;

public enum PropertyMode {

    /** Inherit mode from parent. */
    Inherit,

    /** If we do not have a given property, always return null. */
    UseOwn,

    /** If we do not have a given property, check our parent. */
    UseParentIfUnset,

    /** Only use our property if we ask our parent first and it gives us null. */
    UseOursLast,

}
