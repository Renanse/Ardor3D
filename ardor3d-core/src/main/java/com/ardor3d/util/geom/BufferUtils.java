/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.geom;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.type.ReadOnlyVector4;
import com.ardor3d.scenegraph.ByteBufferData;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.IntBufferData;
import com.ardor3d.scenegraph.ShortBufferData;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.google.common.collect.MapMaker;

/**
 * <code>BufferUtils</code> is a helper class for generating nio buffers from ardor3d data classes
 * such as Vectors and ColorRGBA.
 */
public final class BufferUtils {

  // // -- TRACKER HASH -- ////
  private static final Map<Buffer, Object> trackingHash = new MapMaker().weakKeys().makeMap();
  private static final Object ref = new Object();

  // // -- COLORRGBA METHODS -- ////

  /**
   * Generate a new FloatBuffer using the given array of ColorRGBA objects. The FloatBuffer will be 4
   * * data.length long and contain the color data as data[0].r, data[0].g, data[0].b, data[0].a,
   * data[1].r... etc.
   *
   * @param data
   *          array of ColorRGBA objects to place into a new FloatBuffer
   */
  public static FloatBuffer createFloatBuffer(final ReadOnlyColorRGBA... data) {
    if (data == null) {
      return null;
    }
    return createFloatBuffer(0, data.length, data);
  }

  /**
   * Generate a new FloatBuffer using the given array of ColorRGBA objects. The FloatBuffer will be 4
   * * data.length long and contain the color data as data[0].r, data[0].g, data[0].b, data[0].a,
   * data[1].r... etc.
   *
   * @param offset
   *          the starting index to read from in our data array
   * @param length
   *          the number of colors to read
   * @param data
   *          array of ColorRGBA objects to place into a new FloatBuffer
   */
  public static FloatBuffer createFloatBuffer(final int offset, final int length, final ReadOnlyColorRGBA... data) {
    if (data == null) {
      return null;
    }
    final FloatBuffer buff = createFloatBuffer(4 * length);
    for (int x = offset; x < length; x++) {
      if (data[x] != null) {
        buff.put(data[x].getRed()).put(data[x].getGreen()).put(data[x].getBlue()).put(data[x].getAlpha());
      } else {
        buff.put(0).put(0).put(0).put(0);
      }
    }
    buff.flip();
    return buff;
  }

  /**
   * Create a new FloatBuffer of an appropriate size to hold the specified number of ColorRGBA object
   * data.
   *
   * @param colors
   *          number of colors that need to be held by the newly created buffer
   * @return the requested new FloatBuffer
   */
  public static FloatBuffer createColorBuffer(final int colors) {
    final FloatBuffer colorBuff = createFloatBuffer(4 * colors);
    return colorBuff;
  }

  /**
   * Sets the data contained in the given color into the FloatBuffer at the specified index.
   *
   * @param color
   *          the data to insert
   * @param buf
   *          the buffer to insert into
   * @param index
   *          the position to place the data; in terms of colors not floats
   */
  public static void setInBuffer(final ReadOnlyColorRGBA color, final FloatBuffer buf, final int index) {
    buf.position(index * 4);
    buf.put(color.getRed());
    buf.put(color.getGreen());
    buf.put(color.getBlue());
    buf.put(color.getAlpha());
  }

  /**
   * Updates the values of the given color from the specified buffer at the index provided.
   *
   * @param store
   *          the color to set data on
   * @param buf
   *          the buffer to read from
   * @param index
   *          the position (in terms of colors, not floats) to read from the buf
   */
  public static void populateFromBuffer(final ColorRGBA store, final FloatBuffer buf, final int index) {
    store.setRed(buf.get(index * 4));
    store.setGreen(buf.get(index * 4 + 1));
    store.setBlue(buf.get(index * 4 + 2));
    store.setAlpha(buf.get(index * 4 + 3));
  }

  /**
   * Generates a ColorRGBA array from the given FloatBuffer.
   *
   * @param buff
   *          the FloatBuffer to read from
   * @return a newly generated array of ColorRGBA objects
   */
  public static ColorRGBA[] getColorArray(final FloatBuffer buff) {
    buff.rewind();
    final ColorRGBA[] colors = new ColorRGBA[buff.limit() >> 2];
    for (int x = 0; x < colors.length; x++) {
      final ColorRGBA c = new ColorRGBA(buff.get(), buff.get(), buff.get(), buff.get());
      colors[x] = c;
    }
    return colors;
  }

  /**
   * Generates a ColorRGBA array from the given FloatBufferData.
   *
   * @param buff
   *          the FloatBufferData to read from
   * @param defaults
   *          a default value to set each color to, used when the tuple size of the given
   *          {@link FloatBufferData} is smaller than 4.
   * @return a newly generated array of ColorRGBA objects
   */
  public static ColorRGBA[] getColorArray(final FloatBufferData data, final ReadOnlyColorRGBA defaults) {
    final FloatBuffer buff = data.getBuffer();
    buff.clear();
    final ColorRGBA[] colors = new ColorRGBA[data.getTupleCount()];
    final int tupleSize = data.getValuesPerTuple();
    for (int x = 0; x < colors.length; x++) {
      final ColorRGBA c = new ColorRGBA(defaults);
      c.setRed(buff.get());
      if (tupleSize > 1) {
        c.setGreen(buff.get());
      }
      if (tupleSize > 2) {
        c.setBlue(buff.get());
      }
      if (tupleSize > 3) {
        c.setAlpha(buff.get());
      }
      if (tupleSize > 4) {
        buff.position(buff.position() + tupleSize - 4);
      }
      colors[x] = c;
    }
    return colors;
  }

  /**
   * Copies a ColorRGBA from one position in the buffer to another. The index values are in terms of
   * color number (eg, color number 0 is positions 0-3 in the FloatBuffer.)
   *
   * @param buf
   *          the buffer to copy from/to
   * @param fromPos
   *          the index of the color to copy
   * @param toPos
   *          the index to copy the color to
   */
  public static void copyInternalColor(final FloatBuffer buf, final int fromPos, final int toPos) {
    copyInternal(buf, fromPos * 4, toPos * 4, 4);
  }

  /**
   * Checks to see if the given ColorRGBA is equals to the data stored in the buffer at the given data
   * index.
   *
   * @param check
   *          the color to check against - null will return false.
   * @param buf
   *          the buffer to compare data with
   * @param index
   *          the position (in terms of colors, not floats) of the color in the buffer to check
   *          against
   * @return
   */
  public static boolean equals(final ReadOnlyColorRGBA check, final FloatBuffer buf, final int index) {
    final ColorRGBA temp = new ColorRGBA();
    populateFromBuffer(temp, buf, index);
    return temp.equals(check);
  }

  // // -- Vector4 METHODS -- ////

