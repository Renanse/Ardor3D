/**
 * Copyright (c) 2008-20010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer.utils.atlas;

import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.ardor3d.extension.atlas.AtlasNode;
import com.ardor3d.extension.atlas.AtlasPacker;

/**
 * Test base packer algorithm
 */
public class TestAtlasPacker {

    public static void main(final String[] args) {
        final int width = 512;
        final int height = 512;
        final AtlasPacker packer = new AtlasPacker(width, height);

        final Random rand = new Random();
        for (int i = 0; i < 2000; i++) {
            final AtlasNode node = packer.insert(rand.nextInt(100) + 10, rand.nextInt(100) + 10);
        }

        final JFrame frame = new JFrame("Pack");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final JPanel panel = new AtlasPainter(packer);
        frame.getContentPane().add(panel);
        frame.setSize(width + 100, height + 100);
        frame.setVisible(true);
    }
}
