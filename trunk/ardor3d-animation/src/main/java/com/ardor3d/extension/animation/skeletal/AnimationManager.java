/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal;

import java.util.List;
import java.util.Map;

import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClipInstance;
import com.ardor3d.extension.animation.skeletal.layer.AnimationLayer;
import com.ardor3d.extension.animation.skeletal.state.AbstractFiniteState;
import com.ardor3d.extension.animation.skeletal.util.LoggingMap;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.Timer;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

/**
 * <p>
 * AnimationManager describes and maintains an animation system. It tracks one or more layered animation state machines
 * (AnimationLayer) and uses their combined result to update one or more poses (via a set AnimationApplier.)
 * AnimationClips used in these layers are instanced and tracked specifically for this manager.
 * </p>
 * <p>
 * By default, an animation manager has a single base animation layer. Other layers may be added to this. It is
 * important that the base layer (the layer at index 0) always has a full set of data to put a skeleton pose into a
 * valid state.
 * </p>
 */
public class AnimationManager {

    public enum AnimationUpdateState {
        Play, Pause, Stop
    }

    /**
     * A timer to use as our "global" time keeper. All animation sources under this manager will use this timer as their
     * time reference.
     */
    protected ReadOnlyTimer _globalTimer;

    /** The pose(s) this manager manipulates on update. */
    protected List<SkeletonPose> _applyToPoses;

    /** The root of a scenegraph we can look for transform animation targets under. */
    protected final Spatial _sceneRoot;

    /** Local instance information for any clips referenced by the layers/blend trees in this manager. */
    protected final Map<AnimationClip, AnimationClipInstance> _clipInstances = new MapMaker().weakKeys().makeMap();

    /** A logic object responsible for taking animation data and applying it to skeleton poses. */
    protected AnimationApplier _applier;

    /** Our animation layers. */
    protected final List<AnimationLayer> _layers = Lists.newArrayList();

    /**
     * A map of key->Double values, allowing control over elements under this manager without needing precise knowledge
     * of the layout of those layers, blend trees, etc. Missing keys will return 0.0 and log a warning.
     */
    protected final LoggingMap<String, Double> _valuesStore = new LoggingMap<String, Double>();

    /**
     * The throttle rate of animation. Default is 60fps (1/60.0). Set to 0 to disable throttling.
     */
    protected double _updateRate = 1.0 / 60.0;

    /**
     * The global time we last processed an animation. (To use when checking our throttle.)
     */
    protected double _lastUpdate = 0.0;

    /**
     * Sets the current animationState used to control if animation is playing, pausing or stopped.
     */
    protected AnimationUpdateState _currentAnimationState = AnimationUpdateState.Play;

    /**
     * boolean threshold to allow stop state to be updated one last time...
     */
    private boolean _canSetStopState = false;

    /**
     * Listeners for changes to this manager's AnimationUpdateState.
     */
    protected final List<AnimationUpdateStateListener> _updateStateListeners = Lists.newArrayList();

    /**
     * Construct a new AnimationManager.
     * 
     * @param globalTimer
     *            the timer to use for global time keeping.
     * @param pose
     *            a pose to update. Optional if we won't be animating a {@link SkinnedMesh}.
     */
    public AnimationManager(final ReadOnlyTimer globalTimer, final SkeletonPose pose) {
        this(globalTimer, pose, null);
    }

    /**
     * Construct a new AnimationManager.
     * 
     * @param globalTimer
     *            the timer to use for global time keeping.
     * @param pose
     *            a pose to update. Optional if we won't be animating a {@link SkinnedMesh}.
     * @param sceneRoot
     *            a root we will use to search for spatials when doing transform animations.
     */
    public AnimationManager(final ReadOnlyTimer globalTimer, final SkeletonPose pose, final Spatial sceneRoot) {
        _globalTimer = globalTimer;
        _sceneRoot = sceneRoot;

        // add our base layer
        final AnimationLayer layer = new AnimationLayer(AnimationLayer.BASE_LAYER_NAME);
        layer.setManager(this);
        _layers.add(layer);

        if (pose != null) {
            _applyToPoses = Lists.newArrayList(pose);
        } else {
            _applyToPoses = Lists.newArrayList();
        }

        _valuesStore.setLogOnReplace(false);
        _valuesStore.setDefaultValue(0.0);
    }

