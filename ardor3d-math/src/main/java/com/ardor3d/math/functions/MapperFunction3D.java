/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.functions;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.math.MathUtils;

/**
 * This function takes a "map" function and uses the value it returns at a certain point to look up
 * and evaluate another function from a set of ranged functions.
 */
public class MapperFunction3D implements Function3D {

  private Function3D _mapFunction;
  private final List<Entry> _entries = new ArrayList<>();
  private double _domainStart, _domainEnd;

  /**
   * Construct a mapper function using the given map function and a start and end for the domain we'll
   * use.
   *
   * @param mapFunction
   * @param domainStart
   * @param domainEnd
   */
  public MapperFunction3D(final Function3D mapFunction, final double domainStart, final double domainEnd) {
    _mapFunction = mapFunction;
    _domainStart = domainStart;
    _domainEnd = domainEnd;
  }

  @Override
  public double eval(final double x, final double y, final double z) {
    // grab a value from our map function.
    final double mappingValue = MathUtils.clamp(_mapFunction.eval(x, y, z), _domainStart, _domainEnd);

    // Walk through our entries until we get which entries we are working with (1 or 2 if we are
    // blending between
    // entries)
    Entry prev = null, current = null, next = _entries.get(0);
    double start, end = _domainStart + next.offsetStart;
    for (int i = 1; i <= _entries.size(); i++) {
      prev = current;
      current = next;
      next = i < _entries.size() ? _entries.get(i) : null;
      start = end;
      end = next != null ? end + next.offsetStart : _domainEnd;

      // check if we are in the right main interval
      if (mappingValue <= end) {
        // check if we are in the ease-in region and have a prev function.
        if (prev != null && mappingValue < start + current.easeIn) {
          // ...interpolate with a quintic S-curve.
          final double ratio = (mappingValue - start) / current.easeIn;
          final double amount = MathUtils.scurve5(ratio);
          return MathUtils.lerp(amount, prev.source.eval(x, y, z), current.source.eval(x, y, z));
        }
        // check if we are in the ease-out region and have a next function.
        else if (next != null && mappingValue > end - current.easeOut) {
          // ...interpolate with a quintic S-curve.
          final double ratio = ((mappingValue - end) / current.easeOut) + 1;
          final double amount = MathUtils.scurve5(ratio);
          return MathUtils.lerp(amount, current.source.eval(x, y, z), next.source.eval(x, y, z));
        }
        // else we are in the no-ease region (or did not have a next/prev to blend with)...
        else {
          // ...so just return source func
          return current.source.eval(x, y, z);
        }
      }
    }
    // outside of range... just return an eval of the very last function
    return current.source.eval(x, y, z);
  }

  public Function3D getMapFunction() { return _mapFunction; }

  public void setMapFunction(final Function3D mapFunction) { _mapFunction = mapFunction; }

  public double getDomainStart() { return _domainStart; }

  public void setDomainStart(final double start) { _domainStart = start; }

  public double getDomainEnd() { return _domainEnd; }

  public void setDomainEnd(final double end) { _domainEnd = end; }

  /**
   * Add a new source function to the end of our set of ranged functions. Our place in the range is
   * based on the place of the previous source function and the offsetStart provided.
   *
   * @param source
   *          the new function to add
   * @param offsetStart
   *          our offset from the previous entry
   * @param easeIn
   *          a "fade in" range between the previous function and this function, starting at
   *          offsetState. Over this range we'll lerp between the two functions.
   * @param easeOut
   *          a "fade out" range between this function and the next function, starting at the next
   *          function's offsetStart - easeOut. Over this range we'll lerp between the two functions.
   */
  public void addFunction(final Function3D source, final double offsetStart, final double easeIn,
      final double easeOut) {
    final Entry e = new Entry();
    e.source = source;
    e.offsetStart = offsetStart;
    e.easeIn = easeIn;
    e.easeOut = easeOut;
    _entries.add(e);
  }

  public void removeFunction(final int index) {
    _entries.remove(index);
  }

  public void clearFunctions() {
    _entries.clear();
  }

  private static class Entry {
    double offsetStart;
    double easeIn, easeOut;
    Function3D source;
  }

}
