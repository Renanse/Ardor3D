/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.tool.editor.particle.swing;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ParticleEditorMain {
  private static final Logger logger = Logger.getLogger(ParticleEditorMain.class.getName());

  public static void main(final String[] args) {
    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (final Exception e) {
        logger.logp(Level.SEVERE, ParticleEditorFrame.class.toString(), "main(args)", "Exception", e);
      }
      new ParticleEditorFrame();
    });
  }

}
