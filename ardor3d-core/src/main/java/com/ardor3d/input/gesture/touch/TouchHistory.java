/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.gesture.touch;

import com.ardor3d.math.util.MathUtils;

public class TouchHistory {

  public final String id;
  public final int initialX, initialY;
  public int prevX, prevY;
  public int currX, currY;
  public TouchStatus prevState, currState;
  public long initialTime, prevTime, currTime;

  public TouchHistory(final String id, final int initialX, final int initialY) {
    this(id, initialX, initialY, TouchStatus.Unknown);
  }

  public TouchHistory(final String id, final int initialX, final int initialY, final TouchStatus state) {
    this.id = id;
    prevX = currX = this.initialX = initialX;
    prevY = currY = this.initialY = initialY;
    prevState = currState = state;
    initialTime = System.currentTimeMillis();
  }

  public void update(final int posX, final int posY, final TouchStatus state) {
    prevX = currX;
    currX = posX;

    prevY = currY;
    currY = posY;

    prevState = currState;
    currState = state;

    prevTime = currTime;
    currTime = System.currentTimeMillis();
  }

  public double distanceTo(final TouchHistory other) {
    final double dx = other.currX - currX;
    final double dy = other.currY - currY;

    return Math.sqrt(dx * dx + dy * dy);
  }

  public double distanceFromInitial() {
    final double dx = initialX - currX;
    final double dy = initialY - currY;

    return Math.sqrt(dx * dx + dy * dy);
  }

  public double distanceFromInitialSq() {
    final double dx = initialX - currX;
    final double dy = initialY - currY;

    return dx * dx + dy * dy;
  }

  public double distanceFromPrevious() {
    final double dx = prevX - currX;
    final double dy = prevY - currY;

    return Math.sqrt(dx * dx + dy * dy);
  }

  public double angleBetween(final TouchHistory other) {
    final double dx = other.currX - currX;
    final double dy = other.currY - currY;

    double angle;
    // avoid divide by 0
    if (Math.abs(dx) < 0.001) {
      final double h = Math.sqrt(dx * dx + dy * dy);
      angle = (h < 0.001) ? 0 : Math.asin(dy / h);
    } else {
      angle = Math.atan2(dy, dx);
    }
    angle = angle < 0 ? angle + MathUtils.TWO_PI : angle;
    return angle;
  }
}
