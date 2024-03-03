/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

/**
 *
 */

package com.ardor3d.scenegraph.hint;

/**
 * Describes how a scene object interacts with Ardor3D's frustum culling.
 */
public enum CullHint {

  /**
   * Do whatever our parent does. If no parent, we'll default to dynamic.
   */
  Inherit,

  /**
   * Do not draw if we are not at least partially within the view frustum of the renderer's camera.
   */
  Dynamic,

  /**
   * Always cull this from view.
   */
  Always,

  /**
   * Never cull this from view. Note that we will still get culled if our parent is culled.
   */
  Never;
}
