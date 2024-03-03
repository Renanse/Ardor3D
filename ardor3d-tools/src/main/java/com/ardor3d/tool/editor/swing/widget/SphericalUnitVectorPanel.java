/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
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
import java.io.Serial;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;

public class SphericalUnitVectorPanel extends JPanel implements ChangeListener {

  @Serial
  private static final long serialVersionUID = 1L;

  private final ValuePanel _azimuthPanel = new ValuePanel("Azimuth: ", "", -180.0, +180.0, 1.0);
  private final ValuePanel _elevationPanel = new ValuePanel("Elevation: ", "", -90.0, +90.0, 1.0);
  private final ArrayList<ChangeListener> _changeListeners = new ArrayList<>();
  private final Vector3 _vector = new Vector3();

  private boolean _setting;

  public SphericalUnitVectorPanel() {
    super(new GridBagLayout());
    _azimuthPanel.addChangeListener(this);
    _elevationPanel.addChangeListener(this);

    add(_azimuthPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    add(_elevationPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
  }

  public void setValue(final ReadOnlyVector3 value) {
    MathUtils.cartesianToSpherical(value, _vector);
    _setting = true;
    _azimuthPanel.setValue(_vector.getY() * MathUtils.RAD_TO_DEG);
    _elevationPanel.setValue(_vector.getZ() * MathUtils.RAD_TO_DEG);
    _setting = false;
  }

  public Vector3 getValue() {
    _vector.set(1f, _azimuthPanel.getDoubleValue() * MathUtils.DEG_TO_RAD,
        _elevationPanel.getDoubleValue() * MathUtils.DEG_TO_RAD);
    final Vector3 result = new Vector3();
    MathUtils.sphericalToCartesian(_vector, result);
    return result;
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
