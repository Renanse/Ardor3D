/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.util;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.terrain.client.ClipmapLevel;
import com.ardor3d.extension.terrain.util.AbstractBresenhamTracer.Direction;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Triangle;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyRay3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * A picking assistant to be used with ClipmapLevel and an AbstractBresenhamTracer.
 */
public class ClipmapTerrainPicker {

    private final List<ClipmapLevel> _clipmapLevels;
    private final List<AbstractBresenhamTracer> _tracers;
    private int _maxChecks;

    private final Ray3 _workRay = new Ray3();
    private final Vector3 _workEyePos = new Vector3();
    private final Triangle _gridTriA = new Triangle(), _gridTriB = new Triangle();
    private int _minLevel, _maxLevel;
    private final float[] tileStore = new float[16];

    /**
     * Construct a new picker using the supplied pyramid, tracer and arguments.
     *
     * @param levels
     *            the source for our height information..
     * @param tracerClass
     *            class type for our Bresenham tracer.
     * @param maxChecks
     *            the maximum number of grid spaces we'll walk before giving up our search.
     * @throws IllegalAccessException
     *             if we are unable to create an instance of our tracerClass
     * @throws InstantiationException
     *             if we are unable to create an instance of our tracerClass
     */
    public ClipmapTerrainPicker(final List<ClipmapLevel> levels,
            final Class<? extends AbstractBresenhamTracer> tracerClass, final int maxChecks,
            final Vector3 initialSpacing) throws InstantiationException, IllegalAccessException {
        _clipmapLevels = levels;
        _tracers = new ArrayList<>();
        for (int i = 0, max = levels.size(); i < max; i++) {
            final AbstractBresenhamTracer tracer = tracerClass.newInstance();
            final int space = 1 << i;
            final Vector3 vec = new Vector3(initialSpacing).multiplyLocal(space);
            if (vec.getX() == 0) {
                vec.setX(1);
            }
            if (vec.getY() == 0) {
                vec.setY(1);
            }
            if (vec.getZ() == 0) {
                vec.setZ(1);
            }
            tracer.setGridSpacing(vec);
            _tracers.add(tracer);
        }
        _maxChecks = maxChecks;
        _minLevel = 0;
        _maxLevel = levels.size() - 1;
    }

