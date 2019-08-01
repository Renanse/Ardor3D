/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.obj;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.geom.GeometryTool;
import com.ardor3d.util.geom.GeometryTool.MatchCondition;
import com.ardor3d.util.geom.VertGroupData;

public class ObjGeometryStore {
    private static final String DEFAULT_GROUP = "_default_";

    private final ObjDataStore _dataStore = new ObjDataStore();

    private int _totalPoints = 0;
    private int _totalLines = 0;
    private int _totalMeshes = 0;
    private final Node _root = new Node();
    private final Map<String, Spatial> _groupMap = new HashMap<>();

    private ObjMaterial _currentMaterial = new ObjMaterial("default");
    private String _currentObjectName;
    private String[] _currentGroupNames;

    private ObjSetManager _meshManager;
    private ObjSetManager _lineManager;
    private ObjSetManager _pointManager;

    private final Map<String, ObjMaterial> materialLibrary = new HashMap<>();
    private final Map<Spatial, String> _materialMap = new HashMap<>();

    public ObjGeometryStore() {
        super();
    }

    public Map<String, ObjMaterial> getMaterialLibrary() {
        return materialLibrary;
    }

    public ObjDataStore getDataStore() {
        return _dataStore;
    }

    public Node getScene() {
        return _root;
    }

    void addFace(final List<ObjIndexSet> indices) {
        if (_meshManager == null) {
            _meshManager = new ObjSetManager();
        }

        // Build a fan of triangles
        final ObjIndexSet first = indices.get(0);
        final int firstIndex = _meshManager.findSet(first);
        ObjIndexSet second = indices.get(1);
        int secondIndex = _meshManager.findSet(second);
        for (int i = 2; i < indices.size(); i++) {
            final ObjIndexSet third = indices.get(i);
            final int thirdIndex = _meshManager.findSet(third);
            _meshManager.addIndex(firstIndex);
            _meshManager.addIndex(secondIndex);
            _meshManager.addIndex(thirdIndex);
            if (first.getVnIndex() == -1 || second.getVnIndex() == -1 || third.getVnIndex() == -1) {
                // Generate flat face normal.
                final Vector3 v = new Vector3(_dataStore.getVertices().get(second.getVIndex()));
                final Vector3 w = new Vector3(_dataStore.getVertices().get(third.getVIndex()));
                v.subtractLocal(_dataStore.getVertices().get(first.getVIndex()));
                w.subtractLocal(_dataStore.getVertices().get(first.getVIndex()));
                v.crossLocal(w);
                v.normalizeLocal();
                _dataStore.getGeneratedNormals().add(v);
                final int genIndex = -1 * (_dataStore.getGeneratedNormals().size() - 1) - 2;
                if (first.getVnIndex() == -1) {
                    first.setVnIndex(genIndex);
                }
                if (second.getVnIndex() == -1) {
                    second.setVnIndex(genIndex);
                }
                if (third.getVnIndex() == -1) {
                    third.setVnIndex(genIndex);
                }
            }
            second = third;
            secondIndex = thirdIndex;
        }
    }

    void addLine(final List<ObjIndexSet> indices) {
        if (_lineManager == null) {
            _lineManager = new ObjSetManager();
        }

        // Build a single long line
        for (int i = 0; i < indices.size(); i++) {
            final ObjIndexSet point = indices.get(i);
            final int index = _lineManager.findSet(point);
            _lineManager.addIndex(index);
        }

        _lineManager.addLength(indices.size());
    }

    void addPoints(final List<ObjIndexSet> indices) {
        if (_pointManager == null) {
            _pointManager = new ObjSetManager();
        }

        // Add points
        for (int i = 0; i < indices.size(); i++) {
            final ObjIndexSet point = indices.get(i);
            final int index = _pointManager.findSet(point);
            _pointManager.addIndex(index);
        }
    }

    void setCurrentGroupNames(final String[] names) {
        commitObjects();
        _currentGroupNames = names;
    }

    void setCurrentObjectName(final String name) {
        commitObjects();
        _currentObjectName = name;
    }

    void setCurrentMaterial(final ObjMaterial material) {
        if (material != null) {
            commitObjects();
            _currentMaterial = material;
        }
    }

    void cleanup() {
        _currentGroupNames = null;
        _currentMaterial = null;
        _currentObjectName = null;

        _meshManager = null;
        _lineManager = null;
        _pointManager = null;
    }

