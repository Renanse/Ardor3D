/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.ardor3d.extension.model.md2.Md2Importer;
import com.ardor3d.extension.model.obj.ObjImporter;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.resource.StringResourceSource;

/**
 * Hardening of the OBJ and MD2 model importers: the resource stream must be closed after a load (no
 * handle leak - nothing else closes it, unlike the SAX-based COLLADA path), and a bad asset must
 * raise an {@link Ardor3dException} rather than a bare {@link java.lang.Error}.
 */
public class TestModelImportHardening {

  private static final String VALID_OBJ = "v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n";

  /** Wraps StringResourceSource so we can observe whether the stream it hands out gets closed. */
  private static class TrackingSource extends StringResourceSource {
    private CloseTrackingInputStream lastStream;

    TrackingSource(final String data) {
      super(data);
    }

    @Override
    public InputStream openStream() throws IOException {
      lastStream = new CloseTrackingInputStream(super.openStream());
      return lastStream;
    }
  }

  private static class CloseTrackingInputStream extends FilterInputStream {
    private boolean closed = false;

    CloseTrackingInputStream(final InputStream in) {
      super(in);
    }

    @Override
    public void close() throws IOException {
      closed = true;
      super.close();
    }
  }

  // ---- OBJ ----

  @Test
  public void objStreamIsClosedAfterLoad() {
    final TrackingSource source = new TrackingSource(VALID_OBJ);
    new ObjImporter().load(source);
    assertNotNull(source.lastStream);
    assertTrue("ObjImporter must close the resource stream it opened.", source.lastStream.closed);
  }

  @Test
  public void objMalformedContentThrowsArdor3dExceptionNotError() {
    try {
      // A face needs at least three vertices.
      new ObjImporter().load(new StringResourceSource("v 0 0 0\nv 1 0 0\nf 1 2\n", ".obj"));
      fail("Expected a malformed OBJ to raise an Ardor3dException.");
    } catch (final Ardor3dException expected) {
      // good
    }
  }

  @Test
  public void objMissingResourceThrowsArdor3dExceptionNotError() {
    try {
      new ObjImporter().load("no-such-resource-zzz.obj");
      fail("Expected a missing OBJ resource to raise an Ardor3dException.");
    } catch (final Ardor3dException expected) {
      // good
    }
  }

  // ---- MD2 ----

  @Test
  public void md2BadContentThrowsArdor3dExceptionAndClosesStream() {
    final TrackingSource source = new TrackingSource("this is plainly not an MD2 binary file");
    try {
      new Md2Importer().load(source);
      fail("Expected non-MD2 content to raise an Ardor3dException.");
    } catch (final Ardor3dException expected) {
      // good
    }
    assertNotNull(source.lastStream);
    assertTrue("Md2Importer must close the resource stream even when parsing fails.",
        source.lastStream.closed);
  }

  @Test
  public void md2MissingResourceThrowsArdor3dExceptionNotError() {
    try {
      new Md2Importer().load("no-such-resource-zzz.md2");
      fail("Expected a missing MD2 resource to raise an Ardor3dException.");
    } catch (final Ardor3dException expected) {
      // good
    }
  }
}
