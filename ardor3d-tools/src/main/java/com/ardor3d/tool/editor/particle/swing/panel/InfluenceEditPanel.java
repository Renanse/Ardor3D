/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.tool.editor.particle.swing.panel;

import java.awt.Font;
import java.io.Serial;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.ardor3d.extension.effect.particle.ParticleInfluence;

public abstract class InfluenceEditPanel extends JPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  private ParticleInfluence _influence;

  public abstract void updateWidgets();

  public void setEdittedInfluence(final ParticleInfluence influence) { _influence = influence; }

  public ParticleInfluence getEdittedInfluence() { return _influence; }

  protected TitledBorder createTitledBorder(final String title) {
    final TitledBorder border = new TitledBorder(" " + title + " ");
    border.setTitleFont(new Font("Arial", Font.PLAIN, 10));
    return border;
  }

  protected JLabel createBoldLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(new Font("Arial", Font.BOLD, 13));
    return label;
  }

}
