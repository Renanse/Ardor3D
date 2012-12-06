/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

/**
 * Interface for items that can take and return a text string value.
 */
public interface Textable {

    public void setText(String value);

    public String getText();

    public boolean isStyledText();

    public void setStyledText(boolean value);

}