  /**
   * Generate a new FloatBuffer using the given array of Vector4 objects. The FloatBuffer will be 4 *
   * data.length long and contain the vector data as data[0].x, data[0].y, data[0].z, data[0].w,
   * data[1].x... etc.
   *
   * @param offset
   *          the starting index to read from in our data array
   * @param length
   *          the number of vectors to read
   * @param data
   *          array of Vector4 objects to place into a new FloatBuffer
   */
  public static FloatBuffer createFloatBuffer(final ReadOnlyVector4... data) {
    if (data == null) {
      return null;
    }
    return createFloatBuffer(0, data.length, data);
  }

  /**
   * Generate a new FloatBuffer using the given array of Vector4 objects. The FloatBuffer will be 4 *
   * data.length long and contain the vector data as data[0].x, data[0].y, data[0].z, data[0].w,
   * data[1].x... etc.
   *
   * @param data
   *          array of Vector4 objects to place into a new FloatBuffer
   */
  public static FloatBuffer createFloatBuffer(final int offset, final int length, final ReadOnlyVector4... data) {
    if (data == null) {
      return null;
    }
    final FloatBuffer buff = createFloatBuffer(4 * length);
    for (int x = offset; x < length; x++) {
      if (data[x] != null) {
        buff.put(data[x].getXf()).put(data[x].getYf()).put(data[x].getZf()).put(data[x].getWf());
      } else {
        buff.put(0).put(0).put(0);
      }
    }
    buff.flip();
    return buff;
  }

  /**
   * Create a new FloatBuffer of an appropriate size to hold the specified number of Vector4 object
   * data.
   *
   * @param vertices
   *          number of vertices that need to be held by the newly created buffer
   * @return the requested new FloatBuffer
   */
  public static FloatBuffer createVector4Buffer(final int vertices) {
    final FloatBuffer vBuff = createFloatBuffer(4 * vertices);
    return vBuff;
  }

  /**
   * Create a new FloatBuffer of an appropriate size to hold the specified number of Vector4 object
   * data only if the given buffer if not already the right size.
   *
   * @param buf
   *          the buffer to first check and rewind
   * @param vertices
   *          number of vertices that need to be held by the newly created buffer
   * @return the requested new FloatBuffer
   */
  public static FloatBuffer createVector4Buffer(final FloatBuffer buf, final int vertices) {
    if (buf != null && buf.limit() == 4 * vertices) {
      buf.rewind();
      return buf;
    }

    return createFloatBuffer(4 * vertices);
  }

  /**
   * Sets the data contained in the given Vector4 into the FloatBuffer at the specified index.
   *
   * @param vector
   *          the data to insert
   * @param buf
   *          the buffer to insert into
   * @param index
   *          the position to place the data; in terms of vectors not floats
   */
  public static void setInBuffer(final ReadOnlyVector4 vector, final FloatBuffer buf, final int index) {
    if (buf == null) {
      return;
    }
    if (vector == null) {
      buf.put(index * 4, 0);
      buf.put((index * 4) + 1, 0);
      buf.put((index * 4) + 2, 0);
      buf.put((index * 4) + 3, 0);
    } else {
      buf.put(index * 4, vector.getXf());
      buf.put((index * 4) + 1, vector.getYf());
      buf.put((index * 4) + 2, vector.getZf());
      buf.put((index * 4) + 3, vector.getWf());
    }
  }

  /**
   * Updates the values of the given vector from the specified buffer at the index provided.
   *
   * @param vector
   *          the vector to set data on
   * @param buf
   *          the buffer to read from
   * @param index
   *          the position (in terms of vectors, not floats) to read from the buffer
   */
  public static void populateFromBuffer(final Vector4 vector, final FloatBuffer buf, final int index) {
    vector.setX(buf.get(index * 4));
    vector.setY(buf.get(index * 4 + 1));
    vector.setZ(buf.get(index * 4 + 2));
    vector.setW(buf.get(index * 4 + 3));
  }

  /**
   * Generates a Vector4 array from the given FloatBuffer.
   *
   * @param buff
   *          the FloatBuffer to read from
   * @return a newly generated array of Vector3 objects
   */
  public static Vector4[] getVector4Array(final FloatBuffer buff) {
    buff.clear();
    final Vector4[] verts = new Vector4[buff.limit() / 4];
    for (int x = 0; x < verts.length; x++) {
      final Vector4 v = new Vector4(buff.get(), buff.get(), buff.get(), buff.get());
      verts[x] = v;
    }
    return verts;
  }

  /**
   * Generates a Vector4 array from the given FloatBufferData.
   *
   * @param buff
   *          the FloatBufferData to read from
   * @param defaults
   *          a default value to set each color to, used when the tuple size of the given
   *          {@link FloatBufferData} is smaller than 4.
   * @return a newly generated array of Vector4 objects
   */
  public static Vector4[] getVector4Array(final FloatBufferData data, final ReadOnlyVector4 defaults) {
    final FloatBuffer buff = data.getBuffer();
    buff.clear();
    final Vector4[] verts = new Vector4[data.getTupleCount()];
    final int tupleSize = data.getValuesPerTuple();
    for (int x = 0; x < verts.length; x++) {
      final Vector4 v = new Vector4(defaults);
      v.setX(buff.get());
      if (tupleSize > 1) {
        v.setY(buff.get());
      }
      if (tupleSize > 2) {
        v.setZ(buff.get());
      }
      if (tupleSize > 3) {
        v.setW(buff.get());
      }
      if (tupleSize > 4) {
        buff.position(buff.position() + tupleSize - 4);
      }
      verts[x] = v;
    }
    return verts;
  }

  /**
   * Copies a Vector3 from one position in the buffer to another. The index values are in terms of
   * vector number (eg, vector number 0 is positions 0-2 in the FloatBuffer.)
   *
   * @param buf
   *          the buffer to copy from/to
   * @param fromPos
   *          the index of the vector to copy
   * @param toPos
   *          the index to copy the vector to
   */
  public static void copyInternalVector4(final FloatBuffer buf, final int fromPos, final int toPos) {
    copyInternal(buf, fromPos * 4, toPos * 4, 4);
  }

  /**
   * Normalize a Vector4 in-buffer.
   *
   * @param buf
   *          the buffer to find the Vector4 within
   * @param index
   *          the position (in terms of vectors, not floats) of the vector to normalize
   */
  public static void normalizeVector4(final FloatBuffer buf, final int index) {
    final Vector4 temp = Vector4.fetchTempInstance();
    populateFromBuffer(temp, buf, index);
    temp.normalizeLocal();
    setInBuffer(temp, buf, index);
    Vector4.releaseTempInstance(temp);
  }

