/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.client;

import java.nio.FloatBuffer;
import java.util.Set;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.extension.terrain.util.Region;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Camera.FrustumIntersect;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.util.geom.BufferUtils;

/**
 * ClipmapLevel is the visual representation of one lod level of height data.
 */
public class ClipmapLevel extends Mesh {

    /**
     * Precalculated useful variables.
     */
    private final int doubleVertexDistance; // vertexDistance * 2;
    private final int frameDistance; // (frameSize - 1) * vertexDistance

    /**
     * Gets the distance between two horizontal/vertical vertices. The finest level L = 0 has a distance of
     * vertexDistance = 1. Then every level it doubles so vertexDistance is always 2^L
     */
    private final int vertexDistance;

    /**
     * Gets framesize in number of vertices between outer border of current clip and outer border of next finer (inner)
     * clip.
     */
    private final int frameSize;

    /**
     * Gets the width of a clip in number of vertices. This is always one less than power of two (2^x - 1)
     */
    private final int clipSideSize;

    /**
     * Gets the region of the current clip.
     */
    private final Region clipRegion;
    private final Region intersectionRegion;

    /**
     * Value that indicates the height scaling. It is also represents the maximum terrain height.
     */
    private final float heightScale;

    private float heightRangeMin = 0.0f;
    private float heightRangeMax = 1.0f;

    /**
     * Index to indicate how much vertices are added to the triangle strip.
     */
    private int stripIndex = 0;

    /**
     * The used terrain heightfield. This must be set per reference so all clip levels share the same memory for that
     * variable. The values are between 0.0f and 1.0f
     */
    private final TerrainCache cache;

    /**
     * Number of components for vertices
     */
    public static final int VERT_SIZE = 4;

    /**
     * Camera frustum to test clipmap tiles against for culling
     */
    private final Camera clipmapTestFrustum;

    /**
     * Used to handle transformations of the terrain
     */
    private final Vector3 transformedFrustumPos = new Vector3();

    /**
     * Should the clipmap generate per vertex normals
     */
    private final boolean generateNormals = false;

    /**
     * Bounding box used for culling
     */
    private final BoundingBox frustumCheckBounds = new BoundingBox();

    /**
     * Possible nio speedup when storing indices
     */
    private int[] tmpIndices;

    /**
     * Should cull blocks outside camera frustum
     */
    private boolean cullingEnabled = true;

    /**
     * Creates a new clipmaplevel.
     *
     * @param levelIndex
     *            Levelindex of the clipmap. If is 0 this will be the finest level
     * @param clipSideSize
     *            Number of vertices per clipside. Must be one less than power of two.
     * @param heightScale
     *            Maximum terrainheight and heightscale
     * @param fieldsize
     *            Width and heightvalue of the heightfield
     * @param heightfield
     *            Heightvalues with a range of 0.0f - 1.0f
     * @exception Exception
     */
    public ClipmapLevel(final int levelIndex, final Camera clipmapTestFrustum, final int clipSideSize,
            final float heightScale, final TerrainCache cache) throws Exception {
        super("Clipmap Level " + levelIndex);

        // Check some exception cases
        if (levelIndex < 0) {
            throw new Exception("levelIndex must be positive");
        }
        if (!MathUtils.isPowerOfTwo(clipSideSize + 1)) {
            throw new Exception("clipSideSize must be one less than power of two");
        }

        // Apply the values
        this.clipmapTestFrustum = clipmapTestFrustum;
        this.cache = cache;

        this.heightScale = heightScale;
        this.clipSideSize = clipSideSize;

        // Calculate common variables
        vertexDistance = (int) Math.pow(2, levelIndex);
        frameSize = (clipSideSize + 1) / 4;
        doubleVertexDistance = vertexDistance * 2;
        frameDistance = (frameSize - 1) * vertexDistance;
        clipRegion = new Region(0, 0, (clipSideSize - 1) * vertexDistance, (clipSideSize - 1) * vertexDistance);
        intersectionRegion = new Region(0, 0, clipSideSize * vertexDistance, clipSideSize * vertexDistance);

        // Initialize the vertices
        initialize();
    }

