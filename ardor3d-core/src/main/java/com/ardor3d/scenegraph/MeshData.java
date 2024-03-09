/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.ardor3d.buffer.AbstractBufferData;
import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.ByteBufferData;
import com.ardor3d.buffer.FloatBufferData;
import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.buffer.IntBufferData;
import com.ardor3d.buffer.ShortBufferData;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.RenderContext.RenderContextRef;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.renderer.material.IShaderUtils;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.collection.Multimap;
import com.ardor3d.util.collection.SimpleMultimap;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.gc.ContextValueReference;

/**
 * MeshData contains all the commonly used buffers for rendering a mesh.
 */
public class MeshData implements Savable {

  public final static String KEY_VertexCoords = "vertex";
  public final static String KEY_NormalCoords = "normal";
  public final static String KEY_ColorCoords = "color";
  public final static String KEY_TangentCoords = "tangent";
  public final static String KEY_TextureCoordsPrefix = "uv";
  public final static String KEY_TextureCoords0 = MeshData.KEY_TextureCoordsPrefix + 0;
  public final static String KEY_TextureCoords1 = MeshData.KEY_TextureCoordsPrefix + 1;
  public final static String KEY_TextureCoords2 = MeshData.KEY_TextureCoordsPrefix + 2;
  public final static String KEY_TextureCoords3 = MeshData.KEY_TextureCoordsPrefix + 3;
  public static final String KEY_InstanceMatrix = "instanceMatrix";

  /** The Constant logger. */
  private static final Logger logger = Logger.getLogger(MeshData.class.getName());

  private static final Map<MeshData, Object> _identityCache = new WeakHashMap<>();
  private static final Object STATIC_REF = new Object();

  private static final ReferenceQueue<MeshData> _vaoRefQueue = new ReferenceQueue<>();

  static {
    ContextManager.addContextCleanListener(renderContext -> MeshData.cleanAllVertexArrays(null, renderContext));
  }

  /** Number of vertices represented by this data. */
  protected int _vertexCount;

  /** Number of primitives represented by this data. */
  protected transient int[] _primitiveCounts = new int[1];

  /** Buffer data holding buffers and number of coordinates per vertex */
  protected Map<String, AbstractBufferData<? extends Buffer>> _vertexDataItems = new HashMap<>();

  /** Index data. */
  protected IndexBufferData<?> _indexBuffer;
  protected int[] _indexLengths;
  protected IndexMode[] _indexModes = new IndexMode[] {IndexMode.Triangles};

  protected transient ContextValueReference<MeshData, Integer> _vaoIdCache;

  public MeshData() {
    MeshData._identityCache.put(this, MeshData.STATIC_REF);
  }

  /**
   * Gets the vertex count.
   *
   * @return the vertex count
   */
  public int getVertexCount() { return _vertexCount; }

  /**
   * @param context
   *          the OpenGL context a vao belongs to.
   * @return the id of a vao in the given context. If the vao is not found in the given context, 0 is
   *         returned.
   */
  public int getVAOID(final RenderContext context) {
    return getVAOIDByRef(context.getUniqueContextRef());
  }

  /**
   * @param contextRef
   *          the reference to a shared OpenGL context a vao belongs to.
   * @return the id of a vao in the given context. If the vao is not found in the given context, 0 is
   *         returned.
   */
  public int getVAOIDByRef(final RenderContextRef contextRef) {
    if (_vaoIdCache != null) {
      final Integer id = _vaoIdCache.getValue(contextRef);
      if (id != null) {
        return id;
      }
    }
    return -1;
  }

  /**
   * Removes any vao id from this buffer's data for the given OpenGL context.
   *
   * @param context
   *          the OpenGL context a vao would belong to.
   * @return the id removed or 0 if not found.
   */
  public int removeVAOID(final RenderContext context) {
    final Integer id = _vaoIdCache.removeValue(context.getUniqueContextRef());
    return id != null ? id : 0;
  }

  /**
   * Sets the id for a vao based on this data in regards to the given OpenGL context.
   *
   * @param context
   *          the OpenGL context a vao belongs to.
   * @param id
   *          the id of a vao. To be valid, this must be not equal to 0.
   * @throws IllegalArgumentException
   *           if id is less than or equal to 0.
   */
  public void setVAOID(final RenderContext context, final int id) {
    if (id <= 0) {
      throw new IllegalArgumentException("id must be > 0");
    }

    if (_vaoIdCache == null) {
      _vaoIdCache = ContextValueReference.newReference(this, MeshData._vaoRefQueue);
    }
    _vaoIdCache.put(context.getUniqueContextRef(), id);
  }

  /**
   * Mark a specific data buffer as dirty in this MeshData. Also calls {@link #markBuffersDirty()}
   *
   * @param key
   *          the key of the buffer to mark dirty.
   * @throws Ardor3DException
   *           if buffer is not found
   */
  public void markBufferDirty(final String key) {
    final AbstractBufferData<?> data = getCoords(key);
    if (data == null) {
      throw new Ardor3dException("Buffer not found: " + key);
    }

    data.markDirty();
  }

