/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.layout;

/**
 * A data class that represents which slot a component should fall into when used with a BorderLayout.
 */
public enum BorderLayoutData implements UILayoutData {
    NORTH, WEST, SOUTH, CENTER, EAST;
}