/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image;

public enum TextureStoreFormat {
    /**
     * When used in texture loading, this indicates to convert the image's data properties to its closest
     * TextureStoreFormat <strong>compressed</strong> format.
     */
    GuessCompressedFormat,
    /**
     * When used in texture loading, this indicates to convert the image's data properties to its closest
     * TextureStoreFormat format.
     */
    GuessNoCompressedFormat,
    /**
     * 4 bit alpha only format - usually forced to 8bit by the card
     */
    Alpha4,
    /**
     * 8 bit alpha only format
     */
    Alpha8,
    /**
     * 12 bit alpha only format - often forced to 8bit or 16bit by the card
     */
    Alpha12,
    /**
     * 16 bit alpha only format - older cards will often use 8bit instead.
     */
    Alpha16,
    /**
     * 4 bit luminance only format - usually forced to 8bit by the card
     */
    Luminance4,
    /**
     * 8 bit luminance only format
     */
    Luminance8,
    /**
     * 12 bit luminance only format - often forced to 8bit or 16bit by the card
     */
    Luminance12,
    /**
     * 16 bit luminance only format - older cards will often use 8bit instead.
     */
    Luminance16,
    /**
     * 4 bit luminance, 4 bit alpha format
     */
    Luminance4Alpha4,
    /**
     * 6 bit luminance, 2 bit alpha format
     */
    Luminance6Alpha2,
    /**
     * 8 bit luminance, 8 bit alpha format
     */
    Luminance8Alpha8,
    /**
     * 12 bit luminance, 4 bit alpha format
     */
    Luminance12Alpha4,
    /**
     * 12 bit luminance, 12 bit alpha format
     */
    Luminance12Alpha12,
    /**
     * 16 bit luminance, 16 bit alpha format
     */
    Luminance16Alpha16,
    /**
     * 4 bit intensity only format - usually forced to 8bit by the card
     */
    Intensity4,
    /**
     * 8 bit intensity only format
     */
    Intensity8,
    /**
     * 12 bit intensity only format - often forced to 8bit or 16bit by the card
     */
    Intensity12,
    /**
     * 16 bit intensity only format - older cards will often use 8bit instead.
     */
    Intensity16,
    /**
     * 3 bit red, 3 bit green, 3 bit blue - often forced to 16 bit by the card
     */
    R3G3B2,
    /**
     * 4 bits per red, green and blue
     */
    RGB4,
    /**
     * 5 bits per red, green and blue
     */
    RGB5,
    /**
     * 8 bits per red, green and blue
     */
    RGB8,
    /**
     * 10 bits per red, green and blue - usually falls back to 8 bits on the card
     */
    RGB10,
    /**
     * 12 bits per red, green and blue - usually falls back to 8 bits on the card
     */
    RGB12,
    /**
     * 16 bits per red, green and blue - usually falls back to 8 bits on the card
     */
    RGB16,
    /**
     * 2 bits per red, green, blue and alpha - often forced to RGBA4 by the card
     */
    RGBA2,
    /**
     * 4 bits per red, green, blue and alpha
     */
    RGBA4,
    /**
     * 5 bits per red, green and blue. 1 bit of alpha
     */
    RGB5A1,
    /**
     * 8 bits per red, green, blue and alpha
     */
    RGBA8,
    /**
     * 10 bits per red, green and blue. 2 bits of alpha - often forced to RGBA8 by the card
     */
    RGB10A2,
    /**
     * 12 bits per red, green, blue and alpha - often forced to RGBA8 by the card
     */
    RGBA12,
    /**
     * 16 bits per red, green, blue and alpha - often forced to RGBA8 by the card
     */
    RGBA16,
    /**
     * RGB, potentially compressed and stored by the card.
     */
    CompressedRed,
    /**
     * RGB, potentially compressed and stored by the card.
     */
    CompressedRG,
    /**
     * RGB, potentially compressed and stored by the card.
     */
    CompressedRGB,
    /**
     * RGBA, potentially compressed and stored by the card.
     */
    CompressedRGBA,
    /**
     * Luminance, potentially compressed and stored by the card.
     */
    CompressedLuminance,
    /**
     * LuminanceAlpha, potentially compressed and stored by the card.
     */
    CompressedLuminanceAlpha,
    /**
     * Image data already in DXT1 format.
     */
    NativeDXT1,
    /**
     * Image data already in DXT1 (with Alpha) format.
     */
    NativeDXT1A,
    /**
     * Image data already in DXT3 format.
     */
    NativeDXT3,
    /**
     * Image data already in DXT5 format.
     */
    NativeDXT5,
    /**
     * Image data already in LATC format - Luminance only
     */
    NativeLATC_L,
    /**
     * Image data already in LATC format - Luminance+Alpha
     */
    NativeLATC_LA,
    /**
     * depth component format - let card choose bit size
     */
    Depth,
    /**
     * 16 bit depth component format
     */
    Depth16,
    /**
     * 24 bit depth component format
     */
    Depth24,
    /**
     * 32 bit depth component format - often stored in Depth24 format by the card.
     */
    Depth32,
    /**
     * Floating point depth format.
     */
    Depth32F,
    /**
     * 16 bit float per red, green and blue
     */
    RGB16F,
    /**
     * 32 bit float per red, green and blue
     */
    RGB32F,
    /**
     * 16 bit float per red, green, blue and alpha
     */
    RGBA16F,
    /**
     * 32 bit float per red, green, blue and alpha
     */
    RGBA32F,
    /**
     * 16 bit float, alpha only format
     */
    Alpha16F,
    /**
     * 16 bit float, alpha only format
     */
    Alpha32F,
    /**
     * 16 bit float, luminance only format
     */
    Luminance16F,
    /**
     * 32 bit float, luminance only format
     */
    Luminance32F,
    /**
     * 16 bit float per luminance and alpha
     */
    LuminanceAlpha16F,
    /**
     * 32 bit float per luminance and alpha
     */
    LuminanceAlpha32F,
    /**
     * 16 bit float, intensity only format
     */
    Intensity16F,
    /**
     * 32 bit float, intensity only format
     */
    Intensity32F,
    /**
     * 8 bit, one-component format
     */
    R8,
    /**
     * 8 bit integer, one-component format
     */
    R8I,
    /**
     * 8 bit unsigned integer, one-component format
     */
    R8UI,
    /**
     * 16 bit, one-component format
     */
    R16,
    /**
     * 16 bit integer, one-component format
     */
    R16I,
    /**
     * 16 bit unsigned integer, one-component format
     */
    R16UI,
    /**
     * 16 bit float, one-component format
     */
    R16F,
    /**
     * 32 bit integer, one-component format
     */
    R32I,
    /**
     * 32 bit unsigned integer, one-component format
     */
    R32UI,
    /**
     * 32 bit float, one-component format
     */
    R32F,
    /**
     * 8 bit, two-component format
     */
    RG8,
    /**
     * 8 bit integer, two-component format
     */
    RG8I,
    /**
     * 8 bit unsigned integer, two-component format
     */
    RG8UI,
    /**
     * 16 bit, two-component format
     */
    RG16,
    /**
     * 16 bit integer, two-component format
     */
    RG16I,
    /**
     * 16 bit unsigned integer, two-component format
     */
    RG16UI,
    /**
     * 16 bit float, two-component format
     */
    RG16F,
    /**
     * 32 bit integer, two-component format
     */
    RG32I,
    /**
     * 32 bit unsigned integer, two-component format
     */
    RG32UI,
    /**
     * 32 bit float, two-component format.
     */
    RG32F;

    public boolean isDepthFormat() {
        return this == Depth16 || this == Depth24 || this == Depth32 || this == Depth || this == Depth32F;
    }

    public boolean isCompressed() {
        switch (this) {
            case NativeDXT1:
            case NativeDXT1A:
            case NativeDXT3:
            case NativeDXT5:
            case NativeLATC_L:
            case NativeLATC_LA:
                return true;
            default:
                return false;
        }
    }
}
