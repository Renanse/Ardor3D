/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.awt;

import java.util.List;

import com.ardor3d.math.type.ReadOnlyVector4;
import com.google.common.collect.Lists;

public class AwtElementProvider implements ElementUpdateListener {
    private final List<AbstractAwtElement> _elements = Lists.newLinkedList();
    private final List<ElementUpdateListener> _updateListeners = Lists.newLinkedList();

    public List<AbstractAwtElement> getElements() {
        return _elements;
    }

    public void addElement(final AbstractAwtElement element) {
        _elements.add(element);
        element.setUpdateListener(this);
        elementUpdated(element.getBounds(), element.getBounds());
    }

    public void clear() {
        for (final AbstractAwtElement element : _elements) {
            element.setUpdateListener(null);
        }
        _elements.clear();
    }

    public void removeElement(final AbstractAwtElement element) {
        element.setUpdateListener(null);
        _elements.remove(element);
    }

    public void addElementUpdateListener(final ElementUpdateListener listener) {
        _updateListeners.add(listener);
    }

    public void removeElementUpdateListener(final ElementUpdateListener listener) {
        _updateListeners.remove(listener);
    }

    public void cleanElementUpdateListeners() {
        _updateListeners.clear();
    }

    @Override
    public void elementUpdated(final ReadOnlyVector4 oldBounds, final ReadOnlyVector4 newBounds) {
        for (final ElementUpdateListener listener : _updateListeners) {
            listener.elementUpdated(oldBounds, newBounds);
        }
    }
}