    /**
     * Initializes the vertices and indices.
     */
    private void initialize() {
        getMeshData().setIndexMode(IndexMode.TriangleStrip);

        // clipSideSize is the number of vertices per clipmapside, so number of all vertices is clipSideSize *
        // clipSideSize
        final FloatBuffer vertices = BufferUtils.createVector4Buffer(clipSideSize * clipSideSize);
        getMeshData().setVertexCoords(new FloatBufferData(vertices, 4));

        if (generateNormals) {
            final FloatBuffer normals = BufferUtils.createVector3Buffer(clipSideSize * clipSideSize);
            getMeshData().setNormalCoords(new FloatBufferData(normals, 3));
        }

        // final FloatBuffer textureCoords = BufferUtils.createVector2Buffer(N * N);
        // getMeshData().setTextureBuffer(textureCoords, 0);

        final int indicesSize = 4 * (3 * frameSize * frameSize + clipSideSize * clipSideSize / 2 + 4 * frameSize - 10);
        final IndexBufferData<?> indices = BufferUtils.createIndexBufferData(indicesSize, vertices.capacity() - 1);
        tmpIndices = new int[indicesSize];
        getMeshData().setIndices(indices);

        // Go through all rows and fill them with vertexindices.
        for (int z = 0; z < clipSideSize - 1; z++) {
            fillRow(0, clipSideSize - 1, z, z + 1);
        }
    }

    public void updateCache() {
        getWorldTransform().applyInverse(clipmapTestFrustum.getLocation(), transformedFrustumPos);
        final int cx = (int) transformedFrustumPos.getX();
        final int cz = (int) transformedFrustumPos.getZ();

        // Calculate the new position
        int clipX = cx - (clipSideSize + 1) * vertexDistance / 2;
        int clipY = cz - (clipSideSize + 1) * vertexDistance / 2;

        // Calculate the modulo to doubleVertexDistance of the new position.
        // This makes sure that the current level always fits in the hole of the
        // coarser level. The gridspacing of the coarser level is vertexDistance * 2, so here doubleVertexDistance.
        final int modX = MathUtils.moduloPositive(clipX, doubleVertexDistance);
        final int modY = MathUtils.moduloPositive(clipY, doubleVertexDistance);
        clipX = clipX + doubleVertexDistance - modX;
        clipY = clipY + doubleVertexDistance - modY;

        cache.setCurrentPosition(clipX / vertexDistance, clipY / vertexDistance);

        // TODO
        cache.handleUpdateRequests();
    }

    /**
     * Update clipmap vertices
     *
     * @param center
     */
    public void updateVertices() {
        getWorldTransform().applyInverse(clipmapTestFrustum.getLocation(), transformedFrustumPos);
        final int cx = (int) transformedFrustumPos.getX();
        final int cz = (int) transformedFrustumPos.getZ();

        // Store the old position to be able to recover it if needed
        final int oldX = clipRegion.getX();
        final int oldZ = clipRegion.getY();

        // Calculate the new position
        clipRegion.setX(cx - (clipSideSize + 1) * vertexDistance / 2);
        clipRegion.setY(cz - (clipSideSize + 1) * vertexDistance / 2);

        // Calculate the modulo to doubleVertexDistance of the new position.
        // This makes sure that the current level always fits in the hole of the
        // coarser level. The gridspacing of the coarser level is vertexDistance * 2, so here doubleVertexDistance.
        final int modX = MathUtils.moduloPositive(clipRegion.getX(), doubleVertexDistance);
        final int modY = MathUtils.moduloPositive(clipRegion.getY(), doubleVertexDistance);
        clipRegion.setX(clipRegion.getX() + doubleVertexDistance - modX);
        clipRegion.setY(clipRegion.getY() + doubleVertexDistance - modY);

        // Calculate the moving distance
        final int dx = clipRegion.getX() - oldX;
        final int dz = clipRegion.getY() - oldZ;

        intersectionRegion.setX(clipRegion.getX());
        intersectionRegion.setY(clipRegion.getY());

        cache.setCurrentPosition(clipRegion.getX() / vertexDistance, clipRegion.getY() / vertexDistance);

        updateVertices(dx, dz);

        final Set<Tile> updatedTiles = cache.handleUpdateRequests();
        if (updatedTiles != null) {
            // TODO: only update what's changed
            regenerate();
        }
    }

    public void regenerate() {
        updateVertices(clipRegion.getWidth(), clipRegion.getHeight());
    }

