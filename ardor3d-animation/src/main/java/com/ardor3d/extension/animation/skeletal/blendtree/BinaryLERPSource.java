/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.blendtree;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.clip.TransformData;
import com.ardor3d.math.util.MathUtils;

/**
 * <p>
 * Takes two blend sources and uses linear interpolation to merge TransformData values. If one of
 * the sources is null, or does not have a key that the other does, we disregard weighting and use
 * the non-null side's full value.
 * </p>
 * <p>
 * Source data that is not TransformData is not combined, rather A's value will always be used
 * unless it is null.
 * </p>
 */
public class BinaryLERPSource extends AbstractTwoPartSource {

  /**
   * Construct a new lerp source. The two sub sources should be set separately before use.
   */
  public BinaryLERPSource() {}

  /**
   * Construct a new lerp source using the supplied sources.
   * 
   * @param sourceA
   *          our first source.
   * @param sourceB
   *          our second source.
   */
  public BinaryLERPSource(final BlendTreeSource sourceA, final BlendTreeSource sourceB) {
    setSourceA(sourceA);
    setSourceB(sourceB);
  }

  @Override
  public Map<String, ? extends Object> getSourceData(final AnimationManager manager) {
    // grab our data maps from the two sources
    final Map<String, ? extends Object> sourceAData = getSourceA() != null ? getSourceA().getSourceData(manager) : null;
    final Map<String, ? extends Object> sourceBData = getSourceB() != null ? getSourceB().getSourceData(manager) : null;

    return BinaryLERPSource.combineSourceData(sourceAData, sourceBData, manager.getValuesStore().get(getBlendKey()));
  }

  @Override
  public boolean setTime(final double globalTime, final AnimationManager manager) {
    // set our time on the two sub sources
    boolean foundActive = false;
    if (getSourceA() != null) {
      foundActive |= getSourceA().setTime(globalTime, manager);
    }
    if (getSourceB() != null) {
      foundActive |= getSourceB().setTime(globalTime, manager);
    }
    return foundActive;
  }

  @Override
  public void resetClips(final AnimationManager manager, final double globalStartTime) {
    // reset our two sub sources
    if (getSourceA() != null) {
      getSourceA().resetClips(manager, globalStartTime);
    }
    if (getSourceB() != null) {
      getSourceB().resetClips(manager, globalStartTime);
    }
  }

  /**
   * Combines two sets of source data maps by matching elements with the same key. Map values of type
   * TransformData are combined via linear interpolation. Other value types are not combined, rather
   * the value from source A is used unless null. Keys that exist only in one map or the other are
   * preserved in the resulting map.
   * 
   * @param sourceAData
   *          our first source map
   * @param sourceBData
   *          our second source map
   * @param blendWeight
   *          our blend weight - used to perform linear interpolation on TransformData values.
   * @return our combined data map.
   */
  public static Map<String, ? extends Object> combineSourceData(final Map<String, ? extends Object> sourceAData,
      final Map<String, ? extends Object> sourceBData, final Double blendWeight) {
    return BinaryLERPSource.combineSourceData(sourceAData, sourceBData,
        blendWeight != null ? blendWeight.doubleValue() : 0.0, null);
  }

  public static Map<String, ? extends Object> combineSourceData(final Map<String, ? extends Object> sourceAData,
      final Map<String, ? extends Object> sourceBData, final double blendWeight, final Map<String, Object> store) {
    // XXX: Should blendWeight of 0 or 1 disable non transform data from B/A respectively? Currently
    // blendWeight is
    // ignored in such.

    if (sourceBData == null) {
      return sourceAData;
    } else if (sourceAData == null) {
      return sourceBData;
    }

    Map<String, Object> rVal = store;
    if (rVal == null) {
      rVal = new HashMap<>();
    }

    for (final Entry<String, ? extends Object> entryAData : sourceAData.entrySet()) {
      final String key = entryAData.getKey();
      final Object dataA = entryAData.getValue();
      final Object dataB = sourceBData.get(key);
      if (dataA instanceof float[]) {
        BinaryLERPSource.blendFloatValue(rVal, key, blendWeight, (float[]) dataA, (float[]) dataB);
        continue;
      } else if (dataA instanceof double[]) {
        BinaryLERPSource.blendDoubleValue(rVal, key, blendWeight, (double[]) dataA, (double[]) dataB);
        continue;
      } else if (!(dataA instanceof TransformData)) {
        // A will always override if not null.
        rVal.put(key, dataA);
        continue;
      }

      // Grab the transform data for each clip
      final TransformData transformA = (TransformData) dataA;
      final TransformData transformB = (TransformData) dataB;
      if (transformB != null) {
        rVal.put(key, transformA.blend(transformB, blendWeight, (TransformData) rVal.get(key)));
      } else {
        rVal.put(key, transformA);
      }
    }
    for (final Entry<String, ? extends Object> entryBData : sourceBData.entrySet()) {
      final String key = entryBData.getKey();
      if (rVal.containsKey(key)) {
        continue;
      }
      rVal.put(key, entryBData.getValue());
    }

    return rVal;
  }

  protected static void blendFloatValue(final Map<String, Object> rVal, final String key, final double blendWeight,
      final float[] dataA, final float[] dataB) {
    if (dataB == null) {
      rVal.put(key, dataA);
    } else {
      float[] store = (float[]) rVal.get(key);
      if (store == null) {
        store = new float[1];
        rVal.put(key, store);
      }
      store[0] = MathUtils.lerp((float) blendWeight, dataA[0], dataB[0]);
    }
  }

  protected static void blendDoubleValue(final Map<String, Object> rVal, final String key, final double blendWeight,
      final double[] dataA, final double[] dataB) {
    if (dataB == null) {
      rVal.put(key, dataA);
    } else {
      double[] store = (double[]) rVal.get(key);
      if (store == null) {
        store = new double[1];
        rVal.put(key, store);
      }
      store[0] = MathUtils.lerp(blendWeight, dataA[0], dataB[0]);
    }
  }

  @Override
  public boolean isActive(final AnimationManager manager) {
    boolean foundActive = false;
    if (getSourceA() != null) {
      foundActive |= getSourceA().isActive(manager);
    }
    if (getSourceB() != null) {
      foundActive |= getSourceB().isActive(manager);
    }
    return foundActive;
  }
}
