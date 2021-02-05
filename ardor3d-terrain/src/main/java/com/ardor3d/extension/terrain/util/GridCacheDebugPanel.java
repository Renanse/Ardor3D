/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.ardor3d.extension.terrain.client.AbstractGridCache;
import com.ardor3d.extension.terrain.client.AbstractGridCache.TileLoadingData;
import com.ardor3d.math.util.MathUtils;

public class GridCacheDebugPanel extends JPanel implements Runnable {
  private static final long serialVersionUID = 1L;

  protected final List<AbstractGridCache> cacheList = new ArrayList<>();
  protected final int cacheSize;

  protected final int size = 4;

  protected boolean _run;

  public GridCacheDebugPanel(final Stream<AbstractGridCache> cacheStream, final int cacheSize) {
    cacheStream.forEach(cacheList::add);
    this.cacheSize = cacheSize;
    setSize(cacheList.size() * (size * cacheSize + 5) + 60, 100);
  }

  @Override
  public void run() {
    _run = true;
    while (_run) {
      SwingUtilities.invokeLater(() -> repaint());
      try {
        Thread.sleep(250);
      } catch (final InterruptedException e) {}
    }
  }

  public void stop() {
    _run = false;
  }

  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);

    final Graphics2D g2 = (Graphics2D) g;

    for (int i = 0; i < cacheList.size(); i++) {
      final var cache = cacheList.get(i);
      for (final TileLoadingData data : cache.getDebugTiles()) {
        switch (data.state) {
          case cancelled:
            g2.setColor(Color.orange);
            break;
          case error:
            g2.setColor(Color.red);
            break;
          case finished:
            g2.setColor(Color.green);
            break;
          case loading:
            g2.setColor(Color.blue);
            break;
          case init:
          default:
            g2.setColor(Color.lightGray);
            break;
        }
        final int x = MathUtils.moduloPositive(data.sourceTile.getX(), cacheSize);
        final int y = MathUtils.moduloPositive(data.sourceTile.getY(), cacheSize);
        final int xPos = x * size + 20 + (cacheList.size() - i - 1) * (size * cacheSize + 5);
        final int yPos = y * size + 20;
        g2.fillRect(xPos, yPos, size, size);
        g2.setColor(Color.darkGray);
        g2.drawRect(xPos, yPos, size, size);
      }
      g2.drawString("" + (cacheList.size() - i - 1), (cacheList.size() - i - 1) * (size * cacheSize + 5) + 25, 15);
    }
  }
}