  /**
   * Add to a Vector4 in-buffer.
   *
   * @param toAdd
   *          the vector to add from
   * @param buf
   *          the buffer to find the Vector4 within
   * @param index
   *          the position (in terms of vectors, not floats) of the vector to add to
   */
  public static void addInBuffer(final ReadOnlyVector4 toAdd, final FloatBuffer buf, final int index) {
    final Vector4 temp = Vector4.fetchTempInstance();
    populateFromBuffer(temp, buf, index);
    temp.addLocal(toAdd);
    setInBuffer(temp, buf, index);
    Vector4.releaseTempInstance(temp);
  }

  /**
   * Multiply and store a Vector3 in-buffer.
   *
   * @param toMult
   *          the vector to multiply against
   * @param buf
   *          the buffer to find the Vector3 within
   * @param index
   *          the position (in terms of vectors, not floats) of the vector to multiply
   */
  public static void multInBuffer(final ReadOnlyVector4 toMult, final FloatBuffer buf, final int index) {
    final Vector4 temp = Vector4.fetchTempInstance();
    populateFromBuffer(temp, buf, index);
    temp.multiplyLocal(toMult);
    setInBuffer(temp, buf, index);
    Vector4.releaseTempInstance(temp);
  }

  /**
   * Checks to see if the given Vector3 is equals to the data stored in the buffer at the given data
   * index.
   *
   * @param check
   *          the vector to check against - null will return false.
   * @param buf
   *          the buffer to compare data with
   * @param index
   *          the position (in terms of vectors, not floats) of the vector in the buffer to check
   *          against
   * @return
   */
  public static boolean equals(final ReadOnlyVector4 check, final FloatBuffer buf, final int index) {
    final Vector4 temp = Vector4.fetchTempInstance();
    populateFromBuffer(temp, buf, index);
    final boolean equals = temp.equals(check);
    Vector4.releaseTempInstance(temp);
    return equals;
  }

  // // -- Vector3 METHODS -- ////

  /**
   * Generate a new FloatBuffer using the given array of Vector3 objects. The FloatBuffer will be 3 *
   * data.length long and contain the vector data as data[0].x, data[0].y, data[0].z, data[1].x...
   * etc.
   *
   * @param data
   *          array of Vector3 objects to place into a new FloatBuffer
   */
  public static FloatBuffer createFloatBuffer(final ReadOnlyVector3... data) {
    if (data == null) {
      return null;
    }
    return createFloatBuffer(0, data.length, data);
  }

  /**
   * Generate a new FloatBuffer using the given array of Vector3 objects. The FloatBuffer will be 3 *
   * data.length long and contain the vector data as data[0].x, data[0].y, data[0].z, data[1].x...
   * etc.
   *
   * @param offset
   *          the starting index to read from in our data array
   * @param length
   *          the number of vectors to read
   * @param data
   *          array of Vector3 objects to place into a new FloatBuffer
   */
  public static FloatBuffer createFloatBuffer(final int offset, final int length, final ReadOnlyVector3... data) {
    if (data == null) {
      return null;
    }
    final FloatBuffer buff = createFloatBuffer(3 * length);
    for (int x = offset; x < length; x++) {
      if (data[x] != null) {
        buff.put(data[x].getXf()).put(data[x].getYf()).put(data[x].getZf());
      } else {
        buff.put(0).put(0).put(0);
      }
    }
    buff.flip();
    return buff;
  }

  /**
   * Create a new FloatBuffer of an appropriate size to hold the specified number of Vector3 object
   * data.
   *
   * @param vertices
   *          number of vertices that need to be held by the newly created buffer
   * @return the requested new FloatBuffer
   */
  public static FloatBuffer createVector3Buffer(final int vertices) {
    final FloatBuffer vBuff = createFloatBuffer(3 * vertices);
    return vBuff;
  }

  /**
   * Create a new FloatBuffer of an appropriate size to hold the specified number of Vector3 object
   * data only if the given buffer is not already the right size.
   *
   * @param buf
   *          the buffer to first check and rewind
   * @param vertices
   *          number of vertices that need to be held by the newly created buffer
   * @return the requested new FloatBuffer
   */
  public static FloatBuffer createVector3Buffer(final FloatBuffer buf, final int vertices) {
    if (buf != null && buf.limit() == 3 * vertices) {
      buf.rewind();
      return buf;
    }

    return createFloatBuffer(3 * vertices);
  }

  /**
   * Sets the data contained in the given Vector3 into the FloatBuffer at the specified index.
   *
   * @param vector
   *          the data to insert
   * @param buf
   *          the buffer to insert into
   * @param index
   *          the position to place the data; in terms of vectors not floats
   */
  public static void setInBuffer(final ReadOnlyVector3 vector, final FloatBuffer buf, final int index) {
    if (buf == null) {
      return;
    }
    if (vector == null) {
      buf.put(index * 3, 0);
      buf.put((index * 3) + 1, 0);
      buf.put((index * 3) + 2, 0);
    } else {
      buf.put(index * 3, vector.getXf());
      buf.put((index * 3) + 1, vector.getYf());
      buf.put((index * 3) + 2, vector.getZf());
    }
  }

  /**
   * Updates the values of the given vector from the specified buffer at the index provided.
   *
   * @param vector
   *          the vector to set data on
   * @param buf
   *          the buffer to read from
   * @param index
   *          the position (in terms of vectors, not floats) to read from the buf
   */
  public static void populateFromBuffer(final Vector3 vector, final FloatBuffer buf, final int index) {
    vector.setX(buf.get(index * 3));
    vector.setY(buf.get(index * 3 + 1));
    vector.setZ(buf.get(index * 3 + 2));
  }

  /**
   * Generates a Vector3 array from the given FloatBuffer.
   *
   * @param buff
   *          the FloatBuffer to read from
   * @return a newly generated array of Vector3 objects
   */
  public static Vector3[] getVector3Array(final FloatBuffer buff) {
    buff.clear();
    final Vector3[] verts = new Vector3[buff.limit() / 3];
    for (int x = 0; x < verts.length; x++) {
      final Vector3 v = new Vector3(buff.get(), buff.get(), buff.get());
      verts[x] = v;
    }
    return verts;
  }

  /**
   * Generates a Vector3 array from the given FloatBufferData.
   *
   * @param buff
   *          the FloatBufferData to read from
   * @param defaults
   *          a default value to set each color to, used when the tuple size of the given
   *          {@link FloatBufferData} is smaller than 3.
   * @return a newly generated array of Vector3 objects
   */
  public static Vector3[] getVector3Array(final FloatBufferData data, final ReadOnlyVector3 defaults) {
    final FloatBuffer buff = data.getBuffer();
    buff.clear();
    final Vector3[] verts = new Vector3[data.getTupleCount()];
    final int tupleSize = data.getValuesPerTuple();
    for (int x = 0; x < verts.length; x++) {
      final Vector3 v = new Vector3(defaults);
      v.setX(buff.get());
      if (tupleSize > 1) {
        v.setY(buff.get());
      }
      if (tupleSize > 2) {
        v.setZ(buff.get());
      }
      if (tupleSize > 3) {
        buff.position(buff.position() + tupleSize - 3);
      }
      verts[x] = v;
    }
    return verts;
  }

