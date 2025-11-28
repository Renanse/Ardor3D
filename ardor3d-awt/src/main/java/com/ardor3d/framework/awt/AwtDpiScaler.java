/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.framework.awt;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;

/**
 * Utility for handling DPI scaling in AWT-based canvases.
 * Uses the component's GraphicsConfiguration to determine the display scale factor.
 */
public enum AwtDpiScaler {

  INSTANCE;

  /**
   * Scale a size value from logical pixels to physical screen pixels based on the
   * component's display DPI.
   *
   * @param component
   *          the component to get the scale factor from
   * @param size
   *          the logical pixel size
   * @return the scaled physical pixel size
   */
  public double scaleToScreenDpi(final Component component, final double size) {
    if (!ApplyScale) {
      return size;
    }
    final double scale = getScaleFactor(component);
    return size * scale;
  }

  /**
   * Scale a size value from physical screen pixels to logical pixels based on the
   * component's display DPI.
   *
   * @param component
   *          the component to get the scale factor from
   * @param size
   *          the physical pixel size
   * @return the unscaled logical pixel size
   */
  public double scaleFromScreenDpi(final Component component, final double size) {
    if (!ApplyScale) {
      return size;
    }
    final double scale = getScaleFactor(component);
    return size / scale;
  }

  /**
   * Get the display scale factor for a component.
   *
   * @param component
   *          the component to query
   * @return the scale factor (1.0 for standard DPI, 2.0 for 200% scaling, etc.)
   */
  public double getScaleFactor(final Component component) {
    if (component == null) {
      return 1.0;
    }
    final GraphicsConfiguration gc = component.getGraphicsConfiguration();
    if (gc == null) {
      return 1.0;
    }
    final AffineTransform transform = gc.getDefaultTransform();
    if (transform == null) {
      return 1.0;
    }
    // Use X scale - typically X and Y scale are the same
    final double scale = transform.getScaleX();
    // Guard against degenerate values
    if (!Double.isFinite(scale) || scale <= 0) {
      return 1.0;
    }
    return scale;
  }

  public int scaleToScreenDpiInt(final Component component, final double size) {
    return (int) Math.round(scaleToScreenDpi(component, size));
  }

  public int scaleFromScreenDpiInt(final Component component, final double size) {
    return (int) Math.round(scaleFromScreenDpi(component, size));
  }

  /**
   * Whether to apply DPI scaling. Defaults to true, but disabled on macOS
   * where AWT typically handles scaling automatically.
   */
  public static boolean ApplyScale = true;

  static {
    final String os = System.getProperty("os.name").toLowerCase();
    if (os.startsWith("mac os x")) {
      ApplyScale = false;
    }
  }
}
