/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.tool.editor.particle.swing.panel;

import java.awt.BorderLayout;

import com.ardor3d.extension.effect.particle.FloorInfluence;
import com.ardor3d.tool.editor.swing.widget.ValuePanel;
import com.ardor3d.tool.editor.swing.widget.Vector3Panel;

public class FloorInfluencePanel extends InfluenceEditPanel {

  private static final long serialVersionUID = 1L;

  private final ValuePanel _constantValue = new ValuePanel("Constant: ", "", -Double.MAX_VALUE, Double.MAX_VALUE, 0.1);

  private final Vector3Panel _normalVector = new Vector3Panel(-Double.MAX_VALUE, Double.MAX_VALUE, 0.1);

  private final ValuePanel _bouncinessValue = new ValuePanel("Bounciness: ", "", 0, Double.MAX_VALUE, 0.01);

  public FloorInfluencePanel() {
    super();
    setLayout(new BorderLayout());

    _constantValue.setBorder(createTitledBorder(" PLANE CONSTANT "));
    _constantValue
        .addChangeListener(e -> ((FloorInfluence) getEdittedInfluence()).setConstant(_constantValue.getDoubleValue()));
    add(_constantValue, BorderLayout.NORTH);

    _normalVector.setBorder(createTitledBorder(" PLANE NORMAL "));
    _normalVector.addChangeListener(e -> ((FloorInfluence) getEdittedInfluence()).setNormal(_normalVector.getValue()));
    add(_normalVector, BorderLayout.CENTER);

    _bouncinessValue.addChangeListener(
        e -> ((FloorInfluence) getEdittedInfluence()).setBounciness(_bouncinessValue.getDoubleValue()));
    add(_bouncinessValue, BorderLayout.SOUTH);
  }

  @Override
  public void updateWidgets() {
    _constantValue.setValue(((FloorInfluence) getEdittedInfluence()).getConstant());
    _normalVector.setValue(((FloorInfluence) getEdittedInfluence()).getNormal());
    _bouncinessValue.setValue(((FloorInfluence) getEdittedInfluence()).getBounciness());
  }
}