  /**
   * Copies a Vector3 from one position in the buffer to another. The index values are in terms of
   * vector number (eg, vector number 0 is positions 0-2 in the FloatBuffer.)
   *
   * @param buf
   *          the buffer to copy from/to
   * @param fromPos
   *          the index of the vector to copy
   * @param toPos
   *          the index to copy the vector to
   */
  public static void copyInternalVector3(final FloatBuffer buf, final int fromPos, final int toPos) {
    copyInternal(buf, fromPos * 3, toPos * 3, 3);
  }

  /**
   * Normalize a Vector3 in-buffer.
   *
   * @param buf
   *          the buffer to find the Vector3 within
   * @param index
   *          the position (in terms of vectors, not floats) of the vector to normalize
   */
  public static void normalizeVector3(final FloatBuffer buf, final int index) {
    final Vector3 temp = Vector3.fetchTempInstance();
    populateFromBuffer(temp, buf, index);
    temp.normalizeLocal();
    setInBuffer(temp, buf, index);
    Vector3.releaseTempInstance(temp);
  }

  /**
   * Add to a Vector3 in-buffer.
   *
   * @param toAdd
   *          the vector to add from
   * @param buf
   *          the buffer to find the Vector3 within
   * @param index
   *          the position (in terms of vectors, not floats) of the vector to add to
   */
  public static void addInBuffer(final ReadOnlyVector3 toAdd, final FloatBuffer buf, final int index) {
    final Vector3 temp = Vector3.fetchTempInstance();
    populateFromBuffer(temp, buf, index);
    temp.addLocal(toAdd);
    setInBuffer(temp, buf, index);
    Vector3.releaseTempInstance(temp);
  }

  /**
   * Multiply and store a Vector3 in-buffer.
   *
   * @param toMult
   *          the vector to multiply against
   * @param buf
   *          the buffer to find the Vector3 within
   * @param index
   *          the position (in terms of vectors, not floats) of the vector to multiply
   */
  public static void multInBuffer(final ReadOnlyVector3 toMult, final FloatBuffer buf, final int index) {
    final Vector3 temp = Vector3.fetchTempInstance();
    populateFromBuffer(temp, buf, index);
    temp.multiplyLocal(toMult);
    setInBuffer(temp, buf, index);
    Vector3.releaseTempInstance(temp);
  }

  /**
   * Checks to see if the given Vector3 is equals to the data stored in the buffer at the given data
   * index.
   *
   * @param check
   *          the vector to check against - null will return false.
   * @param buf
   *          the buffer to compare data with
   * @param index
   *          the position (in terms of vectors, not floats) of the vector in the buffer to check
   *          against
   * @return
   */
  public static boolean equals(final ReadOnlyVector3 check, final FloatBuffer buf, final int index) {
    final Vector3 temp = Vector3.fetchTempInstance();
    populateFromBuffer(temp, buf, index);
    final boolean equals = temp.equals(check);
    Vector3.releaseTempInstance(temp);
    return equals;
  }

  // // -- Vector2 METHODS -- ////

  /**
   * Generate a new FloatBuffer using the given array of Vector2 objects. The FloatBuffer will be 2 *
   * data.length long and contain the vector data as data[0].x, data[0].y, data[1].x... etc.
   *
   * @param data
   *          array of Vector2 objects to place into a new FloatBuffer
   */
  public static FloatBuffer createFloatBuffer(final ReadOnlyVector2... data) {
    if (data == null) {
      return null;
    }
    return createFloatBuffer(0, data.length, data);
  }

  /**
   * Generate a new FloatBuffer using the given array of Vector2 objects. The FloatBuffer will be 2 *
   * data.length long and contain the vector data as data[0].x, data[0].y, data[1].x... etc.
   *
   * @param offset
   *          the starting index to read from in our data array
   * @param length
   *          the number of vectors to read
   * @param data
   *          array of Vector2 objects to place into a new FloatBuffer
   */
  public static FloatBuffer createFloatBuffer(final int offset, final int length, final ReadOnlyVector2... data) {
    if (data == null) {
      return null;
    }
    final FloatBuffer buff = createFloatBuffer(2 * length);
    for (int x = offset; x < length; x++) {
      if (data[x] != null) {
        buff.put(data[x].getXf()).put(data[x].getYf());
      } else {
        buff.put(0).put(0);
      }
    }
    buff.flip();
    return buff;
  }

  /**
   * Create a new FloatBuffer of an appropriate size to hold the specified number of Vector2 object
   * data.
   *
   * @param vertices
   *          number of vertices that need to be held by the newly created buffer
   * @return the requested new FloatBuffer
   */
  public static FloatBuffer createVector2Buffer(final int vertices) {
    final FloatBuffer vBuff = createFloatBuffer(2 * vertices);
    return vBuff;
  }

  /**
   * Create a new FloatBuffer of an appropriate size to hold the specified number of Vector2 object
   * data only if the given buffer if not already the right size.
   *
   * @param buf
   *          the buffer to first check and rewind
   * @param vertices
   *          number of vertices that need to be held by the newly created buffer
   * @return the requested new FloatBuffer
   */
  public static FloatBuffer createVector2Buffer(final FloatBuffer buf, final int vertices) {
    if (buf != null && buf.limit() == 2 * vertices) {
      buf.rewind();
      return buf;
    }

    return createFloatBuffer(2 * vertices);
  }

  /**
   * Sets the data contained in the given Vector2 into the FloatBuffer at the specified index.
   *
   * @param vector
   *          the data to insert
   * @param buf
   *          the buffer to insert into
   * @param index
   *          the position to place the data; in terms of vectors not floats
   */
  public static void setInBuffer(final ReadOnlyVector2 vector, final FloatBuffer buf, final int index) {
    buf.put(index * 2, vector.getXf());
    buf.put((index * 2) + 1, vector.getYf());
  }

  /**
   * Updates the values of the given vector from the specified buffer at the index provided.
   *
   * @param vector
   *          the vector to set data on
   * @param buf
   *          the buffer to read from
   * @param index
   *          the position (in terms of vectors, not floats) to read from the buf
   */
  public static void populateFromBuffer(final Vector2 vector, final FloatBuffer buf, final int index) {
    vector.setX(buf.get(index * 2));
    vector.setY(buf.get(index * 2 + 1));
  }