    public Vector3 getTerrainIntersection(final ReadOnlyTransform terrainWorldTransform, final ReadOnlyVector3 eyePos,
            final ReadOnlyRay3 pickRay, final Vector3 store, final Vector3 normalStore) {
        _workRay.setOrigin(terrainWorldTransform.applyInverse(pickRay.getOrigin(), null));
        _workRay.setDirection(terrainWorldTransform.applyInverseVector(pickRay.getDirection(), null).normalizeLocal());
        terrainWorldTransform.applyInverse(eyePos, _workEyePos);

        // check which clipmap level we start in
        int index = findClipIndex(_workRay.getOrigin().subtract(_workEyePos, null));
        // simple test to see if our level at least has SOME data. XXX: could look to the tile level.
        while (index > 0 && !_clipmapLevels.get(index).isReady()) {
            index--;
        }
        AbstractBresenhamTracer tracer = _tracers.get(index);
        ClipmapLevel level = _clipmapLevels.get(index);

        // start our tracer
        tracer.startWalk(_workRay);

        final Vector3 intersection = store != null ? store : new Vector3();

        if (tracer.isRayPerpendicularToGrid()) {
            // XXX: "HACK" for perpendicular ray
            level.getCache().getEyeCoords(tileStore, tracer.getGridLocation()[0], tracer.getGridLocation()[1],
                    _workEyePos);
            final float scaledClipSideSize = level.getClipSideSize() * level.getVertexDistance() * 0.5f;

            final float h1 = getWeightedHeight(tileStore[0], tileStore[1], tileStore[2], tileStore[3],
                    scaledClipSideSize);
            final float h2 = getWeightedHeight(tileStore[4], tileStore[5], tileStore[6], tileStore[7],
                    scaledClipSideSize);
            final float h3 = getWeightedHeight(tileStore[8], tileStore[9], tileStore[10], tileStore[11],
                    scaledClipSideSize);
            final float h4 = getWeightedHeight(tileStore[12], tileStore[13], tileStore[14], tileStore[15],
                    scaledClipSideSize);

            final double x = _workEyePos.getX();
            final double z = _workEyePos.getZ();
            final double intOnX = x - Math.floor(x), intOnZ = z - Math.floor(z);
            final double height = MathUtils.lerp(intOnZ, MathUtils.lerp(intOnX, h1, h2),
                    MathUtils.lerp(intOnX, h3, h4));

            intersection.set(x, height, z);
            terrainWorldTransform.applyForward(intersection, intersection);
            return intersection;
        }

        // walk our way along the ray, asking for intersections along the way
        int iter = 0;
        while (iter < _maxChecks) {

            // check the triangles of main square for intersection.
            if (checkTriangles(tracer.getGridLocation()[0], tracer.getGridLocation()[1], intersection, normalStore,
                    tracer, level, _workEyePos)) {
                // we found an intersection, so return that!
                terrainWorldTransform.applyForward(intersection, intersection);
                terrainWorldTransform.applyForward(normalStore, normalStore);
                return intersection;
            }

            // because of how we get our height coords, we will
            // sometimes be off be a grid spot, so we check the next
            // grid space up.
            int dx = 0, dy = 0;
            final Direction d = tracer.getLastStepDirection();
            switch (d) {
                case PositiveX:
                case NegativeX:
                    dx = 0;
                    dy = 1;
                    break;
                case PositiveZ:
                case NegativeZ:
                    dx = 1;
                    dy = 0;
                    break;
                case NegativeY:
                case None:
                case PositiveY:
                default:
                    ; // ignore
            }

            if (checkTriangles(tracer.getGridLocation()[0] + dx, tracer.getGridLocation()[1] + dy, intersection,
                    normalStore, tracer, level, _workEyePos)) {
                // we found an intersection, so return that!
                terrainWorldTransform.applyForward(intersection, intersection);
                terrainWorldTransform.applyForward(normalStore, normalStore);
                return intersection;
            }

            final double dist = tracer.getTotalTraveled();
            // look at where we are and switch to the next cliplevel if needed
            final Vector3 loc = new Vector3(_workRay.getDirection()).multiplyLocal(dist).addLocal(_workRay.getOrigin());
            final int newIndex = findClipIndex(loc.subtract(_workEyePos, null));
            // simple test to see if our next level at least has SOME data. XXX: could look to the tile level.
            if (newIndex != index && _clipmapLevels.get(index).isReady()) {
                _workRay.setOrigin(loc);
                index = newIndex;
                tracer = _tracers.get(index);
                level = _clipmapLevels.get(index);
                tracer.startWalk(_workRay);
            } else {
                tracer.next();
            }

            iter++;
        }

        return null;
    }

    private int findClipIndex(final ReadOnlyVector3 pointInEyeSpace) {
        final Vector2 gridPoint = _tracers.get(_minLevel).get2DPoint(pointInEyeSpace, null);
        final int maxDist = Math.max(Math.abs((int) gridPoint.getX()), Math.abs((int) gridPoint.getY()))
                / (_clipmapLevels.get(_minLevel).getClipSideSize() + 1 >> 1);
        int index = (int) MathUtils.floor(Math.log(maxDist) / Math.log(2)) + 1;
        index = MathUtils.clamp(index, _minLevel, _maxLevel);
        return index;
    }

    public int getMaxChecks() {
        return _maxChecks;
    }

    public void setMaxChecks(final int max) {
        _maxChecks = max;
    }

    public List<ClipmapLevel> getPyramid() {
        return _clipmapLevels;
    }

    /**
     * Check the two triangles of a given grid space for intersection.
     *
     * @param gridX
     *            grid row
     * @param gridY
     *            grid column
     * @param store
     *            the store variable
     * @param tracer
     * @return true if a pick was found on these triangles.
     */
    private boolean checkTriangles(final int gridX, final int gridY, final Vector3 store, final Vector3 normalStore,
            final AbstractBresenhamTracer tracer, final ClipmapLevel level, final ReadOnlyVector3 eyePos) {
        if (!getTriangles(gridX, gridY, tracer, level, eyePos)) {
            return false;
        }

        if (!_workRay.intersectsTriangle(_gridTriA.getA(), _gridTriA.getB(), _gridTriA.getC(), store)) {
            final boolean intersects = _workRay.intersectsTriangle(_gridTriB.getA(), _gridTriB.getB(), _gridTriB.getC(),
                    store);
            if (intersects && normalStore != null) {
                final Vector3 edge1 = Vector3.fetchTempInstance().set(_gridTriB.getB()).subtractLocal(_gridTriB.getA());
                final Vector3 edge2 = Vector3.fetchTempInstance().set(_gridTriB.getC()).subtractLocal(_gridTriB.getA());
                normalStore.set(edge1).crossLocal(edge2).normalizeLocal();
            }
            return intersects;
        } else {
            if (normalStore != null) {
                final Vector3 edge1 = Vector3.fetchTempInstance().set(_gridTriA.getB()).subtractLocal(_gridTriA.getA());
                final Vector3 edge2 = Vector3.fetchTempInstance().set(_gridTriA.getC()).subtractLocal(_gridTriA.getA());
                normalStore.set(edge1).crossLocal(edge2).normalizeLocal();
            }

            return true;
        }
    }

