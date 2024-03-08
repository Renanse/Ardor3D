/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

import java.util.NoSuchElementException;

/**
 * An abstract base class for implementing the {@link PeekingIterator} interface.
 * Idea and code borrowed from Google Guava.
 * 
 * @param <T> the type of elements returned by this iterator
 */
public abstract class AbstractIterator<T> implements PeekingIterator<T> {
  private State state = State.NOT_READY;

  protected AbstractIterator() {}

  private enum State {
    /** We have computed the next element and haven't returned it yet. */
    READY,

    /** We haven't yet computed or have already returned the element. */
    NOT_READY,

    /** We have reached the end of the data and are finished. */
    DONE,

    /** We've suffered an exception and are kaput. */
    FAILED,
  }
  
  private T next;

  protected abstract T computeNext();

  protected final T endOfData() {
    state = State.DONE;
    return null;
  }

  @Override
  public final boolean hasNext() {
    if (state == State.FAILED) throw new IllegalStateException();
    return switch (state) {
      case DONE -> false;
      case READY -> true;
      default -> tryToComputeNext();
    };
  }

  private boolean tryToComputeNext() {
    state = State.FAILED;
    next = computeNext();
    if (state != State.DONE) {
      state = State.READY;
      return true;
    }
    return false;
  }

  public final T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    state = State.NOT_READY;
    T result = next;
    next = null;
    return result;
  }

  public final T peek() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return next;
  }

  public final void remove() {
    throw new UnsupportedOperationException();
  }
}
