/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.controller;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;

import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>ComplexSpatialController</code> provides a base class for creation of controllers to modify
 * nodes and render states over time. The base controller provides a repeat type, min and max time,
 * as well as speed. Subclasses of this will provide the update method that takes the time between
 * the last call and the current one and modifies an object in a application specific way.
 */
public abstract class ComplexSpatialController<T extends Spatial>
    implements SpatialController<T>, Serializable, Savable {

  @Serial
  private static final long serialVersionUID = 1;

  public enum RepeatType {
    /**
     * A clamped repeat type signals that the controller should look like its final state when it's done
     * <br>
     * Example: 0 1 5 8 9 10 10 10 10 10 10 10 10 10 10 10...
     */
    CLAMP,

    /**
     * A wrapped repeat type signals that the controller should start back at the begining when it's
     * final state is reached <br>
     * Example: 0 1 5 8 9 10 0 1 5 8 9 10 0 1 5 ....
     */
    WRAP,

    /**
     * A cycled repeat type signals that the controller should cycle it's states forwards and backwards
     * <br>
     * Example: 0 1 5 8 9 10 9 8 5 1 0 1 5 8 9 10 9 ....
     */
    CYCLE;
  }

  /**
   * Defines how this controller should repeat itself. Default is {@link RepeatType#CLAMP}.
   */
  private RepeatType _repeatType = RepeatType.CLAMP;

  /**
   * The controller's minimum cycle time
   */
  private double _minTime;

  /**
   * The controller's maximum cycle time
   */
  private double _maxTime;

  /**
   * The 'speed' of this Controller. Generically speaking, less than 1 is slower, more than 1 is
   * faster, and 1 represents the base speed
   */
  private double _speed = 1;

  /**
   * True if this controller is active, false otherwise
   */
  private boolean _active = true;

  /**
   * @return The speed of this controller. Speed is 1 by default.
   */
  public double getSpeed() { return _speed; }

  /**
   * Sets the speed of this controller
   * 
   * @param speed
   *          The new speed
   */
  public void setSpeed(final double speed) { _speed = speed; }

  /**
   * Returns the current maximum time for this controller.
   * 
   * @return This controller's maximum time.
   */
  public double getMaxTime() { return _maxTime; }

  /**
   * Sets the maximum time for this controller
   * 
   * @param maxTime
   *          The new maximum time
   */
  public void setMaxTime(final double maxTime) { _maxTime = maxTime; }

  /**
   * Returns the current minimum time of this controller
   * 
   * @return This controller's minimum time
   */
  public double getMinTime() { return _minTime; }

  /**
   * Sets the minimum time of this controller
   * 
   * @param minTime
   *          The new minimum time.
   */
  public void setMinTime(final double minTime) { _minTime = minTime; }

  /**
   * Returns the current repeat type of this controller.
   * 
   * @return The current repeat type
   */
  public RepeatType getRepeatType() { return _repeatType; }

  /**
   * Sets the repeat type of this controller. The default is {@link RepeatType#CLAMP}.
   * 
   * @param repeatType
   *          The new repeat type, can not be <code>null</code>.
   */
  public void setRepeatType(final RepeatType repeatType) {
    if (null == repeatType) {
      throw new IllegalArgumentException("repeatType can not be null!");
    }

    _repeatType = repeatType;
  }

  /**
   * Sets the active flag of this controller. Note: updates on controllers are still called even if
   * this flag is set to false. It is the responsibility of the extending class to check isActive if
   * it wishes to be turn-off-able.
   * 
   * @param active
   *          The new active state.
   */
  public void setActive(final boolean active) { _active = active; }

  /**
   * Returns if this Controller is active or not.
   * 
   * @return True if this controller is set to active, false if not.
   */
  public boolean isActive() { return _active; }

  /**
   * @return <code>true</code> if the {@link #getRepeatType() repeat type} is {@link RepeatType#CLAMP
   *         clamp}, <code>false</code> otherwise.
   */
  public boolean isRepeatTypeClamp() { return RepeatType.CLAMP.equals(getRepeatType()); }

  /**
   * @return <code>true</code> if the {@link #getRepeatType() repeat type} is {@link RepeatType#WRAP
   *         wrap}, <code>false</code> otherwise.
   */
  public boolean isRepeatTypeWrap() { return RepeatType.WRAP.equals(getRepeatType()); }

  /**
   * @return <code>true</code> if the {@link #getRepeatType() repeat type} is {@link RepeatType#CYCLE
   *         cycle}, <code>false</code> otherwise.
   */
  public boolean isRepeatTypeCycle() { return RepeatType.CYCLE.equals(getRepeatType()); }

  @Override
  public abstract void update(double time, T caller);

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_repeatType, "repeatType", RepeatType.CLAMP);
    capsule.write(_minTime, "minTime", 0);
    capsule.write(_maxTime, "maxTime", 0);
    capsule.write(_speed, "speed", 1);
    capsule.write(_active, "active", true);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _repeatType = capsule.readEnum("repeatType", RepeatType.class, RepeatType.CLAMP);
    _minTime = capsule.readDouble("minTime", 0);
    _maxTime = capsule.readDouble("maxTime", 0);
    _speed = capsule.readDouble("speed", 1);
    _active = capsule.readBoolean("active", true);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Class<? extends ComplexSpatialController> getClassTag() { return this.getClass(); }

  public void getControllerValues(final HashMap<String, Object> store) {

  }

  public void setControllerValues(final HashMap<String, Object> values) {

  }
}
