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

import com.ardor3d.extension.effect.particle.SimpleParticleInfluenceFactory;
import com.ardor3d.tool.editor.swing.widget.ValuePanel;

public class DragInfluencePanel extends InfluenceEditPanel {

  private static final long serialVersionUID = 1L;

  private final ValuePanel _dragCoefficientPanel = new ValuePanel("Drag Coefficient: ", "", 0.0, Double.MAX_VALUE, 0.1);

  public DragInfluencePanel() {
    super();
    setLayout(new BorderLayout());

    _dragCoefficientPanel.setBorder(createTitledBorder(" DRAG PARAMETERS "));
    _dragCoefficientPanel.addChangeListener(e -> ((SimpleParticleInfluenceFactory.BasicDrag) getEdittedInfluence())
        .setDragCoefficient(_dragCoefficientPanel.getFloatValue()));
    add(_dragCoefficientPanel, BorderLayout.CENTER);
  }

  @Override
  public void updateWidgets() {
    _dragCoefficientPanel
        .setValue(((SimpleParticleInfluenceFactory.BasicDrag) getEdittedInfluence()).getDragCoefficient());
  }

}