    /**
     *
     * @param cx
     * @param cz
     */
    private void updateVertices(int dx, int dz) {
        if (dx == 0 && dz == 0) {
            return;
        }

        dx = MathUtils.clamp(dx, -clipSideSize + 1, clipSideSize - 1);
        dz = MathUtils.clamp(dz, -clipSideSize + 1, clipSideSize - 1);

        // Create some better readable variables.
        // This are just the bounds of the current level (the new region).
        final int xmin = clipRegion.getLeft() / vertexDistance;
        final int xmax = clipRegion.getRight() / vertexDistance;
        final int zmin = clipRegion.getTop() / vertexDistance;
        final int zmax = clipRegion.getBottom() / vertexDistance;

        final FloatBuffer vertices = getMeshData().getVertexBuffer();

        // Update the L shaped region.
        // This replaces the old data with the new one.
        if (dz > 0) {
            if (dx > 0) {
                cache.updateRegion(vertices, xmax - dx, zmin, dx + 1, zmax - zmin - dz + 1);
            } else if (dx < 0) {
                cache.updateRegion(vertices, xmin, zmin, -dx + 1, zmax - zmin - dz + 1);
            }

            cache.updateRegion(vertices, xmin, zmax - dz, xmax - xmin + 1, dz + 1);
        } else {
            if (dx > 0) {
                cache.updateRegion(vertices, xmax - dx, zmin - dz, dx + 1, zmax - zmin + dz + 1);
            } else if (dx < 0) {
                cache.updateRegion(vertices, xmin, zmin - dz, -dx + 1, zmax - zmin + dz + 1);
            }

            if (dz < 0) {
                cache.updateRegion(vertices, xmin, zmin, xmax - xmin + 1, -dz + 1);
            }
        }
        markDirty(DirtyType.Bounding);
    }

    /**
     * Updates the whole indexarray.
     *
     * @param nextFinerLevel
     * @param frustum
     */
    public void updateIndices(final ClipmapLevel nextFinerLevel) {
        // set the stripindex to zero. We start count vertices from here.
        // The stripindex will tell us how much of the array is used.
        stripIndex = 0;

        // MxM Block 1
        fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getTop(), clipRegion.getTop()
                + frameDistance);

