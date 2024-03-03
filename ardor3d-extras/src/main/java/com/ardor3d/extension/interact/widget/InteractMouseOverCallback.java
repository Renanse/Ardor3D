/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.widget;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.mouse.MouseState;

public interface InteractMouseOverCallback {
  void mouseEntered(Canvas source, MouseState current, InteractManager manager);

  void mouseDeparted(Canvas source, MouseState current, InteractManager manager);
}
