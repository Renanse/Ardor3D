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

import java.util.List;

import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.event.SelectionListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.model.ComboBoxModel;
import com.ardor3d.extension.ui.model.DefaultComboBoxModel;
import com.ardor3d.extension.ui.skin.SkinningTask;
import com.ardor3d.extension.ui.text.StyleConstants;
import com.ardor3d.input.InputState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.scenegraph.Spatial;
import com.google.common.collect.Lists;

/**
 * A UI component that contains several possible choices, but shows only the currently selected one. Changing the
 * selection is allowed via a popup menu shown when the component is clicked on.
 */
public class UIComboBox extends UIPanel {

    protected ComboBoxModel _model;

    protected UILabel _valueLabel;
    protected UIButton _openButton;
    protected UIPopupMenu _valuesMenu;

    protected int _selectedIndex = 0;

    private final List<SelectionListener<UIComboBox>> _listeners = Lists.newArrayList();

    private SkinningTask _itemSkinCallback;

    public UIComboBox() {
        this(new DefaultComboBoxModel());
    }

    public UIComboBox(final ComboBoxModel model) {
        super(new BorderLayout());
        if (model == null) {
            throw new IllegalArgumentException("model can not be null.");
        }
        _model = model;

        _valueLabel = new UILabel(_model.size() > 0 ? _model.getViewAt(0) : "") {
            @Override
            public boolean mouseReleased(final MouseButton button, final InputState state) {
                _openButton.doClick();
                return true;
            }
        };
        _valueLabel.setLayoutData(BorderLayoutData.CENTER);
        add(_valueLabel);

        _openButton = new UIButton("\\/");
        _openButton.setLayoutData(BorderLayoutData.EAST);
        add(_openButton);

        _valuesMenu = new UIPopupMenu() {
            @Override
            public UIComponent getUIComponent(final int hudX, final int hudY) {
                final UIComponent component = super.getUIComponent(hudX, hudY);
                if (component != null) {
                    if (component instanceof UIButton) {
                        final UIButton over = (UIButton) component;
                        final UIState state = over.getCurrentState();
                        if (state == over.getDefaultState() || state == over.getSelectedState()) {
                            clearOver();
                        }
                    }
                }
                return component;
            }
        };

        _openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                if (!isEnabled()) {
                    return;
                }

                if (_valuesMenu.isAttachedToHUD()) {
                    _valuesMenu.getHud().closePopupMenus();
                }

                _valuesMenu.clearItems();
                for (int i = 0; i < _model.size(); i++) {
                    final UIMenuItem item = new UIMenuItem("" + _model.getValueAt(i));
                    if (_itemSkinCallback != null) {
                        _itemSkinCallback.skinComponent(item);
                    }
                    if (i == getSelectedIndex()) {
                        item.addFontStyle(StyleConstants.KEY_BOLD, Boolean.TRUE);
                    }
                    final int index = i;
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(final ActionEvent event) {
                            setSelectedIndex(index);
                        }
                    });

                    // Apply model for name and toolTipText
                    item.setName(_model.getViewAt(i));
                    item.setTooltipText(_model.getToolTipAt(i));
                    _valuesMenu.addItem(item);
                }

                _valuesMenu.updateMinimumSizeFromContents();
                if (_valuesMenu.getLocalComponentWidth() < UIComboBox.this.getLocalComponentWidth()) {
                    _valuesMenu.setLocalComponentWidth(UIComboBox.this.getLocalComponentWidth());
                }
                _valuesMenu.layout();
                getHud().closePopupMenus();
                getHud().showSubPopupMenu(_valuesMenu);
                _valuesMenu.showAt(_valueLabel.getHudX(), _valueLabel.getHudY());
            }
        });

        applySkin();
    }

    protected void clearOver() {
        _openButton.switchState(_openButton.getDefaultState());
        for (int i = _valuesMenu.getNumberOfChildren(); --i >= 0;) {
            final Spatial item = _valuesMenu.getChild(i);
            if (item instanceof UIButton) {
                final UIButton over = (UIButton) item;
                over.switchState(over.getDefaultState());
            }
        }
    }

    public int getSelectedIndex() {
        return _selectedIndex;
    }

    public Object getSelectedValue() {
        return _model.getValueAt(getSelectedIndex());
    }

    public void setSelectedIndex(final int index) {
        setSelectedIndex(index, true);
    }

    public void setSelectedIndex(final int index, final boolean fireEvent) {
        if (index != _selectedIndex) {
            _selectedIndex = index;
            if (fireEvent) {
                fireSelectionEvent();
            }
            fireComponentDirty();
        }

        if (index == -1 || _model == null || _model.size() == 0) {
            _valueLabel.setText("");
        } else {
            _valueLabel.setText(_model.getViewAt(index));
        }
    }

    protected void fireSelectionEvent() {
        if (!isEnabled()) {
            return;
        }
        for (final SelectionListener<UIComboBox> listener : _listeners) {
            listener.selectionChanged(this, getSelectedValue());
        }
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        _valueLabel.setEnabled(enabled);
        _openButton.setEnabled(enabled);
    }

    @Override
    public String getTooltipText() {
        if (_tooltipText == null) {
            final String rVal = _model.getToolTipAt(_selectedIndex);
            if (rVal != null) {
                return rVal;
            }
        }
        return super.getTooltipText();
    }

    public UILabel getValueLabel() {
        return _valueLabel;
    }

    public ComboBoxModel getModel() {
        return _model;
    }

    public UIButton getOpenButton() {
        return _openButton;
    }

    public UIPopupMenu getValuesMenu() {
        return _valuesMenu;
    }

    public void addSelectionListener(final SelectionListener<UIComboBox> listener) {
        _listeners.add(listener);
    }

    public void removeSelectionListener(final SelectionListener<UIComboBox> listener) {
        _listeners.remove(listener);
    }

    public void clearSelectionListeners() {
        _listeners.clear();
    }

    public void setItemSkinCallback(final SkinningTask callback) {
        _itemSkinCallback = callback;
    }

    public SkinningTask getItemSkinCallback() {
        return _itemSkinCallback;
    }
}