        // MxM Block 2
        fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + 2 * frameDistance, clipRegion.getTop(),
                clipRegion.getTop() + frameDistance);

        // MxM Block 3
        fillBlock(clipRegion.getRight() - 2 * frameDistance, clipRegion.getRight() - frameDistance,
                clipRegion.getTop(), clipRegion.getTop() + frameDistance);

        // MxM Block 4
        fillBlock(clipRegion.getRight() - frameDistance, clipRegion.getRight(), clipRegion.getTop(),
                clipRegion.getTop() + frameDistance);

        // MxM Block 5
        fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getTop() + frameDistance,
                clipRegion.getTop() + 2 * frameDistance);

        // MxM Block 6
        fillBlock(clipRegion.getRight() - frameDistance, clipRegion.getRight(), clipRegion.getTop() + frameDistance,
                clipRegion.getTop() + 2 * frameDistance);

        // MxM Block 7
        fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getBottom() - 2
                * frameDistance, clipRegion.getBottom() - frameDistance);

        // MxM Block 8
        fillBlock(clipRegion.getRight() - frameDistance, clipRegion.getRight(), clipRegion.getBottom() - 2
                * frameDistance, clipRegion.getBottom() - frameDistance);

        // MxM Block 9
        fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getBottom() - frameDistance,
                clipRegion.getBottom());

        // MxM Block 10
        fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + 2 * frameDistance,
                clipRegion.getBottom() - frameDistance, clipRegion.getBottom());

        // MxM Block 11
        fillBlock(clipRegion.getRight() - 2 * frameDistance, clipRegion.getRight() - frameDistance,
                clipRegion.getBottom() - frameDistance, clipRegion.getBottom());

        // MxM Block 12
        fillBlock(clipRegion.getRight() - frameDistance, clipRegion.getRight(), clipRegion.getBottom() - frameDistance,
                clipRegion.getBottom());

        // Fixup Top
        fillBlock(clipRegion.getLeft() + 2 * frameDistance, clipRegion.getLeft() + 2 * frameDistance
                + doubleVertexDistance, clipRegion.getTop(), clipRegion.getTop() + frameDistance);

        // Fixup Left
        fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getTop() + 2 * frameDistance,
                clipRegion.getTop() + 2 * frameDistance + doubleVertexDistance);

        // Fixup Right
        fillBlock(clipRegion.getRight() - frameDistance, clipRegion.getRight(),
                clipRegion.getTop() + 2 * frameDistance, clipRegion.getTop() + 2 * frameDistance + doubleVertexDistance);

        // Fixup Bottom
        fillBlock(clipRegion.getLeft() + 2 * frameDistance, clipRegion.getLeft() + 2 * frameDistance
                + doubleVertexDistance, clipRegion.getBottom() - frameDistance, clipRegion.getBottom());

        if (nextFinerLevel != null) {
            if ((nextFinerLevel.clipRegion.getX() - clipRegion.getX()) / vertexDistance == frameSize) {
                if ((nextFinerLevel.clipRegion.getY() - clipRegion.getY()) / vertexDistance == frameSize) {
                    // Upper Left L Shape

                    // Up
                    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getRight() - frameDistance,
                            clipRegion.getTop() + frameDistance, clipRegion.getTop() + frameDistance + vertexDistance);
                    // Left
                    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + frameDistance
                            + vertexDistance, clipRegion.getTop() + frameDistance + vertexDistance,
                            clipRegion.getBottom() - frameDistance);
                } else {
                    // Lower Left L Shape

                    // Left
                    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + frameDistance
                            + vertexDistance, clipRegion.getTop() + frameDistance, clipRegion.getBottom()
                            - frameDistance - vertexDistance);

                    // Bottom
                    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getRight() - frameDistance,
                            clipRegion.getBottom() - frameDistance - vertexDistance, clipRegion.getBottom()
                            - frameDistance);
                }
            } else {
                if ((nextFinerLevel.clipRegion.getY() - clipRegion.getY()) / vertexDistance == frameSize) {
                    // Upper Right L Shape

                    // Up
                    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getRight() - frameDistance,
                            clipRegion.getTop() + frameDistance, clipRegion.getTop() + frameDistance + vertexDistance);
                    // Right
                    fillBlock(clipRegion.getRight() - frameDistance - vertexDistance, clipRegion.getRight()
                            - frameDistance, clipRegion.getTop() + frameDistance + vertexDistance,
                            clipRegion.getBottom() - frameDistance);
                } else {
                    // Lower Right L Shape

                    // Right
                    fillBlock(clipRegion.getRight() - frameDistance - vertexDistance, clipRegion.getRight()
                            - frameDistance, clipRegion.getTop() + frameDistance, clipRegion.getBottom()
                            - frameDistance - vertexDistance);

                    // Bottom
                    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getRight() - frameDistance,
                            clipRegion.getBottom() - frameDistance - vertexDistance, clipRegion.getBottom()
                            - frameDistance);
                }
            }
        }

        // Fill in the middle patch if most detailed layer
        if (nextFinerLevel == null) {
            fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + frameDistance + clipSideSize / 2,
                    clipRegion.getTop() + frameDistance, clipRegion.getTop() + frameDistance + clipSideSize / 2);

            fillBlock(clipRegion.getLeft() + frameDistance + clipSideSize / 2, clipRegion.getRight() - frameDistance,
                    clipRegion.getTop() + frameDistance, clipRegion.getTop() + frameDistance + clipSideSize / 2);

            fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + frameDistance + clipSideSize / 2,
                    clipRegion.getTop() + frameDistance + clipSideSize / 2, clipRegion.getBottom() - frameDistance);

            fillBlock(clipRegion.getLeft() + frameDistance + clipSideSize / 2, clipRegion.getRight() - frameDistance,
                    clipRegion.getTop() + frameDistance + clipSideSize / 2, clipRegion.getBottom() - frameDistance);
        }

        final IndexBufferData<?> indices = getMeshData().getIndices();
        indices.clear();
        indices.put(tmpIndices, 0, getStripIndex());
        indices.flip();
    }

    /**
     * Fills a specified area to indexarray. This will be added only after a bounding test pass.
     *
     * @param left
     * @param right
     * @param top
     * @param bottom
     */
    private void fillBlock(int left, int right, int top, int bottom) {
        if (cullingEnabled) {
            // Setup the boundingbox of the block to fill.
            // The lowest value is zero, the highest is the scalesize.
            frustumCheckBounds.setCenter((left + right) * 0.5, (heightRangeMax + heightRangeMin) * heightScale * 0.5,
                    (top + bottom) * 0.5);
            frustumCheckBounds.setXExtent((left - right) * 0.5);
            frustumCheckBounds.setYExtent((heightRangeMax - heightRangeMin) * heightScale * 0.5);
            frustumCheckBounds.setZExtent((top - bottom) * 0.5);

            frustumCheckBounds.transform(getWorldTransform(), frustumCheckBounds);

            final int state = clipmapTestFrustum.getPlaneState();

            final boolean isVisible = clipmapTestFrustum.contains(frustumCheckBounds) != FrustumIntersect.Outside;
            clipmapTestFrustum.setPlaneState(state);

            if (!isVisible) {
                return;
            }
        }

        // Same moduloprocedure as when we updated the vertices.
        // Maps the terrainposition to arrayposition.
        left = left / vertexDistance % clipSideSize;
        right = right / vertexDistance % clipSideSize;
        top = top / vertexDistance % clipSideSize;
        bottom = bottom / vertexDistance % clipSideSize;
        left += left < 0 ? clipSideSize : 0;
        right += right < 0 ? clipSideSize : 0;
        top += top < 0 ? clipSideSize : 0;
        bottom += bottom < 0 ? clipSideSize : 0;

        // Now fill the block.
        if (bottom < top) {
            // Bottom border is positioned somwhere over the top border,
            // we have a wrapover so we must split up the update in two parts.

            // Go from top border to the end of the array and update every row
            for (int z = top; z <= clipSideSize - 2; z++) {
                fillRow(left, right, z, z + 1);
            }

            // Update the wrapover row
            fillRow(left, right, clipSideSize - 1, 0);

            // Go from arraystart to the bottom border and update every row.
            for (int z = 0; z <= bottom - 1; z++) {
                fillRow(left, right, z, z + 1);
            }
        } else {
            // Top border is over the bottom border. Update from top to bottom.
            for (int z = top; z <= bottom - 1; z++) {
                fillRow(left, right, z, z + 1);
            }
        }
    }

    /**
     * Fills a strip of triangles that can be build between vertices row Zn and Zn1.
     *
     * @param startX
     *            Start x-coordinate
     * @param endX
     *            End x-coordinate
     * @param rowZ
     *            Row n
     * @param rowZPlus1
     *            Row n + 1
     */
    private void fillRow(final int startX, final int endX, final int rowZ, final int rowZPlus1) {
        addIndex(startX, rowZPlus1);
        if (startX <= endX) {
            for (int x = startX; x <= endX; x++) {
                addIndex(x, rowZPlus1);
                addIndex(x, rowZ);
            }
        } else {
            for (int x = startX; x <= clipSideSize - 1; x++) {
                addIndex(x, rowZPlus1);
                addIndex(x, rowZ);
            }
            for (int x = 0; x <= endX; x++) {
                addIndex(x, rowZPlus1);
                addIndex(x, rowZ);
            }
        }
        addIndex(endX, rowZ);
    }

    /**
     * Adds a specific index to indexarray.
     *
     * @param x
     * @param z
     */
    private void addIndex(final int x, final int z) {
        // add the index and increment counter.
        tmpIndices[stripIndex++] = x + z * clipSideSize;
    }

    /**
     * Gets the number of triangles that are visible in current frame. This changes every frame.
     */
    public int getStripIndex() {
        return stripIndex;
    }

    /**
     * @return the vertexDistance
     */
    public int getVertexDistance() {
        return vertexDistance;
    }

    public boolean isReady() {
        return cache.isValid();
    }

    public TerrainCache getCache() {
        return cache;
    }

    public void setHeightRange(final float heightRangeMin, final float heightRangeMax) {
        this.heightRangeMin = heightRangeMin;
        this.heightRangeMax = heightRangeMax;
    }

    public float getHeightRangeMax() {
        return heightRangeMax;
    }

    public float getHeightRangeMin() {
        return heightRangeMin;
    }

    public int getClipSideSize() {
        return clipSideSize;
    }

    public Region getClipRegion() {
        return clipRegion;
    }

    public Region getIntersectionRegion() {
        return intersectionRegion;
    }

    public float getHeightScale() {
        return heightScale;
    }

    public boolean isCullingEnabled() {
        return cullingEnabled;
    }

    public void setCullingEnabled(final boolean cullingEnabled) {
        this.cullingEnabled = cullingEnabled;
    }
}