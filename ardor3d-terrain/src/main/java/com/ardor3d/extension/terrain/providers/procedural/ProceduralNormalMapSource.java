/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.providers.procedural;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.extension.terrain.client.TextureConfiguration;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.type.ReadOnlyColorRGBA;

public class ProceduralNormalMapSource implements TextureSource {
  private final Function3D function;
  private final double heightScale;
  private final double xGridSpacing;
  private final double zGridSpacing;

  private static final int tileSize = 128;
  private static final int availableClipmapLevels = 8;

  private final double[][] cache = new double[tileSize + 2][tileSize + 2];

  private final ReentrantLock textureLock = new ReentrantLock();
  private final ThreadLocal<ByteBuffer> tileDataPool = new ThreadLocal<>() {
    @Override
    protected ByteBuffer initialValue() {
      return BufferUtils.createByteBufferOnHeap(tileSize * tileSize * 3);
    }
  };

  public ProceduralNormalMapSource(final Function3D function, final double heightScale, final double xGridSpacing,
    final double zGridSpacing) {
    this.function = function;
    this.heightScale = heightScale;
    this.xGridSpacing = xGridSpacing;
    this.zGridSpacing = zGridSpacing;
  }

  @Override
  public TextureConfiguration getConfiguration() {
    final Map<Integer, TextureStoreFormat> textureStoreFormat = new HashMap<>();
    textureStoreFormat.put(0, TextureStoreFormat.RGB8);

    return new TextureConfiguration(availableClipmapLevels, textureStoreFormat, tileSize, 1f, false, false);
  }

  @Override
  public ByteBuffer getTile(final int clipmapLevel, final Tile tile) throws Exception {
    final ByteBuffer data = tileDataPool.get();
    final int tileX = tile.getX();
    final int tileY = tile.getY();

    final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

    final Vector3 n = new Vector3();
    final Vector3 n2 = new Vector3();
    textureLock.lock();
    try {
      // clear our cache
      for (final double[] row : cache) {
        Arrays.fill(row, Double.NEGATIVE_INFINITY);
      }

      for (int y = 0; y < tileSize; y++) {
        for (int x = 0; x < tileSize; x++) {
          if (Thread.interrupted()) {
            return null;
          }

          final int heightX = tileX * tileSize + x;
          final int heightY = tileY * tileSize + y;

          final double eval1 = getValue(x - 1, y, heightX - 1, heightY, baseClipmapLevel);
          final double eval2 = getValue(x + 1, y, heightX + 1, heightY, baseClipmapLevel);
          final double eval3 = getValue(x, y - 1, heightX, heightY - 1, baseClipmapLevel);
          final double eval4 = getValue(x, y + 1, heightX, heightY + 1, baseClipmapLevel);

          double dXh = eval1 - eval2;
          if (dXh != 0) {
            // alter by our height scale
            dXh *= heightScale;
            // determine slope of perpendicular line
            final double slopeX = 2.0 * xGridSpacing / dXh;
            // now plug into cos(arctan(x)) to get unit length vector
            n.setX(Math.copySign(1.0 / Math.sqrt(1 + slopeX * slopeX), dXh));
            n.setY(0);
            n.setZ(Math.abs(slopeX * n.getX()));
          } else {
            n.set(0, 0, 1);
          }

          double dZh = eval3 - eval4;
          if (dZh != 0) {
            // alter by our height scale
            dZh *= heightScale;
            // determine slope of perpendicular line
            final double slopeZ = 2.0 * zGridSpacing / dZh;
            // now plug into cos(arctan(x)) to get unit length vector
            n2.setX(0);
            n2.setY(Math.copySign(1.0 / Math.sqrt(1 + slopeZ * slopeZ), dZh));
            n2.setZ(Math.abs(slopeZ * n2.getY()));
          } else {
            n2.set(0, 0, 1);
          }

          // add together the vectors across X and Z and normalize to get final normal
          n.addLocal(n2).normalizeLocal();

          // place data in buffer, scaled to roughly fit [-1, 1] in [0, 255]
          final int index = (x + y * tileSize) * 3;
          data.put(index + 0, (byte) ((int) (127 * n.getX()) + 128));
          data.put(index + 1, (byte) ((int) (127 * n.getY()) + 128));
          data.put(index + 2, (byte) ((int) (127 * n.getZ()) + 128));
        }
      }
    } finally {
      textureLock.unlock();
    }
    return data;
  }

  private double getValue(final int x, final int y, final int heightX, final int heightY, final int baseClipmapLevel) {
    double val = cache[x + 1][y + 1];
    if (val == Double.NEGATIVE_INFINITY) {
      val = cache[x + 1][y + 1] = function.eval(heightX << baseClipmapLevel, heightY << baseClipmapLevel, 0);
    }

    return val;
  }

  protected String name;

  @Override
  public String getName() { return name; }

  @Override
  public void setName(final String value) { name = value; }

  protected ColorRGBA tint = new ColorRGBA(ColorRGBA.WHITE);

  @Override
  public ReadOnlyColorRGBA getTintColor() { return tint; }

  @Override
  public void setTintColor(final ReadOnlyColorRGBA value) {
    tint.set(value);
  }
}
