/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

public class Ardor3dException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public Ardor3dException() {
        super();
    }

    public Ardor3dException(final String desc) {
        super(desc);
    }

    public Ardor3dException(final Throwable cause) {
        super(cause);
    }

    public Ardor3dException(final String desc, final Throwable cause) {
        super(desc, cause);
    }
}