    /**
     * Calculate the triangles (in world coordinate space) of a Pyramid that correspond to the given grid location. The
     * triangles are stored in the class fields _gridTriA and _gridTriB.
     *
     * @param gridX
     *            grid row
     * @param gridY
     *            grid column
     * @return true if the grid square was found, false otherwise.
     */
    private boolean getTriangles(final int gridX, final int gridY, final AbstractBresenhamTracer tracer,
            final ClipmapLevel level, final ReadOnlyVector3 eyePos) {
        // TODO: pull this with updateRegion instead, then apply W
        level.getCache().getEyeCoords(tileStore, gridX, gridY, eyePos);
        // final float h1 = level.getCache().getHeight(gridX, gridY);
        // final float h2 = level.getCache().getHeight(gridX + 1, gridY);
        // final float h3 = level.getCache().getHeight(gridX, gridY + 1);
        // final float h4 = level.getCache().getHeight(gridX + 1, gridY + 1);

        final float scaledClipSideSize = level.getClipSideSize() * level.getVertexDistance() * 0.5f;

        final float h1 = getWeightedHeight(tileStore[0], tileStore[1], tileStore[2], tileStore[3], scaledClipSideSize);
        final float h2 = getWeightedHeight(tileStore[4], tileStore[5], tileStore[6], tileStore[7], scaledClipSideSize);
        final float h3 = getWeightedHeight(tileStore[8], tileStore[9], tileStore[10], tileStore[11],
                scaledClipSideSize);
        final float h4 = getWeightedHeight(tileStore[12], tileStore[13], tileStore[14], tileStore[15],
                scaledClipSideSize);

        final Vector3 scaleVec = Vector3.fetchTempInstance();
        final Vector3 workVec = Vector3.fetchTempInstance();

        scaleVec.set(tracer.getGridSpacing());

        // First triangle (h1, h3, h2)
        tracer.get3DPoint(gridX, gridY, h1, workVec);
        workVec.multiplyLocal(scaleVec).addLocal(tracer.getGridOrigin());
        _gridTriA.setA(workVec);

        tracer.get3DPoint(gridX, gridY + 1, h3, workVec);
        workVec.multiplyLocal(scaleVec).addLocal(tracer.getGridOrigin());
        _gridTriA.setB(workVec);

        tracer.get3DPoint(gridX + 1, gridY, h2, workVec);
        workVec.multiplyLocal(scaleVec).addLocal(tracer.getGridOrigin());
        _gridTriA.setC(workVec);

        // Second triangle (h2, h3, h4)
        _gridTriB.setA(_gridTriA.getC());
        _gridTriB.setB(_gridTriA.getB());

        tracer.get3DPoint(gridX + 1, gridY + 1, h4, workVec);
        workVec.multiplyLocal(scaleVec).addLocal(tracer.getGridOrigin());
        _gridTriB.setC(workVec);

        Vector3.releaseTempInstance(scaleVec);
        Vector3.releaseTempInstance(workVec);

        return true;
    }

    private float getWeightedHeight(final float viewX, final float viewY, final float h, final float w,
            final float scaledClipSideSize) {
        final float maxDistance = Math.max(viewX, viewY) / scaledClipSideSize;
        final float blend = MathUtils.clamp((maxDistance - 0.51f) * 2.2f, 0.0f, 1.0f);
        return MathUtils.lerp(blend, h, w);
    }

    public void setMaxLevel(final int maxLevel) {
        _maxLevel = maxLevel;
    }

    public int getMaxLevel() {
        return _maxLevel;
    }

    public void setMinLevel(final int minLevel) {
        _minLevel = minLevel;
    }

    public int getMinLevel() {
        return _minLevel;
    }
}
