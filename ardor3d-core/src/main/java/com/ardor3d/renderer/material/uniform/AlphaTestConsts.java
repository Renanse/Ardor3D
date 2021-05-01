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

public final class AlphaTestConsts {
  public static final String KEY_AlphaTestType = "alphaTestType";
  public static final String KEY_AlphaReference = "alphaReference";

  public enum TestFunction {
    /**
     * Comparison always passes.
     */
    Always,
    /**
     * Comparison never passes.
     */
    Never,
    /**
     * Passes if the incoming value is the same as the reference.
     */
    EqualTo,
    /**
     * Passes if the incoming value is NOT equal to the reference.
     */
    NotEqualTo,
    /**
     * Passes if the incoming value is less than the reference.
     */
    LessThan,
    /**
     * Passes if the incoming value is less than or equal to the reference.
     */
    LessThanOrEqualTo,
    /**
     * Passes if the incoming value is greater than the reference.
     */
    GreaterThan,
    /**
     * Passes if the incoming value is greater than or equal to the reference.
     */
    GreaterThanOrEqualTo;
  }

}
