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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Locale;

import org.jdom2.Element;
import org.junit.Test;

import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.DataCache;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.resource.StringResourceSource;

/**
 * Hardening of the COLLADA import path: the SAXBuilder must refuse DOCTYPE declarations (XXE /
 * billion-laughs), and a malformed XPath query must fail loudly rather than be swallowed and cached
 * as a null that is then silently treated as "no results".
 *
 * <p>
 * (The resource stream is <em>not</em> covered here: the SAX parser closes the stream handed to
 * {@code build()} on both the success and failure paths, so there is no leak to fix - verified by
 * an earlier close-tracking probe.)
 */
public class TestColladaImportHardening {

  /** A minimal, DOCTYPE-free COLLADA document that loads cleanly. */
  private static final String BENIGN_DAE =
      "<?xml version=\"1.0\"?><COLLADA version=\"1.4.1\"><asset/></COLLADA>";

  private static boolean chainMentionsDoctype(Throwable t) {
    while (t != null) {
      final String msg = t.getMessage();
      if (msg != null && msg.toLowerCase(Locale.ROOT).contains("doctype")) {
        return true;
      }
      t = t.getCause();
    }
    return false;
  }

  @Test
  public void doctypeWithExternalEntityIsRejected() {
    // An external-entity XXE payload. The SYSTEM target need not exist: the point is that the
    // DOCTYPE declaration itself must be refused before any entity is resolved.
    final String xxe = "<?xml version=\"1.0\"?>\n" //
        + "<!DOCTYPE COLLADA [ <!ENTITY xxe SYSTEM \"file:///nonexistent-ardor3d-xxe-probe\"> ]>\n" //
        + "<COLLADA version=\"1.4.1\"><asset><contributor><author>&xxe;</author></contributor></asset></COLLADA>";
    try {
      new ColladaImporter().load(new StringResourceSource(xxe, ".dae"));
      fail("Expected the load to fail: a DOCTYPE declaration should be disallowed.");
    } catch (final Exception e) {
      assertTrue("Expected a DOCTYPE-disallowed failure, but got: " + e, chainMentionsDoctype(e));
    }
  }

  @Test
  public void benignDocumentWithoutDoctypeStillLoads() throws Exception {
    final ColladaImporter importer = new ColladaImporter();
    importer.setLoadAnimations(false);
    final ColladaStorage storage = importer.load(new StringResourceSource(BENIGN_DAE, ".dae"));
    assertNotNull("A DOCTYPE-free COLLADA document should still parse after hardening.", storage);
  }

  @Test
  public void malformedXPathQueryFailsLoudlyInsteadOfBeingSwallowed() {
    final ColladaDOMUtil util = new ColladaDOMUtil(new DataCache());
    try {
      util.selectNodes(new Element("root"), "!!! not a valid xpath [");
      fail("Expected a malformed XPath query to be reported, not swallowed and cached as null.");
    } catch (final Ardor3dException expected) {
      // good: the compile failure surfaces instead of being cached as a null that is then silently
      // treated as an empty result set.
    }
  }
}
