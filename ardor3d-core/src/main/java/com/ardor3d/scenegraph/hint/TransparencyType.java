/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.hint;

public enum TransparencyType {

    /**
     * Do whatever our parent does. If no parent, we'll default to OnePass.
     */
    Inherit,

    /**
     * Single pass. Best for most circumstances.
     */
    OnePass,

    /**
     * Two passes, one with CullState enforced to Front and another with it enforced to Back. The back face pass will
     * not write to depth buffer. The front face will use the ZBufferState from the scene or enforced on the context.
     */
    TwoPass;

}
