/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export.binary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.ardor3d.util.export.Savable;

/**
 * Simple utility class that uses BinaryImporter/Exporter in memory to clone a spatial.
 */
public class BinaryCloner {
  @SuppressWarnings("unchecked")
  public <T extends Savable> T copy(final T source) {
    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      final BinaryExporter exporter = new BinaryExporter();
      exporter.save(source, bos);
      bos.flush();
      final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      final BinaryImporter importer = new BinaryImporter();
      return (T) importer.load(bis);
    } catch (final IOException ex) {
      // should not happen, since we are dealing with only byte array streams.
      throw new RuntimeException(ex);
    }
  }

}
