/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.client;

import java.nio.FloatBuffer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.buffer.AbstractBufferData.VBOAccessMode;
import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.FloatBufferData;
import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.extension.terrain.util.Region;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Camera.FrustumIntersect;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.event.DirtyType;

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
   * Distance between two horizontal/vertical vertices. The finest level L = 0 has a distance of
   * vertexDistance = 1. Then every level it doubles so vertexDistance is always 2^L
   */
  private final int vertexDistance;

  /**
   * Number of vertices between outer border of current clip and outer border of next finer (inner)
   * clip.
   */
  private final int frameSize;

  /**
   * Width of a clip in number of vertices. This is always one less than power of two (2^x - 1)
   */
  private final int clipSideSize;

  /**
   * Region of the current clip.
   */
  private final Region clipRegion;
  private final Region intersectionRegion;

  /**
   * Maximum terrain height.
   */
  private final float heightScale;

  private float heightRangeMin = 0.0f;
  private float heightRangeMax = 1.0f;

  private int oldCX = Integer.MIN_VALUE;
  private int oldCZ = Integer.MIN_VALUE;

  /**
   * Index to indicate how many vertices are added to the triangle strip.
   */
  private int stripIndex = 0;

  /**
   * The used terrain height-field. This must be set per reference so all clip levels share the same
   * memory for that variable. The values are between 0.0f and 1.0f
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
   * Bounding box used for culling
   */
  private final BoundingBox frustumCheckBounds = new BoundingBox();

  /**
   * Possible nio speedup when storing indices
   */
  private byte[] tmpIndicesByte;
  private short[] tmpIndicesShort;
  private int[] tmpIndicesInt;
  private int indexType;

  /**
   * Should cull blocks outside camera frustum
   */
  private boolean cullingEnabled = true;

  /**
   * Creates a new clipmaplevel.
   *
   * @param levelIndex
   *          Levelindex of the clipmap. If is 0 this will be the finest level
   * @param clipmapTestFrustum
   *          Camera used to acquire a frustum to test view clipping.
   * @param clipSideSize
   *          Number of vertices per clipside. Must be one less than power of two.
   * @param heightScale
   *          Maximum terrain height
   * @param cache
   *          Level specific height data cache.
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

    setProperty("vertexDistance", vertexDistance);

    // Initialize the vertices
    initialize();
  }

  /**
   * Initializes the vertices and indices.
   */
  private void initialize() {
    final MeshData meshData = getMeshData();

    meshData.setIndexMode(IndexMode.TriangleStrip);

    // clipSideSize is the number of vertices per clipmapside, so number of all vertices is clipSideSize
    // *
    // clipSideSize
    final FloatBufferData vertices =
        new FloatBufferData(BufferUtils.createVector4Buffer(clipSideSize * clipSideSize), 4);
    vertices.setVboAccessMode(VBOAccessMode.DynamicDraw);
    meshData.setVertexCoords(vertices);

    final int indicesSize = 4 * (3 * frameSize * frameSize + clipSideSize * clipSideSize / 2 + 4 * frameSize - 10);
    final IndexBufferData<?> indices = BufferUtils.createIndexBufferData(indicesSize, vertices.getBufferCapacity() - 1);
    indices.setVboAccessMode(VBOAccessMode.DynamicDraw);
    indexType = indices.getByteCount();
    switch (indexType) {
      case 2:
        tmpIndicesShort = new short[indicesSize];
        break;
      case 4:
        tmpIndicesInt = new int[indicesSize];
        break;
      case 1:
        tmpIndicesByte = new byte[indicesSize];
        break;
    }
    meshData.setIndices(indices);

    // Go through all rows and fill them with vertexindices.
    for (int z = 0; z < clipSideSize - 1; z++) {
      fillRow(0, clipSideSize - 1, z, z + 1);
    }
  }

  /**
   * Update our vertex buffer for any changes in frustum camera position or cache events.
   */
  public void updateVertices() {
    // get our frustum camera's location relative to the world transform of this clipmap.
    getWorldTransform().applyInverse(clipmapTestFrustum.getLocation(), transformedFrustumPos);
    final int cx = (int) transformedFrustumPos.getX();
    final int cz = (int) transformedFrustumPos.getZ();

    // Check if we need to update our clip or intersection regions)
    // The calculations are stable if our integer position has not changed.
    if (oldCX != cx || oldCZ != cz) {
      oldCX = cx;
      oldCZ = cz;

      // Store the old region positions to diff against any changes
      final int oldClipX = clipRegion.getX();
      final int oldClipZ = clipRegion.getY();

      // Calculate the new position
      clipRegion.setX(cx - (clipSideSize + 1) * vertexDistance / 2);
      clipRegion.setY(cz - (clipSideSize + 1) * vertexDistance / 2);

      // Calculate the modulo to doubleVertexDistance of the new position.
      // This makes sure that the current level always fits in the hole of the
      // coarser level. The grid spacing of the coarser level is vertexDistance * 2, so here
      // doubleVertexDistance.
      final int modX = MathUtils.moduloPositive(clipRegion.getX(), doubleVertexDistance);
      final int modY = MathUtils.moduloPositive(clipRegion.getY(), doubleVertexDistance);
      clipRegion.setX(clipRegion.getX() + doubleVertexDistance - modX);
      clipRegion.setY(clipRegion.getY() + doubleVertexDistance - modY);

      // Update our intersection region
      intersectionRegion.setX(clipRegion.getX());
      intersectionRegion.setY(clipRegion.getY());

      // Calculate the moving distance
      final int dx = clipRegion.getX() - oldClipX;
      final int dz = clipRegion.getY() - oldClipZ;

      // Use our clip region to set the cache position - this is not the same thing as our camera
      // position. We do this, even if the region has not changed, because it gives the system a chance to
      // refresh tiles
      cache.setCurrentPosition(clipRegion.getX() / vertexDistance, clipRegion.getY() / vertexDistance);

      // Apply our delta change to the vertex buffer.
      if (updateVerticesForMovement(dx, dz)) {
        // We have updated vertex data, so mark our buffer to be re-sent to the card
        getMeshData().markBufferDirty(MeshData.KEY_VertexCoords);
        markDirty(DirtyType.Bounding);
      }
    }

    // Ask the cache to look for any invalid data in the current region. These will be sent to the
    // mailbox
    cache.checkForInvalidatedRegions();
  }

  public void regenerate() {
    cache.regenerate();
  }

  /**
   * Update vertices in our clip level based on a movement from our old position.
   *
   * @param deltaX
   *          localized delta change in X
   * @param deltaZ
   *          localized delta change in Z (recall that Ardor3D uses Y up)
   * @return true if we touched the vertex buffer
   */
  private boolean updateVerticesForMovement(final int deltaX, final int deltaZ) {
    if (deltaX == 0 && deltaZ == 0) {
      return false;
    }

    final int dx = MathUtils.clamp(deltaX, -clipSideSize + 1, clipSideSize - 1);
    final int dz = MathUtils.clamp(deltaZ, -clipSideSize + 1, clipSideSize - 1);

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

    return true;
  }

  /**
   * Updates the whole index buffer.
   *
   * FIXME: Check if we can avoid doing this *every* frame.
   *
   * @param nextFinerLevel
   * @param frustum
   */
  public void updateIndices(final ClipmapLevel nextFinerLevel) {
    // set the stripindex to zero. We start count vertices from here.
    // The stripindex will tell us how much of the array is used.
    stripIndex = 0;

    // MxM Block 1
    fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getTop(),
        clipRegion.getTop() + frameDistance);

    // MxM Block 2
    fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + 2 * frameDistance, clipRegion.getTop(),
        clipRegion.getTop() + frameDistance);

    // MxM Block 3
    fillBlock(clipRegion.getRight() - 2 * frameDistance, clipRegion.getRight() - frameDistance, clipRegion.getTop(),
        clipRegion.getTop() + frameDistance);

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
    fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getBottom() - 2 * frameDistance,
        clipRegion.getBottom() - frameDistance);

    // MxM Block 8
    fillBlock(clipRegion.getRight() - frameDistance, clipRegion.getRight(), clipRegion.getBottom() - 2 * frameDistance,
        clipRegion.getBottom() - frameDistance);

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
    fillBlock(clipRegion.getLeft() + 2 * frameDistance, clipRegion.getLeft() + 2 * frameDistance + doubleVertexDistance,
        clipRegion.getTop(), clipRegion.getTop() + frameDistance);

    // Fixup Left
    fillBlock(clipRegion.getLeft(), clipRegion.getLeft() + frameDistance, clipRegion.getTop() + 2 * frameDistance,
        clipRegion.getTop() + 2 * frameDistance + doubleVertexDistance);

    // Fixup Right
    fillBlock(clipRegion.getRight() - frameDistance, clipRegion.getRight(), clipRegion.getTop() + 2 * frameDistance,
        clipRegion.getTop() + 2 * frameDistance + doubleVertexDistance);

    // Fixup Bottom
    fillBlock(clipRegion.getLeft() + 2 * frameDistance, clipRegion.getLeft() + 2 * frameDistance + doubleVertexDistance,
        clipRegion.getBottom() - frameDistance, clipRegion.getBottom());

    if (nextFinerLevel != null) {
      if ((nextFinerLevel.clipRegion.getX() - clipRegion.getX()) / vertexDistance == frameSize) {
        if ((nextFinerLevel.clipRegion.getY() - clipRegion.getY()) / vertexDistance == frameSize) {
          // Upper Left L Shape

          // Up
          fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getRight() - frameDistance,
              clipRegion.getTop() + frameDistance, clipRegion.getTop() + frameDistance + vertexDistance);
          // Left
          fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + frameDistance + vertexDistance,
              clipRegion.getTop() + frameDistance + vertexDistance, clipRegion.getBottom() - frameDistance);
        } else {
          // Lower Left L Shape

          // Left
          fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getLeft() + frameDistance + vertexDistance,
              clipRegion.getTop() + frameDistance, clipRegion.getBottom() - frameDistance - vertexDistance);

          // Bottom
          fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getRight() - frameDistance,
              clipRegion.getBottom() - frameDistance - vertexDistance, clipRegion.getBottom() - frameDistance);
        }
      } else {
        if ((nextFinerLevel.clipRegion.getY() - clipRegion.getY()) / vertexDistance == frameSize) {
          // Upper Right L Shape

          // Up
          fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getRight() - frameDistance,
              clipRegion.getTop() + frameDistance, clipRegion.getTop() + frameDistance + vertexDistance);
          // Right
          fillBlock(clipRegion.getRight() - frameDistance - vertexDistance, clipRegion.getRight() - frameDistance,
              clipRegion.getTop() + frameDistance + vertexDistance, clipRegion.getBottom() - frameDistance);
        } else {
          // Lower Right L Shape

          // Right
          fillBlock(clipRegion.getRight() - frameDistance - vertexDistance, clipRegion.getRight() - frameDistance,
              clipRegion.getTop() + frameDistance, clipRegion.getBottom() - frameDistance - vertexDistance);

          // Bottom
          fillBlock(clipRegion.getLeft() + frameDistance, clipRegion.getRight() - frameDistance,
              clipRegion.getBottom() - frameDistance - vertexDistance, clipRegion.getBottom() - frameDistance);
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

    final MeshData meshData = getMeshData();
    final IndexBufferData<?> indices = meshData.getIndices();
    indices.clear();
    switch (indexType) {
      case 2:
        indices.put(tmpIndicesShort, 0, getStripIndex());
        break;
      case 4:
        indices.put(tmpIndicesInt, 0, getStripIndex());
        break;
      case 1:
        indices.put(tmpIndicesByte, 0, getStripIndex());
        break;
    }
    indices.flip();
    meshData.markIndicesDirty();
  }

  /**
   * Does a quick frustum check, then fills a specified area of our indices.
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
   *          Start x-coordinate
   * @param endX
   *          End x-coordinate
   * @param rowZ
   *          Row n
   * @param rowZPlus1
   *          Row n + 1
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
    switch (indexType) {
      case 2:
        tmpIndicesShort[stripIndex++] = (short) (x + z * clipSideSize);
        break;
      case 4:
        tmpIndicesInt[stripIndex++] = x + z * clipSideSize;
        break;
      case 1:
        tmpIndicesByte[stripIndex++] = (byte) (x + z * clipSideSize);
        break;
    }
  }

  /**
   * Gets the number of triangles that are visible in current frame. This changes every frame.
   */
  public int getStripIndex() { return stripIndex; }

  /**
   * @return the vertexDistance
   */
  public int getVertexDistance() { return vertexDistance; }

  public boolean isReady() { return cache.isValid(); }

  public TerrainCache getCache() { return cache; }

  public void setHeightRange(final float heightRangeMin, final float heightRangeMax) {
    this.heightRangeMin = heightRangeMin;
    this.heightRangeMax = heightRangeMax;
  }

  public float getHeightRangeMax() { return heightRangeMax; }

  public float getHeightRangeMin() { return heightRangeMin; }

  public int getClipSideSize() { return clipSideSize; }

  public Region getClipRegion() { return clipRegion; }

  public Region getIntersectionRegion() { return intersectionRegion; }

  public float getHeightScale() { return heightScale; }

  public boolean isCullingEnabled() { return cullingEnabled; }

  public void setCullingEnabled(final boolean cullingEnabled) { this.cullingEnabled = cullingEnabled; }
}