    /**
     * @return the "local time", in seconds reported by our global timer.
     */
    public double getCurrentGlobalTime() {
        return _globalTimer.getTimeInSeconds();
    }

    /**
     * @return the timer used by this manager for global time keeping.
     */
    public ReadOnlyTimer getGlobalTimer() {
        return _globalTimer;
    }

    /**
     * @param timer
     *            the timer to be used by this manager for global time keeping.
     */
    public void setGlobalTimer(final Timer timer) {
        _globalTimer = timer;
    }

    public void play() {
        setAnimationUpdateState(AnimationUpdateState.Play);
    }

    public void pause() {
        setAnimationUpdateState(AnimationUpdateState.Pause);
    }

    public void stop() {
        setAnimationUpdateState(AnimationUpdateState.Stop);
    }

    public boolean isPlaying() {
        return _currentAnimationState == AnimationUpdateState.Play;
    }

    public boolean isPaused() {
        return _currentAnimationState == AnimationUpdateState.Pause;
    }

    /**
     * @param newAnimationState
     *            the new animation state in the animation Manager.
     */
    public void setAnimationUpdateState(final AnimationUpdateState newAnimationState) {
        if (newAnimationState == _currentAnimationState) {
            // ignore if unchanged.
            return;
        }
        final double currentTime = _globalTimer.getTimeInSeconds();
        if (newAnimationState == AnimationUpdateState.Pause) {
            if (_currentAnimationState == AnimationUpdateState.Stop) {
                // ignore a non-allowed situation
                return;
            }

            // Keep track of current time so we can resume active clips
            _lastUpdate = currentTime;
        } else if (newAnimationState == AnimationUpdateState.Play) {
            // reset instances
            if (_currentAnimationState == AnimationUpdateState.Pause) {
                final double offset = currentTime - _lastUpdate;
                for (final AnimationClipInstance instance : _clipInstances.values()) {
                    if (instance.isActive()) {
                        instance.setStartTime(instance.getStartTime() + offset);
                    }
                }
            } else {
                // must be resuming from stop, so restart clips
                for (final AnimationClipInstance instance : _clipInstances.values()) {
                    if (instance.isActive()) {
                        instance.setStartTime(currentTime);
                    }
                }
            }
        } else {
            for (final AnimationClipInstance instance : _clipInstances.values()) {
                if (instance.isActive()) {
                    instance.setStartTime(currentTime);
                }
            }
        }

        final AnimationUpdateState oldState = _currentAnimationState;
        _currentAnimationState = newAnimationState;

        // Let listeners know we have changed state.
        fireAnimationUpdateStateChange(oldState);
    }

    /**
     * Notify any listeners of the state change
     * 
     * @param oldState
     *            previous state
     */
    protected void fireAnimationUpdateStateChange(final AnimationUpdateState oldState) {
        for (final AnimationUpdateStateListener listener : _updateStateListeners) {
            listener.stateChanged(oldState, _currentAnimationState);
        }
    }

    /**
     * Add an AnimationUpdateStateListener to this manager.
     * 
     * @param listener
     *            the listener to add.
     */
    public void addAnimationUpdateStateListener(final AnimationUpdateStateListener listener) {
        _updateStateListeners.add(listener);
    }

    /**
     * Remove an AnimationUpdateStateListener from this manager.
     * 
     * @param listener
     *            the listener to remove.
     * @return true if the listener was found
     */
    public boolean removeAnimationUpdateStateListener(final AnimationUpdateStateListener listener) {
        return _updateStateListeners.remove(listener);
    }

    /**
     * Remove any AnimationUpdateStateListeners registered with this manager.
     */
    public void clearAnimationUpdateStateListeners() {
        _updateStateListeners.clear();
    }

