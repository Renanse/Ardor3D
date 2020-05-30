/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.layer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.state.AbstractFiniteState;
import com.ardor3d.extension.animation.skeletal.state.AbstractTransitionState;
import com.ardor3d.extension.animation.skeletal.state.StateOwner;
import com.ardor3d.extension.animation.skeletal.state.SteadyState;

/**
 * Animation layers are essentially independent state machines, managed by a single
 * AnimationManager. Each maintains a set of possible "steady states" - main states that the layer
 * can be in. The layer can only be in one state at any given time. It may transition between
 * states, provided that a path is defined for transition from the current state to the desired one.
 */
public class AnimationLayer implements StateOwner {

  /** The layer name of the default base layer. */
  public static final String BASE_LAYER_NAME = "-BASE_LAYER-";

  /** our class logger */
  private static final Logger logger = Logger.getLogger(AnimationLayer.class.getName());

  /** Our animation states */
  private final Map<String, SteadyState> _steadyStates = new HashMap<>();

  /** Our current animation state */
  private AbstractFiniteState _currentState;

  /** our parent manager */
  private AnimationManager _manager;

  /** our layer blending module */
  private LayerBlender _layerBlender;

  /** the name of this layer, used for identification, so best if unique. */
  private final String _name;

  /** A map of general transitions for moving from the current state to another. */
  private final Map<String, AbstractTransitionState> _transitions = new HashMap<>();

  /**
   * Construct a new AnimationLayer.
   * 
   * @param name
   *          the name of this layer, used for id purposes.
   */
  public AnimationLayer(final String name) {
    _name = name;
  }

  /**
   * Force the current state of the machine to the steady state with the given name. Used to set the
   * FSM's initial state.
   * 
   * @param stateName
   *          the name of our state. If null, or is not present in this state machine, the current
   *          state is not changed.
   * @param rewind
   *          if true, the clip(s) in the given state will be rewound by setting its start time to the
   *          current time and setting it active.
   * @return true if succeeds
   */
  public boolean setCurrentState(final String stateName, final boolean rewind) {
    if (stateName != null) {
      final AbstractFiniteState state = _steadyStates.get(stateName);
      if (state != null) {
        setCurrentState(state, rewind);
        return true;
      } else {
        AnimationLayer.logger.warning("unable to find SteadyState named: " + stateName);
      }
    }
    return false;
  }

  /**
   * Sets the current finite state to the given state. Generally for transitional state use.
   * 
   * @param state
   *          our new state. If null, then no state is currently set on this layer.
   * @param rewind
   *          if true, the clip(s) in the given state will be rewound by setting its start time to the
   *          current time and setting it active.
   */
  public void setCurrentState(final AbstractFiniteState state, final boolean rewind) {
    _currentState = state;
    if (state != null) {
      state.setLastStateOwner(this);
      if (rewind) {
        state.resetClips(_manager);
      }
    }
  }

  /**
   * Set the currently playing state on this layer to null.
   */
  public void clearCurrentState() {
    setCurrentState((AbstractFiniteState) null, false);
  }

  /**
   * Attempt to perform a transition. First, check the current state to see if it has a transition for
   * the given key. If not, check this layer for a general purpose transition. If no transition is
   * found, this does nothing.
   * 
   * @param key
   *          the transition key, a string key used to look up a transition in the current animation
   *          state.
   * @return true if there is a current state and we were able to do the given transition.
   */
  public boolean doTransition(final String key) {
    final AbstractFiniteState state = getCurrentState();
    // see if current state has a transition
    if (state instanceof SteadyState) {
      final SteadyState steadyState = (SteadyState) state;
      AbstractFiniteState nextState = steadyState.doTransition(key, this);
      if (nextState == null) {
        // no transition found, check if there is a global transition
        AbstractTransitionState transition = _transitions.get(key);
        if (transition == null) {
          transition = _transitions.get("*");
        }
        if (transition != null) {
          nextState = transition.doTransition(state, this);
        }
      }

      if (nextState != null) {
        if (nextState != state) {
          setCurrentState(nextState, false);
          return true;
        }
      }
    } else if (state == null) {
      // check if there is a global transition
      AbstractTransitionState transition = _transitions.get(key);
      if (transition == null) {
        transition = _transitions.get("*");
      }
      if (transition != null) {
        setCurrentState(transition.doTransition(state, this), true);
        return true;
      }
    }

    // no transition found
    return false;
  }

