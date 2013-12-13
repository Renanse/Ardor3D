/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.obj;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.GeometryTool;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.google.common.collect.Lists;

/**
 * Wavefront OBJ importer. See <a href="http://local.wasp.uwa.edu.au/~pbourke/dataformats/obj/">the format spec</a>
 */
public class ObjImporter {
    private static final Logger logger = Logger.getLogger(ObjImporter.class.getName());

    private boolean _loadTextures = true;
    private ResourceLocator _textureLocator;
    private ResourceLocator _modelLocator;
    private ResourceLocator _materialLocator;

    private float _specularMax = 200;

    // texture defaults
    private MinificationFilter _minificationFilter = MinificationFilter.Trilinear;
    private boolean _useCompression = true;
    private boolean _flipTextureVertically = true;

    public boolean isLoadTextures() {
        return _loadTextures;
    }

    public ObjImporter setLoadTextures(final boolean loadTextures) {
        _loadTextures = loadTextures;
        return this;
    }

    public ObjImporter setTextureLocator(final ResourceLocator locator) {
        _textureLocator = locator;
        return this;
    }

    public ObjImporter setModelLocator(final ResourceLocator locator) {
        _modelLocator = locator;
        return this;
    }

    public ObjImporter setMaterialLocator(final ResourceLocator locator) {
        _materialLocator = locator;
        return this;
    }

    public void setFlipTextureVertically(final boolean flipTextureVertically) {
        _flipTextureVertically = flipTextureVertically;
    }

    public boolean isFlipTextureVertically() {
        return _flipTextureVertically;
    }

    public void setUseCompression(final boolean useCompression) {
        _useCompression = useCompression;
    }

    public boolean isUseCompression() {
        return _useCompression;
    }

    public void setMinificationFilter(final MinificationFilter minificationFilter) {
        _minificationFilter = minificationFilter;
    }

    public MinificationFilter getMinificationFilter() {
        return _minificationFilter;
    }

    public float getObjSpecularMax() {
        return _specularMax;
    }

    public void setObjSpecularMax(final float max) {
        _specularMax = max;
    }

    /**
     * Reads a Wavefront OBJ file from the given resource
     * 
     * @param resource
     *            the name of the resource to find.
     * @return an ObjGeometryStore data object containing the scene and other useful elements.
     */
    public ObjGeometryStore load(final String resource) {
        final ResourceSource source;
        if (_modelLocator == null) {
            source = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, resource);
        } else {
            source = _modelLocator.locateResource(resource);
        }

        if (source == null) {
            throw new Error("Unable to locate '" + resource + "'");
        }

