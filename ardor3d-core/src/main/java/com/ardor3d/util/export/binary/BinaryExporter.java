/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export.binary;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import com.ardor3d.math.MathUtils;
import com.ardor3d.util.export.Ardor3dExporter;
import com.ardor3d.util.export.ByteUtils;
import com.ardor3d.util.export.Savable;

/**
 * Exports to the ardor3d Binary Format. Format descriptor: (each numbered item denotes a series of
 * bytes that follows sequentially one after the next.)
 * <p>
 * 1. "number of classes" - four bytes - int value representing the number of entries in the class
 * lookup table.
 * </p>
 * <p>
 * CLASS TABLE: There will be X blocks each consisting of numbers 2 thru 9, where X = the number
 * read in 1.
 * </p>
 * <p>
 * 2. "class alias" - 1...X bytes, where X = ((int) MathUtils.log(aliasCount, 256) + 1) - an alias
 * used when writing object data to match an object to its appropriate object class type.
 * </p>
 * <p>
 * 3. "full class name size" - four bytes - int value representing number of bytes to read in for
 * next field.
 * </p>
 * <p>
 * 4. "full class name" - 1...X bytes representing a String value, where X = the number read in 3.
 * The String is the fully qualified class name of the Savable class, eg
 * "<code>com.ardor3d.math.Vector3</code>"
 * </p>
 * <p>
 * 5. "number of fields" - four bytes - int value representing number of blocks to read in next
 * (numbers 6 - 9), where each block represents a field in this class.
 * </p>
 * <p>
 * 6. "field alias" - 1 byte - the alias used when writing out fields in a class. Because it is a
 * single byte, a single class can not save out more than a total of 256 fields.
 * </p>
 * <p>
 * 7. "field type" - 1 byte - a value representing the type of data a field contains. This value is
 * taken from the static fields of <code>com.ardor3d.util.export.binary.BinaryClassField</code>.
 * </p>
 * <p>
 * 8. "field name size" - 4 bytes - int value representing the size of the next field.
 * </p>
 * <p>
 * 9. "field name" - 1...X bytes representing a String value, where X = the number read in 8. The
 * String is the full String value used when writing the current field.
 * </p>
 * <p>
 * 10. "number of unique objects" - four bytes - int value representing the number of data entries
 * in this file.
 * </p>
 * <p>
 * DATA LOOKUP TABLE: There will be X blocks each consisting of numbers 11 and 12, where X = the
 * number read in 10.
 * </p>
 * <p>
 * 11. "data id" - four bytes - int value identifying a single unique object that was saved in this
 * data file.
 * </p>
 * <p>
 * 12. "data location" - four bytes - int value representing the offset in the object data portion
 * of this file where the object identified in 11 is located.
 * </p>
 * <p>
 * 13. "future use" - four bytes - hardcoded int value 1.
 * </p>
 * <p>
 * 14. "root id" - four bytes - int value identifying the top level object.
 * </p>
 * <p>
 * OBJECT DATA SECTION: There will be X blocks each consisting of numbers 15 thru 19, where X = the
 * number of unique location values named in 12.
 * <p>
 * 15. "class alias" - see 2.
 * </p>
 * <p>
 * 16. "data length" - four bytes - int value representing the length in bytes of data stored in
 * fields 17 and 18 for this object.
 * </p>
 * <p>
 * FIELD ENTRY: There will be X blocks each consisting of numbers 18 and 19
 * </p>
 * <p>
 * 17. "field alias" - see 6.
 * </p>
 * <p>
 * 18. "field data" - 1...X bytes representing the field data. The data length is dependent on the
 * field type and contents.
 * </p>
 */

public class BinaryExporter implements Ardor3dExporter {
  private static final Logger logger = Logger.getLogger(BinaryExporter.class.getName());

  /**
   * The default compression level to use during output. Defaults to Deflater.BEST_COMPRESSION.
   */
  public static int DEFAULT_COMPRESSION = Deflater.BEST_COMPRESSION;

