/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.visitor;

import java.nio.Buffer;
import java.util.Map.Entry;

import com.ardor3d.renderer.material.IShaderUtils;
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Spatial;

public class DeleteVBOsVisitor implements Visitor {
    final IShaderUtils _utils;

    public DeleteVBOsVisitor(final IShaderUtils utils) {
        _utils = utils;
    }

    public void visit(final Spatial spatial) {
        if (spatial instanceof Mesh) {
            final Mesh mesh = (Mesh) spatial;
            final MeshData meshData = mesh.getMeshData();
            for (final Entry<String, AbstractBufferData<? extends Buffer>> entry : meshData.listDataItems()) {
                _utils.deleteBuffer(entry.getValue());
            }
        }
    }
}
