/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.tool.editor.swing.widget;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector3;

public class Vector2Panel extends JPanel implements ChangeListener {

  private static final long serialVersionUID = 1L;

  private final ValuePanel _xPanel, _yPanel;
  private final ArrayList<ChangeListener> _changeListeners = new ArrayList<>();
  private boolean _setting;

  public Vector2Panel(final double min, final double max, final double step) {
    this(min, max, step, true);
  }

  public Vector2Panel(final double min, final double max, final double step, final boolean vertical) {
    super(new GridBagLayout());

    _xPanel = new ValuePanel("X: ", "", min, max, step);
    _xPanel.addChangeListener(this);

    _yPanel = new ValuePanel("Y: ", "", min, max, step);
    _yPanel.addChangeListener(this);

    if (vertical) {
      add(_xPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
          GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
      add(_yPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
          GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    } else {
      add(_xPanel, new GridBagConstraints(0, 0, 1, 1, .5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
          new Insets(5, 5, 5, 5), 0, 0));
      add(_yPanel, new GridBagConstraints(1, 0, 1, 1, .5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
          new Insets(5, 5, 5, 5), 0, 0));
    }
  }

  public void setValue(final ReadOnlyVector2 value) {
    _setting = true;
    _xPanel.setValue(value.getX());
    _yPanel.setValue(value.getY());
    _setting = false;
  }

  public void setValue(final ReadOnlyVector3 value) {
    _setting = true;
    _xPanel.setValue(value.getX());
    _yPanel.setValue(value.getY());
    _setting = false;
  }

  public Vector2 getValue() { return new Vector2(_xPanel.getDoubleValue(), _yPanel.getDoubleValue()); }

  public ReadOnlyVector3 getVector3Value() {
    return new Vector3(_xPanel.getDoubleValue(), _yPanel.getDoubleValue(), 0);
  }

  public void addChangeListener(final ChangeListener l) {
    _changeListeners.add(l);
  }

  @Override
  public void stateChanged(final ChangeEvent e) {
    if (!_setting) {
      for (final ChangeListener l : _changeListeners) {
        l.stateChanged(e);
      }
    }
  }
}
