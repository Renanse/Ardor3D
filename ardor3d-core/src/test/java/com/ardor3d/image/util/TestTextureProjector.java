/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.renderer.Camera.ProjectionMode;

public class TestTextureProjector {

  private static final double EPSILON = 1e-9;

  /**
   * The texture matrix must take a world position to projective texture coordinates when applied the
   * way every other camera matrix is, M * v: the projector's centre line lands on (0.5, 0.5), the
   * frustum edges on 0 and 1, and the near and far planes on depth 0 and 1. The buggy implementation
   * still assembled view * projection * bias - the order for row vectors - from camera matrices that
   * are laid out for column vectors, so the result mapped nothing correctly under either convention.
   */
  @Test
  public void testOrthographicTextureMatrixMapsWorldToTextureSpace() {
    // looks down -Z from (10, 20, 30); 8 wide, 4 high, near 1, far 2
    final TextureProjector projector = new TextureProjector();
    projector.setAxes(new Vector3(-1, 0, 0), new Vector3(0, 1, 0), new Vector3(0, 0, -1));
    projector.setProjectionMode(ProjectionMode.Orthographic);
    projector.setFrustum(1, 2, -4, 4, 2, -2);
    projector.setLocation(new Vector3(10, 20, 30));

    final Matrix4 texMat = new Matrix4();
    projector.updateTextureMatrix(texMat);

    // on the centre line, halfway between near and far
    assertTexCoords(texMat, new Vector3(10, 20, 28.5), 0.5, 0.5, 0.5);
    // a quarter of the width to the projector's right, a quarter of the height up
    assertTexCoords(texMat, new Vector3(12, 21, 28.5), 0.75, 0.75, 0.5);
    // the left/bottom corner of the near plane, the right/top corner of the far plane
    assertTexCoords(texMat, new Vector3(6, 18, 29), 0.0, 0.0, 0.0);
    assertTexCoords(texMat, new Vector3(14, 22, 28), 1.0, 1.0, 1.0);
  }

  /** As above, for a perspective projector, where the divide by q matters. */
  @Test
  public void testPerspectiveTextureMatrixMapsWorldToTextureSpace() {
    // at the origin looking down -Z, 90 degree field of view, square
    final TextureProjector projector = new TextureProjector();
    projector.setAxes(new Vector3(-1, 0, 0), new Vector3(0, 1, 0), new Vector3(0, 0, -1));
    projector.setFrustumPerspective(90.0, 1.0, 1.0, 100.0);
    projector.setLocation(new Vector3(0, 0, 0));

    final Matrix4 texMat = new Matrix4();
    projector.updateTextureMatrix(texMat);

    final Vector4 center = texMat.applyPost(new Vector4(0, 0, -10, 1), null);
    assertEquals(0.5, center.getX() / center.getW(), EPSILON);
    assertEquals(0.5, center.getY() / center.getW(), EPSILON);

    // at 10 units out the frustum is 20 across: 5 to the right is three quarters of the way over
    final Vector4 right = texMat.applyPost(new Vector4(5, 0, -10, 1), null);
    assertEquals(0.75, right.getX() / right.getW(), EPSILON);
    assertEquals(0.5, right.getY() / right.getW(), EPSILON);

    final Vector4 down = texMat.applyPost(new Vector4(0, -5, -10, 1), null);
    assertEquals(0.5, down.getX() / down.getW(), EPSILON);
    assertEquals(0.25, down.getY() / down.getW(), EPSILON);
  }

  private static void assertTexCoords(final Matrix4 texMat, final Vector3 world, final double s, final double t,
      final double r) {
    final Vector4 coords = texMat.applyPost(new Vector4(world.getX(), world.getY(), world.getZ(), 1), null);
    assertEquals("s for " + world, s, coords.getX() / coords.getW(), EPSILON);
    assertEquals("t for " + world, t, coords.getY() / coords.getW(), EPSILON);
    assertEquals("r for " + world, r, coords.getZ() / coords.getW(), EPSILON);
  }
}
