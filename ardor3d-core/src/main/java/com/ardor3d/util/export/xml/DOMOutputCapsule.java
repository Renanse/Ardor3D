/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export.xml;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.export.binary.BinaryClassField;

/**
 * Part of the ardor3d XML IO system
 */
public class DOMOutputCapsule implements OutputCapsule {

  private static final String _dataAttributeName = "data";
  private final Document _doc;
  private Element _currentElement;
  private final Map<Savable, Element> _writtenSavables = new IdentityHashMap<>();

  public DOMOutputCapsule(final Document doc) {
    _doc = doc;
    _currentElement = null;
  }

  public Document getDoc() { return _doc; }

  /**
   * appends a new Element with the given name to currentElement, sets currentElement to be new
   * Element, and returns the new Element as well
   */
  private Element appendElement(final String name) {
    Element ret = null;
    ret = _doc.createElement(name);
    if (_currentElement == null) {
      _doc.appendChild(ret);
    } else {
      _currentElement.appendChild(ret);
    }
    _currentElement = ret;
    return ret;
  }

  private static String encodeString(String s) {
    if (s == null) {
      return null;
    }
    s = s.replaceAll("\\&", "&amp;").replaceAll("\\\"", "&quot;").replaceAll("\\<", "&lt;");
    return s;
  }

  @Override
  public void write(final byte value, final String name, final byte defVal) throws IOException {
    if (value == defVal) {
      return;
    }
    _currentElement.setAttribute(name, String.valueOf(value));
  }

