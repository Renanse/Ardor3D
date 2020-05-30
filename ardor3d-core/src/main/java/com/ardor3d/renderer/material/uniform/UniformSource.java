/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.material.uniform;

public enum UniformSource {
  /**
   * Uniform Value is an Object convertible by the IShaderUtils implementation to a suitable
   * nio.Buffer.
   */
  Value,

  /**
   * Uniform Value is a String key to be used to pull an Object from the local properties of the
   * Spatial being drawn. Object returned should be convertible by the IShaderUtils implementation to
   * a suitable nio.Buffer.
   */
  SpatialProperty,

  /** Uniform Value is an RenderMatrixType enum value. */
  RendererMatrix,

  /**
   * Uniform Value is of type BiFunction<Mesh, Object, Object> returning an Object convertible by the
   * IShaderUtils implementation to a suitable nio.Buffer.
   */
  Function,

  /**
   * Uniform Value is an Ardor3dStateProperty enum value, indicating a value to pull from the current
   * state of the engine, or current spatial being drawn.
   */
  Ardor3dState,

  /**
   * Uniform value is of type Supplier<Object> returning an Object convertible by the IShaderUtils
   * implementation to a suitable nio.Buffer.
   */
  Supplier
}
