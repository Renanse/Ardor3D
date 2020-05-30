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

public enum UniformType {
  /** */
  Float1,

  /** */
  Float2,

  /** */
  Float3,

  /** */
  Float4,

  /** */
  Int1,

  /** */
  Int2,

  /** */
  Int3,

  /** */
  Int4,

  /** */
  UInt1,

  /** */
  UInt2,

  /** */
  UInt3,

  /** */
  UInt4,

  /** */
  Double1,

  /** */
  Double2,

  /** */
  Double3,

  /** */
  Double4,

  /** */
  Matrix2x2,

  /** */
  Matrix2x3,

  /** */
  Matrix2x4,

  /** */
  Matrix2x2D,

  /** */
  Matrix2x3D,

  /** */
  Matrix2x4D,

  /** */
  Matrix3x2,

  /** */
  Matrix3x3,

  /** */
  Matrix3x4,

  /** */
  Matrix3x2D,

  /** */
  Matrix3x3D,

  /** */
  Matrix3x4D,

  /** */
  Matrix4x2,

  /** */
  Matrix4x3,

  /** */
  Matrix4x4,

  /** */
  Matrix4x2D,

  /** */
  Matrix4x3D,

  /** */
  Matrix4x4D,

  /** This uniform's value is an object that supplies other uniforms. */
  UniformSupplier
}
