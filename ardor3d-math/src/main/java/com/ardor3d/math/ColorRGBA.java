/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math;

import java.io.*;

import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.util.EqualsUtil;
import com.ardor3d.math.util.HashUtil;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * ColorRGBA is a 4 component color value (red, green, blue, alpha). The standard range for each
 * individual component is [0f, 1f]. Non-standard use of color (for example HDR rendering) may need
 * to use values outside of this range however, so the value is not clipped or enforced.
 */
public class ColorRGBA implements Cloneable, Savable, Externalizable, ReadOnlyColorRGBA, Poolable {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final ObjectPool<ColorRGBA> COLOR_POOL =
      ObjectPool.create(ColorRGBA.class, MathConstants.maxMathPoolSize);

  /**
   * the color black (0, 0, 0, 1).
   */
  public static final ReadOnlyColorRGBA BLACK = new ColorRGBA(0f, 0f, 0f, 1f);
  /**
   * the color black with a zero alpha value (0, 0, 0, 0).
   */
  public static final ReadOnlyColorRGBA BLACK_NO_ALPHA = new ColorRGBA(0f, 0f, 0f, 0f);
  /**
   * the color white (1, 1, 1, 1).
   */
  public static final ReadOnlyColorRGBA WHITE = new ColorRGBA(1f, 1f, 1f, 1f);
  /**
   * the color gray (.2f, .2f, .2f, 1).
   */
  public static final ReadOnlyColorRGBA DARK_GRAY = new ColorRGBA(0.2f, 0.2f, 0.2f, 1.0f);
  /**
   * the color gray (.5f, .5f, .5f, 1).
   */
  public static final ReadOnlyColorRGBA GRAY = new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f);
  /**
   * the color gray (.8f, .8f, .8f, 1).
   */
  public static final ReadOnlyColorRGBA LIGHT_GRAY = new ColorRGBA(0.8f, 0.8f, 0.8f, 1.0f);
  /**
   * the color red (1, 0, 0, 1).
   */
  public static final ReadOnlyColorRGBA RED = new ColorRGBA(1f, 0f, 0f, 1f);
  /**
   * the color green (0, 1, 0, 1).
   */
  public static final ReadOnlyColorRGBA GREEN = new ColorRGBA(0f, 1f, 0f, 1f);
  /**
   * the color blue (0, 0, 1, 1).
   */
  public static final ReadOnlyColorRGBA BLUE = new ColorRGBA(0f, 0f, 1f, 1f);
  /**
   * the color yellow (1, 1, 0, 1).
   */
  public static final ReadOnlyColorRGBA YELLOW = new ColorRGBA(1f, 1f, 0f, 1f);
  /**
   * the color magenta (1, 0, 1, 1).
   */
  public static final ReadOnlyColorRGBA MAGENTA = new ColorRGBA(1f, 0f, 1f, 1f);
  /**
   * the color cyan (0, 1, 1, 1).
   */
  public static final ReadOnlyColorRGBA CYAN = new ColorRGBA(0f, 1f, 1f, 1f);
  /**
   * the color orange (251/255f, 130/255f, 0, 1).
   */
  public static final ReadOnlyColorRGBA ORANGE = new ColorRGBA(251f / 255f, 130f / 255f, 0f, 1f);
  /**
   * the color brown (65/255f, 40/255f, 25/255f, 1).
   */
  public static final ReadOnlyColorRGBA BROWN = new ColorRGBA(65f / 255f, 40f / 255f, 25f / 255f, 1f);
  /**
   * the color pink (1, 0.68f, 0.68f, 1).
   */
  public static final ReadOnlyColorRGBA PINK = new ColorRGBA(1f, 0.68f, 0.68f, 1f);

  protected float _r = 0;
  protected float _g = 0;
  protected float _b = 0;
  protected float _a = 0;

  /**
   * Constructs a new, mutable color set to (1, 1, 1, 1).
   */
  public ColorRGBA() {
    this(1, 1, 1, 1);
  }

  /**
   * Constructs a new, mutable color set to the (r, g, b, a) values of the provided source color.
   *
   * @param src
   */
  public ColorRGBA(final ReadOnlyColorRGBA src) {
    this(src.getRed(), src.getGreen(), src.getBlue(), src.getAlpha());
  }

  /**
   * Constructs a new color set to (r, g, b, a).
   *
   * @param r
   * @param g
   * @param b
   * @param a
   */
  public ColorRGBA(final float r, final float g, final float b, final float a) {
    _r = r;
    _g = g;
    _b = b;
    _a = a;
  }

  @Override
  public float getRed() { return _r; }

  @Override
  public float getGreen() { return _g; }

  @Override
  public float getBlue() { return _b; }

  @Override
  public float getAlpha() { return _a; }

  /**
   * @param index
   * @return r value if index == 0, g value if index == 1, b value if index == 2 or a value if index
   *         == 3
   * @throws IllegalArgumentException
   *           if index is not one of 0, 1, 2, 3.
   */
  @Override
  public float getValue(final int index) {
    return switch (index) {
      case 0 -> getRed();
      case 1 -> getGreen();
      case 2 -> getBlue();
      case 3 -> getAlpha();
      default -> throw new IllegalArgumentException("index must be either 0, 1, 2 or 3");
    };
  }

  /**
   * @param index
   *          which field index in this color to set.
   * @param value
   *          to set to one of r, g, b or a.
   * @throws IllegalArgumentException
   *           if index is not one of 0, 1, 2, 3.
   */
  public void setValue(final int index, final float value) {
    switch (index) {
      case 0:
        setRed(value);
        return;
      case 1:
        setGreen(value);
        return;
      case 2:
        setBlue(value);
        return;
      case 3:
        setAlpha(value);
        return;
    }
    throw new IllegalArgumentException("index must be either 0, 1, 2 or 3");
  }

  /**
   * Stores the float values of this color in the given float array.
   *
   * @param store
   *          if null, a new float[4] array is created.
   * @return the float array
   * @throws NullPointerException
   *           if store is null.
   * @throws ArrayIndexOutOfBoundsException
   *           if store is not at least length 4.
   */
  @Override
  public float[] toArray(float[] store) {
    if (store == null) {
      store = new float[4];
    }
    // do last first to ensure size is correct before any edits occur.
    store[3] = getAlpha();
    store[2] = getBlue();
    store[1] = getGreen();
    store[0] = getRed();
    return store;
  }

  /**
   * Sets the red component of this color to the given float value.
   *
   * @param r
   *          new red value, generally should be in the range [0.0f, 1.0f]
   */
  public void setRed(final float r) { _r = r; }

  /**
   * Sets the green component of this color to the given float value.
   *
   * @param g
   *          new green value, generally should be in the range [0.0f, 1.0f]
   */
  public void setGreen(final float g) { _g = g; }

  /**
   * Sets the blue component of this color to the given float value.
   *
   * @param b
   *          new blue value, generally should be in the range [0.0f, 1.0f]
   */
  public void setBlue(final float b) { _b = b; }

  /**
   * Sets the alpha component of this color to the given float value. Consider that an alpha of 1.0f
   * means opaque (can not see through) and 0.0f means transparent.
   *
   * @param a
   *          new alpha value, generally should be in the range [0.0f, 1.0f]
   */
  public void setAlpha(final float a) { _a = a; }

  /**
   * Sets the value of this color to (r, g, b, a)
   *
   * @param r
   *          new red value, generally should be in the range [0.0f, 1.0f]
   * @param g
   *          new green value, generally should be in the range [0.0f, 1.0f]
   * @param b
   *          new blue value, generally should be in the range [0.0f, 1.0f]
   * @param a
   *          new alpha value, generally should be in the range [0.0f, 1.0f]
   * @return this color for chaining
   */
  public ColorRGBA set(final float r, final float g, final float b, final float a) {
    setRed(r);
    setGreen(g);
    setBlue(b);
    setAlpha(a);
    return this;
  }

  /**
   * Sets the value of this color to the (r, g, b, a) values of the provided source color.
   *
   * @param source
   * @return this color for chaining
   * @throws NullPointerException
   *           if source is null.
   */
  public ColorRGBA set(final ReadOnlyColorRGBA source) {
    _r = source.getRed();
    _g = source.getGreen();
    _b = source.getBlue();
    _a = source.getAlpha();
    return this;
  }

  /**
   * Sets the value of this color to (0, 0, 0, 0)
   *
   * @return this color for chaining
   */
  public ColorRGBA zero() {
    return set(0, 0, 0, 0);
  }

  /**
   * Brings all values (r,g,b,a) into the range [0.0f, 1.0f]. If a value is above or below this range
   * it is replaced with the appropriate end of the range.
   *
   * @param store
   *          the color to store the result in for return. If null, a new color object is created and
   *          returned.
   */
  @Override
  public ColorRGBA clamp(final ColorRGBA store) {
    ColorRGBA result = store;
    if (result == null) {
      result = new ColorRGBA(this);
    } else if (result != this) {
      result.set(this);
    }

    if (result._r < 0.0f) {
      result._r = 0.0f;
    } else if (result._r > 1.0f) {
      result._r = 1.0f;
    }

    if (result._g < 0.0f) {
      result._g = 0.0f;
    } else if (result._g > 1.0f) {
      result._g = 1.0f;
    }

    if (result._b < 0.0f) {
      result._b = 0.0f;
    } else if (result._b > 1.0f) {
      result._b = 1.0f;
    }

    if (result._a < 0.0f) {
      result._a = 0.0f;
    } else if (result._a > 1.0f) {
      result._a = 1.0f;
    }

    return result;
  }

  /**
   * Brings all values (r,g,b,a) into the range [0.0f, 1.0f]. If a value is above or below this range
   * it is replaced with the appropriate end of the range.
   *
   * @return this color for chaining
   */
  public ColorRGBA clampLocal() {
    return clamp(this);
  }

  /**
   * @param store
   *          the color to store the result in for return. If null, a new color object is created and
   *          returned.
   * @return a random, mutable opaque color.
   */
  public static ColorRGBA randomColor(final ColorRGBA store) {
    ColorRGBA result = store;
    if (result == null) {
      result = new ColorRGBA();
    }

    result._r = MathUtils.nextRandomFloat();
    result._g = MathUtils.nextRandomFloat();
    result._b = MathUtils.nextRandomFloat();
    result._a = 1.0f;
    return result;
  }

  /**
   * @return this color, stored as an integer by converting the values to the range [0, 255] and
   *         combining them as single byte values into a 4 byte int in the order ARGB. Note that this
   *         method expects color values in the [0.0f, 1.0f] range.
   */
  @Override
  public int asIntARGB() {
    final int argb = ((int) (_a * 255) & 0xFF) << 24 | ((int) (_r * 255) & 0xFF) << 16 | ((int) (_g * 255) & 0xFF) << 8
        | (int) (_b * 255) & 0xFF;
    return argb;
  }

  /**
   * @return this color, stored as an integer by converting the values to the range [0, 255] and
   *         combining them as single byte values into a 4 byte int in the order RGBA. Note that this
   *         method expects color values in the [0.0f, 1.0f] range.
   */
  @Override
  public int asIntRGBA() {
    final int rgba = ((int) (_r * 255) & 0xFF) << 24 | ((int) (_g * 255) & 0xFF) << 16 | ((int) (_b * 255) & 0xFF) << 8
        | (int) (_a * 255) & 0xFF;
    return rgba;
  }

  /**
   * Reads a color, packed into a 4 byte int as 1 byte values in the order ARGB. These byte values are
   * normalized to the range [0.0f, 1.0f]
   *
   * @param color
   * @return this color for chaining
   */
  public ColorRGBA fromIntARGB(final int color) {
    _a = ((byte) (color >> 24) & 0xFF) / 255f;
    _r = ((byte) (color >> 16) & 0xFF) / 255f;
    _g = ((byte) (color >> 8) & 0xFF) / 255f;
    _b = ((byte) color & 0xFF) / 255f;
    return this;
  }

  /**
   * Reads a color, packed into a 4 byte int as 1 byte values in the order RGBA. These byte values are
   * normalized to the range [0.0f, 1.0f]
   *
   * @param color
   * @return this color for chaining
   */
  public ColorRGBA fromIntRGBA(final int color) {
    _r = ((byte) (color >> 24) & 0xFF) / 255f;
    _g = ((byte) (color >> 16) & 0xFF) / 255f;
    _b = ((byte) (color >> 8) & 0xFF) / 255f;
    _a = ((byte) color & 0xFF) / 255f;
    return this;
  }

  /**
   * @return this string as a hex value (#RRGGBBAA). e.g. opaque blue is #0000ffff
   */
  @Override
  public String asHexRRGGBBAA() {
    final StringBuilder sb = new StringBuilder("#");
    final String red = Integer.toHexString(Math.round(MathUtils.clamp(getRed(), 0f, 1f) * 255));
    final String green = Integer.toHexString(Math.round(MathUtils.clamp(getGreen(), 0f, 1f) * 255));
    final String blue = Integer.toHexString(Math.round(MathUtils.clamp(getBlue(), 0f, 1f) * 255));
    final String alpha = Integer.toHexString(Math.round(MathUtils.clamp(getAlpha(), 0f, 1f) * 255));
    if (red.length() < 2) {
      sb.append('0');
    }
    sb.append(red);
    if (green.length() < 2) {
      sb.append('0');
    }
    sb.append(green);
    if (blue.length() < 2) {
      sb.append('0');
    }
    sb.append(blue);
    if (alpha.length() < 2) {
      sb.append('0');
    }
    sb.append(alpha);
    return sb.toString();
  }

  /**
   * Adds the given values to those of this color and returns them in store.
   *
   * @param r
   * @param g
   * @param b
   * @param a
   * @param store
   *          the color to store the result in for return. If null, a new color object is created and
   *          returned.
   * @return (this.r + r, this.g + g, this.b + b, this.a + a)
   */
  @Override
  public ColorRGBA add(final float r, final float g, final float b, final float a, final ColorRGBA store) {
    ColorRGBA result = store;
    if (result == null) {
      result = new ColorRGBA();
    }

    return result.set(getRed() + r, getGreen() + g, getBlue() + b, getAlpha() + a);
  }

  /**
   * Increments the values of this color with the given r, g, b and a values.
   *
   * @param r
   * @param g
   * @param b
   * @param a
   * @return this color for chaining
   */
  public ColorRGBA addLocal(final float r, final float g, final float b, final float a) {
    return set(getRed() + r, getGreen() + g, getBlue() + b, getAlpha() + a);
  }

  /**
   * Adds the values of the given source color to those of this color and returns them in store.
   *
   * @param source
   * @param store
   *          the color to store the result in for return. If null, a new color object is created and
   *          returned.
   * @return (this.r + source.r, this.g + source.g, this.b + source.b, this.a + source.a)
   * @throws NullPointerException
   *           if source is null.
   */
  @Override
  public ColorRGBA add(final ReadOnlyColorRGBA source, final ColorRGBA store) {
    return add(source.getRed(), source.getGreen(), source.getBlue(), source.getAlpha(), store);
  }

  /**
   * Increments the values of this color with the r, g, b and a values of the given color.
   *
   * @param source
   * @return this color for chaining
   * @throws NullPointerException
   *           if source is null.
   */
  public ColorRGBA addLocal(final ReadOnlyColorRGBA source) {
    return addLocal(source.getRed(), source.getGreen(), source.getBlue(), source.getAlpha());
  }

  /**
   * Subtracts the given values from those of this color and returns them in store.
   *
   * @param r
   * @param g
   * @param b
   * @param a
   * @param store
   *          the color to store the result in for return. If null, a new color object is created and
   *          returned.
   * @return (this.r - r, this.g - g, this.b - b, this.a - a)
   */
  @Override
  public ColorRGBA subtract(final float r, final float g, final float b, final float a, final ColorRGBA store) {
    ColorRGBA result = store;
    if (result == null) {
      result = new ColorRGBA();
    }

    return result.set(getRed() - r, getGreen() - g, getBlue() - b, getAlpha() - a);
  }

  /**
   * Decrements the values of this color by the given r, g, b and a values.
   *
   * @param r
   * @param g
   * @param b
   * @param a
   * @return this color for chaining
   */
  public ColorRGBA subtractLocal(final float r, final float g, final float b, final float a) {
    return set(getRed() - r, getGreen() - g, getBlue() - b, getAlpha() - a);
  }

  /**
   * Subtracts the values of the given source color from those of this color and returns them in
   * store.
   *
   * @param source
   * @param store
   *          the color to store the result in for return. If null, a new color object is created and
   *          returned.
   * @return (this.r - source.r, this.g - source.g, this.b - source.b, this.a - source.a)
   * @throws NullPointerException
   *           if source is null.
   */
  @Override
  public ColorRGBA subtract(final ReadOnlyColorRGBA source, final ColorRGBA store) {
    return subtract(source.getRed(), source.getGreen(), source.getBlue(), source.getAlpha(), store);
  }

  /**
   * Decrements the values of this color by the r, g, b and a values from the given source color.
   *
   * @param source
   * @return this color for chaining
   * @throws NullPointerException
   *           if source is null.
   */
  public ColorRGBA subtractLocal(final ReadOnlyColorRGBA source) {
    return subtractLocal(source.getRed(), source.getGreen(), source.getBlue(), source.getAlpha());
  }

  /**
   * Multiplies the values of this color by the given scalar value and returns the result in store.
   *
   * @param scalar
   * @param store
   *          the color to store the result in for return. If null, a new color object is created and
   *          returned.
   * @return a new color (this.r * scalar, this.g * scalar, this.b * scalar, this.a * scalar)
   */
  @Override
  public ColorRGBA multiply(final float scalar, final ColorRGBA store) {
    ColorRGBA result = store;
    if (result == null) {
      result = new ColorRGBA();
    }

    return result.set(getRed() * scalar, getGreen() * scalar, getBlue() * scalar, getAlpha() * scalar);
  }

  /**
   * Internally modifies the values of this color by multiplying them each by the given scalar value.
   *
   * @param scalar
   * @return this color for chaining
   *
   *         .
   */
  public ColorRGBA multiplyLocal(final float scalar) {
    return set(getRed() * scalar, getGreen() * scalar, getBlue() * scalar, getAlpha() * scalar);
  }

  /**
   * Multiplies the values of this color by the given scalar value and returns the result in store.
   *
   * @param scale
   * @param store
   *          the color to store the result in for return. If null, a new color object is created and
   *          returned.
   * @return a new color (this.r * scale.r, this.g * scale.g, this.b * scale.b, this.a * scale.a)
   */
  @Override
  public ColorRGBA multiply(final ReadOnlyColorRGBA scale, final ColorRGBA store) {
    ColorRGBA result = store;
    if (result == null) {
      result = new ColorRGBA();
    }

    return result.set(getRed() * scale.getRed(), getGreen() * scale.getGreen(), getBlue() * scale.getBlue(),
        getAlpha() * scale.getAlpha());
  }

  /**
   * Internally modifies the values of this color by multiplying them each by the given scale values.
   *
   * @param scale
   * @return this color for chaining
   */
  public ColorRGBA multiplyLocal(final ReadOnlyColorRGBA scale) {
    return set(getRed() * scale.getRed(), getGreen() * scale.getGreen(), getBlue() * scale.getBlue(),
        getAlpha() * scale.getAlpha());
  }

  /**
   * Divides the values of this color by the given scalar value and returns the result in store.
   *
   * @param scalar
   * @param store
   *          the color to store the result in for return. If null, a new color object is created and
   *          returned.
   * @return a new color (this.r / scalar, this.g / scalar, this.b / scalar, this.a / scalar)
   */
  @Override
  public ColorRGBA divide(final float scalar, final ColorRGBA store) {
    ColorRGBA result = store;
    if (result == null) {
      result = new ColorRGBA();
    }

    return result.set(getRed() / scalar, getGreen() / scalar, getBlue() / scalar, getAlpha() / scalar);
  }

  /**
   * Internally modifies the values of this color by dividing them each by the given scalar value.
   *
   * @param scalar
   * @return this color for chaining
   * @throws ArithmeticException
   *           if scalar is 0
   */
  public ColorRGBA divideLocal(final float scalar) {
    final float invScalar = 1.0f / scalar;

    return set(getRed() * invScalar, getGreen() * invScalar, getBlue() * invScalar, getAlpha() * invScalar);
  }

  /**
   * Divides the values of this color by the given scale values and returns the result in store.
   *
   * @param scale
   * @param store
   *          the color to store the result in for return. If null, a new color object is created and
   *          returned.
   * @return a new color (this.r / scale.r, this.g / scale.g, this.b / scale.b, this.a / scale.a)
   */
  @Override
  public ColorRGBA divide(final ReadOnlyColorRGBA scale, final ColorRGBA store) {
    ColorRGBA result = store;
    if (result == null) {
      result = new ColorRGBA();
    }

    return result.set(getRed() / scale.getRed(), getGreen() / scale.getGreen(), getBlue() / scale.getBlue(),
        getAlpha() / scale.getAlpha());
  }

  /**
   * Internally modifies the values of this color by dividing them each by the given scale values.
   *
   * @param scale
   * @return this color for chaining
   */
  public ColorRGBA divideLocal(final ReadOnlyColorRGBA scale) {
    return set(getRed() / scale.getRed(), getGreen() / scale.getGreen(), getBlue() / scale.getBlue(),
        getAlpha() / scale.getAlpha());
  }

  /**
   * Performs a linear interpolation between this color and the given end color, using the given
   * scalar as a percent. iow, if changeAmnt is closer to 0, the result will be closer to the current
   * value of this color and if it is closer to 1, the result will be closer to the end value.
   *
   * @param endColor
   * @param scalar
   * @param store
   *          the color to store the result in for return. If null, a new color object is created and
   *          returned.
   * @return a new mutable color as described above.
   * @throws NullPointerException
   *           if endVec is null.
   */
  @Override
  public ColorRGBA lerp(final ReadOnlyColorRGBA endColor, final float scalar, final ColorRGBA store) {
    ColorRGBA result = store;
    if (result == null) {
      result = new ColorRGBA();
    }

    final float r = (1.0f - scalar) * getRed() + scalar * endColor.getRed();
    final float g = (1.0f - scalar) * getGreen() + scalar * endColor.getGreen();
    final float b = (1.0f - scalar) * getBlue() + scalar * endColor.getBlue();
    final float a = (1.0f - scalar) * getAlpha() + scalar * endColor.getAlpha();
    return result.set(r, g, b, a);
  }

  /**
   * Performs a linear interpolation between this color and the given end color, using the given
   * scalar as a percent. iow, if changeAmnt is closer to 0, the result will be closer to the current
   * value of this color and if it is closer to 1, the result will be closer to the end value. The
   * result is stored back in this color.
   *
   * @param endColor
   * @param scalar
   * @return this color for chaining
   * @throws NullPointerException
   *           if endVec is null.
   */
  public ColorRGBA lerpLocal(final ReadOnlyColorRGBA endColor, final float scalar) {
    setRed((1.0f - scalar) * getRed() + scalar * endColor.getRed());
    setGreen((1.0f - scalar) * getGreen() + scalar * endColor.getGreen());
    setBlue((1.0f - scalar) * getBlue() + scalar * endColor.getBlue());
    setAlpha((1.0f - scalar) * getAlpha() + scalar * endColor.getAlpha());
    return this;
  }

  /**
   * Performs a linear interpolation between the given begin and end colors, using the given scalar as
   * a percent. iow, if changeAmnt is closer to 0, the result will be closer to the begin value and if
   * it is closer to 1, the result will be closer to the end value.
   *
   * @param beginColor
   * @param endColor
   * @param scalar
   *          the scalar as a percent.
   * @param store
   *          the color to store the result in for return. If null, a new color object is created and
   *          returned.
   * @return a new mutable color as described above.
   * @throws NullPointerException
   *           if beginVec or endVec are null.
   */
  public static ColorRGBA lerp(final ReadOnlyColorRGBA beginColor, final ReadOnlyColorRGBA endColor, final float scalar,
      final ColorRGBA store) {
    ColorRGBA result = store;
    if (result == null) {
      result = new ColorRGBA();
    }

    final float r = (1.0f - scalar) * beginColor.getRed() + scalar * endColor.getRed();
    final float g = (1.0f - scalar) * beginColor.getGreen() + scalar * endColor.getGreen();
    final float b = (1.0f - scalar) * beginColor.getBlue() + scalar * endColor.getBlue();
    final float a = (1.0f - scalar) * beginColor.getAlpha() + scalar * endColor.getAlpha();
    return result.set(r, g, b, a);
  }

  /**
   * Performs a linear interpolation between the given begin and end colors, using the given scalar as
   * a percent. iow, if changeAmnt is closer to 0, the result will be closer to the begin value and if
   * it is closer to 1, the result will be closer to the end value. The result is stored back in this
   * color.
   *
   * @param beginColor
   * @param endColor
   * @param changeAmnt
   *          the scalar as a percent.
   * @return this color for chaining
   * @throws NullPointerException
   *           if beginVec or endVec are null.
   */
  public ColorRGBA lerpLocal(final ReadOnlyColorRGBA beginColor, final ReadOnlyColorRGBA endColor, final float scalar) {
    setRed((1.0f - scalar) * beginColor.getRed() + scalar * endColor.getRed());
    setGreen((1.0f - scalar) * beginColor.getGreen() + scalar * endColor.getGreen());
    setBlue((1.0f - scalar) * beginColor.getBlue() + scalar * endColor.getBlue());
    setAlpha((1.0f - scalar) * beginColor.getAlpha() + scalar * endColor.getAlpha());
    return this;
  }

  /**
   * Check a color... if it is null or its values are NaN or infinite, return false. Else return true.
   *
   * @param color
   *          the color to check
   * @return true or false as stated above.
   * @see java.lang.Float#isFinite(float)
   */
  public static boolean isFinite(final ReadOnlyColorRGBA color) {
    if (color == null) {
      return false;
    }
    return Float.isFinite(color.getRed()) //
        && Float.isFinite(color.getGreen()) //
        && Float.isFinite(color.getBlue()) //
        && Float.isFinite(color.getAlpha());
  }

  /**
   * @return the string representation of this color.
   */
  @Override
  public String toString() {
    return "com.ardor3d.math.ColorRGBA [R=" + getRed() + ", G=" + getGreen() + ", B=" + getBlue() + ", A=" + getAlpha()
        + "]";
  }

  /**
   * @return returns a unique code for this color object based on its values. If two colors are
   *         numerically equal, they will return the same hash code value.
   */
  @Override
  public int hashCode() {
    int result = 17;

    result = HashUtil.hash(result, getRed());
    result = HashUtil.hash(result, getGreen());
    result = HashUtil.hash(result, getBlue());
    result = HashUtil.hash(result, getAlpha());

    return result;
  }

  /**
   * @param o
   *          the object to compare for equality
   * @return true if this color and the provided color have the same r, g, b and a values.
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReadOnlyColorRGBA comp)) {
      return false;
    }
    return EqualsUtil.equals(getRed(), comp.getRed()) //
        && EqualsUtil.equals(getGreen(), comp.getGreen()) //
        && EqualsUtil.equals(getBlue(), comp.getBlue()) //
        && EqualsUtil.equals(getAlpha(), comp.getAlpha());
  }

  // /////////////////
  // Method for Cloneable
  // /////////////////

  @Override
  public ColorRGBA clone() {
    return new ColorRGBA(this);
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends ColorRGBA> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(getRed(), "r", 1);
    capsule.write(getGreen(), "g", 1);
    capsule.write(getBlue(), "b", 1);
    capsule.write(getAlpha(), "a", 1);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    setRed(capsule.readFloat("r", 1));
    setGreen(capsule.readFloat("g", 1));
    setBlue(capsule.readFloat("b", 1));
    setAlpha(capsule.readFloat("a", 1));
  }

  // /////////////////
  // Methods for Externalizable
  // /////////////////

  /**
   * Used with serialization. Not to be called manually.
   *
   * @param in
   *          ObjectInput
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    setRed(in.readFloat());
    setGreen(in.readFloat());
    setBlue(in.readFloat());
    setAlpha(in.readFloat());
  }

  /**
   * Used with serialization. Not to be called manually.
   *
   * @param out
   *          ObjectOutput
   * @throws IOException
   */
  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeFloat(getRed());
    out.writeFloat(getGreen());
    out.writeFloat(getBlue());
    out.writeFloat(getAlpha());
  }

  // /////////////////
  // Methods for creating temp variables (pooling)
  // /////////////////

  /**
   * @return An instance of ColorRGBA that is intended for temporary use in calculations and so forth.
   *         Multiple calls to the method should return instances of this class that are not currently
   *         in use.
   */
  public static ColorRGBA fetchTempInstance() {
    if (MathConstants.useMathPools) {
      return ColorRGBA.COLOR_POOL.fetch();
    } else {
      return new ColorRGBA();
    }
  }

  /**
   * Releases a ColorRGBA back to be used by a future call to fetchTempInstance. TAKE CARE: this
   * ColorRGBA object should no longer have other classes referencing it or "Bad Things" will happen.
   *
   * @param color
   *          the ColorRGBA to release.
   */
  public static void releaseTempInstance(final ColorRGBA color) {
    if (MathConstants.useMathPools) {
      ColorRGBA.COLOR_POOL.release(color);
    }
  }

  /**
   * Parses the given string for a color value. Currently we support hex notation - # followed by 1,
   * 2, 3, 4, 6, or 8 chars 0-9A-F.
   * <ul>
   * <li>chars: pattern - notes</li>
   * <li>1: V - RGB is parsed as val/15, A=1</li>
   * <li>2: VA - RGB is parsed as V/15, A as A/15</li>
   * <li>3: RGB - RGB is parsed as R/15, G/15, B/15, A=1</li>
   * <li>4: RGB - RGBA are parsed as R/15, G/15, B/15, A/15</li>
   * <li>6: RRGGBB - RGB is parsed as RR/255, GG/255, BB/255, A=1</li>
   * <li>8: RRGGBBAA - RGBA is parsed as RR/255, GG/255, BB/255, AA/255</li>
   * </ul>
   *
   * @param colorString
   * @param store
   * @return
   */
  public static ColorRGBA parseColor(final String colorString, final ColorRGBA store) {
    ColorRGBA rVal = store;
    if (rVal == null) {
      rVal = new ColorRGBA();
    }

    // XXX: should we parse words too? eg 'red'...
    if (colorString.length() == 0 || colorString.charAt(0) != '#') {
      throw new IllegalArgumentException("must start with #.");
    }

    float r = 1, g = 1, b = 1, a = 1;
    final int length = colorString.length();
    if (length == 2) {
      r = Integer.parseInt(colorString.substring(1, 2), 16) / 15f;
      g = b = r;
      a = 1;
    } else if (length == 3) {
      r = Integer.parseInt(colorString.substring(1, 2), 16) / 15f;
      g = b = r;
      a = Integer.parseInt(colorString.substring(2, 3), 16) / 15f;
    } else if (length == 4) {
      r = Integer.parseInt(colorString.substring(1, 2), 16) / 15f;
      g = Integer.parseInt(colorString.substring(2, 3), 16) / 15f;
      b = Integer.parseInt(colorString.substring(3, 4), 16) / 15f;
      a = 1;
    } else if (length == 5) {
      r = Integer.parseInt(colorString.substring(1, 2), 16) / 15f;
      g = Integer.parseInt(colorString.substring(2, 3), 16) / 15f;
      b = Integer.parseInt(colorString.substring(3, 4), 16) / 15f;
      a = Integer.parseInt(colorString.substring(4, 5), 16) / 15f;
    } else if (length == 7) {
      r = Integer.parseInt(colorString.substring(1, 3), 16) / 255f;
      g = Integer.parseInt(colorString.substring(3, 5), 16) / 255f;
      b = Integer.parseInt(colorString.substring(5, 7), 16) / 255f;
      a = 1;
    } else if (length == 9) {
      r = Integer.parseInt(colorString.substring(1, 3), 16) / 255f;
      g = Integer.parseInt(colorString.substring(3, 5), 16) / 255f;
      b = Integer.parseInt(colorString.substring(5, 7), 16) / 255f;
      a = Integer.parseInt(colorString.substring(7, 9), 16) / 255f;
    } else {
      throw new IllegalArgumentException("unsupported value, must be 1, 2, 3, 4, 5, 7 or 9 hexvalues: " + colorString);
    }
    rVal.set(r, g, b, a);

    return rVal;
  }
}