  @Override
  public void write(byte[] value, final String name, final byte[] defVal) throws IOException {
    final StringBuilder buf = new StringBuilder();
    if (value == null) {
      value = defVal;
    }
    for (final byte b : value) {
      buf.append(b);
      buf.append(' ');
    }
    // remove last space
    buf.setLength(buf.length() - 1);

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) _currentElement.getParentNode();
  }

  @Override
  public void write(byte[][] value, final String name, final byte[][] defVal) throws IOException {
    final StringBuilder buf = new StringBuilder();
    if (value == null) {
      value = defVal;
    }
    for (final byte[] bs : value) {
      for (final byte b : bs) {
        buf.append(b);
        buf.append(' ');
      }
      buf.append(' ');
    }
    // remove last spaces
    buf.setLength(buf.length() - 2);

    final Element el = appendElement(name);
    el.setAttribute("size_outer", String.valueOf(value.length));
    el.setAttribute("size_inner", String.valueOf(value[0].length));
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) _currentElement.getParentNode();
  }

  @Override
  public void write(final int value, final String name, final int defVal) throws IOException {
    if (value == defVal) {
      return;
    }
    _currentElement.setAttribute(name, String.valueOf(value));
  }

  @Override
  public void write(final int[] value, final String name, final int[] defVal) throws IOException {
    final StringBuilder buf = new StringBuilder();
    if (value == null) {
      return;
    }
    if (Arrays.equals(value, defVal)) {
      return;
    }

    for (final int b : value) {
      buf.append(b);
      buf.append(' ');
    }
    // remove last space
    buf.setLength(Math.max(0, buf.length() - 1));

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) _currentElement.getParentNode();
  }

  @Override
  public void write(final int[][] value, final String name, final int[][] defVal) throws IOException {
    if (value == null) {
      return;
    }
    if (Arrays.deepEquals(value, defVal)) {
      return;
    }

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));

    for (int i = 0; i < value.length; i++) {
      final int[] array = value[i];
      write(array, "array_" + i, defVal == null ? null : defVal[i]);
    }
    _currentElement = (Element) el.getParentNode();
  }

  @Override
  public void write(final float value, final String name, final float defVal) throws IOException {
    if (value == defVal) {
      return;
    }
    _currentElement.setAttribute(name, String.valueOf(value));
  }

  @Override
  public void write(float[] value, final String name, final float[] defVal) throws IOException {
    final StringBuilder buf = new StringBuilder();
    if (value == null) {
      value = defVal;
    }
    for (final float b : value) {
      buf.append(b);
      buf.append(' ');
    }
    // remove last space
    buf.setLength(buf.length() - 1);

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) _currentElement.getParentNode();
  }

  @Override
  public void write(final float[][] value, final String name, final float[][] defVal) throws IOException {
    final StringBuilder buf = new StringBuilder();
    if (value == null) {
      return;
    }
    if (Arrays.deepEquals(value, defVal)) {
      return;
    }

    for (final float[] bs : value) {
      for (final float b : bs) {
        buf.append(b);
        buf.append(' ');
      }
    }
    // remove last space
    buf.setLength(buf.length() - 1);

    final Element el = appendElement(name);
    el.setAttribute("size_outer", String.valueOf(value.length));
    el.setAttribute("size_inner", String.valueOf(value[0].length));
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) _currentElement.getParentNode();
  }

  @Override
  public void write(final double value, final String name, final double defVal) throws IOException {
    if (value == defVal) {
      return;
    }
    _currentElement.setAttribute(name, String.valueOf(value));
  }

  @Override
  public void write(double[] value, final String name, final double[] defVal) throws IOException {
    final StringBuilder buf = new StringBuilder();
    if (value == null) {
      value = defVal;
    }
    for (final double b : value) {
      buf.append(b);
      buf.append(' ');
    }
    // remove last space
    buf.setLength(buf.length() - 1);

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) _currentElement.getParentNode();
  }

  @Override
  public void write(final double[][] value, final String name, final double[][] defVal) throws IOException {
    if (value == null) {
      return;
    }
    if (Arrays.deepEquals(value, defVal)) {
      return;
    }

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));

    for (int i = 0; i < value.length; i++) {
      final double[] array = value[i];
      write(array, "array_" + i, defVal == null ? null : defVal[i]);
    }
    _currentElement = (Element) el.getParentNode();
  }

  @Override
  public void write(final long value, final String name, final long defVal) throws IOException {
    if (value == defVal) {
      return;
    }
    _currentElement.setAttribute(name, String.valueOf(value));
  }

  @Override
  public void write(long[] value, final String name, final long[] defVal) throws IOException {
    final StringBuilder buf = new StringBuilder();
    if (value == null) {
      value = defVal;
    }
    for (final long b : value) {
      buf.append(b);
      buf.append(' ');
    }
    // remove last space
    buf.setLength(buf.length() - 1);

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) _currentElement.getParentNode();
  }

  @Override
  public void write(final long[][] value, final String name, final long[][] defVal) throws IOException {
    if (value == null) {
      return;
    }
    if (Arrays.deepEquals(value, defVal)) {
      return;
    }

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));

    for (int i = 0; i < value.length; i++) {
      final long[] array = value[i];
      write(array, "array_" + i, defVal == null ? null : defVal[i]);
    }
    _currentElement = (Element) el.getParentNode();
  }

  @Override
  public void write(final short value, final String name, final short defVal) throws IOException {
    if (value == defVal) {
      return;
    }
    _currentElement.setAttribute(name, String.valueOf(value));
  }

  @Override
  public void write(short[] value, final String name, final short[] defVal) throws IOException {
    final StringBuilder buf = new StringBuilder();
    if (value == null) {
      value = defVal;
    }
    for (final short b : value) {
      buf.append(b);
      buf.append(' ');
    }
    // remove last space
    buf.setLength(buf.length() - 1);

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) _currentElement.getParentNode();
  }

  @Override
  public void write(final short[][] value, final String name, final short[][] defVal) throws IOException {
    if (value == null) {
      return;
    }
    if (Arrays.deepEquals(value, defVal)) {
      return;
    }

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));

    for (int i = 0; i < value.length; i++) {
      final short[] array = value[i];
      write(array, "array_" + i, defVal == null ? null : defVal[i]);
    }
    _currentElement = (Element) el.getParentNode();
  }

  @Override
  public void write(final boolean value, final String name, final boolean defVal) throws IOException {
    if (value == defVal) {
      return;
    }
    _currentElement.setAttribute(name, String.valueOf(value));
  }

  @Override
  public void write(boolean[] value, final String name, final boolean[] defVal) throws IOException {
    final StringBuilder buf = new StringBuilder();
    if (value == null) {
      value = defVal;
    }
    for (final boolean b : value) {
      buf.append(b);
      buf.append(' ');
    }
    // remove last space
    buf.setLength(Math.max(0, buf.length() - 1));

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) _currentElement.getParentNode();
  }

  @Override
  public void write(final boolean[][] value, final String name, final boolean[][] defVal) throws IOException {
    if (value == null) {
      return;
    }
    if (Arrays.deepEquals(value, defVal)) {
      return;
    }

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));

    for (int i = 0; i < value.length; i++) {
      final boolean[] array = value[i];
      write(array, "array_" + i, defVal == null ? null : defVal[i]);
    }
    _currentElement = (Element) el.getParentNode();
  }

  @Override
  public void write(final String value, final String name, final String defVal) throws IOException {
    if (value == null || value.equals(defVal)) {
      return;
    }
    _currentElement.setAttribute(name, encodeString(value));
  }

  @Override
  public void write(String[] value, final String name, final String[] defVal) throws IOException {
    final Element el = appendElement(name);

    if (value == null) {
      value = defVal;
    }

    el.setAttribute("size", String.valueOf(value.length));

    for (int i = 0; i < value.length; i++) {
      final String b = value[i];
      appendElement("String_" + i);
      final String val = encodeString(b);
      _currentElement.setAttribute("value", val);
      _currentElement = el;
    }
    _currentElement = (Element) _currentElement.getParentNode();
  }

  @Override
  public void write(final String[][] value, final String name, final String[][] defVal) throws IOException {
    if (value == null) {
      return;
    }
    if (Arrays.deepEquals(value, defVal)) {
      return;
    }

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.length));

    for (int i = 0; i < value.length; i++) {
      final String[] array = value[i];
      write(array, "array_" + i, defVal == null ? null : defVal[i]);
    }
    _currentElement = (Element) el.getParentNode();
  }

  @Override
  public void write(final BitSet value, final String name, final BitSet defVal) throws IOException {
    if (value == null || value.equals(defVal)) {
      return;
    }
    final StringBuilder buf = new StringBuilder();
    for (int i = value.nextSetBit(0); i >= 0; i = value.nextSetBit(i + 1)) {
      buf.append(i);
      buf.append(' ');
    }
    buf.setLength(Math.max(0, buf.length() - 1));
    _currentElement.setAttribute(name, buf.toString());

  }

  @Override
  public void write(final Savable object, String name, final Savable defVal) throws IOException {
    if (object == null) {
      return;
    }
    if (object.equals(defVal)) {
      return;
    }

    final Element old = _currentElement;
    Element el = _writtenSavables.get(object);

    String className = null;
    if (!object.getClass().getName().equals(name)) {
      className = object.getClass().getName();
    }
    try {
      _doc.createElement(name);
    } catch (final DOMException e) {
      name = "Object";
      className = object.getClass().getName();
    }

    if (el != null) {
      String refID = el.getAttribute("reference_ID");
      if (refID.length() == 0) {
        refID = object.getClassTag().getName() + "@" + object.hashCode();
        el.setAttribute("reference_ID", refID);
      }
      el = appendElement(name);
      el.setAttribute("ref", refID);
    } else {
      el = appendElement(name);
      _writtenSavables.put(object, el);
      object.write(this);
    }
    if (className != null) {
      el.setAttribute("class", className);
    }

    _currentElement = old;
  }

  @Override
  public void write(final Savable[] objects, final String name, final Savable[] defVal) throws IOException {
    if (objects == null) {
      return;
    }
    if (Arrays.equals(objects, defVal)) {
      return;
    }

    final Element old = _currentElement;
    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(objects.length));
    for (int i = 0; i < objects.length; i++) {
      final Savable o = objects[i];
      if (o == null) {
        // renderStateList has special loading code, so we can leave out the null values
        if (!name.equals("renderStateList")) {
          final Element before = _currentElement;
          appendElement("null");
          _currentElement = before;
        }
      } else {
        write(o, o.getClassTag().getName(), null);
      }
    }
    _currentElement = old;
  }

  @Override
  public void write(final Savable[][] value, final String name, final Savable[][] defVal) throws IOException {
    if (value == null) {
      return;
    }
    if (Arrays.deepEquals(value, defVal)) {
      return;
    }

    final Element el = appendElement(name);
    el.setAttribute("size_outer", String.valueOf(value.length));
    el.setAttribute("size_inner", String.valueOf(value[0].length));
    for (final Savable[] bs : value) {
      for (final Savable b : bs) {
        write(b, b.getClassTag().getSimpleName(), null);
      }
    }
    _currentElement = (Element) _currentElement.getParentNode();
  }

  @Override
  public void writeSavableList(final List<? extends Savable> array, final String name,
      final List<? extends Savable> defVal) throws IOException {
    if (array == null) {
      return;
    }
    if (array.equals(defVal)) {
      return;
    }
    final Element old = _currentElement;
    final Element el = appendElement(name);
    _currentElement = el;
    el.setAttribute(XMLExporter.ATTRIBUTE_SIZE, String.valueOf(array.size()));
    for (final Object o : array) {
      if (o == null) {
        continue;
      } else if (o instanceof Savable) {
        final Savable s = (Savable) o;
        write(s, s.getClassTag().getName(), null);
      } else {
        throw new ClassCastException("Not a Savable instance: " + o);
      }
    }
    _currentElement = old;
  }

  @Override
  public void writeSavableListArray(final List<? extends Savable>[] objects, final String name,
      final List<? extends Savable>[] defVal) throws IOException {
    if (objects == null) {
      return;
    }
    if (Arrays.equals(objects, defVal)) {
      return;
    }

    final Element old = _currentElement;
    final Element el = appendElement(name);
    el.setAttribute(XMLExporter.ATTRIBUTE_SIZE, String.valueOf(objects.length));
    for (int i = 0; i < objects.length; i++) {
      final List<? extends Savable> o = objects[i];
      if (o == null) {
        final Element before = _currentElement;
        appendElement("null");
        _currentElement = before;
      } else {
        final StringBuilder buf = new StringBuilder("SavableArrayList_");
        buf.append(i);
        writeSavableList(o, buf.toString(), null);
      }
    }
    _currentElement = old;
  }

  @Override
  public void writeSavableListArray2D(final List<? extends Savable>[][] value, final String name,
      final List<? extends Savable>[][] defVal) throws IOException {
    if (value == null) {
      return;
    }
    if (Arrays.deepEquals(value, defVal)) {
      return;
    }

    final Element el = appendElement(name);
    final int size = value.length;
    el.setAttribute(XMLExporter.ATTRIBUTE_SIZE, String.valueOf(size));

    for (int i = 0; i < size; i++) {
      final List<? extends Savable>[] vi = value[i];
      writeSavableListArray(vi, "SavableArrayListArray_" + i, null);
    }
    _currentElement = (Element) el.getParentNode();
  }

  @Override
  public void writeFloatBufferList(final List<FloatBuffer> array, final String name, final List<FloatBuffer> defVal)
      throws IOException {
    if (array == null) {
      return;
    }
    if (array.equals(defVal)) {
      return;
    }
    final Element el = appendElement(name);
    el.setAttribute(XMLExporter.ATTRIBUTE_SIZE, String.valueOf(array.size()));
    for (final FloatBuffer o : array) {
      write(o, XMLExporter.ELEMENT_FLOATBUFFER, null);
    }
    _currentElement = (Element) el.getParentNode();
  }

  @Override
  public void writeSavableMap(final Map<? extends Savable, ? extends Savable> map, final String name,
      final Map<? extends Savable, ? extends Savable> defVal) throws IOException {
    if (map == null) {
      return;
    }
    if (map.equals(defVal)) {
      return;
    }
    final Element stringMap = appendElement(name);

    final Iterator<? extends Savable> keyIterator = map.keySet().iterator();
    while (keyIterator.hasNext()) {
      final Savable key = keyIterator.next();
      appendElement(XMLExporter.ELEMENT_MAPENTRY);
      write(key, XMLExporter.ELEMENT_KEY, null);
      final Savable value = map.get(key);
      write(value, XMLExporter.ELEMENT_VALUE, null);
      _currentElement = stringMap;
    }

    _currentElement = (Element) stringMap.getParentNode();
  }

  @Override
  public void writeStringSavableMap(final Map<String, ? extends Savable> map, final String name,
      final Map<String, ? extends Savable> defVal) throws IOException {
    if (map == null) {
      return;
    }
    if (map.equals(defVal)) {
      return;
    }
    final Element stringMap = appendElement(name);

    final Iterator<String> keyIterator = map.keySet().iterator();
    while (keyIterator.hasNext()) {
      final String key = keyIterator.next();
      final Element mapEntry = appendElement("MapEntry");
      mapEntry.setAttribute("key", key);
      final Savable s = map.get(key);
      write(s, "Savable", null);
      _currentElement = stringMap;
    }

    _currentElement = (Element) stringMap.getParentNode();
  }

  @Override
  public void writeStringObjectMap(final Map<String, Object> map, final String name, final Map<String, Object> defVal)
      throws IOException {
    if (map == null) {
      return;
    }
    if (map == defVal) {
      return;
    }
    final Element stringMap = appendElement(name);

    final Iterator<String> keyIterator = map.keySet().iterator();
    while (keyIterator.hasNext()) {
      final String key = keyIterator.next();
      final Element mapEntry = appendElement("MapEntry");
      mapEntry.setAttribute("key", key);
      tryToWriteValue(map.get(key));
      _currentElement = stringMap;
    }

    _currentElement = (Element) stringMap.getParentNode();
  }

  @SuppressWarnings("unchecked")
  private void tryToWriteValue(final Object value) throws IOException {
    if (value instanceof BitSet) {
      write(BinaryClassField.BITSET, "type", (byte) -1);
      write((BitSet) value, "value", null);
      return;
    }
    if (value instanceof Boolean) {
      write(BinaryClassField.BOOLEAN, "type", (byte) -1);
      write((Boolean) value, "value", false);
      return;
    }
    if (value instanceof boolean[]) {
      write(BinaryClassField.BOOLEAN_1D, "type", (byte) -1);
      write((boolean[]) value, "value", null);
      return;
    }
    if (value instanceof boolean[][]) {
      write(BinaryClassField.BOOLEAN_2D, "type", (byte) -1);
      write((boolean[][]) value, "value", null);
      return;
    }
    if (value instanceof Byte) {
      write(BinaryClassField.BYTE, "type", (byte) -1);
      write((Byte) value, "value", (byte) 0);
      return;
    }
    if (value instanceof byte[]) {
      write(BinaryClassField.BYTE_1D, "type", (byte) -1);
      write((byte[]) value, "value", null);
      return;
    }
    if (value instanceof byte[][]) {
      write(BinaryClassField.BYTE_2D, "type", (byte) -1);
      write((byte[][]) value, "value", null);
      return;
    }
    if (value instanceof ByteBuffer) {
      write(BinaryClassField.BYTEBUFFER, "type", (byte) -1);
      write((ByteBuffer) value, "value", null);
      return;
    }
    if (value instanceof Double) {
      write(BinaryClassField.DOUBLE, "type", (byte) -1);
      write((Double) value, "value", 0.0);
      return;
    }
    if (value instanceof double[]) {
      write(BinaryClassField.DOUBLE_1D, "type", (byte) -1);
      write((double[]) value, "value", null);
      return;
    }
    if (value instanceof double[][]) {
      write(BinaryClassField.DOUBLE_2D, "type", (byte) -1);
      write((double[][]) value, "value", null);
      return;
    }
    if (value instanceof Float) {
      write(BinaryClassField.FLOAT, "type", (byte) -1);
      write((Float) value, "value", 0f);
      return;
    }
    if (value instanceof float[]) {
      write(BinaryClassField.FLOAT_1D, "type", (byte) -1);
      write((float[]) value, "value", null);
      return;
    }
    if (value instanceof float[][]) {
      write(BinaryClassField.FLOAT_2D, "type", (byte) -1);
      write((float[][]) value, "value", null);
      return;
    }
    if (value instanceof FloatBuffer) {
      write(BinaryClassField.FLOATBUFFER, "type", (byte) -1);
      write((FloatBuffer) value, "value", null);
      return;
    }
    if (value instanceof Integer) {
      write(BinaryClassField.INT, "type", (byte) -1);
      write((Integer) value, "value", 0);
      return;
    }
    if (value instanceof int[]) {
      write(BinaryClassField.INT_1D, "type", (byte) -1);
      write((int[]) value, "value", null);
      return;
    }
    if (value instanceof int[][]) {
      write(BinaryClassField.INT_2D, "type", (byte) -1);
      write((int[][]) value, "value", null);
      return;
    }
    if (value instanceof IntBuffer) {
      write(BinaryClassField.INTBUFFER, "type", (byte) -1);
      write((IntBuffer) value, "value", null);
      return;
    }
    if (value instanceof Long) {
      write(BinaryClassField.LONG, "type", (byte) -1);
      write((Long) value, "value", 0L);
      return;
    }
    if (value instanceof long[]) {
      write(BinaryClassField.LONG_1D, "type", (byte) -1);
      write((long[]) value, "value", null);
      return;
    }
    if (value instanceof long[][]) {
      write(BinaryClassField.LONG_2D, "type", (byte) -1);
      write((long[][]) value, "value", null);
      return;
    }
    if (value instanceof Savable) {
      write(BinaryClassField.SAVABLE, "type", (byte) -1);
      write((Savable) value, "value", null);
      return;
    }
    if (value instanceof Savable[]) {
      write(BinaryClassField.SAVABLE_1D, "type", (byte) -1);
      write((Savable[]) value, "value", null);
      return;
    }
    if (value instanceof Savable[][]) {
      write(BinaryClassField.SAVABLE_2D, "type", (byte) -1);
      write((Savable[][]) value, "value", null);
      return;
    }
    if (value instanceof Short) {
      write(BinaryClassField.SHORT, "type", (byte) -1);
      write((Short) value, "value", (short) 0);
      return;
    }
    if (value instanceof short[]) {
      write(BinaryClassField.SHORT_1D, "type", (byte) -1);
      write((short[]) value, "value", null);
      return;
    }
    if (value instanceof short[][]) {
      write(BinaryClassField.SHORT_2D, "type", (byte) -1);
      write((short[][]) value, "value", null);
      return;
    }
    if (value instanceof ShortBuffer) {
      write(BinaryClassField.SHORTBUFFER, "type", (byte) -1);
      write((ShortBuffer) value, "value", null);
      return;
    }
    if (value instanceof String) {
      write(BinaryClassField.STRING, "type", (byte) -1);
      write((String) value, "value", null);
      return;
    }
    if (value instanceof String[]) {
      write(BinaryClassField.STRING_1D, "type", (byte) -1);
      write((String[]) value, "value", null);
      return;
    }
    if (value instanceof String[][]) {
      write(BinaryClassField.STRING_2D, "type", (byte) -1);
      write((String[][]) value, "value", null);
      return;
    }
    if (value instanceof List<?>) {
      final List<?> list = (List<?>) value;
      if (list.size() == 0) {
        write(BinaryClassField.UNHANDLED, "type", (byte) -1);
        return;
      }
      final Object first = list.get(0);
      if (list instanceof ByteBuffer) {
        write(BinaryClassField.BYTEBUFFER_ARRAYLIST, "type", (byte) -1);
        writeByteBufferList((List<ByteBuffer>) value, "value", null);
        return;
      }
      if (first instanceof FloatBuffer) {
        write(BinaryClassField.FLOATBUFFER_ARRAYLIST, "type", (byte) -1);
        writeFloatBufferList((List<FloatBuffer>) value, "value", null);
        return;
      }
      if (first instanceof Savable) {
        write(BinaryClassField.SAVABLE_ARRAYLIST, "type", (byte) -1);
        writeSavableList((List<Savable>) value, "value", null);
        return;
      }
    }

    write(BinaryClassField.UNHANDLED, "type", (byte) -1);
  }

  @Override
  public void write(final FloatBuffer value, final String name, final FloatBuffer defVal) throws IOException {
    if (value == null) {
      return;
    }

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.limit()));
    final StringBuilder buf = new StringBuilder();
    final int pos = value.position();
    value.rewind();
    while (value.hasRemaining()) {
      buf.append(value.get());
      buf.append(' ');
    }
    buf.setLength(Math.max(0, buf.length() - 1));
    value.position(pos);
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) el.getParentNode();
  }

  @Override
  public void write(final IntBuffer value, final String name, final IntBuffer defVal) throws IOException {
    if (value == null) {
      return;
    }
    if (value.equals(defVal)) {
      return;
    }

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.limit()));
    final StringBuilder buf = new StringBuilder();
    final int pos = value.position();
    value.rewind();
    while (value.hasRemaining()) {
      buf.append(value.get());
      buf.append(' ');
    }
    buf.setLength(buf.length() - 1);
    value.position(pos);
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) el.getParentNode();
  }

  @Override
  public void write(final ByteBuffer value, final String name, final ByteBuffer defVal) throws IOException {
    if (value == null) {
      return;
    }
    if (value.equals(defVal)) {
      return;
    }

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.limit()));
    final StringBuilder buf = new StringBuilder();
    final int pos = value.position();
    value.rewind();
    while (value.hasRemaining()) {
      buf.append(value.get());
      buf.append(' ');
    }
    buf.setLength(buf.length() - 1);
    value.position(pos);
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) el.getParentNode();
  }

  @Override
  public void write(final ShortBuffer value, final String name, final ShortBuffer defVal) throws IOException {
    if (value == null) {
      return;
    }
    if (value.equals(defVal)) {
      return;
    }

    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(value.limit()));
    final StringBuilder buf = new StringBuilder();
    final int pos = value.position();
    value.rewind();
    while (value.hasRemaining()) {
      buf.append(value.get());
      buf.append(' ');
    }
    buf.setLength(buf.length() - 1);
    value.position(pos);
    el.setAttribute(_dataAttributeName, buf.toString());
    _currentElement = (Element) el.getParentNode();
  }

  @Override
  public void writeByteBufferList(final List<ByteBuffer> array, final String name, final List<ByteBuffer> defVal)
      throws IOException {
    if (array == null) {
      return;
    }
    if (array.equals(defVal)) {
      return;
    }
    final Element el = appendElement(name);
    el.setAttribute("size", String.valueOf(array.size()));
    for (final ByteBuffer o : array) {
      write(o, "ByteBuffer", null);
    }
    _currentElement = (Element) el.getParentNode();

  }

  @Override
  public void write(final Enum<?> value, final String name, final Enum<?> defVal) throws IOException {
    if (value == defVal || value == null) {
      return;
    }
    _currentElement.setAttribute(name, String.valueOf(value));
  }

  @Override
  public void write(final Enum<?>[] value, final String name) throws IOException {
    if (value == null) {
      return;
    } else {
      final String[] toWrite = new String[value.length];
      int i = 0;
      for (final Enum<?> val : value) {
        toWrite[i++] = val.name();
      }
      write(toWrite, name, null);
    }
  }
}
