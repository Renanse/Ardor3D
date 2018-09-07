/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.hint;

public enum PropertyMode {

    /** Inherit mode from parent. */
    Inherit,

    /** If we do not have a given property, always return null. */
    UseOwn,

    /** If we do not have a given property, check our parent. */
    UseParentIfNull,

    /** Only use our property if we ask our parent first and it gives us null. */
    UseOursLast,

}
