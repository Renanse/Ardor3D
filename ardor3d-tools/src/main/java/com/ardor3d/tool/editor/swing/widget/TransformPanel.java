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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.util.MathUtils;

public class TransformPanel extends JPanel {

  @Serial
  private static final long serialVersionUID = 1L;

  public static Font LABEL_FONT = new Font("Arial", Font.BOLD, 13);

  public Vector3Panel _translation = new Vector3Panel(Double.NEGATIVE_INFINITY, Double.MAX_VALUE, 1.0, false);
  public Vector3Panel _rotation = new Vector3Panel(-180.0, 180.0, 1.0, false);
  public Vector3Panel _scale = new Vector3Panel(Double.MIN_NORMAL, Double.MAX_VALUE, .1, false);

  public TransformPanel() {
    super(new GridBagLayout());
    add(createLabel("Translation"), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    add(_translation, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    add(createLabel("Scale"), new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    add(_scale, new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    add(createLabel("Rotation"), new GridBagConstraints(0, 4, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    add(_rotation, new GridBagConstraints(0, 5, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    _translation.setEnabled(enabled);
    _rotation.setEnabled(enabled);
    _scale.setEnabled(enabled);
  }

  public void setValue(final ReadOnlyTransform value) {
    _translation.setValue(value.getTranslation());
    final double[] angles = value.getMatrix().toAngles(null);
    _rotation.setValue(new Vector3(angles[0], angles[1], angles[2]).multiplyLocal(MathUtils.RAD_TO_DEG));
    _scale.setValue(value.getScale());
  }

  public ReadOnlyTransform getValue() {
    final Transform t = new Transform();
    t.setTranslation(_translation.getValue());
    final Vector3 val = _rotation.getValue().multiplyLocal(MathUtils.DEG_TO_RAD);
    final Matrix3 mat = Matrix3.fetchTempInstance().fromAngles(val.getX(), val.getY(), val.getZ());
    t.setRotation(mat);
    Matrix3.releaseTempInstance(mat);
    t.setScale(_scale.getValue());
    return t;
  }

  public void addChangeListener(final ChangeListener l) {
    _translation.addChangeListener(l);
    _rotation.addChangeListener(l);
    _scale.addChangeListener(l);
  }

  private JLabel createLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(LABEL_FONT);
    return label;
  }

}
