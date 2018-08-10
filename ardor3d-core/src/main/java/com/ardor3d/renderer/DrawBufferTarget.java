/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

public enum DrawBufferTarget {

    /**
     * No color buffers
     */
    None,

    /**
     * The front left color buffer
     */
    FrontLeft,

    /**
     * The front right color buffer
     */
    FrontRight,

    /**
     * The back left color buffer
     */
    BackLeft,

    /**
     * The back right color buffer
     */
    BackRight,

    /**
     * The front left and front right (if exists) color buffers.
     */
    Front,

    /**
     * The back left and front right (if exists) color buffers.
     */
    Back,

    /**
     * The front left and back left (if exists) color buffers.
     */
    Left,

    /**
     * The front right and back right (if exists) color buffers.
     */
    Right,

    /**
     * All of FrontLeft, FrontRight, BackLeft, BackRight, if exists.
     */
    FrontAndBack,
}
