package com.ardor3d.util.collection;

import com.ardor3d.input.mouse.MouseButton;

import java.util.Set;

public final class ImmutableMultiset<K> implements Multiset<K> {
  private final Multiset<K> _multiset;

  private static final ImmutableMultiset<?> EMPTY = new ImmutableMultiset<>(new SimpleMultiset<>());

  public ImmutableMultiset(Multiset<K> multiset) {
    _multiset = multiset.copyOf();
  }

  /**
   * Creates an empty immutable multiset.
   */
  public static <T> ImmutableMultiset<T> of() {
    @SuppressWarnings("unchecked")
    var t = (ImmutableMultiset<T>) EMPTY;
    return t;
  }

  /**
   * Creates an immutable view of the given multiset
   */
  public static <T> ImmutableMultiset<T> of(Multiset<T> multiset) {
    if (multiset.isEmpty()) return of();

    return new ImmutableMultiset<>(multiset);
  }

  @Override
  public void add(K element) {
    throw new UnsupportedOperationException("This multiset is immutable.");
  }

  @Override
  public void setCount(K element, int count) {
    throw new UnsupportedOperationException("This multiset is immutable.");
  }

  @Override
  public boolean remove(K element) {
    throw new UnsupportedOperationException("This multiset is immutable.");
  }

  @Override
  public int count(K element) {
    return _multiset.count(element);
  }

  @Override
  public Set<K> elementSet() {
    return _multiset.elementSet();
  }

  @Override
  public void removeAll(K element) {
    throw new UnsupportedOperationException("This multiset is immutable.");
  }

  @Override
  public int size() {
    return _multiset.size();
  }

  @Override
  public boolean isEmpty() {
    return _multiset.isEmpty();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("This multiset is immutable.");
  }

  @Override
  public Multiset<K> copyOf() {
    return new ImmutableMultiset<K>(_multiset);
  }
}
