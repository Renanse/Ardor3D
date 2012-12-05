/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.event;

import com.ardor3d.extension.ui.UIComponent;

/**
 * A listener for component selection changes.
 * 
 * @param <T>
 *            our expected component type.
 */
public interface SelectionListener<T extends UIComponent> {

    /**
     * @param component
     *            the component that changed.
     * @param newValue
     *            our new selection value.
     */
    void selectionChanged(T component, Object newValue);

}
