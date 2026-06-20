/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export.binary;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.FloatBuffer;

import org.junit.Test;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

/**
 * The "protects real user data" test: build a representative scene graph (a transformed Node parent
 * with a transformed Mesh child whose MeshData carries vertex / normal / color buffers and an index
 * buffer), serialize it with {@link BinaryExporter}, reload with {@link BinaryImporter}, and assert
 * the hierarchy, names, transforms and every buffer survive byte-for-byte. This is the scenario the
 * serialization landmines (#3 heap-buffer desync, #4 wrong transform key) silently corrupted.
 */
public class TestBinarySavableGraphRoundTrip {

  private static float[] toArray(final FloatBuffer b) {
    final FloatBuffer dup = b.duplicate();
    dup.rewind();
    final float[] a = new float[dup.remaining()];
    dup.get(a);
    return a;
  }

  @Test
  public void testSceneGraphRoundTrips() throws Exception {
    final float[] verts = {0, 0, 0, 1, 0, 0, 1, 1, 0};
    final float[] normals = {0, 0, 1, 0, 0, 1, 0, 0, 1};
    final float[] colors = {1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1};
    final int[] indices = {0, 1, 2};

    final Node root = new Node("root");
    final Transform rootXf = new Transform();
    rootXf.setTranslation(1, 2, 3);
    rootXf.setScale(2.0);
    root.setTransform(rootXf);

    final Mesh geo = new Mesh("geo");
    final MeshData md = new MeshData();
    md.setVertexBuffer(BufferUtils.createFloatBuffer(verts));
    md.setNormalBuffer(BufferUtils.createFloatBuffer(normals));
    md.setColorBuffer(BufferUtils.createFloatBuffer(colors));
    md.setIndices(BufferUtils.createIndexBufferData(indices, 2));
    geo.setMeshData(md);
    final Transform geoXf = new Transform();
    geoXf.setTranslation(-5, 0.5, 10);
    geo.setTransform(geoXf);

    root.attachChild(geo);

    // round-trip
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    new BinaryExporter().save(root, out);
    final Node r = (Node) new BinaryImporter().load(new ByteArrayInputStream(out.toByteArray()));

    // hierarchy + names
    assertEquals("root", r.getName());
    assertEquals(1, r.getNumberOfChildren());
    final Spatial child = r.getChild(0);
    assertTrue("child must deserialize as a Mesh", child instanceof Mesh);
    assertEquals("geo", child.getName());

    // transforms (local)
    assertEquals(new Vector3(1, 2, 3), r.getTransform().getTranslation());
    assertEquals(2.0, r.getTransform().getScale().getX(), 0.0);
    assertEquals(new Vector3(-5, 0.5, 10), child.getTransform().getTranslation());

    // mesh data buffers
    final MeshData rmd = ((Mesh) child).getMeshData();
    assertArrayEquals(verts, toArray(rmd.getVertexBuffer()), 0f);
    assertArrayEquals(normals, toArray(rmd.getNormalBuffer()), 0f);
    assertArrayEquals(colors, toArray(rmd.getColorBuffer()), 0f);

    final IndexBufferData<?> ri = rmd.getIndices();
    assertEquals(indices.length, ri.getBufferLimit());
    for (int i = 0; i < indices.length; i++) {
      assertEquals(indices[i], ri.get(i));
    }
  }

  /**
   * A deeper, branching hierarchy must keep its exact shape (child counts at each level) after a
   * round-trip.
   */
  @Test
  public void testNestedHierarchyShapePreserved() throws Exception {
    final Node root = new Node("root");
    final Node branchA = new Node("a");
    final Node branchB = new Node("b");
    branchA.attachChild(new Node("a1"));
    branchA.attachChild(new Node("a2"));
    branchB.attachChild(new Node("b1"));
    root.attachChild(branchA);
    root.attachChild(branchB);

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    new BinaryExporter().save(root, out);
    final Node r = (Node) new BinaryImporter().load(new ByteArrayInputStream(out.toByteArray()));

    assertEquals(2, r.getNumberOfChildren());
    final Node ra = (Node) r.getChild(0);
    final Node rb = (Node) r.getChild(1);
    assertEquals("a", ra.getName());
    assertEquals(2, ra.getNumberOfChildren());
    assertEquals("b", rb.getName());
    assertEquals(1, rb.getNumberOfChildren());
    assertEquals("a1", ra.getChild(0).getName());
    assertEquals("a2", ra.getChild(1).getName());
    assertEquals("b1", rb.getChild(0).getName());
  }
}
