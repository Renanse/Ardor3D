/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom.plugin;

import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

import com.ardor3d.renderer.state.CullState;
import com.ardor3d.scenegraph.Mesh;

public class GoogleEarthPlugin implements ColladaExtraPlugin {
    @SuppressWarnings("unchecked")
    @Override
    public boolean processExtra(final Element extra, final Object[] params) {
        if (params.length > 0 && params[0] instanceof Mesh) {
            final Mesh mesh = (Mesh) params[0];
            // should have a child: <technique profile="GOOGLEEARTH">
            final Element technique = extra.getChild("technique");
            if (technique != null) {
                final Attribute profile = technique.getAttribute("profile");
                if (profile != null && "GOOGLEEARTH".equalsIgnoreCase(profile.getValue())) {
                    for (final Element child : (List<Element>) technique.getChildren()) {
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
