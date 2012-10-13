/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export.binary;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.math.MathUtils;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.export.Ardor3dImporter;
import com.ardor3d.util.export.ByteUtils;
import com.ardor3d.util.export.ReadListener;
import com.ardor3d.util.export.Savable;
import com.google.common.collect.Maps;

public class BinaryImporter implements Ardor3dImporter {
    private static final Logger logger = Logger.getLogger(BinaryImporter.class.getName());

    // Key - alias, object - bco
    protected final Map<String, BinaryClassObject> _classes = Maps.newHashMap();
    // Key - id, object - the savable
    protected final Map<Integer, Savable> _contentTable = Maps.newHashMap();
    // Key - savable, object - capsule
    protected final Map<Savable, BinaryInputCapsule> _capsuleTable = Maps.newIdentityHashMap();
    // Key - id, opject - location in the file
    protected final Map<Integer, Integer> _locationTable = Maps.newHashMap();

    protected byte[] _dataArray = null;
    protected int _aliasWidth = 0;

    public BinaryImporter() {}

    public Savable load(final InputStream is) throws IOException {
        return load(is, null, null);
    }

    public Savable load(final InputStream is, final ReadListener listener) throws IOException {
        return load(is, listener, null);
    }

    public Savable load(final InputStream is, final ReadListener listener, final ByteArrayOutputStream reuseableStream)
            throws IOException {
        try {
            final GZIPInputStream zis = new GZIPInputStream(is);
            BufferedInputStream bis = new BufferedInputStream(zis);
            final int numClasses = ByteUtils.readInt(bis);
            int bytes = 4;
            _aliasWidth = ((int) MathUtils.log(numClasses, 256) + 1);
            for (int i = 0; i < numClasses; i++) {
                final String alias = readString(bis, _aliasWidth);

                final int classLength = ByteUtils.readInt(bis);
                final String className = readString(bis, classLength);
                final BinaryClassObject bco = new BinaryClassObject();
                bco._alias = alias.getBytes();
                bco._className = className;

                final int fields = ByteUtils.readInt(bis);
                bytes += (8 + _aliasWidth + classLength);

                bco._nameFields = new HashMap<String, BinaryClassField>(fields);
                bco._aliasFields = new HashMap<Byte, BinaryClassField>(fields);
                for (int x = 0; x < fields; x++) {
                    final byte fieldAlias = (byte) bis.read();
                    final byte fieldType = (byte) bis.read();

                    final int fieldNameLength = ByteUtils.readInt(bis);
                    final String fieldName = readString(bis, fieldNameLength);
                    final BinaryClassField bcf = new BinaryClassField(fieldName, fieldAlias, fieldType);
                    bco._nameFields.put(fieldName, bcf);
                    bco._aliasFields.put(fieldAlias, bcf);
                    bytes += (6 + fieldNameLength);
                }
                _classes.put(alias, bco);
            }
            if (listener != null) {
                listener.readBytes(bytes);
            }

            final int numLocs = ByteUtils.readInt(bis);
            bytes = 4;

            for (int i = 0; i < numLocs; i++) {
                final int id = ByteUtils.readInt(bis);
                final int loc = ByteUtils.readInt(bis);
                _locationTable.put(id, loc);
                bytes += 8;
            }

            @SuppressWarnings("unused")
            final int numbIDs = ByteUtils.readInt(bis); // XXX: NOT CURRENTLY USED
            final int id = ByteUtils.readInt(bis);
            bytes += 8;
            if (listener != null) {
                listener.readBytes(bytes);
            }

            ByteArrayOutputStream baos = reuseableStream;
            if (baos == null) {
                baos = new ByteArrayOutputStream(bytes);
            } else {
                baos.reset();
            }
            int size = -1;
            final byte[] cache = new byte[4096];
            while ((size = bis.read(cache)) != -1) {
                baos.write(cache, 0, size);
                if (listener != null) {
                    listener.readBytes(size);
                }
            }
            bis = null;

            _dataArray = baos.toByteArray();
            baos = null;

            final Savable rVal = readObject(id);

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Importer Stats: ");
                logger.fine("Tags: " + numClasses);
                logger.fine("Objects: " + numLocs);
                logger.fine("Data Size: " + _dataArray.length);
            }
            return rVal;

        } finally {
            // Let go of / reset contents.
            _aliasWidth = 0;
            _contentTable.clear();
            _classes.clear();
            _capsuleTable.clear();
            _locationTable.clear();
            _dataArray = null;
        }
    }

    public Savable load(final URL url) throws IOException {
        return load(url, null);
    }

    public Savable load(final URL url, final ReadListener listener) throws IOException {
        final InputStream is = url.openStream();
        final Savable rVal = load(is, listener);
        is.close();
        return rVal;
    }

    public Savable load(final File file) throws IOException {
        return load(file, null);
    }

    public Savable load(final File file, final ReadListener listener) throws IOException {
        final FileInputStream fis = new FileInputStream(file);
        final Savable rVal = load(fis, listener);
        fis.close();
        return rVal;
    }

    public Savable load(final byte[] data) throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(data);
        final Savable rVal = load(bais);
        bais.close();
        return rVal;
    }

    protected String readString(final InputStream is, final int length) throws IOException {
        final byte[] data = new byte[length];
        is.read(data, 0, length);
        return new String(data);
    }

    protected String readString(final int length, final int offset) throws IOException {
        final byte[] data = new byte[length];
        for (int j = 0; j < length; j++) {
            data[j] = _dataArray[j + offset];
        }

        return new String(data);
    }

    public Savable readObject(final int id) {

        if (_contentTable.get(id) != null) {
            return _contentTable.get(id);
        }

        try {
            int loc = _locationTable.get(id);

            final String alias = readString(_aliasWidth, loc);
            loc += _aliasWidth;

            final BinaryClassObject bco = _classes.get(alias);

            if (bco == null) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "NULL class object: "
                        + alias);
                return null;
            }

            final int dataLength = ByteUtils.convertIntFromBytes(_dataArray, loc);
            loc += 4;

            final BinaryInputCapsule cap = new BinaryInputCapsule(this, bco);
            cap.setContent(_dataArray, loc, loc + dataLength);

            final Savable out;

            try {
                @SuppressWarnings("unchecked")
                final Class<? extends Savable> clazz = (Class<? extends Savable>) Class.forName(bco._className);
                final SavableFactory ann = clazz.getAnnotation(SavableFactory.class);
                if (ann == null) {
                    out = clazz.newInstance();
                } else {
                    out = (Savable) clazz.getMethod(ann.factoryMethod(), (Class<?>[]) null).invoke(null,
                            (Object[]) null);
                }
            } catch (final InstantiationException e) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int)",
                        "Could not access constructor of class '" + bco._className + "'! \n"
                                + "Some types may require the annotation SavableFactory.  Please double check.", e);
                throw new Ardor3dException(e);
            } catch (final NoSuchMethodException e) {
                logger
                        .logp(
                                Level.SEVERE,
                                this.getClass().toString(),
                                "readObject(int)",
                                e.getMessage()
                                        + " \n"
                                        + "Method specified in annotation does not appear to exist or has an invalid method signature.",
                                e);
                throw new Ardor3dException(e);
            }

            _capsuleTable.put(out, cap);
            _contentTable.put(id, out);

            out.read(_capsuleTable.get(out));

            _capsuleTable.remove(out);

            return out;

        } catch (final Ardor3dException e) {
            // rethrow our own exceptions
            throw e;
        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int)", "Exception", e);
            throw new Ardor3dException(e);
        }
    }
}
