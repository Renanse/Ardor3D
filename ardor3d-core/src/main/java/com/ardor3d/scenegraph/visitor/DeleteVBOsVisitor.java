/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.visitor;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Spatial;

public class DeleteVBOsVisitor implements Visitor {
    final Renderer _deleter;

    public DeleteVBOsVisitor(final Renderer deleter) {
        _deleter = deleter;
    }

    public void visit(final Spatial spatial) {
        if (spatial instanceof Mesh) {
            final Mesh mesh = (Mesh) spatial;
            final MeshData meshData = mesh.getMeshData();
            for (final AbstractBufferData<?> buff : meshData.listBufferDataItems()) {
                _deleter.deleteVBOs(buff);
            }
        }
    }
}
