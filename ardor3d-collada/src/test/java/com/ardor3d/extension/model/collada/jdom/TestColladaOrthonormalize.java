/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.collada.jdom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.resource.StringResourceSource;

/**
 * Some exporters (e.g. Autodesk FBX-to-Collada, OpenCollada) bake enough rounding error into a
 * {@code <node>}'s {@code <matrix>} that its rotational 3x3 block is no longer orthonormal, which
 * trips Ardor3D's "non-rotational matrices" handling. {@link ColladaImporter#setOrthonormalizeTransforms(boolean)}
 * cleans that up at import time while preserving translation.
 */
public class TestColladaOrthonormalize {

  /**
   * A minimal scene whose single node carries a near-rotation {@code <matrix>}: the upper-left 3x3 is
   * a slightly skewed identity (off by more than ZERO_TOLERANCE) and the last column is a (5, 6, 7)
   * translation. COLLADA matrices are row-major.
   */
  private static final String SKEWED_MATRIX_DAE = "<?xml version=\"1.0\"?>" //
      + "<COLLADA version=\"1.4.1\">" //
      + "<asset/>" //
      + "<library_visual_scenes><visual_scene id=\"scene\">" //
      + "<node id=\"n1\" name=\"n1\">" //
      + "<matrix>" //
      + "1 0.02 0 5 " //
      + "0 1 0.03 6 " //
      + "0.04 0 1 7 " //
      + "0 0 0 1" //
      + "</matrix>" //
      + "</node>" //
      + "</visual_scene></library_visual_scenes>" //
      + "<scene><instance_visual_scene url=\"#scene\"/></scene>" //
      + "</COLLADA>";

  private static ReadOnlyTransform loadNodeTransform(final boolean orthonormalize) throws Exception {
    final ColladaImporter importer = new ColladaImporter() //
        .setLoadAnimations(false) //
        .setOrthonormalizeTransforms(orthonormalize);
    final ColladaStorage storage = importer.load(new StringResourceSource(SKEWED_MATRIX_DAE, ".dae"));
    final Node node = (Node) storage.getScene().getChild("n1");
    return node.getTransform();
  }

  @Test
  public void offByDefault_leavesTheNonRotationalMatrixAlone() throws Exception {
    final ReadOnlyTransform t = loadNodeTransform(false);
    // The skewed 3x3 is not orthonormal, so the Transform reports it is not a rotation matrix.
    assertFalse(t.isRotationMatrix());
    assertFalse(t.getMatrix().isOrthonormal());
  }

  @Test
  public void whenEnabled_orthonormalizesRotationButKeepsTranslation() throws Exception {
    final ReadOnlyTransform t = loadNodeTransform(true);
    assertTrue("rotation block should have been orthonormalized", t.isRotationMatrix());
    assertTrue(t.getMatrix().isOrthonormal());
    // the translation column must be passed through untouched.
    assertEquals(new Vector3(5, 6, 7), t.getTranslation());
  }
}
