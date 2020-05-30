/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.client.functions;

import java.nio.ByteBuffer;

public interface SourceCacheFunction {

  void doConversion(final ByteBuffer sourceData, final byte[] store, final int destX, final int destY,
      final int dataSize, final int tileSize);

}
