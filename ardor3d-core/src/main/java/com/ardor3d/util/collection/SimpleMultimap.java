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
public class SimpleMultimap<K, V> {
  private final Map<K, List<V>> map = new HashMap<>();

  /**
   * Adds the given value to the list of values for the given key.
   *
   * @param key   the key to add the value to
   * @param value the value to add
   */
  public void put(K key, V value) {
    map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
  }

  /**
   * Returns an unmodifiable list of values for the given key. If the key is not present, an empty list is returned.
   *
   * @param key the key to look up
   * @return an unmodifiable list of values for the given key, or an empty list if the key is not present.
   */
  public List<V> values(K key) {
    var values = map.get(key);
    return (values == null) ? Collections.emptyList() :  Collections.unmodifiableList(values);
  }

  /**
   * Removes the given value from the list of values for the given key. If the key is not present, or the value is not
   * present in the list, nothing happens.
   *
   * @param key   the key to remove the value from
   * @param value the value to remove
   * @return true if the value was removed, false otherwise.
   */
  public boolean remove(K key, V value) {
    if (!map.containsKey(key)) return false;

    var result = map.get(key).remove(value);
    if (map.get(key).isEmpty()) {
      map.remove(key);
    }

    return result;
  }

  /**
   * Removes all values for the given key.
   *
   * @param key the key to remove all values for
   */
  public void removeAll(K key) {
    map.remove(key);
  }

  /**
   * @param key the key to check for
   * @return true if the given key is present and has at least one value, false otherwise.
   */
  public boolean containsKey(K key) {
    return map.containsKey(key) && !map.get(key).isEmpty();
  }

  /**
   * @return a set of all keys present in this multimap.
   */
  public Set<K> keySet() {
    return map.keySet();
  }
}

