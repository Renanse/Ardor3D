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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;

import com.ardor3d.extension.effect.particle.WanderInfluence;
import com.ardor3d.tool.editor.swing.widget.ValuePanel;

public class WanderInfluencePanel extends InfluenceEditPanel {

  @Serial
  private static final long serialVersionUID = 1L;

  private final ValuePanel _wanderRadius = new ValuePanel("Wander Circle Radius: ", "", 0, Double.MAX_VALUE, 0.01);
  private final ValuePanel _wanderDistance =
      new ValuePanel("Wander Circle Distance: ", "", -Double.MIN_VALUE, Double.MAX_VALUE, 0.1);
  private final ValuePanel _wanderJitter =
      new ValuePanel("Jitter Amount: ", "", -Double.MIN_VALUE, Double.MAX_VALUE, 0.001);

  public WanderInfluencePanel() {
    super();
    setLayout(new GridBagLayout());

    _wanderRadius.addChangeListener(
        e -> ((WanderInfluence) getEdittedInfluence()).setWanderRadius(_wanderRadius.getDoubleValue()));
    _wanderDistance.addChangeListener(
        e -> ((WanderInfluence) getEdittedInfluence()).setWanderDistance(_wanderDistance.getDoubleValue()));
    _wanderJitter.addChangeListener(
        e -> ((WanderInfluence) getEdittedInfluence()).setWanderJitter(_wanderJitter.getDoubleValue()));

    setBorder(createTitledBorder(" WANDER PARAMETERS "));
    add(_wanderRadius, new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
    add(_wanderDistance, new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
    add(_wanderJitter, new GridBagConstraints(0, 2, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
  }

  @Override
  public void updateWidgets() {
    final WanderInfluence wander = (WanderInfluence) getEdittedInfluence();
    _wanderRadius.setValue(wander.getWanderRadius());
    _wanderDistance.setValue(wander.getWanderDistance());
    _wanderJitter.setValue(wander.getWanderJitter());
  }
}
