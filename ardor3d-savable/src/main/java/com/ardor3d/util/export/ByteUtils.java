/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <code>ByteUtils</code> is a helper class for converting numeric primitives to and from byte
 * representations.
 */
public abstract class ByteUtils {

  /**
   * Takes an InputStream and returns the complete byte content of it
   *
   * @param inputStream
   *          The input stream to read from
   * @return The byte array containing the data from the input stream
   * @throws java.io.IOException
   *           thrown if there is a problem reading from the input stream provided
   */
  public static byte[] getByteContent(final InputStream inputStream) throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(16 * 1024);
    final byte[] buffer = new byte[1024];
    int byteCount = -1;
    byte[] data = null;

    // Read the byte content into the output stream first
    while ((byteCount = inputStream.read(buffer)) > 0) {
      outputStream.write(buffer, 0, byteCount);
    }

    // Set data with byte content from stream
    data = outputStream.toByteArray();

    // Release resources
    outputStream.close();

    return data;
  }

  // ********** byte <> short METHODS **********

  /**
   * Writes a short out to an OutputStream.
   *
   * @param outputStream
   *          The OutputStream the short will be written to
   * @param value
   *          The short to write
   * @throws IOException
   *           Thrown if there is a problem writing to the OutputStream
   */
  public static void writeShort(final OutputStream outputStream, final short value) throws IOException {
    final byte[] byteArray = convertToBytes(value);

    outputStream.write(byteArray);

    return;
  }

  public static byte[] convertToBytes(final short value) {
    final byte[] byteArray = new byte[2];

    byteArray[0] = (byte) (value >> 8);
    byteArray[1] = (byte) value;
    return byteArray;
  }

  /**
   * Read in a short from an InputStream
   *
   * @param inputStream
   *          The InputStream used to read the short
   * @return A short, which is the next 2 bytes converted from the InputStream
   * @throws IOException
   *           Thrown if there is a problem reading from the InputStream
   */
  public static short readShort(final InputStream inputStream) throws IOException {
    final byte[] byteArray = new byte[2];

    // Read in the next 2 bytes
    inputStream.read(byteArray);

    final short number = convertShortFromBytes(byteArray);

    return number;
  }

  public static short convertShortFromBytes(final byte[] byteArray) {
    return convertShortFromBytes(byteArray, 0);
  }

  public static short convertShortFromBytes(final byte[] byteArray, final int offset) {
    // Convert it to a short
    final short number = (short) ((byteArray[offset + 1] & 0xFF) + ((byteArray[offset + 0] & 0xFF) << 8));
    return number;
  }

  // ********** byte <> int METHODS **********

  /**
   * Writes an integer out to an OutputStream.
   *
   * @param outputStream
   *          The OutputStream the integer will be written to
   * @param integer
   *          The integer to write
   * @throws IOException
   *           Thrown if there is a problem writing to the OutputStream
   */
  public static void writeInt(final OutputStream outputStream, final int integer) throws IOException {
    final byte[] byteArray = convertToBytes(integer);

    outputStream.write(byteArray);

    return;
  }

  public static byte[] convertToBytes(final int integer) {
    final byte[] byteArray = new byte[4];

    byteArray[0] = (byte) (integer >> 24);
    byteArray[1] = (byte) (integer >> 16);
    byteArray[2] = (byte) (integer >> 8);
    byteArray[3] = (byte) integer;
    return byteArray;
  }

  /**
   * Read in an integer from an InputStream
   *
   * @param inputStream
   *          The InputStream used to read the integer
   * @return An int, which is the next 4 bytes converted from the InputStream
   * @throws IOException
   *           Thrown if there is a problem reading from the InputStream
   */
  public static int readInt(final InputStream inputStream) throws IOException {
    final byte[] byteArray = new byte[4];

    // Read in the next 4 bytes
    inputStream.read(byteArray);

    final int number = convertIntFromBytes(byteArray);

    return number;
  }

  public static int convertIntFromBytes(final byte[] byteArray) {
    return convertIntFromBytes(byteArray, 0);
  }

  public static int convertIntFromBytes(final byte[] byteArray, final int offset) {
    // Convert it to an int
    final int number = ((byteArray[offset] & 0xFF) << 24) + ((byteArray[offset + 1] & 0xFF) << 16)
        + ((byteArray[offset + 2] & 0xFF) << 8) + (byteArray[offset + 3] & 0xFF);
    return number;
  }

  // ********** byte <> long METHODS **********

  /**
   * Writes a long out to an OutputStream.
   *
   * @param outputStream
   *          The OutputStream the long will be written to
   * @param value
   *          The long to write
   * @throws IOException
   *           Thrown if there is a problem writing to the OutputStream
   */
  public static void writeLong(final OutputStream outputStream, final long value) throws IOException {
    final byte[] byteArray = convertToBytes(value);

    outputStream.write(byteArray);

    return;
  }

  public static byte[] convertToBytes(long n) {
    final byte[] bytes = new byte[8];

    bytes[7] = (byte) (n);
    n >>>= 8;
    bytes[6] = (byte) (n);
    n >>>= 8;
    bytes[5] = (byte) (n);
    n >>>= 8;
    bytes[4] = (byte) (n);
    n >>>= 8;
    bytes[3] = (byte) (n);
    n >>>= 8;
    bytes[2] = (byte) (n);
    n >>>= 8;
    bytes[1] = (byte) (n);
    n >>>= 8;
    bytes[0] = (byte) (n);

    return bytes;
  }

  /**
   * Read in a long from an InputStream
   *
   * @param inputStream
   *          The InputStream used to read the long
   * @return A long, which is the next 8 bytes converted from the InputStream
   * @throws IOException
   *           Thrown if there is a problem reading from the InputStream
   */
  public static long readLong(final InputStream inputStream) throws IOException {
    final byte[] byteArray = new byte[8];

    // Read in the next 8 bytes
    inputStream.read(byteArray);

    final long number = convertLongFromBytes(byteArray);

    return number;
  }

  public static long convertLongFromBytes(final byte[] bytes) {
    return convertLongFromBytes(bytes, 0);
  }

  public static long convertLongFromBytes(final byte[] bytes, final int offset) {
    // Convert it to an long
    return ((((long) bytes[offset + 7]) & 0xFF) + ((((long) bytes[offset + 6]) & 0xFF) << 8)
        + ((((long) bytes[offset + 5]) & 0xFF) << 16) + ((((long) bytes[offset + 4]) & 0xFF) << 24)
        + ((((long) bytes[offset + 3]) & 0xFF) << 32) + ((((long) bytes[offset + 2]) & 0xFF) << 40)
        + ((((long) bytes[offset + 1]) & 0xFF) << 48) + ((((long) bytes[offset + 0]) & 0xFF) << 56));
  }

  // ********** byte <> double METHODS **********

  /**
   * Writes a double out to an OutputStream.
   *
   * @param outputStream
   *          The OutputStream the double will be written to
   * @param value
   *          The double to write
   * @throws IOException
   *           Thrown if there is a problem writing to the OutputStream
   */
  public static void writeDouble(final OutputStream outputStream, final double value) throws IOException {
    final byte[] byteArray = convertToBytes(value);

    outputStream.write(byteArray);

    return;
  }

  public static byte[] convertToBytes(final double n) {
    final long bits = Double.doubleToLongBits(n);
    return convertToBytes(bits);
  }

  /**
   * Read in a double from an InputStream
   *
   * @param inputStream
   *          The InputStream used to read the double
   * @return A double, which is the next 8 bytes converted from the InputStream
   * @throws IOException
   *           Thrown if there is a problem reading from the InputStream
   */
  public static double readDouble(final InputStream inputStream) throws IOException {
    final byte[] byteArray = new byte[8];

    // Read in the next 8 bytes
    inputStream.read(byteArray);

    final double number = convertDoubleFromBytes(byteArray);

    return number;
  }

  public static double convertDoubleFromBytes(final byte[] bytes) {
    return convertDoubleFromBytes(bytes, 0);
  }

  public static double convertDoubleFromBytes(final byte[] bytes, final int offset) {
    // Convert it to a double
    final long bits = convertLongFromBytes(bytes, offset);
    return Double.longBitsToDouble(bits);
  }

  // ********** byte <> float METHODS **********

  /**
   * Writes an float out to an OutputStream.
   *
   * @param outputStream
   *          The OutputStream the float will be written to
   * @param fVal
   *          The float to write
   * @throws IOException
   *           Thrown if there is a problem writing to the OutputStream
   */
  public static void writeFloat(final OutputStream outputStream, final float fVal) throws IOException {
    final byte[] byteArray = convertToBytes(fVal);

    outputStream.write(byteArray);

    return;
  }

  public static byte[] convertToBytes(final float f) {
    final int temp = Float.floatToIntBits(f);
    return convertToBytes(temp);
  }

  /**
   * Read in a float from an InputStream
   *
   * @param inputStream
   *          The InputStream used to read the float
   * @return A float, which is the next 4 bytes converted from the InputStream
   * @throws IOException
   *           Thrown if there is a problem reading from the InputStream
   */
  public static float readFloat(final InputStream inputStream) throws IOException {
    final byte[] byteArray = new byte[4];

    // Read in the next 4 bytes
    inputStream.read(byteArray);

    final float number = convertFloatFromBytes(byteArray);

    return number;
  }

  public static float convertFloatFromBytes(final byte[] byteArray) {
    return convertFloatFromBytes(byteArray, 0);
  }

  public static float convertFloatFromBytes(final byte[] byteArray, final int offset) {
    // Convert it to an int
    final int number = convertIntFromBytes(byteArray, offset);
    return Float.intBitsToFloat(number);
  }

  // ********** byte <> boolean METHODS **********

  /**
   * Writes a boolean out to an OutputStream.
   *
   * @param outputStream
   *          The OutputStream the boolean will be written to
   * @param bVal
   *          The boolean to write
   * @throws IOException
   *           Thrown if there is a problem writing to the OutputStream
   */
  public static void writeBoolean(final OutputStream outputStream, final boolean bVal) throws IOException {
    final byte[] byteArray = convertToBytes(bVal);

    outputStream.write(byteArray);

    return;
  }

  public static byte[] convertToBytes(final boolean b) {
    final byte[] rVal = new byte[1];
    rVal[0] = b ? (byte) 1 : (byte) 0;
    return rVal;
  }

  /**
   * Read in a boolean from an InputStream
   *
   * @param inputStream
   *          The InputStream used to read the boolean
   * @return A boolean, which is the next byte converted from the InputStream (iow, byte != 0)
   * @throws IOException
   *           Thrown if there is a problem reading from the InputStream
   */
  public static boolean readBoolean(final InputStream inputStream) throws IOException {
    final byte[] byteArray = new byte[1];

    // Read in the next byte
    inputStream.read(byteArray);

    return convertBooleanFromBytes(byteArray);
  }

  public static boolean convertBooleanFromBytes(final byte[] byteArray) {
    return convertBooleanFromBytes(byteArray, 0);
  }

  public static boolean convertBooleanFromBytes(final byte[] byteArray, final int offset) {
    return byteArray[offset] != 0;
  }

  public static byte[] rightAlignBytes(final byte[] bytes, final int width) {
    if (bytes.length != width) {
      final byte[] rVal = new byte[width];
      for (int x = width - bytes.length; x < width; x++) {
        rVal[x] = bytes[x - (width - bytes.length)];
      }
      return rVal;
    }

    return bytes;
  }

}
