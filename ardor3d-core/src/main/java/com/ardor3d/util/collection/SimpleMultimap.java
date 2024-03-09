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

import java.util.*;

/**
 * A simple multimap implementation that uses a Map of Lists to store its data.
 */
public class SimpleMultimap<K, V> implements Multimap<K, V> {
  private final Map<K, List<V>> map = new HashMap<>();

  @Override
  public void put(K key, V value) {
    map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
  }

  @Override
  public void putAll(K key, Collection<V> val) {
    map.computeIfAbsent(key, k -> new ArrayList<>()).addAll(val);
  }

  @Override
  public List<V> values(K key) {
    var values = map.get(key);
    return (values == null) ? Collections.emptyList() :  Collections.unmodifiableList(values);
  }

  @Override
  public boolean remove(K key, V value) {
    if (!map.containsKey(key)) return false;

    var result = map.get(key).remove(value);
    if (map.get(key).isEmpty()) {
      map.remove(key);
    }

    return result;
  }

  @Override
  public void removeAll(K key) {
    map.remove(key);
  }

  @Override
  public boolean containsKey(K key) {
    return map.containsKey(key) && !map.get(key).isEmpty();
  }

  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }
}