  /**
   * Generates a Vector2 array from the given FloatBuffer.
   *
   * @param buff
   *          the FloatBuffer to read from
   * @return a newly generated array of Vector2 objects
   */
  public static Vector2[] getVector2Array(final FloatBuffer buff) {
    buff.clear();
    final Vector2[] verts = new Vector2[buff.limit() / 2];
    for (int x = 0; x < verts.length; x++) {
      final Vector2 v = new Vector2(buff.get(), buff.get());
      verts[x] = v;
    }
    return verts;
  }

  /**
   * Generates a Vector2 array from the given FloatBufferData.
   *
   * @param buff
   *          the FloatBufferData to read from
   * @param defaults
   *          a default value to set each color to, used when the tuple size of the given
   *          {@link FloatBufferData} is smaller than 2.
   * @return a newly generated array of Vector2 objects
   */
  public static Vector2[] getVector2Array(final FloatBufferData data, final ReadOnlyVector2 defaults) {
    final FloatBuffer buff = data.getBuffer();
    buff.clear();
    final Vector2[] verts = new Vector2[data.getTupleCount()];
    final int tupleSize = data.getValuesPerTuple();
    for (int x = 0; x < verts.length; x++) {
      final Vector2 v = new Vector2(defaults);
      v.setX(buff.get());
      if (tupleSize > 1) {
        v.setY(buff.get());
      }
      if (tupleSize > 2) {
        buff.position(buff.position() + tupleSize - 2);
      }
      verts[x] = v;
    }
    return verts;
  }

  /**
   * Copies a Vector2 from one position in the buffer to another. The index values are in terms of
   * vector number (eg, vector number 0 is positions 0-1 in the FloatBuffer.)
   *
   * @param buf
   *          the buffer to copy from/to
   * @param fromPos
   *          the index of the vector to copy
   * @param toPos
   *          the index to copy the vector to
   */
  public static void copyInternalVector2(final FloatBuffer buf, final int fromPos, final int toPos) {
    copyInternal(buf, fromPos * 2, toPos * 2, 2);
  }

  /**
   * Normalize a Vector2 in-buffer.
   *
   * @param buf
   *          the buffer to find the Vector2 within
   * @param index
   *          the position (in terms of vectors, not floats) of the vector to normalize
   */
  public static void normalizeVector2(final FloatBuffer buf, final int index) {
    final Vector2 temp = Vector2.fetchTempInstance();
    populateFromBuffer(temp, buf, index);
    temp.normalizeLocal();
    setInBuffer(temp, buf, index);
    Vector2.releaseTempInstance(temp);
  }

  /**
   * Add to a Vector2 in-buffer.
   *
   * @param toAdd
   *          the vector to add from
   * @param buf
   *          the buffer to find the Vector2 within
   * @param index
   *          the position (in terms of vectors, not floats) of the vector to add to
   */
  public static void addInBuffer(final ReadOnlyVector2 toAdd, final FloatBuffer buf, final int index) {
    final Vector2 temp = Vector2.fetchTempInstance();
    populateFromBuffer(temp, buf, index);
    temp.addLocal(toAdd);
    setInBuffer(temp, buf, index);
    Vector2.releaseTempInstance(temp);
  }

  /**
   * Multiply and store a Vector2 in-buffer.
   *
   * @param toMult
   *          the vector to multiply against
   * @param buf
   *          the buffer to find the Vector2 within
   * @param index
   *          the position (in terms of vectors, not floats) of the vector to multiply
   */
  public static void multInBuffer(final ReadOnlyVector2 toMult, final FloatBuffer buf, final int index) {
    final Vector2 temp = Vector2.fetchTempInstance();
    populateFromBuffer(temp, buf, index);
    temp.multiplyLocal(toMult);
    setInBuffer(temp, buf, index);
    Vector2.releaseTempInstance(temp);
  }

  /**
   * Checks to see if the given Vector2 is equals to the data stored in the buffer at the given data
   * index.
   *
   * @param check
   *          the vector to check against - null will return false.
   * @param buf
   *          the buffer to compare data with
   * @param index
   *          the position (in terms of vectors, not floats) of the vector in the buffer to check
   *          against
   * @return
   */
  public static boolean equals(final ReadOnlyVector2 check, final FloatBuffer buf, final int index) {
    final Vector2 temp = Vector2.fetchTempInstance();
    populateFromBuffer(temp, buf, index);
    final boolean equals = temp.equals(check);
    Vector2.releaseTempInstance(temp);
    return equals;
  }

  // // -- INT METHODS -- ////

  /**
   * Generate a new IntBuffer using the given array of ints. The IntBuffer will be data.length long
   * and contain the int data as data[0], data[1]... etc.
   *
   * @param data
   *          array of ints to place into a new IntBuffer
   */
  public static IntBuffer createIntBuffer(final int... data) {
    if (data == null) {
      return null;
    }
    final IntBuffer buff = createIntBuffer(data.length);
    buff.clear();
    buff.put(data);
    buff.flip();
    return buff;
  }

  /**
   * Create a new int[] array and populate it with the given IntBuffer's contents.
   *
   * @param buff
   *          the IntBuffer to read from
   * @return a new int array populated from the IntBuffer
   */
  public static int[] getIntArray(final IntBuffer buff) {
    if (buff == null) {
      return null;
    }
    buff.rewind();
    final int[] inds = new int[buff.limit()];
    for (int x = 0; x < inds.length; x++) {
      inds[x] = buff.get();
    }
    return inds;
  }

  /**
   * Create a new int[] array and populate it with the given IndexBufferData's contents.
   *
   * @param buff
   *          the IndexBufferData to read from
   * @return a new int array populated from the IndexBufferData
   */
  public static int[] getIntArray(final IndexBufferData<?> buff) {
    if (buff == null || buff.getBufferLimit() == 0) {
      return null;
    }
    buff.getBuffer().rewind();
    final int[] inds = new int[buff.getBufferLimit()];
    for (int x = 0; x < inds.length; x++) {
      inds[x] = buff.get();
    }
    return inds;
  }

  /**
   * Create a new float[] array and populate it with the given FloatBuffer's contents.
   *
   * @param buff
   *          the FloatBuffer to read from
   * @return a new float array populated from the FloatBuffer
   */
  public static float[] getFloatArray(final FloatBuffer buff) {
    if (buff == null) {
      return null;
    }
    buff.clear();
    final float[] inds = new float[buff.limit()];
    for (int x = 0; x < inds.length; x++) {
      inds[x] = buff.get();
    }
    return inds;
  }

  // // -- GENERAL DOUBLE ROUTINES -- ////

  /**
   * Create a new DoubleBuffer of the specified size.
   *
   * @param size
   *          required number of double to store.
   * @return the new DoubleBuffer
   */
  public static DoubleBuffer createDoubleBufferOnHeap(final int size) {
    final DoubleBuffer buf = ByteBuffer.allocate(8 * size).order(ByteOrder.nativeOrder()).asDoubleBuffer();
    buf.clear();
    return buf;
  }