  /**
   * Marks the indices as dirty in this MeshData. Also calls {@link #markBuffersDirty()}
   *
   * @throws Ardor3DException
   *           if buffer is not found
   */
  public void markIndicesDirty() {
    final AbstractBufferData<?> data = getIndices();
    if (data == null) {
      return;
    }

    data.markDirty();
  }

  /**
   * Gets the nio buffer associated with the given key.
   *
   * @return the buffer for the associated buffer data
   */
  public <T extends Buffer> T getBuffer(final String key) {
    final AbstractBufferData<T> coords = getCoords(key);
    if (coords == null) {
      return null;
    }
    return coords.getBuffer();
  }

  /**
   * Gets the Ardor3D buffer data object associated with the given key.
   *
   * @return the buffer data object
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <T extends AbstractBufferData> T getCoords(final String key) {
    return (T) _vertexDataItems.get(key);
  }

  /**
   * @param key
   *          the key to check
   * @return true if we have a vertex data item stored for the given key.
   */
  public boolean containsKey(final String key) {
    return _vertexDataItems.containsKey(key);
  }

  /**
   * Sets the Ardor3D buffer data object associated with a given key.
   *
   * @param key
   *          the key to store under. Also used as the varying name in shaders.
   * @param bufferData
   *          the new buffer data object
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <T extends AbstractBufferData> void setCoords(final String key, final T bufferData) {
    if (bufferData == null) {
      _vertexDataItems.remove(key);
    } else {
      _vertexDataItems.put(key, bufferData);
    }
  }

  public Collection<Entry<String, AbstractBufferData<? extends Buffer>>> listDataItems() {
    return Collections.unmodifiableCollection(_vertexDataItems.entrySet());
  }

  /**
   * Gets the vertex buffer.
   *
   * @return the vertex buffer
   */
  public FloatBuffer getVertexBuffer() { return getBuffer(MeshData.KEY_VertexCoords); }

  /**
   * Sets the vertex buffer.
   *
   * @param vertexBuffer
   *          the new vertex buffer
   */
  public void setVertexBuffer(final FloatBuffer vertexBuffer) {
    if (vertexBuffer == null) {
      setVertexCoords(null);
    } else {
      setVertexCoords(new FloatBufferData(vertexBuffer, 3));
    }
  }

  /**
   * Gets the vertex coords.
   *
   * @return the vertex coords
   */
  public FloatBufferData getVertexCoords() { return getCoords(MeshData.KEY_VertexCoords); }

  /**
   * Sets the vertex coords.
   *
   * @param bufferData
   *          the new vertex coords
   */
  public void setVertexCoords(final FloatBufferData bufferData) {
    setCoords(MeshData.KEY_VertexCoords, bufferData);
    updateVertexCount();
  }

  /**
   * Gets the normal buffer.
   *
   * @return the normal buffer
   */
  public FloatBuffer getNormalBuffer() { return getBuffer(MeshData.KEY_NormalCoords); }

  /**
   * Sets the normal buffer.
   *
   * @param normalBuffer
   *          the new normal buffer
   */
  public void setNormalBuffer(final FloatBuffer normalBuffer) {
    if (normalBuffer == null) {
      setNormalCoords(null);
    } else {
      setNormalCoords(new FloatBufferData(normalBuffer, 3));
    }
  }

  /**
   * Gets the normal coords.
   *
   * @return the normal coords
   */
  public FloatBufferData getNormalCoords() { return getCoords(MeshData.KEY_NormalCoords); }

  /**
   * Sets the normal coords.
   *
   * @param bufferData
   *          the new normal coords
   */
  public void setNormalCoords(final FloatBufferData bufferData) {
    setCoords(MeshData.KEY_NormalCoords, bufferData);
  }

  /**
   * Gets the color buffer.
   *
   * @return the color buffer
   */
  public FloatBuffer getColorBuffer() { return getBuffer(MeshData.KEY_ColorCoords); }

  /**
   * Sets the color buffer.
   *
   * @param colorBuffer
   *          the new color buffer
   */
  public void setColorBuffer(final FloatBuffer colorBuffer) {
    if (colorBuffer == null) {
      setColorCoords(null);
    } else {
      setColorCoords(new FloatBufferData(colorBuffer, 4));
    }
  }

  /**
   * Gets the color coords.
   *
   * @return the color coords
   */
  public FloatBufferData getColorCoords() { return getCoords(MeshData.KEY_ColorCoords); }

  /**
   * Sets the color coords.
   *
   * @param bufferData
   *          the new color coords
   */
  public void setColorCoords(final FloatBufferData bufferData) {
    setCoords(MeshData.KEY_ColorCoords, bufferData);
  }

