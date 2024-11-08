/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.blendtree;

import java.util.Map;
import java.util.WeakHashMap;

import com.ardor3d.extension.animation.skeletal.AnimationApplier;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.clip.JointData;
import com.ardor3d.extension.animation.skeletal.clip.TransformData;
import com.ardor3d.extension.animation.skeletal.clip.TriggerCallback;
import com.ardor3d.extension.animation.skeletal.clip.TriggerData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.collection.Multimap;
import com.ardor3d.util.collection.SimpleMultimap;

/**
 * Very simple applier. Just applies joint transform data, calls any callbacks and updates the
 * pose's global transforms.
 */
public class SimpleAnimationApplier implements AnimationApplier {

  private final Multimap<String, TriggerCallback> _triggerCallbacks = new SimpleMultimap<>();

  private final Map<String, Spatial> _spatialCache = new WeakHashMap<>();

  @Override
  public void apply(final Spatial root, final AnimationManager manager) {
    if (root == null) {
      return;
    }
    final Map<String, ?> data = manager.getCurrentSourceData();

    // cycle through, pulling out and applying those we know about
    if (data != null) {
      for (final String key : data.keySet()) {
        final Object value = data.get(key);
        if (value instanceof JointData) { // ignore
        } else if (value instanceof TransformData transformData) {
          final Spatial applyTo = findChild(root, key);
          if (applyTo != null) {
            transformData.applyTo(applyTo);
          }
        }
      }
    }
  }

  private Spatial findChild(final Spatial root, final String key) {
    if (_spatialCache.containsKey(key)) {
      return _spatialCache.get(key);
    }
    if (key.equals(root.getName())) {
      _spatialCache.put(key, root);
      return root;
    } else if (root instanceof Node) {
      final Spatial spat = ((Node) root).getChild(key);
      if (spat != null) {
        _spatialCache.put(key, spat);
        return spat;
      }
    }
    return null;
  }

  @Override
  public void applyTo(final SkeletonPose applyToPose, final AnimationManager manager) {
    final Map<String, ?> data = manager.getCurrentSourceData();

    // cycle through, pulling out and applying those we know about
    if (data != null) {
      for (final Object value : data.values()) {
        if (value instanceof JointData jointData) {
          if (jointData.getJointIndex() >= 0) {
            jointData.applyTo(applyToPose.getLocalJointTransforms()[jointData.getJointIndex()]);
          }
        } else if (value instanceof TriggerData trigger) {
          if (trigger.isArmed()) {
            try {
              // pull callback(s) for the current trigger key, if exists, and call.
              for (final String curTrig : trigger.getCurrentTriggers()) {
                for (final TriggerCallback cb : _triggerCallbacks.values(curTrig)) {
                  cb.doTrigger(applyToPose, manager);
                }
              }
            } finally {
              trigger.setArmed(false);
            }
          }
        }
      }

      applyToPose.updateTransforms();
    }
  }

  public void clearSpatialCache() {
    _spatialCache.clear();
  }

  @Override
  public void addTriggerCallback(final String key, final TriggerCallback callback) {
    _triggerCallbacks.put(key, callback);
  }

  @Override
  public boolean removeTriggerCallback(final String key, final TriggerCallback callback) {
    return _triggerCallbacks.remove(key, callback);
  }
}
