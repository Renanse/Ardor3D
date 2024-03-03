/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.collada.jdom.plugin;

import org.jdom2.Attribute;
import org.jdom2.Element;

import com.ardor3d.renderer.state.CullState;
import com.ardor3d.scenegraph.Mesh;

public class GoogleEarthPlugin implements ColladaExtraPlugin {

  @Override
  public boolean processExtra(final Element extra, final Object[] params) {
    if (params.length > 0 && params[0] instanceof Mesh) {
      final Mesh mesh = (Mesh) params[0];
      // should have a child: <technique profile="GOOGLEEARTH">
      final Element technique = extra.getChild("technique");
      if (technique != null) {
        final Attribute profile = technique.getAttribute("profile");
        if (profile != null && "GOOGLEEARTH".equalsIgnoreCase(profile.getValue())) {
          for (final Element child : technique.getChildren()) {
            // disable back face culling if it's been enabled.
            if ("double_sided".equalsIgnoreCase(child.getName()) && "1".equals(child.getTextTrim())) {
              final CullState cs = new CullState();
              cs.setEnabled(false);
              mesh.setRenderState(cs);
            }
          }
          return true;
        }
      }
    }
    return false;
  }
}