  /**
   * Gets the tangent buffer.
   *
   * @return the tangent buffer
   */
  public FloatBuffer getTangentBuffer() { return getBuffer(MeshData.KEY_TangentCoords); }

  /**
   * Sets the tangent buffer.
   *
   * @param tangentBuffer
   *          the new tangent buffer
   */
  public void setTangentBuffer(final FloatBuffer tangentBuffer) {
    if (tangentBuffer == null) {
      setTangentCoords(null);
    } else {
      setTangentCoords(new FloatBufferData(tangentBuffer, 3));
    }
  }

  /**
   * Gets the tangent coords.
   *
   * @return the tangent coords
   */
  public FloatBufferData getTangentCoords() { return getCoords(MeshData.KEY_TangentCoords); }

  /**
   * Sets the tangent coords.
   *
   * @param bufferData
   *          the new tangent coords
   */
  public void setTangentCoords(final FloatBufferData bufferData) {
    setCoords(MeshData.KEY_TangentCoords, bufferData);
  }

  /**
   * Gets the FloatBuffer of the FloatBufferData set on a given texture unit.
   *
   * @param index
   *          the unit index
   *
   * @return the texture buffer for the given index, or null if none was set.
   */
  public FloatBuffer getTextureBuffer(final int index) {
    return getBuffer(MeshData.KEY_TextureCoordsPrefix + index);
  }

  /**
   * Sets the texture buffer for a given texture unit index. Interprets it as a 2 component float
   * buffer data. If you need other sizes, use setTextureCoords instead.
   *
   * @param textureBuffer
   *          the texture buffer
   * @param index
   *          the unit index
   * @see #setTextureCoords(FloatBufferData, int)
   */
  public void setTextureBuffer(final FloatBuffer textureBuffer, final int index) {
    if (textureBuffer == null) {
      setTextureCoords(null, index);
    } else {
      setTextureCoords(new FloatBufferData(textureBuffer, 2), index);
    }
  }

  /**
   * Gets the texture coords assigned to a specific texture unit index of this MeshData.
   *
   * @param index
   *          the texture unit index
   *
   * @return the texture coords
   */
  public FloatBufferData getTextureCoords(final int index) {
    return getCoords(MeshData.KEY_TextureCoordsPrefix + index);
  }

  /**
   * Sets the texture coords of a specific texture unit index to the given FloatBufferData.
   *
   * @param textureCoords
   *          the texture coords
   * @param index
   *          the unit index
   */
  public void setTextureCoords(final FloatBufferData textureCoords, final int index) {
    setCoords(MeshData.KEY_TextureCoordsPrefix + index, textureCoords);
  }

  public void clearAllTextureCoords() {
    _vertexDataItems.entrySet().removeIf(entry -> entry.getKey().startsWith(MeshData.KEY_TextureCoordsPrefix));
  }

  /**
   * Update the vertex count based on the current limit of the vertex buffer.
   */
  public void updateVertexCount() {
    final FloatBufferData vertexCoords = getVertexCoords();
    if (vertexCoords == null) {
      _vertexCount = 0;
    } else {
      _vertexCount = vertexCoords.getTupleCount();
    }
    // update primitive count if we are using arrays
    if (_indexBuffer == null) {
      updatePrimitiveCounts();
    }
  }

  /**
   * <code>copyTextureCoords</code> copies the texture coordinates of a given texture unit to another
   * location. If the texture unit is not valid, then the coordinates are ignored. Coords are
   * multiplied by the given factor.
   *
   * @param fromIndex
   *          the coordinates to copy.
   * @param toIndex
   *          the texture unit to set them to. Must not be the same as the fromIndex.
   * @param factor
   *          a multiple to apply when copying
   */
  public void copyTextureCoordinates(final int fromIndex, final int toIndex, final float factor) {
    copyTextureCoordinates(fromIndex, toIndex, factor, factor);
  }

  /**
   * <code>copyTextureCoords</code> copies the texture coordinates of a given texture unit to another
   * location. If the texture unit is not valid, then the coordinates are ignored. Coords are
   * multiplied by the given S and T factors.
   *
   * @param fromIndex
   *          the coordinates to copy.
   * @param toIndex
   *          the texture unit to set them to. Must not be the same as the fromIndex.
   * @param factorS
   *          a multiple to apply to the S channel when copying
   * @param factorT
   *          a multiple to apply to the T channel when copying
   */
  public void copyTextureCoordinates(final int fromIndex, final int toIndex, final float factorS, final float factorT) {
    final FloatBufferData src = getCoords(MeshData.KEY_TextureCoordsPrefix + fromIndex);
    if (src == null) {
      return;
    }

    FloatBufferData dest = getCoords(MeshData.KEY_TextureCoordsPrefix + toIndex);

    if (dest == null || dest.getBuffer().capacity() != src.getBuffer().limit()) {
      dest = new FloatBufferData(BufferUtils.createFloatBuffer(src.getBuffer().capacity()), src.getValuesPerTuple());
      setCoords(MeshData.KEY_TextureCoordsPrefix + toIndex, dest);
    }
    dest.getBuffer().clear();
    final int oldLimit = src.getBuffer().limit();
    src.getBuffer().clear();
    for (int i = 0, len = dest.getBuffer().capacity(); i < len; i++) {
      if (i % 2 == 0) {
        dest.getBuffer().put(factorS * src.getBuffer().get());
      } else {
        dest.getBuffer().put(factorT * src.getBuffer().get());
      }
    }
    src.getBuffer().limit(oldLimit);
    dest.getBuffer().limit(oldLimit);
  }

