/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.EOFException;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

/**
 * These tests are fairly brittle, since they rely on the implementation of BufferedInputStream. It
 * is necessary for the bytes available to always be larger than the buffer size of the buffered
 * input stream for the tests to work. This size is currently 8192, but if it changes, or if the
 * implementation changes, these tests can break.
 */
public class TestLittleEndianDataInput {
  MockInputStream in;
  byte[] array;
  LittleEndianDataInput littleEndien;

  @Before
  public void setup() throws Exception {
    in = new MockInputStream();

    array = new byte[10];

    littleEndien = new LittleEndianDataInput(in);
  }

  @Test
  public void testReadFully1() throws Exception {
    in.addBytesAvailable(11111);

    littleEndien.readFully(array);

    // not caring about whether the bytes were actually copied successfully in this test
  }

  @Test
  public void testReadFully2() throws Exception {
    in.addBytesAvailable(11240);

    littleEndien.readFully(array, 0, 4);

    // not caring about whether the bytes were actually copied successfully in this test
  }

  @Test
  public void testReadFully3() throws Exception {
    array = new byte[30003];

    final Thread testThread = new Thread(() -> {
      try {
        littleEndien.readFully(array);
      } catch (final IOException e) {
        e.printStackTrace();
        fail("ioexception");
      }
    });

    testThread.start();

    in.addBytesAvailable(10000);

    assertTrue("still alive", testThread.isAlive());

    Thread.sleep(6);

    in.addBytesAvailable(10001);
    assertTrue("still alive", testThread.isAlive());

    in.addBytesAvailable(10002); // now the test thread can die

    testThread.join();

    // not caring about whether the bytes were actually copied successfully in this test
  }

  @Test(expected = EOFException.class)
  public void testReadFully4() throws Exception {
    in.setEof(true);

    littleEndien.readFully(array);

    // not caring about whether the bytes were actually copied successfully in this test
  }
}
