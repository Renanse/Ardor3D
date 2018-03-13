/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.tool.editor.particle.swing;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.ardor3d.scene.state.lwjgl.util.SharedLibraryLoader;

public class ParticleEditorMain {
    private static final Logger logger = Logger.getLogger(ParticleEditorMain.class.getName());

    public static void main(final String[] args) {
        try {
            SharedLibraryLoader.load(true);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (final Exception e) {
                    logger.logp(Level.SEVERE, ParticleEditorFrame.class.toString(), "main(args)", "Exception", e);
                }
                new ParticleEditorFrame();
            }
        });
    }

}