  /**
   * <code>getMaxTextureUnitUsed</code> returns the max texture unit this mesh data is currently
   * using.
   *
   * @return the max unit in use, or -1 if none found.
   */
  public int getMaxTextureUnitUsed() {
    int max = -1;
    for (final String key : _vertexDataItems.keySet()) {
      if (key.startsWith(MeshData.KEY_TextureCoordsPrefix)) {
        final int unit = Integer.parseInt(key.substring(MeshData.KEY_TextureCoordsPrefix.length()));
        if (unit > max) {
          max = unit;
        }
      }
    }

    return max;
  }

  /**
   * Gets the index buffer.
   *
   * @return the index buffer
   */
  public Buffer getIndexBuffer() {
    if (_indexBuffer == null) {
      return null;
    }
    return _indexBuffer.getBuffer();
  }

  /**
   * Sets the index buffer.
   *
   * @param indices
   *          the new index buffer
   */
  public void setIndexBuffer(final IntBuffer indices) {
    if (indices == null) {
      _indexBuffer = null;
    } else {
      _indexBuffer = new IntBufferData(indices);
    }
    updatePrimitiveCounts();
  }

  /**
   * Sets the index buffer.
   *
   * @param indices
   *          the new index buffer
   */
  public void setIndexBuffer(final ShortBuffer indices) {
    if (indices == null) {
      _indexBuffer = null;
    } else {
      _indexBuffer = new ShortBufferData(indices);
    }
    updatePrimitiveCounts();
  }

  /**
   * Sets the index buffer.
   *
   * @param indices
   *          the new index buffer
   */
  public void setIndexBuffer(final ByteBuffer indices) {
    if (indices == null) {
      _indexBuffer = null;
    } else {
      _indexBuffer = new ByteBufferData(indices);
    }
    updatePrimitiveCounts();
  }

  /**
   * Gets the indices.
   *
   * @return the indices
   */
  public IndexBufferData<?> getIndices() { return _indexBuffer; }

  /**
   * Sets the indices
   *
   * @param bufferData
   *          the new indices
   */
  public void setIndices(final IndexBufferData<?> bufferData) {
    _indexBuffer = bufferData;
    updatePrimitiveCounts();
  }

  /**
   * Sets the index mode.
   *
   * @param indexMode
   *          the new IndexMode to use for the first section of this MeshData.
   */
  public void setIndexMode(final IndexMode indexMode) {
    _indexModes[0] = indexMode;
    updatePrimitiveCounts();
  }

  /**
   * Gets the index lengths.
   *
   * @return the index lengths
   */
  public int[] getIndexLengths() { return _indexLengths; }

  /**
   * Sets the index lengths.
   *
   * @param indexLengths
   *          the new index lengths
   */
  public void setIndexLengths(final int[] indexLengths) {
    _indexLengths = indexLengths;
    updatePrimitiveCounts();
  }

  /**
   * Gets the index modes.
   *
   * @return the index modes
   */
  public IndexMode[] getIndexModes() { return _indexModes; }

  /**
   * Gets the index mode.
   *
   * @param sectionIndex
   *          the section index
   *
   * @return the index mode
   */
  public IndexMode getIndexMode(final int sectionIndex) {
    if (sectionIndex < 0 || sectionIndex >= getSectionCount()) {
      throw new IllegalArgumentException("invalid section index: " + sectionIndex);
    }
    return _indexModes.length > sectionIndex ? _indexModes[sectionIndex] : _indexModes[_indexModes.length - 1];
  }

  /**
   * Note: Also updates primitive counts.
   *
   * @param indexModes
   *          the index modes to use for this MeshData.
   */
  public void setIndexModes(final IndexMode[] indexModes) {
    _indexModes = indexModes;
    updatePrimitiveCounts();
  }

  /**
   * Gets the section count.
   *
   * @return the number of sections (lengths, indexModes, etc.) this MeshData contains.
   */
  public int getSectionCount() { return _indexLengths != null ? _indexLengths.length : 1; }

  /**
   * Gets the total primitive count.
   *
   * @return the sum of the primitive counts on all sections of this mesh data.
   */
  public int getTotalPrimitiveCount() {
    int count = 0;
    for (int i = 0; i < _primitiveCounts.length; i++) {
      count += _primitiveCounts[i];
    }
    return count;
  }

