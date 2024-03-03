/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
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
import java.util.Set;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.BlendTreeSource;
import com.ardor3d.extension.animation.skeletal.layer.AnimationLayer;

/**
 * A "steady" state is an animation state that is concrete and stand-alone (vs. a state that handles
 * transitioning between two states, for example.)
 */
public class SteadyState extends AbstractFiniteState {

  /** The name of this state. */
  private final String _name;

  /** A map of possible transitions for moving from this state to another. */
  private final Map<String, AbstractTransitionState> _transitions = new HashMap<>();

  /** A transition to use if we reach the end of this state. May be null. */
  private AbstractTransitionState _endTransition;

  /** Our state may be a blend of multiple clips, etc. This is the root of our blend tree. */
  private BlendTreeSource _sourceTree;

  /**
   * Create a new steady state.
   * 
   * @param name
   *          the name of our new state. Immutable.
   */
  public SteadyState(final String name) {
    _name = name;
  }

  /**
   * @return the name of this state.
   */
  public String getName() { return _name; }

  /**
   * @return the transition to use if we reach the end of this state. May be null.
   */
  public AbstractTransitionState getEndTransition() { return _endTransition; }

  /**
   * @param endTransition
   *          a transition to use if we reach the end of this state. May be null.
   */
  public void setEndTransition(final AbstractTransitionState endTransition) { _endTransition = endTransition; }

  /**
   * @return the root of our blend tree
   */
  public BlendTreeSource getSourceTree() { return _sourceTree; }

  /**
   * @param tree
   *          the new root of our blend tree
   */
  public void setSourceTree(final BlendTreeSource tree) { _sourceTree = tree; }

  /**
   * Add a new possible transition to this state.
   * 
   * @param keyword
   *          the reference key for the added transition.
   * @param state
   *          the transition state to add.
   * @throws IllegalArgumentException
   *           if keyword or state are null.
   */
  public void addTransition(final String keyword, final AbstractTransitionState state) {
    if (state == null) {
      throw new IllegalArgumentException("state must not be null.");
    }
    if (keyword == null) {
      throw new IllegalArgumentException("keyword must not be null.");
    }
    _transitions.put(keyword, state);
  }

  /**
   * 
   * @param keyword
   *          the reference key for the transition state we wish to pull from this steady state.
   * @return the transition related to the given keyword, or null if none are found.
   * @throws IllegalArgumentException
   *           if keyword is null.
   */
  public AbstractTransitionState getTransition(final String keyword) {
    if (keyword == null) {
      throw new IllegalArgumentException("keyword must not be null.");
    }
    return _transitions.get(keyword);
  }

  /**
   * @return a Set of the transition state keywords used by this steady state.
   */
  public Set<String> getTransitionKeywords() { return _transitions.keySet(); }

  /**
   * Remove a transition state by keyword.
   * 
   * @param keyword
   *          the reference key for the transition state we wish to remove from this steady state.
   * @return the removed transition, or null if none was found by the given keyword.
   * @throws IllegalArgumentException
   *           if keyword is null.
   */
  public AbstractTransitionState removeTransition(final String keyword) {
    if (keyword == null) {
      throw new IllegalArgumentException("keyword must not be null.");
    }
    return _transitions.remove(keyword);
  }

  /**
   * Remove the first instance of a specific transition state from this steady state.
   * 
   * @param transition
   *          the transition state we wish to remove from this steady state.
   * @return true if we found and removed the given transition.
   * @throws IllegalArgumentException
   *           if transition is null.
   */
  public boolean removeTransition(final AbstractTransitionState transition) {
    if (transition == null) {
      throw new IllegalArgumentException("transition must not be null.");
    }
    for (final String keyword : _transitions.keySet()) {
      if (_transitions.get(keyword).equals(transition)) {
        _transitions.remove(keyword);
        return true;
      }
    }
    return false;
  }

  /**
   * Request that this state transition to another.
   * 
   * @param key
   *          a key to match against a map of possible transitions.
   * @param layer
   *          the layer our state belongs to.
   * @return the new state to transition to. May be null if the transition was not possible or was
   *         ignored for some reason.
   */
  public AbstractFiniteState doTransition(final String key, final AnimationLayer layer) {
    AbstractTransitionState state = _transitions.get(key);
    if (state == null) {
      state = _transitions.get("*");
    } else {
      return state.doTransition(this, layer);
    }
    return null;
  }

  @Override
  public void update(final double globalTime, final AnimationLayer layer) {
    if (!getSourceTree().setTime(globalTime, layer.getManager())) {
      final StateOwner lastOwner = getLastStateOwner();
      if (_endTransition != null) {
        // time to move to end transition
        final AbstractFiniteState newState = _endTransition.doTransition(this, layer);
        if (newState != null) {
          newState.resetClips(layer.getManager());
          newState.update(globalTime, layer);
        }
        if (this != newState) {
          lastOwner.replaceState(this, newState);
        }
      }
    }
  }

  @Override
  public void postUpdate(final AnimationLayer layer) {
    if (!getSourceTree().isActive(layer.getManager())) {
      final StateOwner lastOwner = getLastStateOwner();
      if (_endTransition == null) {
        // we're done. end.
        lastOwner.replaceState(this, null);
      }
    }
  }

  @Override
  public Map<String, ? extends Object> getCurrentSourceData(final AnimationManager manager) {
    return getSourceTree().getSourceData(manager);
  }

  @Override
  public void resetClips(final AnimationManager manager, final double globalStartTime) {
    super.resetClips(manager, globalStartTime);
    getSourceTree().resetClips(manager, globalStartTime);
  }
}
