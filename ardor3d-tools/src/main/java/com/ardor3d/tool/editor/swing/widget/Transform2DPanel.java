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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyTransform;

public class Transform2DPanel extends JPanel {

  private static final long serialVersionUID = 1L;

  public static Font LABEL_FONT = new Font("Arial", Font.BOLD, 13);

  public ValuePanel _translationX = new ValuePanel("Offset X: ", "", Double.NEGATIVE_INFINITY, Double.MAX_VALUE, 1.0);
  public ValuePanel _translationY = new ValuePanel("Offset Y: ", "", Double.NEGATIVE_INFINITY, Double.MAX_VALUE, 1.0);
  public ValuePanel _scale = new ValuePanel("Scale: ", "", Double.MIN_VALUE, Double.MAX_VALUE, .1);
  public ValuePanel _rotation = new ValuePanel("Angle: ", "", -180.0, 180.0, 1.0);

  public Transform2DPanel() {
    super(new GridBagLayout());
    add(_translationX, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    add(_translationY, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    add(_scale, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));
    add(_rotation, new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    _translationX.setEnabled(enabled);
    _translationY.setEnabled(enabled);
    _rotation.setEnabled(enabled);
    _scale.setEnabled(enabled);
  }

  public void setValue(final ReadOnlyTransform value) {
    _translationX.setValue(value.getTranslation().getX());
    _translationY.setValue(value.getTranslation().getY());
    final double[] angles = value.getMatrix().toAngles(null);
    _rotation.setValue(angles[2] * MathUtils.RAD_TO_DEG);
    _scale.setValue(value.getScale().getX());
  }

  public ReadOnlyTransform getValue() {
    final Transform t = new Transform();
    t.setTranslation(_translationX.getDoubleValue(), _translationY.getDoubleValue(), 0);
    final double val = _rotation.getDoubleValue() * MathUtils.DEG_TO_RAD;
    final Matrix3 mat = Matrix3.fetchTempInstance().fromAngles(0, 0, val);
    t.setRotation(mat);
    Matrix3.releaseTempInstance(mat);
    t.setScale(_scale.getDoubleValue());
    return t;
  }

  public void addChangeListener(final ChangeListener l) {
    _translationX.addChangeListener(l);
    _translationY.addChangeListener(l);
    _rotation.addChangeListener(l);
    _scale.addChangeListener(l);
  }

}
