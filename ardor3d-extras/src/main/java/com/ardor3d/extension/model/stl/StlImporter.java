/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.stl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.LittleEndianRandomAccessDataInput;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.geom.GeometryTool;
import com.ardor3d.util.geom.GeometryTool.MatchCondition;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public class StlImporter {
    private ResourceLocator _modelLocator;
    private final EnumSet<MatchCondition> _optimizeSettings = EnumSet.of(MatchCondition.Normal, MatchCondition.Color);
    private double _maxSmoothAngle = MathUtils.QUARTER_PI;
    private double _maxVertDistanceSq = 0.00001;
    private boolean _runSmoothing = false;

    public StlImporter setModelLocator(final ResourceLocator locator) {
        _modelLocator = locator;
        return this;
    }

    public StlDataStore load(final String resource) {
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

    public StlDataStore load(final ResourceSource resource) {
        if (resource == null) {
            throw new NullPointerException("Unable to load null resource");
        }

        try {
            final InputStream stream = resource.openStream();
            if (stream == null) {
                throw new NullPointerException("resource had null stream: " + resource);
            }

            final StlDataStore store = new StlDataStore();
            final LittleEndianRandomAccessDataInput in = new LittleEndianRandomAccessDataInput(stream);

            // check if we are binary
            if (isBinary(in)) {
                LoadFromBinary(in, store);
            } else if (isAscii(in)) {
                LoadFromAscii(resource, store);
            } else {
                throw new Error("Unable to determine stl resource type for: " + resource);
            }

            store.vertMap = GeometryTool.minimizeVerts(store.mesh, _optimizeSettings);

            // add a simple color material
            final MaterialState ms = new MaterialState();
            ms.setDiffuse(store.defaultColor);

            store.getScene().setRenderState(ms);

            return store;
        } catch (final Exception e) {
            throw new Error("Unable to load stl resource from URL: " + resource, e);
        }
    }

    private boolean isAscii(final LittleEndianRandomAccessDataInput in) throws IOException {
        if (isBinary(in)) {
            return false;
        }

        final int capacity = in.capacity();
        if (capacity < 6) {
            return false;
        }

        in.seek(0);
        final String header = in.readString(6);

        if ("solid ".compareToIgnoreCase(header) == 0) {
            // double check the rest of the file is only ascii
            for (int i = 6, maxI = capacity; i < maxI; i++) {
                if (in.readByte() > 127) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isBinary(final LittleEndianRandomAccessDataInput in) throws IOException {
        final int capacity = in.capacity();

        // smallest binary file is 80 byte header + 4 byte face count
        if (capacity < 80 + 4) {
            return false;
        }

        in.seek(80);
        final int faceCount = in.readInt();

        final int expectedCapacity = faceCount * 50 + 80 + 4;

        return expectedCapacity == capacity;
    }

    public void put(final FloatBufferData data, final Vector3 vector) {
        data.getBuffer().put(vector.getXf());
        data.getBuffer().put(vector.getYf());
        data.getBuffer().put(vector.getZf());
    }

    // NB: Not optimized since we are opening a new stream, but our LittleEndianRandomAccessDataInput does not have
    // readLine.
    private void LoadFromAscii(final ResourceSource resource, final StlDataStore store) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()));
        String line = reader.readLine();

        // handle name first
        final String name = line.length() > 6 ? line.substring(6).trim() : "mesh";
        store.mesh.setName(name);

        final List<Vector3> normalList = new ArrayList<>();
        final List<Vector3> vertList = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            // tokenize line
            final String[] tokens = line.trim().split("\\s+");

            // no tokens? must be an empty line. goto next line
            if (tokens.length == 0) {
                continue;
            }

            // grab our "keyword"
            final String keyword = tokens[0];

            if ("endsolid".equalsIgnoreCase(keyword)) {
                break;
            }

            if ("facet".equalsIgnoreCase(keyword)) {
                normalList.add(new Vector3(Float.valueOf(tokens[2]), Float.valueOf(tokens[3]), Float.valueOf(tokens[4]))
                        .normalizeLocal());
            } else if ("vertex".equalsIgnoreCase(keyword)) {
                vertList.add(new Vector3(Float.valueOf(tokens[1]), Float.valueOf(tokens[2]), Float.valueOf(tokens[3])));
            }
        }

        final int faceCount = normalList.size();
        if (faceCount * 3 != vertList.size()) {
            throw new Error(
                    "Malformed Stl ASCII file.  normal count and vertex count do not match 1:3 ratio... resource: "
                            + resource);
        }

        final MeshData data = store.getScene().getMeshData();

        final FloatBufferData positions = new FloatBufferData(faceCount * 3 * 3, 3);
        final FloatBufferData normals = new FloatBufferData(faceCount * 3 * 3, 3);
        data.setVertexCoords(positions);
        data.setNormalCoords(normals);

        final Multimap<Vector3, Integer> normIndices = ArrayListMultimap.create(faceCount, 3);
        for (int i = 0; i < faceCount; i++) {
            final Vector3 normal = normalList.get(i);
            final Vector3 posA = vertList.get(i * 3 + 0);
            final Vector3 posB = vertList.get(i * 3 + 1);
            final Vector3 posC = vertList.get(i * 3 + 2);

            put(normals, normal);
            put(positions, posA);
            normIndices.put(posA, i * 3 + 0);
            put(normals, normal);
            put(positions, posB);
            normIndices.put(posB, i * 3 + 1);
            put(normals, normal);
            put(positions, posC);
            normIndices.put(posC, i * 3 + 2);
        }

        if (isRunSmoothing()) {
            smoothNormals(normals, normalList, vertList, normIndices);
        }
    }

    private void LoadFromBinary(final LittleEndianRandomAccessDataInput in, final StlDataStore store)
            throws IOException {

        in.seek(80);
        final int faceCount = in.readInt();

        // setup our mesh
        final MeshData data = store.getScene().getMeshData();

        final FloatBufferData positions = new FloatBufferData(faceCount * 3 * 3, 3);
        final FloatBufferData normals = new FloatBufferData(faceCount * 3 * 3, 3);
        data.setVertexCoords(positions);
        data.setNormalCoords(normals);

        final List<Vector3> normalList = new ArrayList<>();
        final List<Vector3> vertList = new ArrayList<>();
        final Multimap<Vector3, Integer> normIndices = ArrayListMultimap.create(faceCount, 3);

        for (int i = 0; i < faceCount; i++) {
            final Vector3 normal = new Vector3(in.readFloat(), in.readFloat(), in.readFloat()).normalizeLocal();
            normalList.add(normal);
            final Vector3 posA = new Vector3(in.readFloat(), in.readFloat(), in.readFloat());
            vertList.add(posA);
            final Vector3 posB = new Vector3(in.readFloat(), in.readFloat(), in.readFloat());
            vertList.add(posB);
            final Vector3 posC = new Vector3(in.readFloat(), in.readFloat(), in.readFloat());
            vertList.add(posC);

            in.readShort();

            put(normals, normal);
            put(positions, posA);
            normIndices.put(posA, i * 3 + 0);
            put(normals, normal);
            put(positions, posB);
            normIndices.put(posB, i * 3 + 1);
            put(normals, normal);
            put(positions, posC);
            normIndices.put(posC, i * 3 + 2);
        }

        if (isRunSmoothing()) {
            smoothNormals(normals, normalList, vertList, normIndices);
        }
    }

    class NormalGroup {
        List<Vector3> normalValues = new ArrayList<>();
        List<Integer> idxValues = new ArrayList<>();
        Vector3 sum = new Vector3();
        Vector3 avg = new Vector3();

        NormalGroup(final Vector3 initial, final Integer idx) {
            add(initial, idx);
        }

        void add(final Vector3 normal, final Integer idx) {
            normalValues.add(normal);
            idxValues.add(idx);
            sum.addLocal(normal);
            sum.normalize(avg);
        }

        double getAngleOffGroupAverage(final Vector3 normal) {
            if (normalValues.isEmpty()) {
                return 0.0;
            }

            return getAverage().smallestAngleBetween(normal);
        }

        Vector3 getAverage() {
            return avg;
        }
    }

    private void smoothNormals(final FloatBufferData normals, final List<Vector3> normalList,
            final List<Vector3> vertList, final Multimap<Vector3, Integer> normIndices) {
        while (!vertList.isEmpty()) {
            final Vector3 v = vertList.remove(vertList.size() - 1);
            final Set<Integer> inds = new HashSet<>(normIndices.get(v));

            for (int i = vertList.size(); --i >= 0;) {
                final Vector3 v2 = vertList.get(i);
                if (v2.distanceSquared(v) <= _maxVertDistanceSq) {
                    vertList.remove(i);
                    inds.addAll(normIndices.get(v2));
                }
            }

            // ok we have all of the normals that are tied to the given vertices
            // walk through our normal values and make groups
            final List<NormalGroup> groups = new ArrayList<>();
            for (final Integer idx : inds) {
                final Vector3 normal = new Vector3(normalList.get(idx / 3));
                boolean foundGroup = false;
                for (final NormalGroup group : groups) {
                    final double angle = group.getAngleOffGroupAverage(normal);
                    if (angle <= getMaxSmoothAngle()) {
                        group.add(normal, idx);
                        foundGroup = true;
                        break;
                    }
                }

                if (!foundGroup) {
                    groups.add(new NormalGroup(normal, idx));
                }
            }

            // go through each group, replace normals
            for (final NormalGroup group : groups) {
                final Vector3 avg = group.getAverage();
                for (final Integer idx : group.idxValues) {
                    BufferUtils.setInBuffer(avg, normals.getBuffer(), idx);
                }
            }
        }
    }

    public Set<MatchCondition> getOptimizeSettings() {
        return ImmutableSet.copyOf(_optimizeSettings);
    }

    public void setOptimizeSettings(final MatchCondition... optimizeSettings) {
        _optimizeSettings.clear();
        for (final MatchCondition cond : optimizeSettings) {
            _optimizeSettings.add(cond);
        }
    }

    public double getMaxSmoothAngle() {
        return _maxSmoothAngle;
    }

    public void setMaxSmoothAngle(final double angle) {
        _maxSmoothAngle = angle;
    }

    public double getMaxVertDistanceSq() {
        return _maxVertDistanceSq;
    }

    public void setMaxVertDistanceSq(final double distanceSq) {
        _maxVertDistanceSq = distanceSq;
    }

    public void setRunSmoothing(final boolean smooth) {
        _runSmoothing = smooth;
    }

    public boolean isRunSmoothing() {
        return _runSmoothing;
    }
}