  /**
   * Gets the primitive count.
   *
   * @param section
   *          the section
   *
   * @return the number of primitives (triangles, lines, points, etc.) on a given section of this mesh
   *         data.
   */
  public int getPrimitiveCount(final int section) {
    return _primitiveCounts[section];
  }

  /**
   * Returns the vertex indices of a specified primitive.
   *
   * @param primitiveIndex
   *          which triangle, line, etc
   * @param section
   *          which section to pull from (corresponds to array position in indexmodes and lengths)
   * @param store
   *          an int array to store the results in. if null, or the length < the size of the
   *          primitive, a new array is created and returned.
   *
   * @return the primitive's vertex indices as an array
   *
   * @throws IndexOutOfBoundsException
   *           if primitiveIndex is outside of range [0, count-1] where count is the number of
   *           primitives in the given section.
   * @throws ArrayIndexOutOfBoundsException
   *           if section is out of range [0, N-1] where N is the number of sections in this MeshData
   *           object.
   */
  public int[] getPrimitiveIndices(final int primitiveIndex, final int section, final int[] store) {
    final int count = getPrimitiveCount(section);
    if (primitiveIndex >= count || primitiveIndex < 0) {
      throw new IndexOutOfBoundsException("Invalid primitiveIndex '" + primitiveIndex + "'.  Count is " + count);
    }

    final IndexMode mode = getIndexMode(section);
    final int rSize = mode.getVertexCount();

    int[] result = store;
    if (result == null || result.length < rSize) {
      result = new int[rSize];
    }

    for (int i = 0; i < rSize; i++) {
      if (getIndices() != null) {
        result[i] = getIndices().get(getVertexIndex(primitiveIndex, i, section));
      } else {
        result[i] = getVertexIndex(primitiveIndex, i, section);
      }
    }

    return result;
  }

  /**
   * Gets the vertices that make up the given primitive.
   *
   * @param primitiveIndex
   *          the primitive index
   * @param section
   *          the section
   * @param store
   *          the store. If null or the wrong size, we'll make a new array and return that instead.
   *
   * @return the primitive
   */
  public Vector3[] getPrimitiveVertices(final int primitiveIndex, final int section, final Vector3[] store) {
    final int count = getPrimitiveCount(section);
    if (primitiveIndex >= count || primitiveIndex < 0) {
      throw new IndexOutOfBoundsException("Invalid primitiveIndex '" + primitiveIndex + "'.  Count is " + count);
    }

    final IndexMode mode = getIndexMode(section);
    final int rSize = mode.getVertexCount();
    Vector3[] result = store;
    if (result == null || result.length < rSize) {
      result = new Vector3[rSize];
    }

    for (int i = 0; i < rSize; i++) {
      if (result[i] == null) {
        result[i] = new Vector3();
      }
      if (getIndices() != null) {
        // indexed geometry
        BufferUtils.populateFromBuffer(result[i], getVertexBuffer(),
            getIndices().get(getVertexIndex(primitiveIndex, i, section)));
      } else {
        // non-indexed geometry
        BufferUtils.populateFromBuffer(result[i], getVertexBuffer(), getVertexIndex(primitiveIndex, i, section));
      }
    }

    return result;
  }

  /**
   * Gets the texture coordinates of the primitive.
   *
   * @param primitiveIndex
   *          the primitive index
   * @param section
   *          the section
   * @param textureIndex
   *          the texture index
   * @param store
   *          the store
   *
   * @return the texture coordinates of the primitive
   */
  public Vector2[] getPrimitiveTextureCoords(final int primitiveIndex, final int section, final int textureIndex,
      final Vector2[] store) {
    Vector2[] result = null;
    if (getTextureBuffer(textureIndex) != null) {
      final int count = getPrimitiveCount(section);
      if (primitiveIndex >= count || primitiveIndex < 0) {
        throw new IndexOutOfBoundsException("Invalid primitiveIndex '" + primitiveIndex + "'.  Count is " + count);
      }
      final IndexMode mode = getIndexMode(section);
      final int rSize = mode.getVertexCount();
      result = store;
      if (result == null || result.length < rSize) {
        result = new Vector2[rSize];
      }
      for (int i = 0; i < rSize; i++) {
        if (result[i] == null) {
          result[i] = new Vector2();
        }
        if (getIndexBuffer() != null) {// indexed geometry
          BufferUtils.populateFromBuffer(result[i], getTextureBuffer(textureIndex),
              getIndices().get(getVertexIndex(primitiveIndex, i, section)));
        } else {// non-indexed geometry
          BufferUtils.populateFromBuffer(result[i], getTextureBuffer(textureIndex),
              getVertexIndex(primitiveIndex, i, section));
        }
      }
    }
    return result;
  }