  /**
   * @return a set containing the names of our steady states.
   */
  public Set<String> getSteadyStateNames() { return _steadyStates.keySet(); }

  /**
   * @return the current active finite state in this machine.
   */
  public AbstractFiniteState getCurrentState() { return _currentState; }

  /**
   * @param stateName
   *          the name of the steady state we are looking for.
   * @return our animation state, or null if none is found.
   */
  public SteadyState getSteadyState(final String stateName) {
    return _steadyStates.get(stateName);
  }

  /**
   * Add a new steady state to this layer.
   * 
   * @param state
   *          the state to add.
   */
  public void addSteadyState(final SteadyState state) {
    if (state == null) {
      throw new IllegalArgumentException("state must not be null.");
    }
    _steadyStates.put(state.getName(), state);
  }

  /**
   * Remove the given steady state from our layer
   * 
   * @param state
   *          the state to remove
   * @return true if the state was found for removal.
   */
  public boolean removeSteadyState(final SteadyState state) {
    return _steadyStates.remove(state.getName()) != null;
  }

  /**
   * Sets a reference back to the manager associated with this layer. Generally this is handled by the
   * AnimationManager itself as layers are added to it.
   * 
   * @param manager
   *          the animation manager.
   */
  public void setManager(final AnimationManager manager) { _manager = manager; }

  /**
   * @return the manager associated with this layer.
   */
  public AnimationManager getManager() { return _manager; }

  /**
   * @return the name of this layer, used for identification, so best if unique.
   */
  public String getName() { return _name; }

  /**
   * @return a source data mapping for the channels involved in the current state/transition of this
   *         layer.
   */
  public Map<String, ? extends Object> getCurrentSourceData() {
    if (getLayerBlender() != null) {
      return getLayerBlender().getBlendedSourceData(getManager());
    }

    final AbstractFiniteState state = getCurrentState();
    if (state != null) {
      return state.getCurrentSourceData(getManager());
    } else {
      return null;
    }
  }

  /**
   * Update the layer blender in this animation layer to properly point to the previous layer.
   * 
   * @param previousLayer
   *          the layer before this layer in the animation manager.
   */
  public void updateLayerBlending(final AnimationLayer previousLayer) {
    final LayerBlender blender = getLayerBlender();
    if (blender != null) {
      blender.setLayerA(previousLayer);
      blender.setLayerB(this);
    }
  }

  /**
   * @param layerBlender
   *          the layer blender to use for combining this layer's contents with others in the
   *          animation manager.
   */
  public void setLayerBlender(final LayerBlender layerBlender) { _layerBlender = layerBlender; }

  /**
   * @return the layer blender used for combining this layer's contents with others in the animation
   *         manager.
   */
  public LayerBlender getLayerBlender() { return _layerBlender; }

  @Override
  public void replaceState(final AbstractFiniteState currentState, final AbstractFiniteState newState) {
    if (getCurrentState() == currentState) {
      setCurrentState(newState, false);
    }
  }

  /**
   * Add a new general transition to this layer.
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
   *          the reference key for the transition state we wish to pull from this layer.
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
   * @return a Set of the transition state keywords used by this layer.
   */
  public Set<String> getTransitionKeywords() { return _transitions.keySet(); }

  /**
   * Remove a transition state by keyword.
   * 
   * @param keyword
   *          the reference key for the transition state we wish to remove from this layer.
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
   * Remove the first instance of a specific transition state from this layer.
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
}