  protected final int _compression;

  protected int _aliasCount = 1;
  protected int _idCount = 1;

  protected final Map<Savable, BinaryIdContentPair> _contentTable = new IdentityHashMap<>();

  protected final Map<Integer, Integer> _locationTable = new HashMap<>();

  // key - class name, value = bco
  protected final Map<String, BinaryClassObject> _classes = new HashMap<>();

  protected final List<Savable> _contentKeys = new ArrayList<>();

  public BinaryExporter() {
    this(DEFAULT_COMPRESSION);
  }

  /**
   * Construct a new exporter, specifying some options.
   *
   * @param compression
   *          the compression type to use. One of the constants from {@link java.util.zip.Deflater}
   */
  public BinaryExporter(final int compression) {
    _compression = compression;
  }

  @Override
  public void save(final Savable object, final OutputStream os) throws IOException {
    try {
      GZIPOutputStream zos = new GZIPOutputStream(os) {
        {
          def.setLevel(_compression);
        }
      };
      final int id = processBinarySavable(object);

      // write out tag table
      int ttbytes = 0;
      final int classNum = _classes.size();
      final int aliasWidth = ((int) MathUtils.log(classNum, 256) + 1); // make all
      // aliases a
      // fixed width
      zos.write(ByteUtils.convertToBytes(classNum));
      for (final String key : _classes.keySet()) {
        final BinaryClassObject bco = _classes.get(key);

        // write alias
        final byte[] aliasBytes = fixClassAlias(bco._alias, aliasWidth);
        zos.write(aliasBytes);
        ttbytes += aliasWidth;

        // write classname size & classname
        final byte[] classBytes = key.getBytes();
        zos.write(ByteUtils.convertToBytes(classBytes.length));
        zos.write(classBytes);
        ttbytes += 4 + classBytes.length;

        zos.write(ByteUtils.convertToBytes(bco._nameFields.size()));

        for (final String fieldName : bco._nameFields.keySet()) {
          final BinaryClassField bcf = bco._nameFields.get(fieldName);
          zos.write(bcf._alias);
          zos.write(bcf._type);

          // write classname size & classname
          final byte[] fNameBytes = fieldName.getBytes();
          zos.write(ByteUtils.convertToBytes(fNameBytes.length));
          zos.write(fNameBytes);
          ttbytes += 2 + 4 + fNameBytes.length;
        }
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      // write out data to a seperate stream
      int location = 0;
      // keep track of location for each piece
      final Map<String, List<BinaryIdContentPair>> alreadySaved = new HashMap<>(_contentTable.size());
      for (final Savable savable : _contentKeys) {
        // look back at previous written data for matches
        final String savableName = savable.getClassTag().getName();
        final BinaryIdContentPair pair = _contentTable.get(savable);
        List<BinaryIdContentPair> bucket = alreadySaved.get(savableName + getChunk(pair));
        final int prevLoc = findPrevMatch(pair, bucket);
        if (prevLoc != -1) {
          _locationTable.put(pair.getId(), prevLoc);
          continue;
        }

        _locationTable.put(pair.getId(), location);
        if (bucket == null) {
          bucket = new ArrayList<>();
          alreadySaved.put(savableName + getChunk(pair), bucket);
        }
        bucket.add(pair);
        final byte[] aliasBytes = fixClassAlias(_classes.get(savableName)._alias, aliasWidth);
        out.write(aliasBytes);
        location += aliasWidth;
        final BinaryOutputCapsule cap = _contentTable.get(savable).getContent();
        out.write(ByteUtils.convertToBytes(cap._bytes.length));
        location += 4; // length of bytes
        out.write(cap._bytes);
        location += cap._bytes.length;
      }

      // write out location table
      // tag/location
      final int locNum = _locationTable.size();
      zos.write(ByteUtils.convertToBytes(locNum));
      int locbytes = 0;
      for (final Integer key : _locationTable.keySet()) {
        zos.write(ByteUtils.convertToBytes(key));
        zos.write(ByteUtils.convertToBytes(_locationTable.get(key)));
        locbytes += 8;
      }

      // write out number of root ids - hardcoded 1 for now
      zos.write(ByteUtils.convertToBytes(1));

      // write out root id
      zos.write(ByteUtils.convertToBytes(id));

      // append stream to the output stream
      out.writeTo(zos);

      zos.finish();

      out = null;
      zos = null;

      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Stats:");
        logger.fine("classes: " + classNum);
        logger.fine("class table: " + ttbytes + " bytes");
        logger.fine("objects: " + locNum);
        logger.fine("location table: " + locbytes + " bytes");
        logger.fine("data: " + location + " bytes");
      }
    } finally {
      _aliasCount = 1;
      _idCount = 1;

      _contentTable.clear();
      _locationTable.clear();
      _classes.clear();
      _contentKeys.clear();
    }
  }

