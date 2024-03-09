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

import java.util.Set;

/**
 * A multiset is a collection that can contain duplicate elements.  It is similar to a set, but allows for multiple
 * occurrences of an element.  The count of an element is the number of occurrences of that element in the multiset.
 * Elements with a count of zero are not contained in the multiset.
 *
 * @param <K> the type of elements in the multiset
 */
public interface Multiset<K> {

  /**
   * Adds an occurrence of the specified element to the multiset.
   *
   * @param element the element to add
   */
  void add(K element);

  /**
   * Sets the count of the specified element in the multiset.  If the count is zero or negative, the element is removed.
   *
   * @param element the element to set
   * @param count the count to set
   */
  void setCount(K element, int count);

  /**
   * Removes a single occurrence of the specified element from the multiset, if present.
   *
   * @param element the element to remove
   * @return true if an occurrence was removed, false otherwise
   */
  boolean remove(K element);

  /**
   * Gets the count of occurrences of the specified element in the multiset.
   *
   * @param element the element to count
   * @return the count of the element
   */
  int count(K element);

  /**
   * Returns an unmodifiable set view of the elements contained in this multiset.
   *
   * @return a set view of the elements contained in this multiset
   */
  Set<K> elementSet();

  /**
   * Removes all occurrences of the specified element from the multiset.
   *
   * @param element the element to remove completely
   */
  void removeAll(K element);

  /**
   * Returns the total number of occurrences of all elements in the multiset.
   *
   * @return the total count of all elements
   */
  int size();

  /**
   * @return true if the multiset contains no elements
   */
  boolean isEmpty();

  /**
   * Removes all elements from the multiset.
   */
  void clear();

  /**
   * @return a copy of the multiset
   */
  Multiset<K> copyOf();
}
