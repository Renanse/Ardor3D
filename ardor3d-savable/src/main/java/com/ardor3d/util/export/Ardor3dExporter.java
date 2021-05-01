/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface Ardor3dExporter {

  /**
   * Save a Savable object to the given stream.
   *
   * @param savable
   * @param os
   * @throws IOException
   */
  void save(Savable savable, OutputStream os) throws IOException;

  /**
   * Save a Savable object to the given file.
   *
   * @param savable
   * @param file
   * @throws IOException
   */
  void save(Savable savable, File file) throws IOException;
}