  /**
   * Create a new DoubleBuffer of the specified size.
   *
   * @param size
   *          required number of double to store.
   * @return the new DoubleBuffer
   */
  public static DoubleBuffer createDoubleBuffer(final int size) {
    final DoubleBuffer buf = ByteBuffer.allocateDirect(8 * size).order(ByteOrder.nativeOrder()).asDoubleBuffer();
    buf.clear();
    if (Constants.trackDirectMemory) {
      trackingHash.put(buf, ref);
    }
    return buf;
  }

  /**
   * Create a new DoubleBuffer of an appropriate size to hold the specified number of doubles only if
   * the given buffer if not already the right size.
   *
   * @param buf
   *          the buffer to first check and rewind
   * @param size
   *          number of doubles that need to be held by the newly created buffer
   * @return the requested new DoubleBuffer
   */
  public static DoubleBuffer createDoubleBuffer(DoubleBuffer buf, final int size) {
    if (buf != null && buf.limit() == size) {
      buf.rewind();
      return buf;
    }

    buf = createDoubleBuffer(size);
    return buf;
  }

  /**
   * Creates a new DoubleBuffer with the same contents as the given DoubleBuffer. The new DoubleBuffer
   * is seperate from the old one and changes are not reflected across. If you want to reflect
   * changes, consider using Buffer.duplicate().
   *
   * @param buf
   *          the DoubleBuffer to copy
   * @return the copy
   */
  public static DoubleBuffer clone(final DoubleBuffer buf) {
    if (buf == null) {
      return null;
    }
    buf.rewind();

    final DoubleBuffer copy;
    if (buf.isDirect()) {
      copy = createDoubleBuffer(buf.limit());
    } else {
      copy = createDoubleBufferOnHeap(buf.limit());
    }
    copy.put(buf);

    return copy;
  }

  // // -- GENERAL FLOAT ROUTINES -- ////

  /**
   * Create a new FloatBuffer of the specified size.
   *
   * @param size
   *          required number of floats to store.
   * @return the new FloatBuffer
   */
  public static FloatBuffer createFloatBuffer(final int size) {
    final FloatBuffer buf = ByteBuffer.allocateDirect(4 * size).order(ByteOrder.nativeOrder()).asFloatBuffer();
    buf.clear();
    if (Constants.trackDirectMemory) {
      trackingHash.put(buf, ref);
    }
    return buf;
  }

  /**
   * Create a new FloatBuffer of the specified size.
   *
   * @param size
   *          required number of floats to store.
   * @return the new FloatBuffer
   */
  public static FloatBuffer createFloatBufferOnHeap(final int size) {
    final FloatBuffer buf = ByteBuffer.allocate(4 * size).order(ByteOrder.nativeOrder()).asFloatBuffer();
    buf.clear();
    return buf;
  }

  /**
   * Generate a new FloatBuffer using the given array of float primitives.
   *
   * @param data
   *          array of float primitives to place into a new FloatBuffer
   */
  public static FloatBuffer createFloatBuffer(final float... data) {
    return createFloatBuffer(null, data);
  }

  /**
   * Generate a new FloatBuffer using the given array of float primitives.
   *
   * @param data
   *          array of float primitives to place into a new FloatBuffer
   */
  public static FloatBuffer createFloatBuffer(final FloatBuffer reuseStore, final float... data) {
    if (data == null) {
      return null;
    }
    final FloatBuffer buff;
    if (reuseStore == null || reuseStore.capacity() != data.length) {
      buff = createFloatBuffer(data.length);
    } else {
      buff = reuseStore;
      buff.clear();
    }
    buff.clear();
    buff.put(data);
    buff.flip();
    return buff;
  }

  public static IntBuffer createIntBuffer(final IntBuffer reuseStore, final int... data) {
    if (data == null) {
      return null;
    }
    final IntBuffer buff;
    if (reuseStore == null || reuseStore.capacity() != data.length) {
      buff = createIntBuffer(data.length);
    } else {
      buff = reuseStore;
      buff.clear();
    }
    buff.clear();
    buff.put(data);
    buff.flip();
    return buff;
  }

  /**
   * Copies floats from one buffer to another.
   *
   * @param source
   *          the buffer to copy from
   * @param fromPos
   *          the starting point to copy from
   * @param destination
   *          the buffer to copy to
   * @param toPos
   *          the starting point to copy to
   * @param length
   *          the number of floats to copy
   */
  public static void copy(final FloatBuffer source, final int fromPos, final FloatBuffer destination, final int toPos,
      final int length) {
    final int oldLimit = source.limit();
    source.position(fromPos);
    source.limit(fromPos + length);
    destination.position(toPos);
    destination.put(source);
    source.limit(oldLimit);
  }

  /**
   * Copies floats from one position in the buffer to another.
   *
   * @param buf
   *          the buffer to copy from/to
   * @param fromPos
   *          the starting point to copy from
   * @param toPos
   *          the starting point to copy to
   * @param length
   *          the number of floats to copy
   */
  public static void copyInternal(final FloatBuffer buf, final int fromPos, final int toPos, final int length) {
    final float[] data = new float[length];
    buf.position(fromPos);
    buf.get(data);
    buf.position(toPos);
    buf.put(data);
  }

  /**
   * Creates a new FloatBuffer with the same contents as the given FloatBuffer. The new FloatBuffer is
   * seperate from the old one and changes are not reflected across. If you want to reflect changes,
   * consider using Buffer.duplicate().
   *
   * @param buf
   *          the FloatBuffer to copy
   * @return the copy
   */
  public static FloatBuffer clone(final FloatBuffer buf) {
    if (buf == null) {
      return null;
    }
    buf.rewind();

    final FloatBuffer copy;
    if (buf.isDirect()) {
      copy = createFloatBuffer(buf.limit());
    } else {
      copy = createFloatBufferOnHeap(buf.limit());
    }
    copy.put(buf);

    return copy;
  }

  // // -- GENERAL INT ROUTINES -- ////

  /**
   * Create a new IntBuffer of the specified size.
   *
   * @param size
   *          required number of ints to store.
   * @return the new IntBuffer
   */
  public static IntBuffer createIntBufferOnHeap(final int size) {
    final IntBuffer buf = ByteBuffer.allocate(4 * size).order(ByteOrder.nativeOrder()).asIntBuffer();
    buf.clear();
    return buf;
  }

  /**
   * Create a new IntBuffer of the specified size.
   *
   * @param size
   *          required number of ints to store.
   * @return the new IntBuffer
   */
  public static IntBuffer createIntBuffer(final int size) {
    final IntBuffer buf = ByteBuffer.allocateDirect(4 * size).order(ByteOrder.nativeOrder()).asIntBuffer();
    buf.clear();
    if (Constants.trackDirectMemory) {
      trackingHash.put(buf, ref);
    }
    return buf;
  }

