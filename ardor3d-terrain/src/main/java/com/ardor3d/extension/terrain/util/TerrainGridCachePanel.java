/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.ardor3d.extension.terrain.client.TerrainCache;
import com.ardor3d.extension.terrain.client.TerrainGridCache;
import com.ardor3d.extension.terrain.client.TerrainGridCache.TileLoadingData;
import com.ardor3d.extension.terrain.client.TerrainGridCache.TileLoadingData.State;
import com.ardor3d.math.MathUtils;

public class TerrainGridCachePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final List<TerrainCache> cacheList;
    private final int cacheSize;

    private final int size = 4;

    public TerrainGridCachePanel(final List<TerrainCache> cacheList, final int cacheSize) {
        this.cacheList = cacheList;
        this.cacheSize = cacheSize;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            repaint();
                        }
                    });
                    try {
                        Thread.sleep(100);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "repaintalot").start();

        setSize(cacheList.size() * (size * cacheSize + 5) + 60, 100);
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        final Graphics2D g2 = (Graphics2D) g;

        for (int i = 0; i < cacheList.size(); i++) {
            final TerrainGridCache cache = (TerrainGridCache) cacheList.get(i);
            for (final TileLoadingData data : cache.getDebugTiles()) {
                if (data.state == State.init) {
                    g2.setColor(Color.lightGray);
                } else if (data.state == State.loading) {
                    g2.setColor(Color.blue);
                } else if (data.state == State.finished) {
                    g2.setColor(Color.green);
                } else {
                    g2.setColor(Color.white);
                }
                final int x = MathUtils.moduloPositive(data.sourceTile.getX(), cacheSize);
                final int y = MathUtils.moduloPositive(data.sourceTile.getY(), cacheSize);
                final int xPos = x * size + 20 + (cacheList.size() - i - 1) * (size * cacheSize + 5);
                final int yPos = y * size + 20;
                g2.fillRect(xPos, yPos, size, size);
                g2.setColor(Color.darkGray);
                g2.drawRect(xPos, yPos, size, size);
            }
            g2
                    .drawString("" + (cacheList.size() - i - 1), (cacheList.size() - i - 1) * (size * cacheSize + 5)
                            + 25, 15);
        }
    }
}