  /**
   * Gets the vertex index.
   *
   * @param primitiveIndex
   *          which triangle, line, etc.
   * @param point
   *          which point on the triangle, line, etc. (triangle has three points, so this would be
   *          0-2, etc.)
   * @param section
   *          which section to pull from (corresponds to array position in indexmodes and lengths)
   *
   * @return the position you would expect to find the given point in the index buffer
   */
  // FIXME: Need to ignore adjacency verts
  public int getVertexIndex(final int primitiveIndex, final int point, final int section) {
    int index = 0;
    // move our offset up to the beginning of our section
    for (int i = 0; i < section; i++) {
      index += _indexLengths[i];
    }

    // Ok, now pull primitive index based on indexmode.
    switch (getIndexMode(section)) {
      case Triangles:
        index += (primitiveIndex * 3) + point;
        break;
      case TriangleStrip:
        // NB: we need to flip point 0 and 1 on odd primitiveIndex values
        if (point < 2 && primitiveIndex % 2 != 0) {
          index += primitiveIndex + (point == 0 ? 1 : 0);
        } else {
          index += primitiveIndex + point;
        }
        break;
      case TriangleFan:
        if (point == 0) {
          index += 0;
        } else {
          index += primitiveIndex + point;
        }
        break;
      case Points:
        index += primitiveIndex;
        break;
      case Lines:
        index += (primitiveIndex * 2) + point;
        break;
      case LineStrip:
      case LineLoop:
        index += primitiveIndex + point;
        break;
      default:
        MeshData.logger.warning("unimplemented index mode: " + getIndexMode(0));
        return -1;
    }
    return index;
  }

  /**
   * Random vertex.
   *
   * @param store
   *          the vector object to store the result in. if null, a new one is created.
   *
   * @return a random vertex from the vertices stored in this MeshData. null is returned if there are
   *         no vertices.
   */
  public Vector3 randomVertex(final Vector3 store) {
    final FloatBufferData vertexCoords = getVertexCoords();
    if (vertexCoords == null) {
      return null;
    }

    Vector3 result = store;
    if (result == null) {
      result = new Vector3();
    }

    final int i = MathUtils.nextRandomInt(0, getVertexCount() - 1);
    BufferUtils.populateFromBuffer(result, vertexCoords.getBuffer(), i);

    return result;
  }

  /**
   * Random point on primitives.
   *
   * @param store
   *          the vector object to store the result in. if null, a new one is created.
   *
   * @return a random point from the surface of a primitive stored in this MeshData. null is returned
   *         if there are no vertices or indices.
   */
  public Vector3 randomPointOnPrimitives(final Vector3 store) {
    final FloatBufferData vertexCoords = getVertexCoords();
    if (vertexCoords == null || _indexBuffer == null) {
      return null;
    }

    Vector3 result = store;
    if (result == null) {
      result = new Vector3();
    }

    // randomly pick a section (if there are more than 1)
    final int section = MathUtils.nextRandomInt(0, getSectionCount() - 1);

    // randomly pick a primitive in that section
    final int primitiveIndex = MathUtils.nextRandomInt(0, getPrimitiveCount(section) - 1);

    // Now, based on IndexMode, pick a point on that primitive
    final IndexMode mode = getIndexMode(section);
    final boolean hasIndices = getIndices() != null;
    switch (mode) {
      case Triangles:
      case TrianglesAdjacency:
      case TriangleFan:
      case TriangleStrip:
      case TriangleStripAdjacency: {
        int pntA = getVertexIndex(primitiveIndex, 0, section);
        int pntB = getVertexIndex(primitiveIndex, 1, section);
        int pntC = getVertexIndex(primitiveIndex, 2, section);

        if (hasIndices) {
          pntA = getIndices().get(pntA);
          pntB = getIndices().get(pntB);
          pntC = getIndices().get(pntC);
        }

        double b = MathUtils.nextRandomDouble();
        double c = MathUtils.nextRandomDouble();

        // keep it in the triangle by reflecting it across the center diagonal BC
        if (b + c > 1) {
          b = 1 - b;
          c = 1 - c;
        }

        final double a = 1 - b - c;

        final Vector3 work = Vector3.fetchTempInstance();
        BufferUtils.populateFromBuffer(work, getVertexBuffer(), pntA);
        work.multiplyLocal(a);
        result.set(work);

        BufferUtils.populateFromBuffer(work, getVertexBuffer(), pntB);
        work.multiplyLocal(b);
        result.addLocal(work);

        BufferUtils.populateFromBuffer(work, getVertexBuffer(), pntC);
        work.multiplyLocal(c);
        result.addLocal(work);
        Vector3.releaseTempInstance(work);
        break;
      }
      case Points: {
        int pnt = getVertexIndex(primitiveIndex, 0, section);
        if (hasIndices) {
          pnt = getIndices().get(pnt);
        }
        BufferUtils.populateFromBuffer(result, getVertexBuffer(), pnt);
        break;
      }
      case Lines:
      case LinesAdjacency:
      case LineLoop:
      case LineStrip:
      case LineStripAdjacency: {
        int pntA = getVertexIndex(primitiveIndex, 0, section);
        int pntB = getVertexIndex(primitiveIndex, 1, section);
        if (hasIndices) {
          pntA = getIndices().get(pntA);
          pntB = getIndices().get(pntB);
        }

        final Vector3 work = Vector3.fetchTempInstance();
        BufferUtils.populateFromBuffer(result, getVertexBuffer(), pntA);
        BufferUtils.populateFromBuffer(work, getVertexBuffer(), pntB);
        Vector3.lerp(result, work, MathUtils.nextRandomDouble(), result);
        Vector3.releaseTempInstance(work);
        break;
      }
    }

    return result;
  }