  /**
   * Create a new IntBuffer of an appropriate size to hold the specified number of ints only if the
   * given buffer if not already the right size.
   *
   * @param buf
   *          the buffer to first check and rewind
   * @param size
   *          number of ints that need to be held by the newly created buffer
   * @return the requested new IntBuffer
   */
  public static IntBuffer createIntBuffer(IntBuffer buf, final int size) {
    if (buf != null && buf.limit() == size) {
      buf.rewind();
      return buf;
    }

    buf = createIntBuffer(size);
    return buf;
  }

  /**
   * Creates a new IntBuffer with the same contents as the given IntBuffer. The new IntBuffer is
   * seperate from the old one and changes are not reflected across. If you want to reflect changes,
   * consider using Buffer.duplicate().
   *
   * @param buf
   *          the IntBuffer to copy
   * @return the copy
   */
  public static IntBuffer clone(final IntBuffer buf) {
    if (buf == null) {
      return null;
    }
    buf.rewind();

    final IntBuffer copy;
    if (buf.isDirect()) {
      copy = createIntBuffer(buf.limit());
    } else {
      copy = createIntBufferOnHeap(buf.limit());
    }
    copy.put(buf);

    return copy;
  }

  // // -- GENERAL BYTE ROUTINES -- ////

  /**
   * Create a new ByteBuffer of the specified size.
   *
   * @param size
   *          required number of ints to store.
   * @return the new IntBuffer
   */
  public static ByteBuffer createByteBuffer(final int size) {
    final ByteBuffer buf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    if (Constants.trackDirectMemory) {
      trackingHash.put(buf, ref);
    }
    return buf;
  }

  /**
   * Create a new ByteBuffer of an appropriate size to hold the specified number of ints only if the
   * given buffer if not already the right size.
   *
   * @param buf
   *          the buffer to first check and rewind
   * @param size
   *          number of bytes that need to be held by the newly created buffer
   * @return the requested new IntBuffer
   */
  public static ByteBuffer createByteBuffer(ByteBuffer buf, final int size) {
    if (buf != null && buf.limit() == size) {
      buf.rewind();
      return buf;
    }

    buf = createByteBuffer(size);
    return buf;
  }

  /**
   * Creates a new ByteBuffer with the same contents as the given ByteBuffer. The new ByteBuffer is
   * seperate from the old one and changes are not reflected across. If you want to reflect changes,
   * consider using Buffer.duplicate().
   *
   * @param buf
   *          the ByteBuffer to copy
   * @return the copy
   */
  public static ByteBuffer clone(final ByteBuffer buf) {
    if (buf == null) {
      return null;
    }
    buf.rewind();

    final ByteBuffer copy;
    if (buf.isDirect()) {
      copy = createByteBuffer(buf.limit());
    } else {
      copy = createByteBufferOnHeap(buf.limit());
    }
    copy.put(buf);

    return copy;
  }

  // // -- GENERAL SHORT ROUTINES -- ////

  /**
   * Create a new ShortBuffer of the specified size.
   *
   * @param size
   *          required number of shorts to store.
   * @return the new ShortBuffer
   */
  public static ShortBuffer createShortBufferOnHeap(final int size) {
    final ShortBuffer buf = ByteBuffer.allocate(2 * size).order(ByteOrder.nativeOrder()).asShortBuffer();
    buf.clear();
    return buf;
  }

  /**
   * Create a new ShortBuffer of the specified size.
   *
   * @param size
   *          required number of shorts to store.
   * @return the new ShortBuffer
   */
  public static ShortBuffer createShortBuffer(final int size) {
    final ShortBuffer buf = ByteBuffer.allocateDirect(2 * size).order(ByteOrder.nativeOrder()).asShortBuffer();
    buf.clear();
    if (Constants.trackDirectMemory) {
      trackingHash.put(buf, ref);
    }
    return buf;
  }

  /**
   * Generate a new ShortBuffer using the given array of short primitives.
   *
   * @param data
   *          array of short primitives to place into a new ShortBuffer
   */
  public static ShortBuffer createShortBuffer(final short... data) {
    if (data == null) {
      return null;
    }
    final ShortBuffer buff = createShortBuffer(data.length);
    buff.clear();
    buff.put(data);
    buff.flip();
    return buff;
  }

  /**
   * Create a new ShortBuffer of an appropriate size to hold the specified number of shorts only if
   * the given buffer if not already the right size.
   *
   * @param buf
   *          the buffer to first check and rewind
   * @param size
   *          number of shorts that need to be held by the newly created buffer
   * @return the requested new ShortBuffer
   */
  public static ShortBuffer createShortBuffer(ShortBuffer buf, final int size) {
    if (buf != null && buf.limit() == size) {
      buf.rewind();
      return buf;
    }

    buf = createShortBuffer(size);
    return buf;
  }

  /**
   * Creates a new ShortBuffer with the same contents as the given ShortBuffer. The new ShortBuffer is
   * seperate from the old one and changes are not reflected across. If you want to reflect changes,
   * consider using Buffer.duplicate().
   *
   * @param buf
   *          the ShortBuffer to copy
   * @return the copy
   */
  public static ShortBuffer clone(final ShortBuffer buf) {
    if (buf == null) {
      return null;
    }
    buf.rewind();

    final ShortBuffer copy;
    if (buf.isDirect()) {
      copy = createShortBuffer(buf.limit());
    } else {
      copy = createShortBufferOnHeap(buf.limit());
    }
    copy.put(buf);

    return copy;
  }

  /**
   * Ensures there is at least the <code>required</code> number of entries left after the current
   * position of the buffer. If the buffer is too small a larger one is created and the old one copied
   * to the new buffer.
   *
   * @param buffer
   *          buffer that should be checked/copied (may be null)
   * @param required
   *          minimum number of elements that should be remaining in the returned buffer
   * @return a buffer large enough to receive at least the <code>required</code> number of entries,
   *         same position as the input buffer, not null
   */
  public static FloatBuffer ensureLargeEnough(FloatBuffer buffer, final int required) {
    if (buffer == null || (buffer.remaining() < required)) {
      final int position = (buffer != null ? buffer.position() : 0);
      final FloatBuffer newVerts = createFloatBuffer(position + required);
      if (buffer != null) {
        buffer.rewind();
        newVerts.put(buffer);
        newVerts.position(position);
      }
      buffer = newVerts;
    }
    return buffer;
  }

  // // -- GENERAL INDEXBUFFERDATA ROUTINES -- ////

