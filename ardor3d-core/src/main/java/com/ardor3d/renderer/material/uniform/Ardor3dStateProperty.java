/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.material.uniform;

public enum Ardor3dStateProperty {
  /** A Float4 color pulled from Mesh._defaultColor. */
  MeshDefaultColorRGBA,

  /** A Float3 color pulled from Mesh._defaultColor. */
  MeshDefaultColorRGB,

  /** A Float3 vector pulled from current camera's position field. */
  CurrentCameraLocation,

  /**
   * A Float2 vector pulled from current camera's viewport width and height, as passed in glViewport.
   */
  CurrentViewportSizePixels,

  /**
   * A Float2 vector pulled from current camera's viewport offset x and y, as passed in glViewport.
   */
  CurrentViewportOffsetPixels,

  /**
   * Properties of the light in the current LightManager. The light index comes from the extra field
   * of the uniform, which must be of type Integer.
   */
  Light,

  /**
   * Bind a shadow texture to a given unit and return the unit to the shader.
   */
  ShadowTexture,

  /**
   * Current global ambient set on the current LightManager (from current SceneIndexer).
   */
  GlobalAmbientLight
}
