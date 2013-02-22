/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jdom2.Element;
import org.jdom2.xpath.XPath;

import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.extension.animation.skeletal.Skeleton;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.image.Texture;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.geom.VertMap;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * Performance cache and temp storage during parsing.
 */
public class DataCache {
    private final Map<String, Element> _boundMaterials;
    private final Map<String, Texture> _textures;
    private final Map<String, Element> _idCache;
    private final Map<String, Element> _sidCache;
    private final Map<String, XPath> _xPathExpressions;
    private final Pattern _pattern;
    private final List<String> _transformTypes;

    private final Map<Element, float[]> _floatArrays;
    private final Map<Element, double[]> _doubleArrays;
    private final Map<Element, boolean[]> _booleanArrays;
    private final Map<Element, int[]> _intArrays;
    private final Map<Element, String[]> _stringArrays;

    private final Multimap<Element, MeshVertPairs> _vertMappings;
    private final Map<Mesh, VertMap> _meshVertMap;
    private final Multimap<MeshData, FloatBuffer> _parsedVertexColors;
    private final Map<String, MaterialInfo> _materialInfoMap;
    private final Map<Mesh, String> _meshMaterialMap;
    private final Map<Element, Spatial> _elementSpatialMapping;

    private final Map<Element, Joint> _elementJointMapping;
    private final Map<String, Joint> _externalJointMapping;
    private JointNode _rootJointNode;
    private final Map<Joint, Skeleton> _jointSkeletonMapping;
    private final Map<Skeleton, SkeletonPose> _skeletonPoseMapping;
    private final List<Skeleton> _skeletons;
    private final List<ControllerStore> _controllers;

    public DataCache() {
        _boundMaterials = Maps.newHashMap();
        _textures = Maps.newHashMap();
        _idCache = Maps.newHashMap();
        _sidCache = Maps.newHashMap();
        _xPathExpressions = Maps.newHashMap();
        _pattern = Pattern.compile("\\s");

        _transformTypes = Collections.unmodifiableList(Lists.newArrayList("lookat", "matrix", "rotate", "scale",
                "scew", "translate"));

        _floatArrays = Maps.newHashMap();
        _doubleArrays = Maps.newHashMap();
        _booleanArrays = Maps.newHashMap();
        _intArrays = Maps.newHashMap();
        _stringArrays = Maps.newHashMap();
        _vertMappings = ArrayListMultimap.create();
        _meshVertMap = Maps.newIdentityHashMap();
        _parsedVertexColors = ArrayListMultimap.create();
        _materialInfoMap = Maps.newHashMap();
        _meshMaterialMap = Maps.newIdentityHashMap();

        _elementSpatialMapping = Maps.newHashMap();

        _elementJointMapping = Maps.newHashMap();
        _externalJointMapping = Maps.newHashMap();
        _skeletons = Lists.newArrayList();
        _jointSkeletonMapping = Maps.newHashMap();
        _skeletonPoseMapping = Maps.newHashMap();
        _controllers = Lists.newArrayList();
    }

    public void bindMaterial(final String ref, final Element material) {
        if (!_boundMaterials.containsKey(ref)) {
            _boundMaterials.put(ref, material);
        }
    }

    public void unbindMaterial(final String ref) {
        _boundMaterials.remove(ref);
    }

    public Element getBoundMaterial(final String ref) {
        return _boundMaterials.get(ref);
    }

    public boolean containsTexture(final String path) {
        return _textures.containsKey(path);
    }

    public void addTexture(final String path, final Texture texture) {
        _textures.put(path, texture);
    }

    public Texture getTexture(final String path) {
        return _textures.get(path);
    }

    public Map<String, Element> getIdCache() {
        return _idCache;
    }

    public Map<String, Element> getSidCache() {
        return _sidCache;
    }

    public Map<String, XPath> getxPathExpressions() {
        return _xPathExpressions;
    }

    public Pattern getPattern() {
        return _pattern;
    }

    public List<String> getTransformTypes() {
        return _transformTypes;
    }

    public Map<Element, float[]> getFloatArrays() {
        return _floatArrays;
    }

    public Map<Element, double[]> getDoubleArrays() {
        return _doubleArrays;
    }

    public Map<Element, boolean[]> getBooleanArrays() {
        return _booleanArrays;
    }

    public Map<Element, int[]> getIntArrays() {
        return _intArrays;
    }

    public Map<Element, String[]> getStringArrays() {
        return _stringArrays;
    }

    public Multimap<Element, MeshVertPairs> getVertMappings() {
        return _vertMappings;
    }

    public Map<Mesh, VertMap> getMeshVertMap() {
        return _meshVertMap;
    }

    public Multimap<MeshData, FloatBuffer> getParsedVertexColors() {
        return _parsedVertexColors;
    }

    public Map<String, MaterialInfo> getMaterialInfoMap() {
        return _materialInfoMap;
    }

    public Map<Mesh, String> getMeshMaterialMap() {
        return _meshMaterialMap;
    }

    public Map<Element, Spatial> getElementSpatialMapping() {
        return _elementSpatialMapping;
    }

    public Map<Element, Joint> getElementJointMapping() {
        return _elementJointMapping;
    }

    public Map<String, Joint> getExternalJointMapping() {
        return _externalJointMapping;
    }

    public JointNode getRootJointNode() {
        return _rootJointNode;
    }

    public void setRootJointNode(final JointNode rootJointNode) {
        _rootJointNode = rootJointNode;
    }

    public Map<Joint, Skeleton> getJointSkeletonMapping() {
        return _jointSkeletonMapping;
    }

    public Map<Skeleton, SkeletonPose> getSkeletonPoseMapping() {
        return _skeletonPoseMapping;
    }

    public List<ControllerStore> getControllers() {
        return _controllers;
    }

    public List<Skeleton> getSkeletons() {
        return _skeletons;
    }

    public void addSkeleton(final Skeleton skeleton) {
        _skeletons.add(skeleton);
    }

    public void setMeshVertMap(final Mesh geometry, final VertMap map) {
        _meshVertMap.put(geometry, map);
    }
}
