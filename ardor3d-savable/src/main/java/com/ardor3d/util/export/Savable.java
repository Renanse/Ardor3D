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

import java.io.IOException;

public interface Savable {
  void write(OutputCapsule capsule) throws IOException;

  void read(InputCapsule capsule) throws IOException;

  Class<?> getClassTag();
}
