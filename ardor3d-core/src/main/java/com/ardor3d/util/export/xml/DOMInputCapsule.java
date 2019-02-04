/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export.xml;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.image.Texture;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.export.binary.BinaryClassField;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Lists;

/**
 * Part of the ardor3d XML IO system
 */
public class DOMInputCapsule implements InputCapsule {

    private final Document _doc;
    private Element _currentElem;
    private boolean _isAtRoot = true;
    private final Map<String, Savable> _referencedSavables = new HashMap<String, Savable>();

    public DOMInputCapsule(final Document doc) {
        _doc = doc;
        _currentElem = doc.getDocumentElement();
    }

    private static String decodeString(String s) {
        if (s == null) {
            return null;
        }
        s = s.replaceAll("\\&quot;", "\"").replaceAll("\\&lt;", "<").replaceAll("\\&amp;", "&");
        return s;
    }

    private Element findFirstChildElement(final Element parent) {
        Node ret = parent.getFirstChild();
        while (ret != null && (!(ret instanceof Element))) {
            ret = ret.getNextSibling();
        }
        return (Element) ret;
    }

    private Element findChildElement(final Element parent, final String name) {
        if (parent == null) {
            return null;
        }
        Node ret = parent.getFirstChild();
        while (ret != null && (!(ret instanceof Element) || !ret.getNodeName().equals(name))) {
            ret = ret.getNextSibling();
        }
        return (Element) ret;
    }

    private Element findNextSiblingElement(final Element current) {
        Node ret = current.getNextSibling();
        while (ret != null) {
            if (ret instanceof Element) {
                return (Element) ret;
            }
            ret = ret.getNextSibling();
        }
        return null;
    }

