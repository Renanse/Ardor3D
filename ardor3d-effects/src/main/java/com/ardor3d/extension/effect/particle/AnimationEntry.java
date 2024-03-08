/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.effect.particle;

import java.io.IOException;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

public class AnimationEntry implements Savable {
  protected double _offset = 0.05; // 5% of life from previous entry
  protected double _rate = 0.2; // 5 fps
  protected int[] _frames = new int[1];

  public AnimationEntry() {}

  public AnimationEntry(final double offset) {
    _offset = offset;
  }

  public int[] getFrames() { return _frames; }

  public void setFrames(final int[] frames) { _frames = frames; }

  public double getOffset() { return _offset; }

  public void setOffset(final double offset) { _offset = offset; }

  public double getRate() { return _rate; }

  public void setRate(final double rate) { _rate = rate; }

  @Override
  public Class<? extends AnimationEntry> getClassTag() { return getClass(); }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _offset = capsule.readDouble("offsetMS", 0.05);
    _rate = capsule.readDouble("rate", 0.2);
    _frames = capsule.readIntArray("frames", null);
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_offset, "offsetMS", 0.05);
    capsule.write(_rate, "rate", 0.2);
    capsule.write(_frames, "frames", null);
  }

  private static String makeText(final int[] frames) {
    if (frames == null || frames.length == 0) {
      return "";
    }

    final StringBuilder sb = new StringBuilder();
    for (final int frame : frames) {
      sb.append(frame);
      sb.append(',');
    }
    return sb.substring(0, sb.length() - 1);
  }

  @Override
  public String toString() {

    final StringBuilder builder = new StringBuilder();

    builder.append("prev+");
    builder.append((int) (_offset * 100));
    builder.append("% age...");

    builder.append("  rate: ").append(_rate);

    builder.append("  sequence: ").append(makeText(_frames));

    return builder.toString();
  }
}
