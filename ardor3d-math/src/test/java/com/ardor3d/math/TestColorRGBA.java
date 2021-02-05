/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ardor3d.math.util.MathUtils;

public class TestColorRGBA {

  @Test
  public void test() {
    final ColorRGBA clr1 = new ColorRGBA();
    assertTrue(1f == clr1.getRed());
    assertTrue(1f == clr1.getGreen());
    assertTrue(1f == clr1.getBlue());
    assertTrue(1f == clr1.getAlpha());
  }

  @Test
  public void testGetSet() {
    final ColorRGBA clr1 = new ColorRGBA();
    clr1.setRed(0f);
    assertTrue(clr1.getRed() == 0.0f);
    clr1.setRed(Float.POSITIVE_INFINITY);
    assertTrue(clr1.getRed() == Float.POSITIVE_INFINITY);
    clr1.setRed(Float.NEGATIVE_INFINITY);
    assertTrue(clr1.getRed() == Float.NEGATIVE_INFINITY);
    assertTrue(clr1.getValue(0) == Float.NEGATIVE_INFINITY);

    clr1.setGreen(0);
    assertTrue(clr1.getGreen() == 0.0);
    clr1.setGreen(Float.POSITIVE_INFINITY);
    assertTrue(clr1.getGreen() == Float.POSITIVE_INFINITY);
    clr1.setGreen(Float.NEGATIVE_INFINITY);
    assertTrue(clr1.getGreen() == Float.NEGATIVE_INFINITY);
    assertTrue(clr1.getValue(1) == Float.NEGATIVE_INFINITY);

    clr1.setBlue(0);
    assertTrue(clr1.getBlue() == 0.0);
    clr1.setBlue(Float.POSITIVE_INFINITY);
    assertTrue(clr1.getBlue() == Float.POSITIVE_INFINITY);
    clr1.setBlue(Float.NEGATIVE_INFINITY);
    assertTrue(clr1.getBlue() == Float.NEGATIVE_INFINITY);
    assertTrue(clr1.getValue(2) == Float.NEGATIVE_INFINITY);

    clr1.setAlpha(0);
    assertTrue(clr1.getAlpha() == 0.0);
    clr1.setAlpha(Float.POSITIVE_INFINITY);
    assertTrue(clr1.getAlpha() == Float.POSITIVE_INFINITY);
    clr1.setAlpha(Float.NEGATIVE_INFINITY);
    assertTrue(clr1.getAlpha() == Float.NEGATIVE_INFINITY);
    assertTrue(clr1.getValue(3) == Float.NEGATIVE_INFINITY);

    clr1.set((float) Math.PI, (float) Math.PI, (float) Math.PI, (float) Math.PI);
    assertTrue(clr1.getRed() == (float) Math.PI);
    assertTrue(clr1.getGreen() == (float) Math.PI);
    assertTrue(clr1.getBlue() == (float) Math.PI);
    assertTrue(clr1.getAlpha() == (float) Math.PI);

    final ColorRGBA clr2 = new ColorRGBA(ColorRGBA.BLACK);
    clr2.set(clr1);
    assertEquals(clr1, clr2);

    clr1.setValue(0, 1);
    clr1.setValue(1, 1);
    clr1.setValue(2, 1);
    clr1.setValue(3, 1);
    assertEquals(ColorRGBA.WHITE, clr1);

    clr1.zero();
    assertEquals(ColorRGBA.BLACK_NO_ALPHA, clr1);

    // catch a few expected exceptions
    try {
      clr2.getValue(4);
      fail("getValue(4) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      clr2.getValue(-1);
      fail("getValue(-1) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      clr2.setValue(-1, 0);
      fail("setValue(-1, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    try {
      clr2.setValue(4, 0);
      fail("setValue(4, 0) should have thrown IllegalArgumentException.");
    } catch (final IllegalArgumentException e) {}
    // above exceptions shouldn't have altered vec2
    assertEquals(new ColorRGBA((float) Math.PI, (float) Math.PI, (float) Math.PI, (float) Math.PI), clr2);
  }

  @Test
  public void testToArray() {
    final ColorRGBA clr1 = new ColorRGBA(ColorRGBA.DARK_GRAY);

    final float[] farray = clr1.toArray(null);
    final float[] farray2 = clr1.toArray(new float[4]);
    assertNotNull(farray);
    assertNotNull(farray2);
    assertTrue(farray.length == 4);
    assertTrue(farray[0] == .2f);
    assertTrue(farray[1] == .2f);
    assertTrue(farray[2] == .2f);
    assertTrue(farray[3] == 1f);

    try {
      clr1.toArray(new float[1]);
      fail("toFloatArray(d[1]) should have thrown ArrayIndexOutOfBoundsException.");
    } catch (final ArrayIndexOutOfBoundsException e) {}
  }

  @Test
  public void testClamp() {
    final ColorRGBA clr1 = new ColorRGBA(-1, -1, -1, -1);
    final ColorRGBA clr2 = clr1.clamp(new ColorRGBA());
    final ColorRGBA clr3 = clr1.clamp(null);
    assertNotNull(clr2);
    assertNotNull(clr3);
    assertTrue(clr2.getRed() == 0);
    assertTrue(clr2.getGreen() == 0);
    assertTrue(clr2.getBlue() == 0);
    assertTrue(clr2.getAlpha() == 0);

    clr1.set(2, .5f, 1, 0);
    clr1.clamp(clr2);
    assertTrue(clr2.getRed() == 1);
    assertTrue(clr2.getGreen() == .5f);
    assertTrue(clr2.getBlue() == 1);
    assertTrue(clr2.getAlpha() == 0);

    clr1.set(2, 2, 2, 2);
    clr1.clampLocal();
    assertTrue(clr1.getRed() == 1);
    assertTrue(clr1.getGreen() == 1);
    assertTrue(clr1.getBlue() == 1);
    assertTrue(clr1.getAlpha() == 1);

    clr1.set(0.5f, 0.5f, 0.5f, 0.5f);
    assertEquals(clr1, clr1.clamp(null));
  }

  @Test
  public void testRandomColor() {
    final ColorRGBA clr1 = new ColorRGBA(0, 0, 0, 0);
    MathUtils.setRandomSeed(0);
    ColorRGBA.randomColor(clr1);
    assertEquals(new ColorRGBA(0.73096776f, 0.831441f, 0.24053639f, 1.0f), clr1);
    final ColorRGBA clr2 = ColorRGBA.randomColor(null);
    assertEquals(new ColorRGBA(0.6063452f, 0.6374174f, 0.30905056f, 1.0f), clr2);
  }

  @Test
  public void testIntColor() {
    assertTrue(ColorRGBA.BLACK.asIntARGB() == -16777216);
    assertTrue(ColorRGBA.BLACK.asIntRGBA() == 255);
    assertTrue(ColorRGBA.RED.asIntARGB() == -65536);
    assertTrue(ColorRGBA.RED.asIntRGBA() == -16776961);

    assertEquals(ColorRGBA.BLACK, new ColorRGBA().fromIntARGB(-16777216));
    assertEquals(ColorRGBA.BLACK, new ColorRGBA().fromIntRGBA(255));
    assertEquals(ColorRGBA.RED, new ColorRGBA().fromIntARGB(-65536));
    assertEquals(ColorRGBA.RED, new ColorRGBA().fromIntRGBA(-16776961));
  }

  @Test
  public void testHexColor() {
    assertEquals("#00000000", ColorRGBA.BLACK_NO_ALPHA.asHexRRGGBBAA());
    assertEquals("#412819ff", ColorRGBA.BROWN.asHexRRGGBBAA());
    assertEquals("#fb8200ff", ColorRGBA.ORANGE.asHexRRGGBBAA());

    assertEquals(ColorRGBA.BROWN, ColorRGBA.parseColor("#412819ff", new ColorRGBA()));
    assertEquals(ColorRGBA.BLACK_NO_ALPHA, ColorRGBA.parseColor("#00", null));
    assertEquals(ColorRGBA.WHITE, ColorRGBA.parseColor("#F", null));
    assertEquals(ColorRGBA.BLACK, ColorRGBA.parseColor("#0F", null));
    assertEquals(ColorRGBA.BLUE, ColorRGBA.parseColor("#00F", null));
    assertEquals(ColorRGBA.YELLOW, ColorRGBA.parseColor("#FF0F", null));
    assertEquals(ColorRGBA.MAGENTA, ColorRGBA.parseColor("#FF00FF", null));
    assertEquals(ColorRGBA.CYAN, ColorRGBA.parseColor("#00FFFFFF", null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHexFail1() {
    ColorRGBA.parseColor("000", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHexFail2() {
    ColorRGBA.parseColor("#000000000000000000000", null);
  }

  @Test
  public void testClone() {
    final ColorRGBA clr1 = new ColorRGBA();
    final ColorRGBA clr2 = clr1.clone();
    assertEquals(clr1, clr2);
    assertNotSame(clr1, clr2);
  }

  @Test
  public void testSimpleHash() {
    // Just a simple sanity check.
    final ColorRGBA clr1 = new ColorRGBA(1, 0, 0, 1);
    final ColorRGBA clr2 = new ColorRGBA(1, 0, 0, 1);
    final ColorRGBA clr3 = new ColorRGBA(1, 1, 1, 1);

    assertTrue(clr1.hashCode() == clr2.hashCode());
    assertTrue(clr1.hashCode() != clr3.hashCode());
  }

  @Test
  public void testAdd() {
    final ColorRGBA clr1 = new ColorRGBA(ColorRGBA.BLACK_NO_ALPHA);
    final ColorRGBA clr2 = new ColorRGBA(ColorRGBA.WHITE);

    clr1.addLocal(1, 2, 3, 4);
    assertEquals(new ColorRGBA(1, 2, 3, 4), clr1);
    clr1.addLocal(-1, -2, -3, -4);
    assertEquals(ColorRGBA.BLACK_NO_ALPHA, clr1);

    clr1.zero();
    clr1.addLocal(clr2);
    assertEquals(ColorRGBA.WHITE, clr1);

    clr1.zero();
    final ColorRGBA clr3 = clr1.add(clr2, new ColorRGBA());
    assertEquals(ColorRGBA.BLACK_NO_ALPHA, clr1);
    assertEquals(ColorRGBA.WHITE, clr3);

    final ColorRGBA clr4 = clr1.add(0, 0, 0, 1, null);
    assertEquals(ColorRGBA.BLACK_NO_ALPHA, clr1);
    assertEquals(ColorRGBA.BLACK, clr4);
  }

  @Test
  public void testSubtract() {
    final ColorRGBA clr1 = new ColorRGBA(ColorRGBA.BLACK_NO_ALPHA);
    final ColorRGBA clr2 = new ColorRGBA(ColorRGBA.WHITE);

    clr1.subtractLocal(1, 2, 3, 4);
    assertEquals(new ColorRGBA(-1, -2, -3, -4), clr1);
    clr1.subtractLocal(-1, -2, -3, -4);
    assertEquals(ColorRGBA.BLACK_NO_ALPHA, clr1);

    clr1.zero();
    clr1.subtractLocal(clr2);
    assertEquals(new ColorRGBA(-1, -1, -1, -1), clr1);

    clr1.zero();
    final ColorRGBA clr3 = clr1.subtract(clr2, new ColorRGBA());
    assertEquals(ColorRGBA.BLACK_NO_ALPHA, clr1);
    assertEquals(new ColorRGBA(-1, -1, -1, -1), clr3);

    final ColorRGBA clr4 = clr1.subtract(0, 0, 0, 1, null);
    assertEquals(ColorRGBA.BLACK_NO_ALPHA, clr1);
    assertEquals(new ColorRGBA(0, 0, 0, -1), clr4);
  }

  @Test
  public void testMultiply() {
    final ColorRGBA clr1 = new ColorRGBA(1, -1, 2, -2);
    final ColorRGBA clr2 = clr1.multiply(2.0f, null);
    final ColorRGBA clr2B = clr1.multiply(2.0f, new ColorRGBA());
    assertEquals(new ColorRGBA(2.0f, -2.0f, 4.0f, -4.0f), clr2);
    assertEquals(new ColorRGBA(2.0f, -2.0f, 4.0f, -4.0f), clr2B);

    clr2.multiplyLocal(0.5f);
    assertEquals(new ColorRGBA(1.0f, -1.0f, 2.0f, -2.0f), clr2);

    final ColorRGBA clr3 = clr1.multiply(clr2, null);
    final ColorRGBA clr3B = clr1.multiply(clr2, new ColorRGBA());
    assertEquals(new ColorRGBA(1, 1, 4, 4), clr3);
    assertEquals(new ColorRGBA(1, 1, 4, 4), clr3B);

    clr1.multiplyLocal(clr2);
    assertEquals(new ColorRGBA(1, 1, 4, 4), clr1);
  }

  @Test
  public void testDivide() {
    final ColorRGBA clr1 = new ColorRGBA(1, -1, 2, -2);
    final ColorRGBA clr2 = clr1.divide(2.0f, null);
    final ColorRGBA clr2B = clr1.divide(2.0f, new ColorRGBA());
    assertEquals(new ColorRGBA(0.5f, -0.5f, 1.0f, -1.0f), clr2);
    assertEquals(new ColorRGBA(0.5f, -0.5f, 1.0f, -1.0f), clr2B);

    clr2.divideLocal(0.5f);
    assertEquals(new ColorRGBA(1.0f, -1.0f, 2.0f, -2.0f), clr2);

    final ColorRGBA clr3 = clr1.divide(clr2, null);
    final ColorRGBA clr3B = clr1.divide(clr2, new ColorRGBA());
    assertEquals(ColorRGBA.WHITE, clr3);
    assertEquals(ColorRGBA.WHITE, clr3B);

    clr1.divideLocal(clr2);
    assertEquals(ColorRGBA.WHITE, clr1);
  }

  @Test
  public void testLerp() {
    final ColorRGBA clr1 = new ColorRGBA(8, 3, -2, 2);
    final ColorRGBA clr2 = new ColorRGBA(2, 1, 0, -2);
    assertEquals(new ColorRGBA(5, 2, -1, 0), clr1.lerp(clr2, 0.5f, null));
    assertEquals(new ColorRGBA(5, 2, -1, 0), clr1.lerp(clr2, 0.5f, new ColorRGBA()));
    assertEquals(new ColorRGBA(5, 2, -1, 0), ColorRGBA.lerp(clr1, clr2, 0.5f, null));
    assertEquals(new ColorRGBA(5, 2, -1, 0), ColorRGBA.lerp(clr1, clr2, 0.5f, new ColorRGBA()));

    clr1.set(14, 5, 4, 2);
    clr1.lerpLocal(clr2, 0.25f);
    assertEquals(new ColorRGBA(11, 4, 3, 1), clr1);

    clr1.set(15, 7, 6, 8);
    final ColorRGBA clr3 = new ColorRGBA(-1, -1, -1, -1);
    clr3.lerpLocal(clr1, clr2, 0.5f);
    assertEquals(new ColorRGBA(8.5f, 4.0f, 3.0f, 3.0f), clr3);

    // coverage
    assertEquals(clr1.lerp(clr1, .25f, null), clr1);
    assertEquals(clr2.lerpLocal(clr2, .25f), clr2);
    assertEquals(clr2.lerpLocal(clr2, clr2, .25f), clr2);
    assertEquals(ColorRGBA.lerp(clr1, clr1, .25f, null), clr1);
  }

  @Test
  public void testFinite() {
    final ColorRGBA clr1 = new ColorRGBA(0, 0, 0, 0);
    final ColorRGBA clr2A = new ColorRGBA(Float.POSITIVE_INFINITY, 0, 0, 0);
    final ColorRGBA clr2B = new ColorRGBA(0, Float.NEGATIVE_INFINITY, 0, 0);
    final ColorRGBA clr2C = new ColorRGBA(0, 0, Float.POSITIVE_INFINITY, 0);
    final ColorRGBA clr2D = new ColorRGBA(0, 0, 0, Float.POSITIVE_INFINITY);
    final ColorRGBA clr3A = new ColorRGBA(Float.NaN, 0, 0, 0);
    final ColorRGBA clr3B = new ColorRGBA(0, Float.NaN, 0, 0);
    final ColorRGBA clr3C = new ColorRGBA(0, 0, Float.NaN, 0);
    final ColorRGBA clr3D = new ColorRGBA(0, 0, 0, Float.NaN);

    assertTrue(ColorRGBA.isFinite(clr1));
    assertFalse(ColorRGBA.isFinite(clr2A));
    assertFalse(ColorRGBA.isFinite(clr2B));
    assertFalse(ColorRGBA.isFinite(clr2C));
    assertFalse(ColorRGBA.isFinite(clr2D));
    assertFalse(ColorRGBA.isFinite(clr3A));
    assertFalse(ColorRGBA.isFinite(clr3B));
    assertFalse(ColorRGBA.isFinite(clr3C));
    assertFalse(ColorRGBA.isFinite(clr3D));

    clr3C.zero();
    assertTrue(ColorRGBA.isFinite(clr3C));

    assertFalse(ColorRGBA.isFinite(null));

    // couple of equals validity tests
    assertEquals(clr1, clr1);
    assertFalse(clr1.equals(null));

    // throw in a couple pool accesses for coverage
    final ColorRGBA clr6 = ColorRGBA.fetchTempInstance();
    clr6.set(clr1);
    assertEquals(clr1, clr6);
    assertNotSame(clr1, clr6);
    ColorRGBA.releaseTempInstance(clr6);

    // cover more of equals
    clr1.set(0, 1, 2, 3);
    assertFalse(clr1.equals(new ColorRGBA(4, 4, 4, 4)));
    assertFalse(clr1.equals(new ColorRGBA(0, 4, 4, 4)));
    assertFalse(clr1.equals(new ColorRGBA(0, 1, 4, 4)));
    assertFalse(clr1.equals(new ColorRGBA(0, 1, 2, 4)));
    assertTrue(clr1.equals(new ColorRGBA(0, 1, 2, 3)));
  }

}
