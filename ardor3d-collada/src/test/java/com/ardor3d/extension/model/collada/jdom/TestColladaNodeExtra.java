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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.jdom2.Element;
import org.junit.Test;

import com.ardor3d.extension.model.collada.jdom.plugin.ColladaExtraPlugin;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.resource.StringResourceSource;

/**
 * A {@code <node>} in a visual scene may carry one or more {@code <extra>} elements (COLLADA 1.4.1
 * schema). Those must be handed to any registered {@link ColladaExtraPlugin} - with the freshly
 * built Ardor3D {@link Node} as context - exactly as material-level {@code <extra>} already is.
 */
public class TestColladaNodeExtra {

  /** A minimal scene whose single node carries an {@code <extra>}. */
  private static final String NODE_EXTRA_DAE = "<?xml version=\"1.0\"?>" //
      + "<COLLADA version=\"1.4.1\">" //
      + "<asset/>" //
      + "<library_visual_scenes><visual_scene id=\"scene\">" //
      + "<node id=\"n1\" name=\"n1\">" //
      + "<extra><technique profile=\"TEST\"><tag>hello</tag></technique></extra>" //
      + "</node>" //
      + "</visual_scene></library_visual_scenes>" //
      + "<scene><instance_visual_scene url=\"#scene\"/></scene>" //
      + "</COLLADA>";

  @Test
  public void nodeExtraIsHandedToPlugins() throws Exception {
    final ColladaImporter importer = new ColladaImporter();
    importer.setLoadAnimations(false);

    final AtomicReference<Element> seenExtra = new AtomicReference<>();
    final AtomicReference<Object> seenParam = new AtomicReference<>();
    importer.addExtraPlugin((extra, params) -> {
      seenExtra.set(extra);
      if (params.length > 0) {
        seenParam.set(params[0]);
      }
      return true;
    });

    importer.load(new StringResourceSource(NODE_EXTRA_DAE, ".dae"));

    assertNotNull("the node's <extra> element should have been passed to the plugin", seenExtra.get());
    assertEquals("extra", seenExtra.get().getName());
    assertTrue("the plugin should receive the built Ardor3D Node as context", seenParam.get() instanceof Node);
    assertEquals("n1", ((Node) seenParam.get()).getName());
  }
}
