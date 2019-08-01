/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of a ComboBox model.
 */
public class DefaultComboBoxModel implements ComboBoxModel {

    protected List<ModelElement> _elements = new ArrayList<>();

    public DefaultComboBoxModel() {}

    public DefaultComboBoxModel(final Object... data) {
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                addItem(data[i]);
            }
        }
    }

    @Override
    public int addItem(final Object value) {
        final ModelElement element = new ModelElement();
        element._value = value;
        _elements.add(element);
        return _elements.size() - 1;
    }

    @Override
    public void addItem(final int index, final Object value) {
        final ModelElement element = new ModelElement();
        element._value = value;
        while (_elements.size() <= index) {
            _elements.add(new ModelElement());
        }
        _elements.set(index, element);
    }

    @Override
    public void clear() {
        _elements.clear();
    }

    @Override
    public String getToolTipAt(final int index) {
        return _elements.get(index)._toolTip;
    }

    @Override
    public Object getValueAt(final int index) {
        return _elements.get(index)._value;
    }

    @Override
    public String getViewAt(final int index) {
        final ModelElement elem = _elements.get(index);
        return elem._view != null ? elem._view : elem._value != null ? elem._value.toString() : null;
    }

    @Override
    public void setToolTipAt(final int index, final String toolTip) {
        final ModelElement elem = _elements.get(index);
        elem._toolTip = toolTip;
    }

    @Override
    public void setValueAt(final int index, final Object value) {
        final ModelElement elem = _elements.get(index);
        elem._value = value;
    }

    @Override
    public void setViewAt(final int index, final String view) {
        final ModelElement elem = _elements.get(index);
        elem._view = view;
    }

    @Override
    public int size() {
        return _elements.size();
    }

    protected class ModelElement {
        Object _value = null;

        // these are optional
        String _view = null;
        String _toolTip = null;
    }
}