        return load(source);
    }

    /**
     * Reads a Wavefront OBJ file from the given resource
     * 
     * @param resource
     *            the name of the resource to find.
     * @return an ObjGeometryStore data object containing the scene and other useful elements.
     */
    public ObjGeometryStore load(final ResourceSource resource) {
        return load(resource, new GeometryTool());
    }

    /**
     * Reads a Wavefront OBJ file from the given resource
     * 
     * @param resource
     *            the name of the resource to find.
     * @param geometryTool
     *            the geometry tool used to minimize the vertex count.
     * @return an ObjGeometryStore data object containing the scene and other useful elements.
     */
    public ObjGeometryStore load(final ResourceSource resource, final GeometryTool geometryTool) {
        try {
            final ObjGeometryStore store = new ObjGeometryStore(geometryTool);
            long currentSmoothGroup = -1;

            final BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()));
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                line = line.trim();
                // handle line continuation marker \
                while (line.endsWith("\\")) {
                    line = line.substring(0, line.length() - 1);
                    final String s = reader.readLine();
                    if (s != null) {
                        line += s;
                        line = line.trim();
                    }
                }

                // ignore comments. goto next line
                if (line.length() > 0 && line.charAt(0) == '#') {
                    continue;
                }

                // tokenize line
                final String[] tokens = line.split("\\s+");

                // no tokens? must be an empty line. goto next line
                if (tokens.length == 0) {
                    continue;
                }

                // grab our "keyword"
                final String keyword = tokens[0];

                // Act on our keyword...

                // -------- VERTEX DATA KEYWORDS --------
                // if vertex
                if ("v".equals(keyword)) {
                    // XXX: support optional weight?
                    // final double w = tokens.length > 4 ? Double.valueOf(tokens[4]) : 1.0;

                    final Vector3 vertex = new Vector3(Double.valueOf(tokens[1]), Double.valueOf(tokens[2]),
                            Double.valueOf(tokens[3]));
                    store.getDataStore().getVertices().add(vertex);
                }

                // if texture coords
                else if ("vt".equals(keyword)) {
                    final double v = tokens.length > 2 ? Double.valueOf(tokens[2]) : 0;
                    final double w = tokens.length > 3 ? Double.valueOf(tokens[3]) : 0;
                    final Vector3 coord = new Vector3(Double.valueOf(tokens[1]), v, w);
                    store.getDataStore().getUvs().add(coord);
                }

                // if normal vector
                else if ("vn".equals(keyword)) {
                    final Vector3 normal = new Vector3(Double.valueOf(tokens[1]), Double.valueOf(tokens[2]),
                            Double.valueOf(tokens[3]));
                    store.getDataStore().getNormals().add(normal);
                }

                // if parameter space vertices
                else if ("vp".equals(keyword)) {
                    // TODO: Add support for vp
                    ObjImporter.logger.warning("ObjModelImporter: vp not supported.  (line " + lineNo + ") " + line);
                }

                // if curve/surface type
                else if ("cstype".equals(keyword)) {
                    // TODO: Add support for cstype
                    ObjImporter.logger
                            .warning("ObjModelImporter: cstype not supported.  (line " + lineNo + ") " + line);
                }

                // if degree
                else if ("deg".equals(keyword)) {
                    // TODO: Add support for degree
                    ObjImporter.logger.warning("ObjModelImporter: deg not supported.  (line " + lineNo + ") " + line);
                }

                // if basis matrix
                else if ("bmat".equals(keyword)) {
                    // TODO: Add support for basis matrix
                    ObjImporter.logger.warning("ObjModelImporter: bmat not supported.  (line " + lineNo + ") " + line);
                }

                // if step size
                else if ("step".equals(keyword)) {
                    // TODO: Add support for step size
                    ObjImporter.logger.warning("ObjModelImporter: step not supported.  (line " + lineNo + ") " + line);
                }

                // -------- GROUPING KEYWORDS --------

                // if group name(s)
                else if ("g".equals(keyword)) {
                    if (tokens.length < 2) {
                        store.setCurrentGroupNames(null);
                        continue;
                        // throw new Error("wrong number of args.  g must have at least 1 argument.  (line " + lineNo
                        // + ") " + line);
                    }

                    // Each token is a name
                    final String[] currentGroupNames = new String[tokens.length - 1];
                    store.setCurrentGroupNames(currentGroupNames);
                    System.arraycopy(tokens, 1, currentGroupNames, 0, tokens.length - 1);
                }

                // if smoothing group
                else if ("s".equals(keyword)) {
                    if (tokens.length != 2) {
                        throw new Error("wrong number of args.  s must have 1 argument.  (line " + lineNo + ") " + line);
                    }

                    if ("off".equalsIgnoreCase(tokens[1])) {
                        currentSmoothGroup = 0;
                    } else {
                        currentSmoothGroup = Long.parseLong(tokens[1]);
                    }
                }

                // if merge group
                else if ("mg".equals(keyword)) {
                    // TODO: Add support for merge groups
                    ObjImporter.logger.warning("ObjModelImporter: mg not supported.  (line " + lineNo + ") " + line);
                }

                // if object name
                else if ("o".equals(keyword)) {
                    if (tokens.length < 2) {
                        throw new Error("wrong number of args.  o must have 1 argument.  (line " + lineNo + ") " + line);
                    }
                    store.setCurrentObjectName(tokens[1]);
                }

                // -------- RENDER ATTRIBUTES KEYWORDS --------

                // if material library(ies)
                else if ("mtllib".equals(keyword)) {
                    if (tokens.length < 2) {
                        throw new Error("wrong number of args.  mtllib must have at least 1 argument.  (line " + lineNo
                                + ") " + line);
                    }

                    // load material libraries
                    for (int i = 1; i < tokens.length; i++) {
                        loadMaterialLibrary(tokens[i], resource, store.getMaterialLibrary());
                    }
                }

                // if use material command
                else if ("usemtl".equals(keyword)) {
                    if (tokens.length != 2) {
                        throw new Error("wrong number of args.  usemtl must have 1 argument.  (line " + lineNo + ") "
                                + line);
                    }

                    // set new material
                    store.setCurrentMaterial(store.getMaterialLibrary().get(tokens[1]));
                }

                // -------- ELEMENTS KEYWORDS --------

                // if point
                else if ("p".equals(keyword) && tokens.length > 1) {
                    if (tokens.length < 2) {
                        throw new Error("wrong number of args.  p must have at least 1 vertex.  (line " + lineNo + ") "
                                + line);
                    }

                    // Each token corresponds to 1 vertex entry
                    final List<ObjIndexSet> indices = Lists.newArrayList();
                    for (int i = 1; i < tokens.length; i++) {
                        indices.add(new ObjIndexSet(tokens[i], store.getDataStore(), currentSmoothGroup));
                    }
                    store.addPoints(indices);
                }

                // if line
                else if ("l".equals(keyword) && tokens.length > 1) {
                    if (tokens.length < 3) {
                        throw new Error("wrong number of args.  l must have at least 2 vertices.  (line " + lineNo
                                + ") " + line);
                    }

                    // Each token corresponds to 1 vertex entry and possibly one texture entry
                    final List<ObjIndexSet> indices = Lists.newArrayList();
                    for (int i = 1; i < tokens.length; i++) {
                        indices.add(new ObjIndexSet(tokens[i], store.getDataStore(), currentSmoothGroup));
                    }
                    store.addLine(indices);
                }

                // if face
                else if (("f".equals(keyword) || "fo".equals(keyword)) && tokens.length > 1) {
                    if (tokens.length < 4) {
                        throw new Error("wrong number of args.  f must have at least 3 vertices.  (line " + lineNo
                                + ") " + line);
                    }

                    // Each token corresponds to 1 vertex entry and possibly one texture entry and normal entry.
                    final List<ObjIndexSet> indices = Lists.newArrayList();
                    for (int i = 1; i < tokens.length; i++) {
                        indices.add(new ObjIndexSet(tokens[i], store.getDataStore(), currentSmoothGroup));
                    }
                    store.addFace(indices);
                }

                // if curve
                else if ("curv".equals(keyword)) {
                    // TODO: Add support for curves
                    ObjImporter.logger.warning("ObjModelImporter: curv not supported.  (line " + lineNo + ") " + line);
                }

                // if 2d curve
                else if ("curv2".equals(keyword)) {
                    // TODO: Add support for 2d curves
                    ObjImporter.logger.warning("ObjModelImporter: curv2 not supported.  (line " + lineNo + ") " + line);
                }

                // if surface
                else if ("surf".equals(keyword)) {
                    // TODO: Add support for surfaces
                    ObjImporter.logger.warning("ObjModelImporter: surf not supported.  (line " + lineNo + ") " + line);
                }
            }

            store.commitObjects();
            store.cleanup();
            return store;
        } catch (final Exception e) {
            throw new Error("Unable to load obj resource from URL: " + resource, e);
        }
    }

    /**
     * Load a .mtl resource
     * 
     * @param fileName
     *            the name of the mtl resource to load.
     * @param modelSource
     *            a source to pull the mtl relatively. Used only if a material locator was not set on this importer.
     * @param store
     *            our material store to place the contents of the file in.
     */
    private void loadMaterialLibrary(final String fileName, final ResourceSource modelSource,
            final Map<String, ObjMaterial> store) {
        final ResourceSource source;
        if (_materialLocator == null) {
            source = modelSource.getRelativeSource(fileName);
        } else {
            source = _materialLocator.locateResource(fileName);
        }

        if (source == null) {
            throw new Error("Unable to locate mtllib '" + fileName + "'");
        }

        loadMaterialLibrary(source, store);
    }

    /**
     * Load a .mtl resource
     * 
     * @param resource
     *            the mtl file to load, as a ResourceSource
     * @param store
     *            our material store to place the contents of the file in.
     */
    private void loadMaterialLibrary(final ResourceSource resource, final Map<String, ObjMaterial> store) {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()));
            String line;
            ObjMaterial currentMaterial = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // handle line continuation marker \
                while (line.endsWith("\\")) {
                    line = line.substring(0, line.length() - 1);
                    final String s = reader.readLine();
                    if (s != null) {
                        line += s;
                        line = line.trim();
                    }
                }

                // ignore comments. goto next line
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }

                // tokenize line
                final String[] tokens = line.split("\\s+");

                // no tokens? must be an empty line. goto next line
                if (tokens.length == 0) {
                    continue;
                }

                // grab our "keyword"
                final String keyword = tokens[0];

                // Act on our keyword...

                // if newmtl
                if ("newmtl".equals(keyword)) {
                    // start new material
                    currentMaterial = new ObjMaterial(tokens[1]);
                    store.put(tokens[1], currentMaterial);
                    continue;
                }

                if (currentMaterial == null) {
                    ObjImporter.logger.warning("No material is set");
                    return;
                }

                // if ambient value
                if ("Ka".equals(keyword)) {
                    currentMaterial.Ka = new float[] { Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3]) };
                }

                // if diffuse value
                else if ("Kd".equals(keyword)) {
                    currentMaterial.Kd = new float[] { Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3]) };
                }

                // if specular value
                else if ("Ks".equals(keyword)) {
                    currentMaterial.Ks = new float[] { Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3]) };
                }

                // if illumination style
                else if ("illum".equals(keyword)) {
                    currentMaterial.illumType = Integer.parseInt(tokens[1]);
                }

                // if "dissolve" (alpha) value
                else if ("d".equals(keyword)) {
                    currentMaterial.d = Float.parseFloat(tokens[1]);
                }

                // if ambient value
                else if ("Ns".equals(keyword)) {
                    final float Ns = Float.parseFloat(tokens[1]);
                    currentMaterial.Ns = 128 * MathUtils.clamp(Ns, 0, _specularMax) / _specularMax;
                }

                // if we mapped a texture to alpha
                else if ("map_d".equals(keyword)) {
                    // force blending... probably also used texture in map_Kd, etc.
                    currentMaterial.forceBlend = true;
                }

                // if texture
                else if (isLoadTextures() && "map_Kd".equals(keyword)) {
                    // TODO: it's possible for map_Kd to have arguments, then filename.
                    final String textureName = line.substring("map_Kd".length()).trim();
                    currentMaterial.textureName = textureName;
                    if (_textureLocator == null) {
                        currentMaterial.map_Kd = TextureManager.load(textureName, getMinificationFilter(),
                                isUseCompression() ? TextureStoreFormat.GuessCompressedFormat
                                        : TextureStoreFormat.GuessNoCompressedFormat, isFlipTextureVertically());
                    } else {
                        final ResourceSource source = _textureLocator.locateResource(textureName);
                        currentMaterial.map_Kd = TextureManager.load(source, getMinificationFilter(),
                                isUseCompression() ? TextureStoreFormat.GuessCompressedFormat
                                        : TextureStoreFormat.GuessNoCompressedFormat, isFlipTextureVertically());
                    }
                }
            }
        } catch (final Exception e) {
            throw new Error("Unable to load mtllib resource from URL: " + resource, e);
        }
    }
}