  /**
   * Create a new IndexBufferData of the specified size. The specific implementation will be chosen
   * based on the max value you need to store in your buffer. If that value is less than 2^8, a
   * ByteBufferData is used. If it is less than 2^16, a ShortBufferData is used. Otherwise an
   * IntBufferData is used.
   *
   * @param size
   *          required number of values to store.
   * @param maxValue
   *          the largest value you will need to store in your buffer. Often this is equal to ("size
   *          of vertex buffer" - 1).
   * @return the new IndexBufferData
   */
  public static IndexBufferData<?> createIndexBufferData(final int size, final int maxValue) {
    if (maxValue < 256) { // 2^8
      return createIndexBufferData(size, ByteBufferData.class);
    } else if (maxValue < 65536) { // 2^16
      return createIndexBufferData(size, ShortBufferData.class);
    } else {
      return createIndexBufferData(size, IntBufferData.class);
    }
  }

  /**
   * Create a new IndexBufferData large enough to fit the contents of the given array. The specific
   * implementation will be chosen based on the max value you need to store in your buffer. If that
   * value is less than 2^8, a ByteBufferData is used. If it is less than 2^16, a ShortBufferData is
   * used. Otherwise an IntBufferData is used.
   *
   * @param contents
   *          an array of index values to store in your newly created IndexBufferData.
   * @param maxValue
   *          the largest value you will need to store in your buffer. Often this is equal to ("size
   *          of vertex buffer" - 1).
   * @return the new IndexBufferData
   */
  public static IndexBufferData<?> createIndexBufferData(final int[] contents, final int maxValue) {
    final IndexBufferData<?> buffer;
    if (maxValue < 256) { // 2^8
      buffer = createIndexBufferData(contents.length, ByteBufferData.class);
    } else if (maxValue < 65536) { // 2^16
      buffer = createIndexBufferData(contents.length, ShortBufferData.class);
    } else {
      buffer = createIndexBufferData(contents.length, IntBufferData.class);
    }
    buffer.put(contents);
    return buffer;
  }

  /**
   * Create a new IndexBufferData of the specified size and class.
   *
   * @param size
   *          required number of values to store.
   * @param clazz
   *          The class type to instantiate.
   * @return the new IndexBufferData
   */
  public static IndexBufferData<?> createIndexBufferData(final int size,
      final Class<? extends IndexBufferData<?>> clazz) {
    try {
      return clazz.getConstructor(int.class).newInstance(size);
    } catch (final Exception ex) {
      throw new Ardor3dException(ex.getMessage(), ex);
    }
  }

  /**
   * Creates a new IndexBufferData with the same contents as the given IndexBufferData. The new
   * IndexBufferData is separate from the old one and changes are not reflected across.
   *
   * @param buf
   *          the IndexBufferData to copy
   * @return the copy
   */
  @SuppressWarnings("unchecked")
  public static IndexBufferData<?> clone(final IndexBufferData<?> buf) {
    if (buf == null) {
      return null;
    }

    final IndexBufferData<?> copy =
        createIndexBufferData(buf.getBufferLimit(), (Class<? extends IndexBufferData<?>>) buf.getClass());
    if (buf.getBuffer() == null) {
      copy.setBuffer(null);
    } else {
      buf.getBuffer().rewind();
      copy.put(buf);
    }

    return copy;
  }

  // // -- GENERAL HEAP BYTE ROUTINES -- ////

  /**
   * Create a new ByteBuffer of the specified size.
   *
   * @param size
   *          required number of ints to store.
   * @return the new IntBuffer
   */
  public static ByteBuffer createByteBufferOnHeap(final int size) {
    final ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.nativeOrder());
    buf.clear();
    return buf;
  }

  /**
   * Create a new ByteBuffer of an appropriate size to hold the specified number of ints only if the
   * given buffer if not already the right size.
   *
   * @param buf
   *          the buffer to first check and rewind
   * @param size
   *          number of bytes that need to be held by the newly created buffer
   * @return the requested new IntBuffer
   */
  public static ByteBuffer createByteBufferOnHeap(ByteBuffer buf, final int size) {
    if (buf != null && buf.limit() == size) {
      buf.rewind();
      return buf;
    }

    buf = createByteBufferOnHeap(size);
    return buf;
  }

  /**
   * Creates a new ByteBuffer with the same contents as the given ByteBuffer. The new ByteBuffer is
   * seperate from the old one and changes are not reflected across. If you want to reflect changes,
   * consider using Buffer.duplicate().
   *
   * @param buf
   *          the ByteBuffer to copy
   * @return the copy
   */
  public static ByteBuffer cloneOnHeap(final ByteBuffer buf) {
    if (buf == null) {
      return null;
    }
    buf.rewind();

    final ByteBuffer copy = createByteBufferOnHeap(buf.limit());
    copy.put(buf);

    return copy;
  }

  public static void printCurrentDirectMemory(StringBuilder store) {
    long totalHeld = 0;
    // make a new set to hold the keys to prevent concurrency issues.
    final List<Buffer> bufs = new ArrayList<>(trackingHash.keySet());
    int fBufs = 0, bBufs = 0, iBufs = 0, sBufs = 0, dBufs = 0;
    int fBufsM = 0, bBufsM = 0, iBufsM = 0, sBufsM = 0, dBufsM = 0;
    for (final Buffer b : bufs) {
      if (b instanceof ByteBuffer) {
        totalHeld += b.capacity();
        bBufsM += b.capacity();
        bBufs++;
      } else if (b instanceof FloatBuffer) {
        totalHeld += b.capacity() * 4;
        fBufsM += b.capacity() * 4;
        fBufs++;
      } else if (b instanceof IntBuffer) {
        totalHeld += b.capacity() * 4;
        iBufsM += b.capacity() * 4;
        iBufs++;
      } else if (b instanceof ShortBuffer) {
        totalHeld += b.capacity() * 2;
        sBufsM += b.capacity() * 2;
        sBufs++;
      } else if (b instanceof DoubleBuffer) {
        totalHeld += b.capacity() * 8;
        dBufsM += b.capacity() * 8;
        dBufs++;
      }
    }
    final boolean printStout = store == null;
    if (store == null) {
      store = new StringBuilder();
    }
    store.append("Existing buffers: ").append(bufs.size()).append('\n');
    store.append("(b: ").append(bBufs).append("  f: ").append(fBufs).append("  i: ").append(iBufs).append("  s: ")
        .append(sBufs).append("  d: ").append(dBufs).append(')').append('\n');
    store.append("Total direct memory held: ").append(totalHeld / 1024).append("kb\n");
    store.append("(b: ").append(bBufsM / 1024).append("kb  f: ").append(fBufsM / 1024).append("kb  i: ")
        .append(iBufsM / 1024).append("kb  s: ").append(sBufsM / 1024).append("kb  d: ").append(dBufsM / 1024)
        .append("kb)").append('\n');
    if (printStout) {
      System.out.println(store.toString());
    }
  }
}
