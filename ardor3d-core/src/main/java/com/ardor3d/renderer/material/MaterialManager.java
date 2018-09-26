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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.renderer.material.reader.YamlMaterialReader;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

public enum MaterialManager {

    INSTANCE;

    /** Our class logger */
    private static final Logger logger = Logger.getLogger(MaterialManager.class.getName());

    private RenderMaterial _defaultMaterial = null;
    private final Map<ResourceSource, RenderMaterial> _materialCache = new HashMap<>();

    public void setDefaultMaterial(final RenderMaterial material) {
        _defaultMaterial = material;
    }

    public RenderMaterial getDefaultMaterial() {
        return _defaultMaterial;
    }

    public RenderMaterial findMaterial(final String materialUrl) {
        final ResourceSource key = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MATERIAL, materialUrl);
        if (key == null) {
            return null;
        }

        RenderMaterial mat = _materialCache.get(key);
        if (mat == null) {
            mat = YamlMaterialReader.load(key);
            if (mat != null) {
                _materialCache.put(key, mat);
            }
        }

        return mat;
    }

    public MaterialTechnique chooseTechnique(final Mesh mesh) {

        // find the local or inherited material for the given mesh
        RenderMaterial material = mesh.getWorldRenderMaterial();

        // if we have no material, use any set default
        if (material == null) {
            material = _defaultMaterial;
        }

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

    public static String ImportMarker = "@import";

    public static String inflateShaderImports(final String shaderText) {
        return inflateShaderImports(shaderText, new Stack<>());
    }

    protected static String inflateShaderImports(final String shaderText, final Stack<String> history) {
        final StringBuilder builder = new StringBuilder();
        // Read Standard Input:
        try (final BufferedReader in = new BufferedReader(new StringReader(shaderText))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(ImportMarker)) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("found import: " + line);
                    }
                    final String sourceUrl = line.substring(ImportMarker.length()).trim();
                    final String text = getShaderText(sourceUrl, true, history);
                    if (text != null) {
                        builder.append(text);
                    }
                } else {
                    builder.append(line);
                }
                builder.append("\n");
            }
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        return builder.toString();
    }

    public static String getShaderText(final String sourceUrl, final boolean processImports) {
        return getShaderText(sourceUrl, processImports, new Stack<>());
    }

    protected static String getShaderText(final String sourceUrl, final boolean processImports,
            final Stack<String> history) {
        if (history.contains(sourceUrl)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("already seen: " + sourceUrl);
            }
            return null;
        }

        final ResourceSource src = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_SHADER, sourceUrl);
        if (src == null) {
            return null;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("loaded shader: " + sourceUrl);
        }

        final CharBuffer buf = CharBuffer.allocate(2048);
        String text = null;
        try (final Reader reader = new InputStreamReader(src.openStream())) {
            final StringBuilder build = new StringBuilder();
            while (reader.read(buf) != -1) {
                buf.flip();
                build.append(buf);
                buf.clear();
            }
            text = build.toString();
        } catch (final IOException ex) {
            logger.logp(Level.SEVERE, MaterialManager.class.getName(), "getShaderText(String, boolean)",
                    "Failed to read a shader source: " + sourceUrl + " Error: " + ex.getMessage());
            ex.printStackTrace();
        }

        if (text != null && processImports) {
            history.push(sourceUrl);
            text = inflateShaderImports(text, history);
            history.pop();
        }

        return text;
    }

}
