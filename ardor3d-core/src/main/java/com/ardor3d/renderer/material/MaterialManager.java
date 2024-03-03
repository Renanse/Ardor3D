/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
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

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.RenderPhase;
import com.ardor3d.renderer.material.reader.YamlMaterialReader;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.Constants;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

public enum MaterialManager {

  INSTANCE;

  /** Our class logger */
  private static final Logger logger = Logger.getLogger(MaterialManager.class.getName());

  private RenderMaterial _defaultMaterial = null;
  private RenderMaterial _defaultOccluderMaterial = null;
  private final Map<ResourceSource, RenderMaterial> _materialCache = new HashMap<>();

  public void setDefaultMaterial(final RenderMaterial material) { _defaultMaterial = material; }

  public RenderMaterial getDefaultMaterial() { return _defaultMaterial; }

  public void setDefaultOccluderMaterial(final RenderMaterial material) { _defaultOccluderMaterial = material; }

  public RenderMaterial getDefaultOccluderMaterial() { return _defaultOccluderMaterial; }

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

  public MaterialTechnique chooseTechnique(final Mesh mesh, final RenderPhase phase) {

    // find the local or inherited material for the given mesh
    final RenderContext context = ContextManager.getCurrentContext();
    RenderMaterial material = context.getEnforcedMaterial();
    if (material == null) {
      if (phase == RenderPhase.Scene) {
        material = mesh.getWorldRenderMaterial();
      } else if (phase == RenderPhase.ShadowTexture) {
        material = mesh.getWorldOccluderMaterial();
        if (material == null) {
          material = _defaultOccluderMaterial;

          return chooseTechnique(mesh, material);
        }
      }
    }

    // if we have no material and we enable guessing materials, try that first.
    if (material == null && !Constants.ignoreMissingMaterials) {
      MaterialManager.logger.warning(() -> "Mesh " + mesh + " missing material.  Auto-guessing.");
      MaterialUtil.autoMaterials(mesh);
      material = mesh.getWorldRenderMaterial();
    }

    // if we still have no material, use any set default
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

  public static final String IMPORT_MARKER = "@import";

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
        if (line.startsWith(MaterialManager.IMPORT_MARKER)) {
          if (MaterialManager.logger.isLoggable(Level.FINE)) {
            MaterialManager.logger.fine("found import: " + line);
          }
          final String sourceUrl = line.substring(MaterialManager.IMPORT_MARKER.length()).trim();
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
      if (MaterialManager.logger.isLoggable(Level.FINE)) {
        MaterialManager.logger.fine("already seen: " + sourceUrl);
      }
      return null;
    }

    final ResourceSource src = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_SHADER, sourceUrl);
    if (src == null) {
      return null;
    }

    if (MaterialManager.logger.isLoggable(Level.FINE)) {
      MaterialManager.logger.fine("loaded shader: " + sourceUrl);
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
      MaterialManager.logger.logp(Level.SEVERE, MaterialManager.class.getName(), "getShaderText(String, boolean)",
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

  public static String inject(String program, final Iterable<String> injects) {
    if (program == null) {
      return null;
    }

    program = program.trim();
    if (injects == null || !program.startsWith("#version")) {
      return program;
    }

    final StringBuilder sb = new StringBuilder();
    int firstLF = program.indexOf('\n');
    if (firstLF == -1) {
      firstLF = program.indexOf('\r');
    }
    sb.append(program.substring(0, firstLF));
    sb.append('\n');
    injects.forEach((final String inj) -> sb.append(inj).append('\n'));
    sb.append(program.substring(firstLF + 1));
    return sb.toString();
  }

  public static String inject(String program, final String line) {
    if (program == null) {
      return null;
    }

    program = program.trim();
    if (line == null || !program.startsWith("#version")) {
      return program;
    }

    final StringBuilder sb = new StringBuilder();
    int firstLF = program.indexOf('\n');
    if (firstLF == -1) {
      firstLF = program.indexOf('\r');
    }
    sb.append(program.substring(0, firstLF));
    sb.append('\n');
    sb.append(line);
    sb.append('\n');
    sb.append(program.substring(firstLF + 1));
    return sb.toString();
  }

  /**
   * Clear the contents of the material cache
   */
  public void clearMaterialCache() {
    _materialCache.clear();
  }

  /**
   * Remove a specific material from the cache.
   * Useful for editing a material or shader and re-loading at runtime.
   * @return true if the material was successfully removed from the cache.
   */
  public boolean clearMaterialCacheItem(final String materialUrl) {
    final ResourceSource key = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MATERIAL, materialUrl);
    if (key == null) {
      return false;
    }
    if (!_materialCache.containsKey(key)) {
      return false;
    }
    var value = _materialCache.get(key);
    return _materialCache.remove(key, value);
  }
}
