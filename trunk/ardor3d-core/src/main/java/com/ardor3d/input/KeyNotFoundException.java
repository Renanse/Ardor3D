/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

/**
 * Thrown when an attempt at fetching a {@link Key} instance for an invalid/unknown key code is made.
 */
public class KeyNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public KeyNotFoundException(final int keyCode) {
        super("No Key enum value found for code: " + keyCode);
    }
}
