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

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface Multimap<K, V> {
  /**
   * Adds the given value to the list of values for the given key.
   *
   * @param key   the key to add the value to
   * @param value the value to add
   */
  void put(K key, V value);

  /**
   * Adds all of the given values to the list of values for the given key.
   *
   * @param key  the key to add the values to
   * @param vals the values to add
   */
  void putAll(K key, Collection<V> val);

  /**
   * Returns an unmodifiable list of values for the given key. If the key is not present, an empty list is returned.
   *
   * @param key the key to look up
   * @return an unmodifiable list of values for the given key, or an empty list if the key is not present.
   */
  List<V> values(K key);

  /**
   * Removes the given value from the list of values for the given key. If the key is not present, or the value is not
   * present in the list, nothing happens.
   *
   * @param key   the key to remove the value from
   * @param value the value to remove
   * @return true if the value was removed, false otherwise.
   */
  boolean remove(K key, V value);

  /**
   * Removes all values for the given key.
   *
   * @param key the key to remove all values for
   */
  void removeAll(K key);

  /**
   * @param key the key to check for
   * @return true if the given key is present and has at least one value, false otherwise.
   */
  boolean containsKey(K key);

  /**
   * @return a set of all keys present in this multimap.
   */
  Set<K> keySet();

  /**
   * @return true if this multimap is empty, false otherwise.
   */
  boolean isEmpty();
}