  /**
   * Translate points.
   *
   * @param x
   *          the x
   * @param y
   *          the y
   * @param z
   *          the z
   */
  public void translatePoints(final double x, final double y, final double z) {
    translatePoints(new Vector3(x, y, z));
  }

  /**
   * Translate points.
   *
   * @param amount
   *          the amount
   */
  public void translatePoints(final Vector3 amount) {
    final FloatBuffer vertexBuffer = getVertexBuffer();
    for (int x = 0; x < _vertexCount; x++) {
      BufferUtils.addInBuffer(amount, vertexBuffer, x);
    }
  }

  public void transformVertices(final Transform transform) {
    final FloatBuffer vertexBuffer = getVertexBuffer();
    final Vector3 store = new Vector3();
    for (int x = 0; x < _vertexCount; x++) {
      BufferUtils.populateFromBuffer(store, vertexBuffer, x);
      transform.applyForward(store, store);
      BufferUtils.setInBuffer(store, vertexBuffer, x);
    }
  }

  public void transformNormals(final Transform transform, final boolean normalize) {
    final FloatBuffer normalBuffer = getNormalBuffer();
    final Vector3 store = new Vector3();
    for (int x = 0; x < _vertexCount; x++) {
      BufferUtils.populateFromBuffer(store, normalBuffer, x);
      transform.applyForwardVector(store, store);
      if (normalize) {
        store.normalizeLocal();
      }
      BufferUtils.setInBuffer(store, normalBuffer, x);
    }
  }

  /**
   * Rotate points.
   *
   * @param rotate
   *          the rotate
   */
  public void rotatePoints(final Quaternion rotate) {
    final FloatBuffer vertexBuffer = getVertexBuffer();
    final Vector3 store = new Vector3();
    for (int x = 0; x < _vertexCount; x++) {
      BufferUtils.populateFromBuffer(store, vertexBuffer, x);
      rotate.apply(store, store);
      BufferUtils.setInBuffer(store, vertexBuffer, x);
    }
  }

  /**
   * Rotate normals.
   *
   * @param rotate
   *          the rotate
   */
  public void rotateNormals(final Quaternion rotate) {
    final FloatBuffer normalBuffer = getNormalBuffer();
    final Vector3 store = new Vector3();
    for (int x = 0; x < _vertexCount; x++) {
      BufferUtils.populateFromBuffer(store, normalBuffer, x);
      rotate.apply(store, store);
      BufferUtils.setInBuffer(store, normalBuffer, x);
    }
  }

  /**
   * Update primitive counts.
   */
  private void updatePrimitiveCounts() {
    final int maxIndex = _indexBuffer != null ? _indexBuffer.getBufferLimit() : _vertexCount;
    final int maxSection = _indexLengths != null ? _indexLengths.length : 1;
    if (_primitiveCounts.length != maxSection) {
      _primitiveCounts = new int[maxSection];
    }
    for (int i = 0; i < maxSection; i++) {
      final int size = _indexLengths != null ? _indexLengths[i] : maxIndex;
      final int count = IndexMode.getPrimitiveCount(getIndexMode(i), size);
      _primitiveCounts[i] = count;
    }

  }

