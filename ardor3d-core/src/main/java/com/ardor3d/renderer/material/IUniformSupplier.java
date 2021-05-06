/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.material;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ardor3d.renderer.material.uniform.UniformRef;

public interface IUniformSupplier {

  List<UniformRef> getUniforms();

  /**
   * Set default values for this uniform supplier. Used when we have to generate a missing supplier
   * (such as a light for an index with no light.)
   */
  void applyDefaultUniformValues();

  Set<String> nullProviders = new HashSet<>();

  static IUniformSupplier getDefaultProvider(final String className) {
    if (IUniformSupplier.nullProviders.contains(className)) {
      return null;
    }

    try {
      final Class<?> clazz = Class.forName(className);
      final Object val = clazz.getDeclaredConstructor().newInstance();
      if (val instanceof IUniformSupplier) {
        final IUniformSupplier supplier = (IUniformSupplier) val;
        supplier.applyDefaultUniformValues();
        return supplier;
      }
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    }

    IUniformSupplier.nullProviders.add(className);
    return null;
  }
}