  protected String getChunk(final BinaryIdContentPair pair) {
    return new String(pair.getContent()._bytes, 0, Math.min(64, pair.getContent()._bytes.length));
  }

  protected int findPrevMatch(final BinaryIdContentPair oldPair, final List<BinaryIdContentPair> bucket) {
    if (bucket == null) {
      return -1;
    }
    for (int x = bucket.size(); --x >= 0;) {
      final BinaryIdContentPair pair = bucket.get(x);
      if (pair.getContent().equals(oldPair.getContent())) {
        return _locationTable.get(pair.getId());
      }
    }
    return -1;
  }

  protected byte[] fixClassAlias(final byte[] bytes, final int width) {
    if (bytes.length != width) {
      final byte[] newAlias = new byte[width];
      for (int x = width - bytes.length; x < width; x++) {
        newAlias[x] = bytes[x - bytes.length];
      }
      return newAlias;
    }
    return bytes;
  }

  @Override
  public void save(final Savable object, final File file) throws IOException {
    final File parentDirectory = file.getParentFile();
    if (parentDirectory != null && !parentDirectory.exists()) {
      parentDirectory.mkdirs();
    }

    final FileOutputStream fos = new FileOutputStream(file);
    save(object, fos);
    fos.close();
  }

  public int processBinarySavable(final Savable object) throws IOException {
    if (object == null) {
      return -1;
    }
    BinaryClassObject bco = _classes.get(object.getClassTag().getName());
    // is this class been looked at before? in tagTable?
    if (bco == null) {
      bco = new BinaryClassObject();
      bco._alias = generateTag();
      bco._nameFields = new HashMap<>();
      _classes.put(object.getClassTag().getName(), bco);
    }

    // is object in contentTable?
    if (_contentTable.get(object) != null) {
      return (_contentTable.get(object).getId());
    }
    final BinaryIdContentPair newPair = generateIdContentPair(bco);
    final BinaryIdContentPair old = _contentTable.put(object, newPair);
    if (old == null) {
      _contentKeys.add(object);
    }
    object.write(_contentTable.get(object).getContent());
    newPair.getContent().finish();
    return newPair.getId();

  }

  protected byte[] generateTag() {
    final int width = ((int) MathUtils.log(_aliasCount, 256) + 1);
    int count = _aliasCount;
    _aliasCount++;
    final byte[] bytes = new byte[width];
    for (int x = width - 1; x >= 0; x--) {
      final int pow = (int) Math.pow(256, x);
      final int factor = count / pow;
      bytes[width - x - 1] = (byte) factor;
      count %= pow;
    }
    return bytes;
  }

  protected BinaryIdContentPair generateIdContentPair(final BinaryClassObject bco) {
    final BinaryIdContentPair pair = new BinaryIdContentPair(_idCount++, new BinaryOutputCapsule(this, bco));
    return pair;
  }
}
