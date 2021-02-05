/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.state;

import java.util.HashMap;
import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.BinaryLERPSource;
import com.ardor3d.extension.animation.skeletal.layer.AnimationLayer;
import com.ardor3d.math.util.MathUtils;

/**
 * An abstract transition state that blends between two other states.
 */
public abstract class AbstractTwoStateLerpTransition extends AbstractTransitionState implements StateOwner {

  /**
   * Describes how blending should be applied over the span of the transition.
   */
  public enum BlendType {
    /** Blend linearly. */
    Linear,

    /** Blend using a cubic S-curve: 3t^2 - 2t^3 */
    SCurve3,

    /** Blend using a quintic S-curve: 6t^5 - 15t^4 + 10t^3 */
    SCurve5;
  }

  /** Our initial or start state. */
  private AbstractFiniteState _stateA;

  /** Our target or end state. */
  private AbstractFiniteState _stateB;

  /** The length of time for the transition. */
  private double _fadeTime = 0;

  /** The global time when the transition started. */
  private double _start = 0;

  /**
   * A percentage value of how much of each state to blend for our final result, generated based on
   * time and blend type.
   */
  private double _percent = 0;

  /**
   * The method to use in determining how much of each state to use based on the current time of the
   * transition.
   */
  private BlendType _type = BlendType.Linear;

  /** The blended source data. */
  private Map<String, Object> _sourceData;

  /**
   * Construct a new AbstractTwoStateLerpTransition.
   * 
   * @param targetState
   *          the name of the steady state we want the Animation Layer to be in at the end of the
   *          transition.
   * @param fadeTime
   *          the amount of time we should take to do the transition.
   * @param type
   *          the way we should interpolate the weighting during the transition.
   */
  protected AbstractTwoStateLerpTransition(final String targetState, final double fadeTime, final BlendType type) {
    super(targetState);
    setFadeTime(fadeTime);
    setBlendType(type);
  }

  public AbstractFiniteState getStateA() { return _stateA; }

  /**
   * @param stateA
   *          sets the start state. Updates the state's owner to point to this transition.
   */
  public void setStateA(final AbstractFiniteState stateA) {
    if (stateA == this) {
      throw new IllegalArgumentException("Can not set state A to self.");
    }
    _stateA = stateA;
    if (_stateA != null) {
      _stateA.setLastStateOwner(this);
    }

    // clear the _sourceData, the new state probably has different transform data
    if (_sourceData != null) {
      _sourceData.clear();
    }
  }

  public AbstractFiniteState getStateB() { return _stateB; }

  /**
   * @param stateA
   *          sets the end state. Updates the state's owner to point to this transition.
   */
  public void setStateB(final AbstractFiniteState stateB) {
    if (stateB == this) {
      throw new IllegalArgumentException("Can not set state B to self.");
    }
    _stateB = stateB;
    if (_stateB != null) {
      _stateB.setLastStateOwner(this);
    }

    // clear the _sourceData, the new state probably has different transform data
    if (_sourceData != null) {
      _sourceData.clear();
    }
  }

  public void setFadeTime(final double fadeTime) { _fadeTime = fadeTime; }

  public double getFadeTime() { return _fadeTime; }

  public void setBlendType(final BlendType type) { _type = type; }

  public BlendType getBlendType() { return _type; }

  protected void setStart(final double start) { _start = start; }

  protected double getStart() { return _start; }

  protected void setPercent(final double percent) { _percent = percent; }

  protected double getPercent() { return _percent; }

  @Override
  public void update(final double globalTime, final AnimationLayer layer) {
    final double currentTime = globalTime - getStart();

    // if we're outside the fade time...
    if (currentTime > getFadeTime()) {
      // transition over to end state
      getLastStateOwner().replaceState(this, getStateB());
      return;
    }

    // figure out our weight using time, total time and fade type
    final double percent = currentTime / getFadeTime();

    switch (getBlendType()) {
      case SCurve3:
        setPercent(MathUtils.scurve3(percent));
        break;
      case SCurve5:
        setPercent(MathUtils.scurve5(percent));
        break;
      case Linear:
      default:
        setPercent(percent);
        break;
    }
  }

  @Override
  public Map<String, ? extends Object> getCurrentSourceData(final AnimationManager manager) {
    // grab our data maps from the two states
    final Map<String, ? extends Object> sourceAData =
        getStateA() != null ? getStateA().getCurrentSourceData(manager) : null;
    final Map<String, ? extends Object> sourceBData =
        getStateB() != null ? getStateB().getCurrentSourceData(manager) : null;

    // reuse previous _sourceData transforms to avoid re-creating
    // too many new transform data objects. This assumes that a
    // same state always returns the same transform data objects.
    if (_sourceData == null) {
      _sourceData = new HashMap<>();
    }
    return BinaryLERPSource.combineSourceData(sourceAData, sourceBData, getPercent(), _sourceData);
  }

  @Override
  public void replaceState(final AbstractFiniteState currentState, final AbstractFiniteState newState) {
    if (newState != null) {
      if (getStateA() == currentState) {
        setStateA(newState);
      } else if (getStateB() == currentState) {
        setStateB(newState);
      }
    }
  }
}
