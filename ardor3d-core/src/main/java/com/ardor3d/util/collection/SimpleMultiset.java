/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.collection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A simple Multiset implementation using a HashMap to count the occurrences of elements.
 */
public class SimpleMultiset<K> implements Multiset<K> {
  private final Map<K, Integer> _counts = new HashMap<>();

  @Override
  public void add(K element) {
    _counts.merge(element, 1, Integer::sum);
  }

  @Override
  public void setCount(K element, int count) {
    if (count <= 0) {
      _counts.remove(element);
    } else {
      _counts.put(element, count);
    }
  }

  @Override
  public boolean remove(K element) {
    if (!_counts.containsKey(element)) {
      return false;
    }
    var count = _counts.get(element);
    if (count > 1) {
      _counts.put(element, count - 1);
    } else {
      _counts.remove(element);
    }
    return true;
  }

  @Override
  public int count(K element) {
    return _counts.getOrDefault(element, 0);
  }

  @Override
  public Set<K> elementSet() {
    return Collections.unmodifiableSet(_counts.keySet());
  }

  @Override
  public void removeAll(K element) {
    _counts.remove(element);
  }

  @Override
  public int size() {
    return _counts.values().stream().mapToInt(Integer::intValue).sum();
  }

  @Override
  public boolean isEmpty() {
    return _counts.isEmpty();
  }

  @Override
  public void clear() {
    _counts.clear();
  }

  @Override
  public SimpleMultiset<K> copyOf() {
    var multiset = new SimpleMultiset<K>();
    multiset._counts.putAll(_counts);
    return multiset;
  }
}