    void commitObjects() {
        // go through each manager, if not null, turn into a scenegraph object and attach to root.
        if (_pointManager != null) {
            String name = _currentObjectName;
            if (name == null) {
                name = "obj_points" + _totalPoints;
            }

            final Vector3[] vertices = new Vector3[_pointManager.getStore().size()];
            int i = 0;
            for (final ObjIndexSet set : _pointManager.getStore().keySet()) {
                vertices[i++] = _dataStore.getVertices().get(set.getVIndex());
            }

            final Point points = new Point(name, vertices, null, null, null);
            final IndexBufferData<? extends Buffer> indexBuffer = BufferUtils
                    .createIndexBufferData(_pointManager.getIndices().size(), vertices.length - 1);
            for (final int index : _pointManager.getIndices()) {
                indexBuffer.put(index);
            }
            points.getMeshData().setIndices(indexBuffer);

            GeometryTool.minimizeVerts(points, EnumSet.noneOf(MatchCondition.class));

            applyCurrentMaterial(points);
            mapToGroups(points);

            points.updateModelBound();

            _root.attachChild(points);
            _pointManager = null;
            _totalPoints++;
        }

        if (_lineManager != null) {
            String name = _currentObjectName;
            if (name == null) {
                name = "obj_lines" + _totalLines;
            }

            final Vector3[] vertices = new Vector3[_lineManager.getStore().size()];
            final Vector2[] uvs = new Vector2[vertices.length];
            boolean hasUVs = false;
            int i = 0;
            for (final ObjIndexSet set : _lineManager.getStore().keySet()) {
                vertices[i] = _dataStore.getVertices().get(set.getVIndex());
                if (set.getVtIndex() >= 0) {
                    final Vector3 uv = _dataStore.getUvs().get(set.getVtIndex());
                    // our line only supports 2d uvs
                    uvs[i] = new Vector2(uv.getX(), uv.getY());
                    hasUVs = true;
                }
                i++;
            }

            final Line line = new Line(name, vertices, null, null, hasUVs ? uvs : null);
            final IndexBufferData<? extends Buffer> indexBuffer = BufferUtils
                    .createIndexBufferData(_lineManager.getIndices().size(), vertices.length - 1);
            for (final int index : _lineManager.getIndices()) {
                indexBuffer.put(index);
            }
            line.getMeshData().setIndices(indexBuffer);
            if (_lineManager.getLengths().size() > 1) {
                final int[] lengths = new int[_lineManager.getLengths().size()];
                i = 0;
                for (final int l : _lineManager.getLengths()) {
                    lengths[i++] = l;
                }
                line.getMeshData().setIndexLengths(lengths);
            }
            GeometryTool.minimizeVerts(line, EnumSet.of(MatchCondition.UVs));

            applyCurrentMaterial(line);
            mapToGroups(line);

            line.updateModelBound();

            _root.attachChild(line);
            _lineManager = null;
            _totalLines++;
        }

        if (_meshManager != null) {
            String name = _currentObjectName;
            if (name == null) {
                name = "obj_mesh" + _totalMeshes;
            }

            final Mesh mesh = new Mesh(name);

            final FloatBuffer vertices = BufferUtils.createVector3Buffer(_meshManager.getStore().size());
            final FloatBuffer normals = BufferUtils.createFloatBuffer(vertices.capacity());
            final FloatBuffer uvs = BufferUtils.createFloatBuffer(vertices.capacity());
            boolean hasNormals = false, hasUVs = false;

            int j = 0;
            final long[] vertGroups = new long[_meshManager.getStore().size()];
            final List<Long> groups = new ArrayList<>();
            Vector3 vector;
            for (final ObjIndexSet set : _meshManager.getStore().keySet()) {
                vertGroups[j] = set.getSmoothGroup();
                if (!groups.contains(set.getSmoothGroup())) {
                    groups.add(set.getSmoothGroup());
                }
                vector = _dataStore.getVertices().get(set.getVIndex());
                vertices.put(vector.getXf()).put(vector.getYf()).put(vector.getZf());
                if (set.getVnIndex() >= 0) {
                    vector = _dataStore.getNormals().get(set.getVnIndex());
                    normals.put(vector.getXf()).put(vector.getYf()).put(vector.getZf());
                    hasNormals = true;
                } else if (set.getVnIndex() < -1) {
                    vector = _dataStore.getGeneratedNormals().get(-1 * set.getVnIndex() - 2);
                    normals.put(vector.getXf()).put(vector.getYf()).put(vector.getZf());
                    hasNormals = true;
                }
                if (set.getVtIndex() >= 0) {
                    vector = _dataStore.getUvs().get(set.getVtIndex());
                    // TODO: add 3d tex support?
                    uvs.put(vector.getXf()).put(vector.getYf());
                    hasUVs = true;
                }
                j++;
            }

            mesh.getMeshData().setVertexBuffer(vertices);
            if (hasNormals) {
                mesh.getMeshData().setNormalBuffer(normals);
            }
            if (hasUVs) {
                mesh.getMeshData().setTextureBuffer(uvs, 0);
            }

            final IndexBufferData<? extends Buffer> indexBuffer = BufferUtils
                    .createIndexBufferData(_meshManager.getIndices().size(), _meshManager.getStore().size() - 1);
            for (final int index : _meshManager.getIndices()) {
                indexBuffer.put(index);
            }
            mesh.getMeshData().setIndices(indexBuffer);

            final VertGroupData groupData = new VertGroupData();
            // set all smooth groups to use "blend as long as UVs and SmoothGroup are same".
            for (final long group : groups) {
                groupData.setGroupConditions(group, EnumSet.of(MatchCondition.UVs));
            }
            // set the "no smooth" smooth group to use "blend only if vertex is same". (No color data in obj, so
            // ignoring)
            groupData.setVertGroups(vertGroups);
            groupData.setGroupConditions(VertGroupData.DEFAULT_GROUP,
                    EnumSet.of(MatchCondition.Normal, MatchCondition.UVs));
            GeometryTool.minimizeVerts(mesh, groupData);

            applyCurrentMaterial(mesh);
            mapToGroups(mesh);

            mesh.updateModelBound();

            _root.attachChild(mesh);
            _meshManager = null;
            _totalMeshes++;
        }
    }

    private void applyCurrentMaterial(final Spatial target) {
        _currentMaterial.applyMaterialProperties(target);

        final TextureState tState = _currentMaterial.getTextureState();
        if (tState != null) {
            target.setRenderState(tState);
        }

        final BlendState blend = _currentMaterial.getBlendState();
        if (blend != null) {
            target.setRenderState(blend);
            target.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
        }

        if (_currentMaterial.illumType == 0) {
            target.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        }

        _materialMap.put(target, _currentMaterial.getName());
    }

    private void mapToGroups(final Spatial target) {
        if (_currentGroupNames != null) {
            for (final String groupName : _currentGroupNames) {
                _groupMap.put(groupName, target);
            }
        } else {
            _groupMap.put(ObjGeometryStore.DEFAULT_GROUP, target);
        }

    }

    public Map<Spatial, String> getMaterialMap() {
        return _materialMap;
    }
}