    /**
     * @return the currentAnimationState.
     */
    public AnimationUpdateState getAnimationUpdateState() {
        return _currentAnimationState;
    }

    /**
     * @param pose
     *            a pose to add to be updated by this manager.
     */
    public void addPose(final SkeletonPose pose) {
        _applyToPoses.add(pose);
    }

    /**
     * @param pose
     *            the pose to remove from this manager.
     * @return true if the pose was found to be removed.
     */
    public boolean removePose(final SkeletonPose pose) {
        return _applyToPoses.remove(pose);
    }

    /**
     * @param pose
     *            a pose to look for
     * @return true if the pose was found in this manager.
     */
    public boolean containsPose(final SkeletonPose pose) {
        return _applyToPoses.contains(pose);
    }

    /**
     * @return the number of poses managed by this manager.
     */
    public int getPoseCount() {
        return _applyToPoses.size();
    }

    /**
     * @param index
     *            the index to pull the pose from.
     * @return pose at the given index
     */
    public SkeletonPose getSkeletonPose(final int index) {
        return _applyToPoses.get(index);
    }

    /**
     * @return the logic object responsible for taking animation data and applying it to skeleton poses.
     */
    public AnimationApplier getApplier() {
        return _applier;
    }

    /**
     * @param applier
     *            a logic object to be responsible for taking animation data and applying it to skeleton poses.
     */
    public void setApplier(final AnimationApplier applier) {
        _applier = applier;
    }

    /**
     * Move associated layers forward to the current global time and then apply the associated animation data to any
     * SkeletonPoses set on the manager.
     */
    public void update() {

        if (_currentAnimationState != AnimationUpdateState.Play) {
            if (_currentAnimationState == AnimationUpdateState.Stop && !_canSetStopState) {
                _canSetStopState = true;
            } else {
                return;
            }
        } else {
            _canSetStopState = false;
        }
        // grab current global time
        final double globalTime = _globalTimer.getTimeInSeconds();

        // check throttle
        if (_updateRate != 0.0) {
            if (globalTime - _lastUpdate < _updateRate) {
                return;
            }

            // we subtract a bit to maintain our desired rate, even if there are some gc pauses, etc.
            _lastUpdate = globalTime - (globalTime - _lastUpdate) % _updateRate;
        }

        // move the time forward on the layers
        for (int i = 0; i < _layers.size(); ++i) {
            final AnimationLayer layer = _layers.get(i);
            final AbstractFiniteState state = layer.getCurrentState();
            if (state != null) {
                state.update(globalTime, layer);
            }
        }

        // call apply on blend module, passing in pose
        if (!_applyToPoses.isEmpty()) {
            for (int i = 0; i < _applyToPoses.size(); ++i) {
                final SkeletonPose pose = _applyToPoses.get(i);
                _applier.applyTo(pose, this);
            }
        }

        // apply for non-pose related assets
        _applier.apply(_sceneRoot, this);

        // post update to clear states
        for (int i = 0; i < _layers.size(); ++i) {
            final AnimationLayer layer = _layers.get(i);
            final AbstractFiniteState state = layer.getCurrentState();
            if (state != null) {
                state.postUpdate(layer);
            }
        }
    }

    /**
     * Retrieve and track an instance of an animation clip to be used with this manager.
     * 
     * @param clip
     *            the clip to instance.
     * @return our new clip instance.
     */
    public AnimationClipInstance getClipInstance(final AnimationClip clip) {
        AnimationClipInstance instance = _clipInstances.get(clip);
        if (instance == null) {
            instance = new AnimationClipInstance();
            instance.setStartTime(_globalTimer.getTimeInSeconds());
            _clipInstances.put(clip, instance);
        }

        return instance;
    }