  public MeshData makeCopy() {
    final MeshData data = new MeshData();
    data._vertexCount = _vertexCount;
    data._primitiveCounts = new int[_primitiveCounts.length];
    System.arraycopy(_primitiveCounts, 0, data._primitiveCounts, 0, _primitiveCounts.length);

    for (final Entry<String, AbstractBufferData<? extends Buffer>> entry : _vertexDataItems.entrySet()) {
      data.setCoords(entry.getKey(), entry.getValue().makeCopy());
    }

    if (_indexBuffer != null) {
      data._indexBuffer = _indexBuffer.makeCopy();
    }

    if (_indexLengths != null) {
      data._indexLengths = new int[_indexLengths.length];
      System.arraycopy(_indexLengths, 0, data._indexLengths, 0, _indexLengths.length);
    }
    data._indexModes = new IndexMode[_indexModes.length];
    System.arraycopy(_indexModes, 0, data._indexModes, 0, _indexModes.length);

    return data;
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends MeshData> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_vertexCount, "vertexCount", 0);
    capsule.writeStringSavableMap(_vertexDataItems, "vertexDataItems",
        new HashMap<String, AbstractBufferData<? extends Buffer>>());
    capsule.write(_indexBuffer, "indexBuffer", null);
    capsule.write(_indexLengths, "indexLengths", null);
    capsule.write(_indexModes, "indexModes");
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _vertexCount = capsule.readInt("vertexCount", 0);
    _vertexDataItems =
        capsule.readStringSavableMap("vertexDataItems", new HashMap<>());
    _indexBuffer = capsule.readSavable("indexBuffer", null);
    _indexLengths = capsule.readIntArray("indexLengths", null);
    _indexModes = capsule.readEnumArray("indexModes", IndexMode.class, new IndexMode[] {IndexMode.Triangles});

    updatePrimitiveCounts();
  }

  public static void cleanAllVertexArrays(final IShaderUtils utils) {
    final Multimap<RenderContextRef, Integer> idMap = new SimpleMultimap<>();

    // gather up expired vaos... these don't exist in our cache
    gatherGCdIds(idMap);

    // Walk through the cached items and delete those too.
    for (final MeshData data : MeshData._identityCache.keySet()) {
      if (data._vaoIdCache != null) {
        if (Constants.useMultipleContexts) {
          final Set<RenderContextRef> renderRefs = data._vaoIdCache.getContextRefs();
          for (final RenderContextRef renderRef : renderRefs) {
            // Add id to map
            idMap.put(renderRef, data.getVAOIDByRef(renderRef));
          }
        } else {
          // single context mode
          idMap.put(ContextManager.getCurrentContext().getUniqueContextRef(), data.getVAOIDByRef(null));
        }
        data._vaoIdCache.clear();
      }
    }

    handleVAODelete(utils, idMap);
  }

  public static void cleanAllVertexArrays(final IShaderUtils utils, final RenderContext context) {
    final Multimap<RenderContextRef, Integer> idMap = new SimpleMultimap<>();

    // gather up expired vaos... these don't exist in our cache
    gatherGCdIds(idMap);

    final RenderContextRef uniqueRef = context.getUniqueContextRef();
    // Walk through the cached items and delete those too.
    for (final MeshData data : MeshData._identityCache.keySet()) {
      // only worry about data that have received ids.
      if (data._vaoIdCache != null) {
        final Integer id = data._vaoIdCache.removeValue(uniqueRef);
        if (id != null && id != 0) {
          idMap.put(uniqueRef, id);
        }
      }
    }

    handleVAODelete(utils, idMap);
  }

  @SuppressWarnings("unchecked")
  private static Multimap<RenderContextRef, Integer> gatherGCdIds(Multimap<RenderContextRef, Integer> store) {
    // Pull all expired vaos from ref queue and add to an id multimap.
    ContextValueReference<MeshData, Integer> valRef;
    while ((valRef = (ContextValueReference<MeshData, Integer>) MeshData._vaoRefQueue.poll()) != null) {
      if (Constants.useMultipleContexts) {
        final Set<RenderContextRef> contextRefs = valRef.getContextRefs();
        for (final RenderContextRef rep : contextRefs) {
          // Add id to map
          final Integer id = valRef.getValue(rep);
          if (id != null) {
            if (store == null) { // lazy init
              store = new SimpleMultimap<>();
            }
            store.put(rep, id);
          }
        }
      } else {
        final Integer id = valRef.getValue(null);
        if (id != null) {
          if (store == null) { // lazy init
            store = new SimpleMultimap<>();
          }
          store.put(ContextManager.getCurrentContext().getUniqueContextRef(), id);
        }
      }
      valRef.clear();
    }

    return store;
  }

  private static void handleVAODelete(final IShaderUtils utils, final Multimap<RenderContextRef, Integer> idMap) {
    RenderContextRef currentUniqueRef = null;
    // Grab the current context, if any.
    if (utils != null && ContextManager.getCurrentContext() != null) {
      currentUniqueRef = ContextManager.getCurrentContext().getUniqueContextRef();
    }
    // For each affected context...
    for (final RenderContextRef uniqueRef : idMap.keySet()) {
      // If we have a deleter and the context is current, immediately delete
      if (utils != null && uniqueRef.equals(currentUniqueRef)) {
        utils.deleteVertexArrays(idMap.values(uniqueRef));
      }
      // Otherwise, add a delete request to that context's render task queue.
      else {
        GameTaskQueueManager.getManager(ContextManager.getContextForUniqueRef(uniqueRef))
            .render(new RendererCallable<Void>() {
              @Override
              public Void call() {
                getRenderer().getShaderUtils().deleteVertexArrays(idMap.values(uniqueRef));
                return null;
              }
            });
      }
    }
  }

}
