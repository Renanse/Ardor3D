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
import java.io.Serial;

import com.ardor3d.extension.effect.particle.SimpleParticleInfluenceFactory;
import com.ardor3d.tool.editor.swing.widget.Vector3Panel;

public class GravityInfluencePanel extends InfluenceEditPanel {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Vector3Panel _inflVector = new Vector3Panel(-Double.MAX_VALUE, Double.MAX_VALUE, 0.1);

  public GravityInfluencePanel() {
    super();
    setLayout(new BorderLayout());

    _inflVector.setBorder(createTitledBorder(" GRAVITY INFLUENCE "));
    _inflVector.addChangeListener(e -> ((SimpleParticleInfluenceFactory.BasicGravity) getEdittedInfluence())
        .setGravityForce(_inflVector.getValue()));
    add(_inflVector, BorderLayout.CENTER);
  }

  @Override
  public void updateWidgets() {
    _inflVector.setValue(((SimpleParticleInfluenceFactory.BasicGravity) getEdittedInfluence()).getGravityForce());
  }
}