    /**
     * Retrieve an existing clip instance being tracked by this manager.
     * 
     * @param clipName
     *            the name of the clip to find an existing instance of. Case sensitive.
     * @return our existing clip instance, or null if we were not tracking a clip of the given name.
     */
    public AnimationClipInstance findClipInstance(final String clipName) {
        for (final AnimationClip clip : _clipInstances.keySet()) {
            if (clipName.equals(clip.getName())) {
                return _clipInstances.get(clip);
            }
        }

        return null;
    }

    /**
     * Retrieve an existing clip tracked by this manager.
     * 
     * @param clipName
     *            the name of the clip to find. Case sensitive.
     * @return our existing clip, or null if we were not tracking a clip of the given name.
     */
    public AnimationClip findAnimationClip(final String clipName) {
        for (final AnimationClip clip : _clipInstances.keySet()) {
            if (clipName.equals(clip.getName())) {
                return clip;
            }
        }

        return null;
    }

    /**
     * Rewind and reactivate the clip instance associated with the given clip.
     * 
     * @param clip
     *            the clip to pull the instance for.
     * @param globalStartTime
     *            the time to set the clip instance's start as.
     */
    public void resetClipInstance(final AnimationClip clip, final double globalStartTime) {
        final AnimationClipInstance instance = getClipInstance(clip);
        if (instance != null) {
            instance.setStartTime(globalStartTime);
            instance.setActive(true);
        }
    }

    /**
     * @param index
     *            the index of the layer to retrieve.
     * @return the animation layer at that index, or null of index is outside the bounds of our list of layers.
     */
    public AnimationLayer getAnimationLayer(final int index) {
        if (index < 0 || index >= _layers.size()) {
            return null;
        }
        return _layers.get(index);
    }

    /**
     * @param layerName
     *            the name of the layer to find.
     * @return the first animation layer with a matching name, or null of none are found.
     */
    public AnimationLayer findAnimationLayer(final String layerName) {
        for (final AnimationLayer layer : _layers) {
            if (layerName.equals(layer.getName())) {
                return layer;
            }
        }
        return null;
    }

    /**
     * Add a new layer to our list of animation layers.
     * 
     * @param layer
     *            the layer to add.
     * @return the index of our added layer in our list of animation layers.
     */
    public int addAnimationLayer(final AnimationLayer layer) {
        _layers.add(layer);
        layer.setManager(this);
        return _layers.size() - 1;
    }

    /**
     * Insert a given animation layer into our list of layers.
     * 
     * @param layer
     *            the layer to insert.
     * @param index
     *            the index to insert at. Moves any layers at that index over by one before inserting.
     */
    public void insertAnimationLayer(final AnimationLayer layer, final int index) {
        _layers.add(index, layer);
        layer.setManager(this);
    }

    /**
     * @param layer
     *            a layer to remove.
     * @return true if the layer is found to remove.
     */
    public boolean removeAnimationLayer(final AnimationLayer layer) {
        return _layers.remove(layer);
    }

    /**
     * @return our bottom most layer. This layer should always consist of a full skeletal pose data.
     */
    public AnimationLayer getBaseAnimationLayer() {
        return _layers.get(0);
    }

    /**
     * @return the amount of time in seconds between frame rate updates. (throttle) default is 60fps (1.0/60.0).
     */
    public double getUpdateRate() {
        return _updateRate;
    }

    /**
     * @param updateRate
     *            the new throttle rate. Default is 60fps (1.0/60.0). Set to 0 to disable throttling.
     */
    public void setUpdateRate(final double updateRate) {
        _updateRate = updateRate;
    }

    /**
     * @return the current source data from the layers of this manager.
     */
    public Map<String, ? extends Object> getCurrentSourceData() {
        // set up our layer blending.
        for (int i = 0; i < _layers.size() - 1; i++) {
            final AnimationLayer layerA = _layers.get(i);
            final AnimationLayer layerB = _layers.get(i + 1);
            layerB.updateLayerBlending(layerA);
        }

        return _layers.get(_layers.size() - 1).getCurrentSourceData();
    }

    public LoggingMap<String, Double> getValuesStore() {
        return _valuesStore;
    }
}
