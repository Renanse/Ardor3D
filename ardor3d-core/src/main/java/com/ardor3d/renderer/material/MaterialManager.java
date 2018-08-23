/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.material;

import java.io.IOException;
import java.util.List;

import com.ardor3d.renderer.material.uniform.RenderStateProperty;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.resource.ResourceLocatorTool;

public enum MaterialManager {

    INSTANCE;

    private RenderMaterial _defaultMaterial;
    private final String DEFAULT_MATERIAL_FILE = "com/ardor3d/renderer/default_material.txt";

    private MaterialManager() {
        // Load our default Material
        _defaultMaterial = LoadMaterial(DEFAULT_MATERIAL_FILE);
    }

    private RenderMaterial LoadMaterial(final String file) {
        final RenderMaterial material = new RenderMaterial();

        final MaterialTechnique e = new MaterialTechnique();
        material.getTechniques().add(e);

        final TechniquePass pass = new TechniquePass();
        e.getPasses().add(pass);

        pass.addDefaultPositionAttribute();
        pass.addDefaultColorAttribute();
        pass.addDefaultTextureCoordsAttribute(0);

        pass.addDefaultMatrixUniforms();
        pass.addUniform(new UniformRef("defaultColor", UniformType.Float4, UniformSource.RenderState,
                RenderStateProperty.MeshDefaultColorRGBA));

        try {
            pass.setShader(ShaderType.Vertex, ResourceLocatorTool.getClassPathResourceAsString(MaterialManager.class,
                    "com/ardor3d/renderer/default.vert"));
            pass.setShader(ShaderType.Fragment, ResourceLocatorTool.getClassPathResourceAsString(MaterialManager.class,
                    "com/ardor3d/renderer/default.frag"));
        } catch (final IOException ex) {
            ex.printStackTrace();
        }

        return material;
    }

    public void setDefaultMaterial(final RenderMaterial material) {
        _defaultMaterial = material;
    }

    public RenderMaterial getDefaultMaterial() {
        return _defaultMaterial;
    }

    public MaterialTechnique chooseTechnique(final Mesh mesh) {

        // find the local or inherited material for the given mesh
        RenderMaterial material = mesh.getWorldRenderMaterial();

        // if we have no material, use any set default
        material = _defaultMaterial;

        // look for the technique to use in the material
        return chooseTechnique(mesh, material);
    }

    private MaterialTechnique chooseTechnique(final Mesh mesh, final RenderMaterial material) {
        if (material == null || material.getTechniques().isEmpty()) {
            return null;
        }

        // pull out our techniques
        final List<MaterialTechnique> techniques = material.getTechniques();

        // if we have just one technique, return that
        if (techniques.size() == 1) {
            return techniques.get(0);
        }

        MaterialTechnique best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < techniques.size(); i++) {
            final MaterialTechnique tech = techniques.get(i);
            final int score = tech.getScore(mesh);
            if (score > bestScore) {
                best = tech;
                bestScore = score;
            }
        }

        return best;

    }

}
