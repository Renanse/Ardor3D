/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.util.awt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import com.ardor3d.image.Image;
import com.ardor3d.input.mouse.MouseCursor;

/**
 * Generates crisp, high-contrast manipulation cursors - a four-way move arrow, a diagonal resize
 * arrow and a curved rotate arrow - as {@link MouseCursor}s, drawn with Java2D so they antialias
 * cleanly and need no bundled image assets. Each is a white shape with a dark outline, sized and
 * proportioned to read on any background (a thin outline-only cursor is easily mistaken for a
 * missing pointer). Intended for the interact gizmos (translate/scale/rotate) but usable anywhere a
 * manipulation cursor is wanted.
 * <p>
 * Each accessor returns a cached, shared instance, so repeated calls hand back the same
 * {@link MouseCursor} - keeping a downstream cursor cache (e.g. the GLFW mouse manager's) keyed on a
 * stable object.
 */
public final class CursorFactory {

  /** Cursor image size, in pixels. The hotspot is the center. */
  public static final int SIZE = 40;

  private static final Color FILL = Color.WHITE;
  private static final Color OUTLINE = new Color(0, 0, 0, 235);
  private static final float OUTLINE_WIDTH = 2f;

  private static MouseCursor MOVE;
  private static MouseCursor SCALE;
  private static MouseCursor ROTATE;

  private CursorFactory() {}

  /** A four-way (up/down/left/right) move arrow - the translate-gizmo cursor. */
  public static synchronized MouseCursor move() {
    if (CursorFactory.MOVE == null) {
      CursorFactory.MOVE = CursorFactory.fromShape("gizmoMove", CursorFactory.moveShape());
    }
    return CursorFactory.MOVE;
  }

  /** A diagonal double-headed resize arrow - the scale-gizmo cursor. */
  public static synchronized MouseCursor scale() {
    if (CursorFactory.SCALE == null) {
      CursorFactory.SCALE = CursorFactory.fromShape("gizmoScale", CursorFactory.scaleShape());
    }
    return CursorFactory.SCALE;
  }

  /** A curved double-headed rotate arrow - the rotate-gizmo cursor. */
  public static synchronized MouseCursor rotate() {
    if (CursorFactory.ROTATE == null) {
      CursorFactory.ROTATE = CursorFactory.fromShape("gizmoRotate", CursorFactory.rotateShape());
    }
    return CursorFactory.ROTATE;
  }

  /** The four-way arrow, centered. Arrowheads kept well apart so the outline never closes the
   * notches between arms into a solid diamond. */
  private static Shape moveShape() {
    final int r = 18, ab = 12, hh = 6, sh = 3;
    final int[][] pts = {{0, -r}, {hh, -ab}, {sh, -ab}, {sh, -sh}, {ab, -sh}, {ab, -hh}, {r, 0}, {ab, hh}, {ab, sh},
        {sh, sh}, {sh, ab}, {hh, ab}, {0, r}, {-hh, ab}, {-sh, ab}, {-sh, sh}, {-ab, sh}, {-ab, hh}, {-r, 0},
        {-ab, -hh}, {-ab, -sh}, {-sh, -sh}, {-sh, -ab}, {-hh, -ab}};
    return CursorFactory.polygon(pts, null);
  }

  /** A horizontal double-headed arrow rotated 45 degrees into a diagonal resize arrow. */
  private static Shape scaleShape() {
    final int r = 17, ab = 9, ah = 6, sh = 3;
    final int[][] pts = {{-r, 0}, {-ab, -ah}, {-ab, -sh}, {ab, -sh}, {ab, -ah}, {r, 0}, {ab, ah}, {ab, sh}, {-ab, sh},
        {-ab, ah}};
    return CursorFactory.polygon(pts, AffineTransform.getRotateInstance(Math.PI / 4, CursorFactory.SIZE / 2.0,
        CursorFactory.SIZE / 2.0));
  }

  /** A ~240-degree arc band with an arrowhead at each end, tangent to the arc. */
  private static Shape rotateShape() {
    final double radius = 12, band = 4.5, headHalf = 6, tipSweep = Math.toRadians(30);
    final double start = 60, extent = 240; // degrees, Arc2D convention (measured ccw in math space)
    final double c = CursorFactory.SIZE / 2.0;
    final Arc2D arc = new Arc2D.Double(c - radius, c - radius, 2 * radius, 2 * radius, start, extent, Arc2D.OPEN);
    final Area area = new Area(
        new BasicStroke((float) band, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND).createStrokedShape(arc));
    final double startRad = Math.toRadians(start), endRad = Math.toRadians(start + extent);
    area.add(new Area(CursorFactory.arrowhead(c, radius, startRad, startRad - tipSweep, headHalf)));
    area.add(new Area(CursorFactory.arrowhead(c, radius, endRad, endRad + tipSweep, headHalf)));
    return area;
  }

  /**
   * A triangular arrowhead straddling the arc at {@code baseAngle} (base corners at radius +/-
   * headHalf) with its tip swept along the arc to {@code tipAngle}. Angles are in the Arc2D
   * convention (device y is flipped), so screen y uses {@code -sin}.
   */
  private static Shape arrowhead(final double c, final double radius, final double baseAngle, final double tipAngle,
      final double headHalf) {
    final Path2D.Double p = new Path2D.Double();
    p.moveTo(c + (radius - headHalf) * Math.cos(baseAngle), c - (radius - headHalf) * Math.sin(baseAngle));
    p.lineTo(c + (radius + headHalf) * Math.cos(baseAngle), c - (radius + headHalf) * Math.sin(baseAngle));
    p.lineTo(c + radius * Math.cos(tipAngle), c - radius * Math.sin(tipAngle));
    p.closePath();
    return p;
  }

  /** Build a closed polygon from center-relative integer points, optionally transformed. */
  private static Shape polygon(final int[][] pts, final AffineTransform xf) {
    final double c = CursorFactory.SIZE / 2.0;
    final Path2D.Double path = new Path2D.Double();
    path.moveTo(c + pts[0][0], c + pts[0][1]);
    for (int i = 1; i < pts.length; i++) {
      path.lineTo(c + pts[i][0], c + pts[i][1]);
    }
    path.closePath();
    return xf == null ? path : xf.createTransformedShape(path);
  }

  /** Rasterize a shape as a white fill with a dark outline into a center-hotspot cursor. */
  private static MouseCursor fromShape(final String name, final Shape shape) {
    final BufferedImage img = new BufferedImage(CursorFactory.SIZE, CursorFactory.SIZE, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(CursorFactory.FILL);
    g.fill(shape);
    g.setStroke(new BasicStroke(CursorFactory.OUTLINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.setColor(CursorFactory.OUTLINE);
    g.draw(shape);
    g.dispose();

    final Image image = AWTImageLoader.makeArdor3dImage(img, false);
    return new MouseCursor(name, image, CursorFactory.SIZE / 2, CursorFactory.SIZE / 2);
  }
}