    public byte readByte(final String name, final byte defVal) throws IOException {
        byte ret = defVal;
        try {
            ret = Byte.parseByte(_currentElem.getAttribute(name));
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public byte[] readByteArray(final String name, final byte[] defVal) throws IOException {
        byte[] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final byte[] tmp = new byte[size];
            final String[] strings = tmpEl.getAttribute("data").split("\\s+");
            for (int i = 0; i < size; i++) {
                tmp[i] = Byte.parseByte(strings[i]);
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public byte[][] readByteArray2D(final String name, final byte[][] defVal) throws IOException {
        byte[][] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final byte[][] tmp = new byte[size][];
            final NodeList nodes = _currentElem.getChildNodes();
            int strIndex = 0;
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node n = nodes.item(i);
                if (n instanceof Element && n.getNodeName().contains("array")) {
                    if (strIndex < size) {
                        tmp[strIndex++] = readByteArray(n.getNodeName(), null);
                    } else {
                        throw new IOException("String array contains more elements than specified!");
                    }
                }
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        _currentElem = (Element) _currentElem.getParentNode();
        return ret;
    }

    public int readInt(final String name, final int defVal) throws IOException {
        int ret = defVal;
        try {
            final String s = _currentElem.getAttribute(name);
            if (s.length() > 0) {
                ret = Integer.parseInt(s);
            }
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public int[] readIntArray(final String name, final int[] defVal) throws IOException {
        int[] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final int[] tmp = new int[size];
            final String[] strings = tmpEl.getAttribute("data").split("\\s+");
            for (int i = 0; i < size; i++) {
                tmp[i] = Integer.parseInt(strings[i]);
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public int[][] readIntArray2D(final String name, final int[][] defVal) throws IOException {
        int[][] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final int[][] tmp = new int[size][];
            final NodeList nodes = _currentElem.getChildNodes();
            int strIndex = 0;
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node n = nodes.item(i);
                if (n instanceof Element && n.getNodeName().contains("array")) {
                    if (strIndex < size) {
                        tmp[strIndex++] = readIntArray(n.getNodeName(), null);
                    } else {
                        throw new IOException("String array contains more elements than specified!");
                    }
                }
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        _currentElem = (Element) _currentElem.getParentNode();
        return ret;
    }

    public float readFloat(final String name, final float defVal) throws IOException {
        float ret = defVal;
        try {
            final String s = _currentElem.getAttribute(name);
            if (s.length() > 0) {
                ret = Float.parseFloat(s);
            }
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public float[] readFloatArray(final String name, final float[] defVal) throws IOException {
        float[] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final float[] tmp = new float[size];
            final String[] strings = tmpEl.getAttribute("data").split("\\s+");
            for (int i = 0; i < size; i++) {
                tmp[i] = Float.parseFloat(strings[i]);
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public float[][] readFloatArray2D(final String name, final float[][] defVal) throws IOException {
        float[][] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size_outer = Integer.parseInt(tmpEl.getAttribute("size_outer"));
            final int size_inner = Integer.parseInt(tmpEl.getAttribute("size_outer"));

            final float[][] tmp = new float[size_outer][size_inner];

            final String[] strings = tmpEl.getAttribute("data").split("\\s+");
            for (int i = 0; i < size_outer; i++) {
                tmp[i] = new float[size_inner];
                for (int k = 0; k < size_inner; k++) {
                    tmp[i][k] = Float.parseFloat(strings[i]);
                }
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public double readDouble(final String name, final double defVal) throws IOException {
        double ret = defVal;
        try {
            final String s = _currentElem.getAttribute(name);
            if (s.length() > 0) {
                ret = Double.parseDouble(s);
            }
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public double[] readDoubleArray(final String name, final double[] defVal) throws IOException {
        double[] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final double[] tmp = new double[size];
            final String[] strings = tmpEl.getAttribute("data").split("\\s+");
            for (int i = 0; i < size; i++) {
                tmp[i] = Double.parseDouble(strings[i]);
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public double[][] readDoubleArray2D(final String name, final double[][] defVal) throws IOException {
        double[][] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final double[][] tmp = new double[size][];
            final NodeList nodes = _currentElem.getChildNodes();
            int strIndex = 0;
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node n = nodes.item(i);
                if (n instanceof Element && n.getNodeName().contains("array")) {
                    if (strIndex < size) {
                        tmp[strIndex++] = readDoubleArray(n.getNodeName(), null);
                    } else {
                        throw new IOException("String array contains more elements than specified!");
                    }
                }
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        _currentElem = (Element) _currentElem.getParentNode();
        return ret;
    }

    public long readLong(final String name, final long defVal) throws IOException {
        long ret = defVal;
        try {
            final String s = _currentElem.getAttribute(name);
            if (s.length() > 0) {
                ret = Long.parseLong(s);
            }
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public long[] readLongArray(final String name, final long[] defVal) throws IOException {
        long[] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final long[] tmp = new long[size];
            final String[] strings = tmpEl.getAttribute("data").split("\\s+");
            for (int i = 0; i < size; i++) {
                tmp[i] = Long.parseLong(strings[i]);
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public long[][] readLongArray2D(final String name, final long[][] defVal) throws IOException {
        long[][] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final long[][] tmp = new long[size][];
            final NodeList nodes = _currentElem.getChildNodes();
            int strIndex = 0;
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node n = nodes.item(i);
                if (n instanceof Element && n.getNodeName().contains("array")) {
                    if (strIndex < size) {
                        tmp[strIndex++] = readLongArray(n.getNodeName(), null);
                    } else {
                        throw new IOException("String array contains more elements than specified!");
                    }
                }
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        _currentElem = (Element) _currentElem.getParentNode();
        return ret;
    }

    public short readShort(final String name, final short defVal) throws IOException {
        short ret = defVal;
        try {
            final String s = _currentElem.getAttribute(name);
            if (s.length() > 0) {
                ret = Short.parseShort(s);
            }
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public short[] readShortArray(final String name, final short[] defVal) throws IOException {
        short[] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final short[] tmp = new short[size];
            final String[] strings = tmpEl.getAttribute("data").split("\\s+");
            for (int i = 0; i < size; i++) {
                tmp[i] = Short.parseShort(strings[i]);
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public short[][] readShortArray2D(final String name, final short[][] defVal) throws IOException {
        short[][] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final short[][] tmp = new short[size][];
            final NodeList nodes = _currentElem.getChildNodes();
            int strIndex = 0;
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node n = nodes.item(i);
                if (n instanceof Element && n.getNodeName().contains("array")) {
                    if (strIndex < size) {
                        tmp[strIndex++] = readShortArray(n.getNodeName(), null);
                    } else {
                        throw new IOException("String array contains more elements than specified!");
                    }
                }
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        _currentElem = (Element) _currentElem.getParentNode();
        return ret;
    }

    public boolean readBoolean(final String name, final boolean defVal) throws IOException {
        boolean ret = defVal;
        try {
            final String s = _currentElem.getAttribute(name);
            if (s.length() > 0) {
                ret = Boolean.parseBoolean(s);
            }
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public boolean[] readBooleanArray(final String name, final boolean[] defVal) throws IOException {
        boolean[] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final boolean[] tmp = new boolean[size];
            final String[] strings = tmpEl.getAttribute("data").split("\\s+");
            for (int i = 0; i < size; i++) {
                tmp[i] = Boolean.parseBoolean(strings[i]);
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public boolean[][] readBooleanArray2D(final String name, final boolean[][] defVal) throws IOException {
        boolean[][] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final boolean[][] tmp = new boolean[size][];
            final NodeList nodes = _currentElem.getChildNodes();
            int strIndex = 0;
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node n = nodes.item(i);
                if (n instanceof Element && n.getNodeName().contains("array")) {
                    if (strIndex < size) {
                        tmp[strIndex++] = readBooleanArray(n.getNodeName(), null);
                    } else {
                        throw new IOException("String array contains more elements than specified!");
                    }
                }
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        _currentElem = (Element) _currentElem.getParentNode();
        return ret;
    }

    public String readString(final String name, final String defVal) throws IOException {
        String ret = defVal;
        try {
            ret = decodeString(_currentElem.getAttribute(name));
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public String[] readStringArray(final String name, final String[] defVal) throws IOException {
        String[] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final String[] tmp = new String[size];
            final NodeList nodes = tmpEl.getChildNodes();
            int strIndex = 0;
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node n = nodes.item(i);
                if (n instanceof Element && n.getNodeName().contains("String")) {
                    if (strIndex < size) {
                        tmp[strIndex++] = ((Element) n).getAttributeNode("value").getValue();
                    } else {
                        throw new IOException("String array contains more elements than specified!");
                    }
                }
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        // _currentElem = (Element) _currentElem.getParentNode();
        return ret;
    }

    public String[][] readStringArray2D(final String name, final String[][] defVal) throws IOException {
        String[][] ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final String[][] tmp = new String[size][];
            final NodeList nodes = _currentElem.getChildNodes();
            int strIndex = 0;
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node n = nodes.item(i);
                if (n instanceof Element && n.getNodeName().contains("array")) {
                    if (strIndex < size) {
                        tmp[strIndex++] = readStringArray(n.getNodeName(), null);
                    } else {
                        throw new IOException("String array contains more elements than specified!");
                    }
                }
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        _currentElem = (Element) _currentElem.getParentNode();
        return ret;
    }

    public BitSet readBitSet(final String name, final BitSet defVal) throws IOException {
        BitSet ret = defVal;
        try {
            final BitSet set = new BitSet();
            final String bitString = _currentElem.getAttribute(name);
            final String[] strings = bitString.split("\\s+");
            for (int i = 0; i < strings.length; i++) {
                final int isSet = Integer.parseInt(strings[i]);
                if (isSet == 1) {
                    set.set(i);
                }
            }
            ret = set;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public <E extends Savable> E readSavable(final String name, final E defVal) throws IOException {
        Savable ret = defVal;

        try {
            Element tmpEl = null;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
                if (tmpEl == null) {
                    return defVal;
                }
            } else if (_isAtRoot) {
                tmpEl = _doc.getDocumentElement();
                _isAtRoot = false;
            } else {
                tmpEl = findFirstChildElement(_currentElem);
            }
            _currentElem = tmpEl;
            ret = readSavableFromCurrentElem(defVal);
            if (_currentElem.getParentNode() instanceof Element) {
                _currentElem = (Element) _currentElem.getParentNode();
            } else {
                _currentElem = null;
            }
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }

        return (E) ret;
    }

    private Savable readSavableFromCurrentElem(final Savable defVal)
            throws InstantiationException, ClassNotFoundException, IOException, IllegalAccessException {
        Savable ret = defVal;
        Savable tmp = null;

        if (_currentElem == null || _currentElem.getNodeName().equals("null")) {
            return null;
        }
        final String reference = _currentElem.getAttribute("ref");
        if (reference.length() > 0) {
            ret = _referencedSavables.get(reference);
        } else {
            String className = _currentElem.getNodeName();
            if (defVal != null) {
                className = defVal.getClass().getName();
            } else if (_currentElem.hasAttribute("class")) {
                className = _currentElem.getAttribute("class");
            }

            try {
                @SuppressWarnings("unchecked")
                final Class<? extends Savable> clazz = (Class<? extends Savable>) Class.forName(className);
                final SavableFactory ann = clazz.getAnnotation(SavableFactory.class);
                if (ann == null) {
                    tmp = clazz.newInstance();
                } else {
                    tmp = (Savable) clazz.getMethod(ann.factoryMethod(), (Class<?>[]) null).invoke(null,
                            (Object[]) null);
                }
            } catch (final InstantiationException e) {
                Logger.getLogger(getClass().getName()).logp(Level.SEVERE, this.getClass().toString(),
                        "readSavableFromCurrentElem(Savable)",
                        "Could not access constructor of class '" + className + "'! \n"
                                + "Some types may require the annotation SavableFactory.  Please double check.");
                throw new Ardor3dException(e);
            } catch (final NoSuchMethodException e) {
                Logger.getLogger(getClass().getName()).logp(Level.SEVERE, this.getClass().toString(),
                        "readSavableFromCurrentElem(Savable)", e.getMessage() + " \n"
                                + "Method specified in annotation does not appear to exist or has an invalid method signature.");
                throw new Ardor3dException(e);
            } catch (final Exception e) {
                Logger.getLogger(getClass().getName()).logp(Level.SEVERE, this.getClass().toString(),
                        "readSavableFromCurrentElem(Savable)", "Exception", e);
                return null;
            }

            final String refID = _currentElem.getAttribute("reference_ID");
            if (refID.length() > 0) {
                _referencedSavables.put(refID, tmp);
            }
            if (tmp != null) {
                tmp.read(this);
                ret = tmp;
            }
        }
        return ret;
    }

    private TextureState readTextureStateFromCurrent() {
        final Element el = _currentElem;
        TextureState ret = null;
        try {
            ret = (TextureState) readSavableFromCurrentElem(null);
            final Savable[] savs = readSavableArray("texture", new Texture[0]);
            for (int i = 0; i < savs.length; i++) {
                Texture t = (Texture) savs[i];
                final TextureKey tKey = t.getTextureKey();
                t = TextureManager.loadFromKey(tKey, null, t);
                ret.setTexture(t, i);
            }
        } catch (final Exception e) {
            Logger.getLogger(DOMInputCapsule.class.getName()).log(Level.SEVERE, null, e);
        }
        _currentElem = el;
        return ret;
    }

    private Savable[] readRenderStateList(final Element fromElement, final Savable[] defVal) {
        Savable[] ret = defVal;
        try {
            final List<RenderState> tmp = new ArrayList<RenderState>();
            _currentElem = findFirstChildElement(fromElement);
            while (_currentElem != null) {
                final Element el = _currentElem;
                RenderState rs = null;
                if (el.getNodeName().equals("com.ardor3d.scene.state.TextureState")) {
                    rs = readTextureStateFromCurrent();
                } else {
                    rs = (RenderState) (readSavableFromCurrentElem(null));
                }
                if (rs != null) {
                    tmp.add(rs);
                }
                _currentElem = findNextSiblingElement(el);
            }
            ret = tmp.toArray(new RenderState[0]);
        } catch (final Exception e) {
            Logger.getLogger(DOMInputCapsule.class.getName()).log(Level.SEVERE, null, e);
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    public <E extends Savable> E[] readSavableArray(final String name, final E[] defVal) throws IOException {
        Savable[] ret = defVal;
        try {
            final Element tmpEl = findChildElement(_currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            if (name.equals("renderStateList")) {
                ret = readRenderStateList(tmpEl, defVal);
            } else {
                final int size = Integer.parseInt(tmpEl.getAttribute("size"));
                final Savable[] tmp = new Savable[size];
                _currentElem = findFirstChildElement(tmpEl);
                for (int i = 0; i < size; i++) {
                    tmp[i] = (readSavableFromCurrentElem(null));
                    if (i == size - 1) {
                        break;
                    }
                    _currentElem = findNextSiblingElement(_currentElem);
                }
                ret = tmp;
            }
            _currentElem = (Element) tmpEl.getParentNode();
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return (E[]) ret;
    }

    @SuppressWarnings("unchecked")
    public <E extends Savable> E[][] readSavableArray2D(final String name, final E[][] defVal) throws IOException {
        Savable[][] ret = defVal;
        try {
            final Element tmpEl = findChildElement(_currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            final int size_outer = Integer.parseInt(tmpEl.getAttribute("size_outer"));
            final int size_inner = Integer.parseInt(tmpEl.getAttribute("size_outer"));

            final Savable[][] tmp = new Savable[size_outer][size_inner];
            _currentElem = findFirstChildElement(tmpEl);
            for (int i = 0; i < size_outer; i++) {
                for (int j = 0; j < size_inner; j++) {
                    tmp[i][j] = (readSavableFromCurrentElem(null));
                    if (i == size_outer - 1 && j == size_inner - 1) {
                        break;
                    }
                    _currentElem = findNextSiblingElement(_currentElem);
                }
            }
            ret = tmp;
            _currentElem = (Element) tmpEl.getParentNode();
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return (E[][]) ret;
    }

    @SuppressWarnings("unchecked")
    public <E extends Savable> List<E> readSavableList(final String name, final List<E> defVal) throws IOException {
        List<E> ret = defVal;
        try {
            final Element tmpEl = findChildElement(_currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            final String s = tmpEl.getAttribute("size");
            final int size = Integer.parseInt(s);
            @SuppressWarnings("rawtypes")
            final List tmp = Lists.newArrayList();
            _currentElem = findFirstChildElement(tmpEl);
            for (int i = 0; i < size; i++) {
                tmp.add(readSavableFromCurrentElem(null));
                if (i == size - 1) {
                    break;
                }
                _currentElem = findNextSiblingElement(_currentElem);
            }
            ret = tmp;
            _currentElem = (Element) tmpEl.getParentNode();
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public <E extends Savable> List<E>[] readSavableListArray(final String name, final List<E>[] defVal)
            throws IOException {
        List<E>[] ret = defVal;
        try {
            final Element tmpEl = findChildElement(_currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }
            _currentElem = tmpEl;

            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final List<E>[] tmp = new ArrayList[size];
            for (int i = 0; i < size; i++) {
                final StringBuilder buf = new StringBuilder("SavableArrayList_");
                buf.append(i);
                final List<E> al = readSavableList(buf.toString(), null);
                tmp[i] = al;
                if (i == size - 1) {
                    break;
                }
            }
            ret = tmp;
            _currentElem = (Element) tmpEl.getParentNode();
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <E extends Savable> List<E>[][] readSavableListArray2D(final String name, final List<E>[][] defVal)
            throws IOException {
        List<E>[][] ret = defVal;
        try {
            final Element tmpEl = findChildElement(_currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }
            _currentElem = tmpEl;
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));

            final List[][] tmp = new ArrayList[size][];
            for (int i = 0; i < size; i++) {
                final List[] arr = readSavableListArray("SavableArrayListArray_" + i, null);
                tmp[i] = arr;
            }
            ret = tmp;
            _currentElem = (Element) tmpEl.getParentNode();
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public List<FloatBuffer> readFloatBufferList(final String name, final List<FloatBuffer> defVal) throws IOException {
        List<FloatBuffer> ret = defVal;
        try {
            final Element tmpEl = findChildElement(_currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final List<FloatBuffer> tmp = new ArrayList<FloatBuffer>(size);
            _currentElem = findFirstChildElement(tmpEl);
            for (int i = 0; i < size; i++) {
                tmp.add(readFloatBuffer(null, null));
                if (i == size - 1) {
                    break;
                }
                _currentElem = findNextSiblingElement(_currentElem);
            }
            ret = tmp;
            _currentElem = (Element) tmpEl.getParentNode();
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public <K extends Savable, V extends Savable> Map<K, V> readSavableMap(final String name, final Map<K, V> defVal)
            throws IOException {
        Map<K, V> ret;
        Element tempEl;

        if (name != null) {
            tempEl = findChildElement(_currentElem, name);
        } else {
            tempEl = _currentElem;
        }
        ret = new HashMap<K, V>();

        final NodeList nodes = tempEl.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node n = nodes.item(i);
            if (n instanceof Element && n.getNodeName().equals("MapEntry")) {
                final Element elem = (Element) n;
                _currentElem = elem;
                final K key = (K) readSavable(XMLExporter.ELEMENT_KEY, null);
                final V val = (V) readSavable(XMLExporter.ELEMENT_VALUE, null);
                ret.put(key, val);
            }
        }
        _currentElem = (Element) tempEl.getParentNode();
        return ret;
    }

    @SuppressWarnings("unchecked")
    public <V extends Savable> Map<String, V> readStringSavableMap(final String name, final Map<String, V> defVal)
            throws IOException {
        Map<String, V> ret = null;
        Element tempEl;

        if (name != null) {
            tempEl = findChildElement(_currentElem, name);
        } else {
            tempEl = _currentElem;
        }
        if (tempEl != null) {
            ret = new HashMap<String, V>();

            final NodeList nodes = tempEl.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node n = nodes.item(i);
                if (n instanceof Element && n.getNodeName().equals("MapEntry")) {
                    final Element elem = (Element) n;
                    _currentElem = elem;
                    final String key = _currentElem.getAttribute("key");
                    final V val = (V) readSavable("Savable", null);
                    ret.put(key, val);
                }
            }
        } else {
            return defVal;
        }
        _currentElem = (Element) tempEl.getParentNode();
        return ret;
    }

    public Map<String, Object> readStringObjectMap(final String name, final Map<String, Object> defVal)
            throws IOException {
        Map<String, Object> ret = null;
        Element tempEl;

        if (name != null) {
            tempEl = findChildElement(_currentElem, name);
        } else {
            tempEl = _currentElem;
        }
        if (tempEl != null) {
            ret = new HashMap<>();

            final NodeList nodes = tempEl.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node n = nodes.item(i);
                if (n instanceof Element && n.getNodeName().equals("MapEntry")) {
                    final Element elem = (Element) n;
                    _currentElem = elem;
                    final String key = _currentElem.getAttribute("key");
                    final Object val = tryToReadValue("value");
                    ret.put(key, val);
                }
            }
        } else {
            return defVal;
        }
        _currentElem = (Element) tempEl.getParentNode();
        return ret;
    }

    private Object tryToReadValue(final String string) throws IOException {
        final byte type = readByte("type", (byte) -1);
        switch (type) {
            case BinaryClassField.BITSET: {
                return readBitSet("value", null);

            }
            case BinaryClassField.BOOLEAN: {
                return readBoolean("value", false);

            }
            case BinaryClassField.BOOLEAN_1D: {
                return readBooleanArray("value", null);

            }
            case BinaryClassField.BOOLEAN_2D: {
                return readBooleanArray2D("value", null);

            }
            case BinaryClassField.BYTE: {
                return readByte("value", (byte) 0);

            }
            case BinaryClassField.BYTE_1D: {
                return readByteArray("value", null);

            }
            case BinaryClassField.BYTE_2D: {
                return readByteArray2D("value", null);

            }
            case BinaryClassField.BYTEBUFFER: {
                return readByteBuffer("value", null);

            }
            case BinaryClassField.DOUBLE: {
                return readDouble("value", 0.0);

            }
            case BinaryClassField.DOUBLE_1D: {
                return readDoubleArray("value", null);

            }
            case BinaryClassField.DOUBLE_2D: {
                return readDoubleArray2D("value", null);

            }
            case BinaryClassField.FLOAT: {
                return readFloat("value", 0f);

            }
            case BinaryClassField.FLOAT_1D: {
                return readFloatArray("value", null);

            }
            case BinaryClassField.FLOAT_2D: {
                return readFloatArray2D("value", null);

            }
            case BinaryClassField.FLOATBUFFER: {
                return readFloatBuffer("value", null);

            }
            case BinaryClassField.FLOATBUFFER_ARRAYLIST: {
                return readFloatBufferList("value", null);

            }
            case BinaryClassField.BYTEBUFFER_ARRAYLIST: {
                return readByteBufferList("value", null);

            }
            case BinaryClassField.INT: {
                return readInt("value", 0);

            }
            case BinaryClassField.INT_1D: {
                return readIntArray("value", null);

            }
            case BinaryClassField.INT_2D: {
                return readIntArray2D("value", null);

            }
            case BinaryClassField.INTBUFFER: {
                return readIntBuffer("value", null);

            }
            case BinaryClassField.LONG: {
                return readLong("value", 0L);

            }
            case BinaryClassField.LONG_1D: {
                return readLongArray("value", null);

            }
            case BinaryClassField.LONG_2D: {
                return readLongArray2D("value", null);

            }
            case BinaryClassField.SAVABLE: {
                return readSavable("value", null);

            }
            case BinaryClassField.SAVABLE_1D: {
                return readSavableArray("value", null);

            }
            case BinaryClassField.SAVABLE_2D: {
                return readSavableArray2D("value", null);

            }
            case BinaryClassField.SAVABLE_ARRAYLIST: {
                return readSavableList("value", null);

            }
            case BinaryClassField.SAVABLE_ARRAYLIST_1D: {
                return readSavableArray("value", null);

            }
            case BinaryClassField.SAVABLE_ARRAYLIST_2D: {
                return readSavableArray2D("value", null);

            }
            case BinaryClassField.SAVABLE_MAP: {
                return readSavableMap("value", null);

            }
            case BinaryClassField.STRING_SAVABLE_MAP: {
                return readStringSavableMap("value", null);

            }
            case BinaryClassField.STRING_OBJECT_MAP: {
                return readStringObjectMap("value", null);

            }
            case BinaryClassField.SHORT: {
                return readShort("value", (short) 0);

            }
            case BinaryClassField.SHORT_1D: {
                return readShortArray("value", null);

            }
            case BinaryClassField.SHORT_2D: {
                return readShortArray2D("value", null);

            }
            case BinaryClassField.SHORTBUFFER: {
                return readShortBuffer("value", null);

            }
            case BinaryClassField.STRING: {
                return readString("value", null);

            }
            case BinaryClassField.STRING_1D: {
                return readStringArray("value", null);

            }
            case BinaryClassField.STRING_2D: {
                return readStringArray2D("value", null);

            }
        }
        return null;
    }

    /**
     * reads from currentElem if name is null
     */
    public FloatBuffer readFloatBuffer(final String name, final FloatBuffer defVal) throws IOException {
        FloatBuffer ret = defVal;
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(_currentElem, name);
            } else {
                tmpEl = _currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final FloatBuffer tmp = BufferUtils.createFloatBuffer(size);
            if (size > 0) {
                final String[] strings = tmpEl.getAttribute("data").split("\\s+");
                for (final String s : strings) {
                    tmp.put(Float.parseFloat(s));
                }
                tmp.flip();
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public IntBuffer readIntBuffer(final String name, final IntBuffer defVal) throws IOException {
        IntBuffer ret = defVal;
        try {
            final Element tmpEl = findChildElement(_currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final IntBuffer tmp = BufferUtils.createIntBuffer(size);
            if (size > 0) {
                final String[] strings = tmpEl.getAttribute("data").split("\\s+");
                for (final String s : strings) {
                    tmp.put(Integer.parseInt(s));
                }
                tmp.flip();
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public ByteBuffer readByteBuffer(final String name, final ByteBuffer defVal) throws IOException {
        ByteBuffer ret = defVal;
        try {
            final Element tmpEl = findChildElement(_currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final ByteBuffer tmp = BufferUtils.createByteBuffer(size);
            if (size > 0) {
                final String[] strings = tmpEl.getAttribute("data").split("\\s+");
                for (final String s : strings) {
                    tmp.put(Byte.valueOf(s));
                }
                tmp.flip();
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public ShortBuffer readShortBuffer(final String name, final ShortBuffer defVal) throws IOException {
        ShortBuffer ret = defVal;
        try {
            final Element tmpEl = findChildElement(_currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final ShortBuffer tmp = BufferUtils.createShortBuffer(size);
            if (size > 0) {
                final String[] strings = tmpEl.getAttribute("data").split("\\s+");
                for (final String s : strings) {
                    tmp.put(Short.valueOf(s));
                }
                tmp.flip();
            }
            ret = tmp;
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public List<ByteBuffer> readByteBufferList(final String name, final List<ByteBuffer> defVal) throws IOException {
        List<ByteBuffer> ret = defVal;
        try {
            final Element tmpEl = findChildElement(_currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            final int size = Integer.parseInt(tmpEl.getAttribute("size"));
            final List<ByteBuffer> tmp = new ArrayList<ByteBuffer>(size);
            _currentElem = findFirstChildElement(tmpEl);
            for (int i = 0; i < size; i++) {
                tmp.add(readByteBuffer(null, null));
                if (i == size - 1) {
                    break;
                }
                _currentElem = findNextSiblingElement(_currentElem);
            }
            ret = tmp;
            _currentElem = (Element) tmpEl.getParentNode();
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    public <T extends Enum<T>> T readEnum(final String name, final Class<T> enumType, final T defVal)
            throws IOException {
        T ret = defVal;
        try {
            final String eVal = _currentElem.getAttribute(name);
            if (eVal != null && eVal.length() > 0) {
                ret = Enum.valueOf(enumType, eVal);
            }
        } catch (final Exception e) {
            final IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T[] readEnumArray(final String name, final Class<T> enumType, final T[] defVal)
            throws IOException {
        final String[] eVals = readStringArray(name, null);
        if (eVals != null) {
            final T[] rVal = (T[]) Array.newInstance(enumType, eVals.length);
            int i = 0;
            for (final String eVal : eVals) {
                rVal[i++] = Enum.valueOf(enumType, eVal);
            }
            return rVal;
        } else {
            return defVal;
        }
    }
}
