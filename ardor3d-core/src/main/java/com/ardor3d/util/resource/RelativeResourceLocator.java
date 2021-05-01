/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.resource;

/**
 * This locator takes a base resource and tries to find other resources that are "relative" to it.
 * What relative means may be up to the type of ResourceSource given.
 */
public class RelativeResourceLocator implements ResourceLocator {

  private final ResourceSource _baseSource;

  /**
   * Construct a new RelativeResourceLocator using the given source as our base.
   * 
   * @param resource
   *          our base source.
   */
  public RelativeResourceLocator(final ResourceSource resource) {
    assert (resource != null) : "source may not be null.";

    _baseSource = resource;
  }

  public ResourceSource getBaseSource() { return _baseSource; }

  @Override
  public ResourceSource locateResource(String resourceName) {
    // Trim off any prepended local dir.
    while (resourceName.startsWith("./") && resourceName.length() > 2) {
      resourceName = resourceName.substring(2);
    }
    while (resourceName.startsWith(".\\") && resourceName.length() > 2) {
      resourceName = resourceName.substring(2);
    }

    return _baseSource.getRelativeSource(resourceName);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof RelativeResourceLocator) {
      return _baseSource.equals(((RelativeResourceLocator) obj)._baseSource);
    }
    return false;
  }
}
