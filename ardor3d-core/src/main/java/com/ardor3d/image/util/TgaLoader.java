/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Loads image files in the Targa format. Handles RLE Targa files. Does not handle Targa files in Black-and-White
 * format.
 */
public final class TgaLoader implements ImageLoader {

    // 0 - no image data in file
    public static final int TYPE_NO_IMAGE = 0;

    // 1 - uncompressed, color-mapped image
    public static final int TYPE_COLORMAPPED = 1;

    // 2 - uncompressed, true-color image
    public static final int TYPE_TRUECOLOR = 2;

    // 3 - uncompressed, black and white image
    public static final int TYPE_BLACKANDWHITE = 3;

    // 9 - run-length encoded, color-mapped image
    public static final int TYPE_COLORMAPPED_RLE = 9;

    // 10 - run-length encoded, true-color image
    public static final int TYPE_TRUECOLOR_RLE = 10;

    // 11 - run-length encoded, black and white image
    public static final int TYPE_BLACKANDWHITE_RLE = 11;

    public TgaLoader() {}

    /**
     * Load an image from Targa format.
     * 
     * @param is
     *            the input stream delivering the targa data.
     * @param flip
     *            if true, we will flip the given targa image on the vertical axis.
     * @return the new loaded Image.
     * @throws IOException
     *             if an error occurs during read.
     */
    public Image load(final InputStream is, boolean flip) throws IOException {
        boolean flipH = false;
        // open a stream to the file
        final BufferedInputStream bis = new BufferedInputStream(is, 8192);
        final DataInputStream dis = new DataInputStream(bis);
        boolean createAlpha = false;

        // ---------- Start Reading the TGA header ---------- //
        // length of the image id (1 byte)
        final int idLength = dis.readUnsignedByte();

        // Type of color map (if any) included with the image
        // 0 - no color map data is included
        // 1 - a color map is included
        final int colorMapType = dis.readUnsignedByte();

        // Type of image being read:
        final int imageType = dis.readUnsignedByte();

        // Read Color Map Specification (5 bytes)
        // Index of first color map entry (if we want to use it, uncomment and remove extra read.)
        // short cMapStart = flipEndian(dis.readShort());
        dis.readShort();
        // number of entries in the color map
        final short cMapLength = flipEndian(dis.readShort());
        // number of bits per color map entry
        final int cMapDepth = dis.readUnsignedByte();

        // Read Image Specification (10 bytes)
        // horizontal coordinate of lower left corner of image. (if we want to use it, uncomment and remove extra read.)
        // int xOffset = flipEndian(dis.readShort());
        dis.readShort();
        // vertical coordinate of lower left corner of image. (if we want to use it, uncomment and remove extra read.)
        // int yOffset = flipEndian(dis.readShort());
        dis.readShort();
        // width of image - in pixels
        final int width = flipEndian(dis.readShort());
        // height of image - in pixels
        final int height = flipEndian(dis.readShort());
        // bits per pixel in image.
        final int pixelDepth = dis.readUnsignedByte();
        final int imageDescriptor = dis.readUnsignedByte();
        if ((imageDescriptor & 32) != 0) {
            flip = !flip;
        }
        if ((imageDescriptor & 16) != 0) {
            flipH = !flipH;
        }

        // ---------- Done Reading the TGA header ---------- //

        // Skip image ID
        if (idLength > 0) {
            if (idLength != bis.skip(idLength)) {
                throw new IOException("Unexpected number of bytes in file - too few.");
            }
        }

        ColorMapEntry[] cMapEntries = null;
        if (colorMapType != 0) {
            // read the color map.
            final int bytesInColorMap = (cMapDepth * cMapLength) >> 3;
            final int bitsPerColor = Math.min(cMapDepth / 3, 8);

            final byte[] cMapData = new byte[bytesInColorMap];
            if (-1 == bis.read(cMapData)) {
                throw new EOFException();
            }

            // Only go to the trouble of constructing the color map
            // table if this is declared a color mapped image.
            if (imageType == TYPE_COLORMAPPED || imageType == TYPE_COLORMAPPED_RLE) {
                cMapEntries = new ColorMapEntry[cMapLength];
                final int alphaSize = cMapDepth - (3 * bitsPerColor);
                final float scalar = 255f / (int) (Math.pow(2, bitsPerColor) - 1);
                final float alphaScalar = 255f / (int) (Math.pow(2, alphaSize) - 1);
                for (int i = 0; i < cMapLength; i++) {
                    final ColorMapEntry entry = new ColorMapEntry();
                    final int offset = cMapDepth * i;
                    entry.red = (byte) (int) (getBitsAsByte(cMapData, offset, bitsPerColor) * scalar);
                    entry.green = (byte) (int) (getBitsAsByte(cMapData, offset + bitsPerColor, bitsPerColor) * scalar);
                    entry.blue = (byte) (int) (getBitsAsByte(cMapData, offset + (2 * bitsPerColor), bitsPerColor) * scalar);
                    if (alphaSize <= 0) {
                        entry.alpha = (byte) 255;
                    } else {
                        entry.alpha = (byte) (int) (getBitsAsByte(cMapData, offset + (3 * bitsPerColor), alphaSize) * alphaScalar);
                    }

                    cMapEntries[i] = entry;
                }
            }
        }

        // Allocate image data array
        byte[] rawData = null;
        int dl;
        if ((pixelDepth == 32)) {
            rawData = new byte[width * height * 4];
            dl = 4;
            createAlpha = true;
        } else {
            rawData = new byte[width * height * 3];
            dl = 3;
        }
        int rawDataIndex = 0;

        if (imageType == TYPE_TRUECOLOR) {
            byte red = 0;
            byte green = 0;
            byte blue = 0;
            byte alpha = 0;

            // Faster than doing a 16-or-24-or-32 check on each individual pixel,
            // just make a separate loop for each.
            if (pixelDepth == 16) {
                final byte[] data = new byte[2];
                final float scalar = 255f / 31f;
                for (int i = 0; i <= (height - 1); i++) {
                    if (!flip) {
                        rawDataIndex = (height - 1 - i) * width * dl;
                    }
                    for (int j = 0; j < width; j++) {
                        data[1] = dis.readByte();
                        data[0] = dis.readByte();
                        rawData[rawDataIndex++] = (byte) (int) (getBitsAsByte(data, 1, 5) * scalar);
                        rawData[rawDataIndex++] = (byte) (int) (getBitsAsByte(data, 6, 5) * scalar);
                        rawData[rawDataIndex++] = (byte) (int) (getBitsAsByte(data, 11, 5) * scalar);
                        if (createAlpha) {
                            alpha = getBitsAsByte(data, 0, 1);
                            if (alpha == 1) {
                                alpha = (byte) 255;
                            }
                            rawData[rawDataIndex++] = alpha;
                        }
                    }
                }
            } else if (pixelDepth == 24) {
                for (int i = 0; i <= (height - 1); i++) {
                    if (!flip) {
                        rawDataIndex = (height - 1 - i) * width * dl;
                    }
                    for (int j = 0; j < width; j++) {
                        blue = dis.readByte();
                        green = dis.readByte();
                        red = dis.readByte();
                        rawData[rawDataIndex++] = red;
                        rawData[rawDataIndex++] = green;
                        rawData[rawDataIndex++] = blue;
                        if (createAlpha) {
                            rawData[rawDataIndex++] = (byte) 255;
                        }

                    }
                }
            } else if (pixelDepth == 32) {
                for (int i = 0; i <= (height - 1); i++) {
                    if (!flip) {
                        rawDataIndex = (height - 1 - i) * width * dl;
                    }
                    for (int j = 0; j < width; j++) {
                        blue = dis.readByte();
                        green = dis.readByte();
                        red = dis.readByte();
                        alpha = dis.readByte();
                        rawData[rawDataIndex++] = red;
                        rawData[rawDataIndex++] = green;
                        rawData[rawDataIndex++] = blue;
                        rawData[rawDataIndex++] = alpha;
                    }
                }
            } else {
                throw new Ardor3dException("Unsupported TGA true color depth: " + pixelDepth);
            }
        } else if (imageType == TYPE_TRUECOLOR_RLE) {
            byte red = 0;
            byte green = 0;
            byte blue = 0;
            byte alpha = 0;

            // Faster than doing a 16-or-24-or-32 check on each individual pixel,
            // just make a separate loop for each.
            if (pixelDepth == 32) {
                for (int i = 0; i <= (height - 1); ++i) {
                    if (!flip) {
                        rawDataIndex = (height - 1 - i) * width * dl;
                    }

                    for (int j = 0; j < width; ++j) {
                        // Get the number of pixels the next chunk covers (either packed or unpacked)
                        int count = dis.readByte();
                        if ((count & 0x80) != 0) {
                            // Its an RLE packed block - use the following 1 pixel for the next <count> pixels
                            count &= 0x07f;
                            j += count;
                            blue = dis.readByte();
                            green = dis.readByte();
                            red = dis.readByte();
                            alpha = dis.readByte();
                            while (count-- >= 0) {
                                rawData[rawDataIndex++] = red;
                                rawData[rawDataIndex++] = green;
                                rawData[rawDataIndex++] = blue;
                                rawData[rawDataIndex++] = alpha;
                            }
                        } else {
                            // Its not RLE packed, but the next <count> pixels are raw.
                            j += count;
                            while (count-- >= 0) {
                                blue = dis.readByte();
                                green = dis.readByte();
                                red = dis.readByte();
                                alpha = dis.readByte();
                                rawData[rawDataIndex++] = red;
                                rawData[rawDataIndex++] = green;
                                rawData[rawDataIndex++] = blue;
                                rawData[rawDataIndex++] = alpha;
                            }
                        }
                    }
                }

            } else if (pixelDepth == 24) {
                for (int i = 0; i <= (height - 1); i++) {
                    if (!flip) {
                        rawDataIndex = (height - 1 - i) * width * dl;
                    }
                    for (int j = 0; j < width; ++j) {
                        // Get the number of pixels the next chunk covers (either packed or unpacked)
                        int count = dis.readByte();
                        if ((count & 0x80) != 0) {
                            // Its an RLE packed block - use the following 1 pixel for the next <count> pixels
                            count &= 0x07f;
                            j += count;
                            blue = dis.readByte();
                            green = dis.readByte();
                            red = dis.readByte();
                            while (count-- >= 0) {
                                rawData[rawDataIndex++] = red;
                                rawData[rawDataIndex++] = green;
                                rawData[rawDataIndex++] = blue;
                                if (createAlpha) {
                                    rawData[rawDataIndex++] = (byte) 255;
                                }
                            }
                        } else {
                            // Its not RLE packed, but the next <count> pixels are raw.
                            j += count;
                            while (count-- >= 0) {
                                blue = dis.readByte();
                                green = dis.readByte();
                                red = dis.readByte();
                                rawData[rawDataIndex++] = red;
                                rawData[rawDataIndex++] = green;
                                rawData[rawDataIndex++] = blue;
                                if (createAlpha) {
                                    rawData[rawDataIndex++] = (byte) 255;
                                }
                            }
                        }
                    }
                }

            } else if (pixelDepth == 16) {
                final byte[] data = new byte[2];
                final float scalar = 255f / 31f;
                for (int i = 0; i <= (height - 1); i++) {
                    if (!flip) {
                        rawDataIndex = (height - 1 - i) * width * dl;
                    }
                    for (int j = 0; j < width; j++) {
                        // Get the number of pixels the next chunk covers (either packed or unpacked)
                        int count = dis.readByte();
                        if ((count & 0x80) != 0) {
                            // Its an RLE packed block - use the following 1 pixel for the next <count> pixels
                            count &= 0x07f;
                            j += count;
                            data[1] = dis.readByte();
                            data[0] = dis.readByte();
                            blue = (byte) (int) (getBitsAsByte(data, 1, 5) * scalar);
                            green = (byte) (int) (getBitsAsByte(data, 6, 5) * scalar);
                            red = (byte) (int) (getBitsAsByte(data, 11, 5) * scalar);
                            while (count-- >= 0) {
                                rawData[rawDataIndex++] = red;
                                rawData[rawDataIndex++] = green;
                                rawData[rawDataIndex++] = blue;
                                if (createAlpha) {
                                    rawData[rawDataIndex++] = (byte) 255;
                                }
                            }
                        } else {
                            // Its not RLE packed, but the next <count> pixels are raw.
                            j += count;
                            while (count-- >= 0) {
                                data[1] = dis.readByte();
                                data[0] = dis.readByte();
                                blue = (byte) (int) (getBitsAsByte(data, 1, 5) * scalar);
                                green = (byte) (int) (getBitsAsByte(data, 6, 5) * scalar);
                                red = (byte) (int) (getBitsAsByte(data, 11, 5) * scalar);
                                rawData[rawDataIndex++] = red;
                                rawData[rawDataIndex++] = green;
                                rawData[rawDataIndex++] = blue;
                                if (createAlpha) {
                                    rawData[rawDataIndex++] = (byte) 255;
                                }
                            }
                        }
                    }
                }

            } else {
                throw new Ardor3dException("Unsupported TGA true color depth: " + pixelDepth);
            }

        } else if (imageType == TYPE_COLORMAPPED) {
            final int bytesPerIndex = pixelDepth / 8;

            if (bytesPerIndex == 1) {
                for (int i = 0; i <= (height - 1); i++) {
                    if (!flip) {
                        rawDataIndex = (height - 1 - i) * width * dl;
                    }
                    for (int j = 0; j < width; j++) {
                        final int index = dis.readUnsignedByte();
                        if (index >= cMapEntries.length || index < 0) {
                            throw new Ardor3dException("TGA: Invalid color map entry referenced: " + index);
                        }
                        final ColorMapEntry entry = cMapEntries[index];
                        rawData[rawDataIndex++] = entry.red;
                        rawData[rawDataIndex++] = entry.green;
                        rawData[rawDataIndex++] = entry.blue;
                        if (dl == 4) {
                            rawData[rawDataIndex++] = entry.alpha;
                        }

                    }
                }
            } else if (bytesPerIndex == 2) {
                for (int i = 0; i <= (height - 1); i++) {
                    if (!flip) {
                        rawDataIndex = (height - 1 - i) * width * dl;
                    }
                    for (int j = 0; j < width; j++) {
                        final int index = flipEndian(dis.readShort());
                        if (index >= cMapEntries.length || index < 0) {
                            throw new Ardor3dException("TGA: Invalid color map entry referenced: " + index);
                        }
                        final ColorMapEntry entry = cMapEntries[index];
                        rawData[rawDataIndex++] = entry.red;
                        rawData[rawDataIndex++] = entry.green;
                        rawData[rawDataIndex++] = entry.blue;
                        if (dl == 4) {
                            rawData[rawDataIndex++] = entry.alpha;
                        }
                    }
                }
            } else {
                throw new Ardor3dException("TGA: unknown colormap indexing size used: " + bytesPerIndex);
            }
        }

        // Get a pointer to the image memory
        final ByteBuffer scratch = BufferUtils.createByteBuffer(rawData.length);
        scratch.clear();
        scratch.put(rawData);
        scratch.rewind();
        // Create the ardor3d.image.Image object
        final com.ardor3d.image.Image textureImage = new com.ardor3d.image.Image();
        if (dl == 4) {
            textureImage.setDataFormat(ImageDataFormat.RGBA);
        } else {
            textureImage.setDataFormat(ImageDataFormat.RGB);
        }
        textureImage.setDataType(PixelDataType.UnsignedByte);
        textureImage.setWidth(width);
        textureImage.setHeight(height);
        textureImage.setData(scratch);
        return textureImage;
    }

    private static byte getBitsAsByte(final byte[] data, final int offset, final int length) {
        int offsetBytes = offset / 8;
        int indexBits = offset % 8;
        int rVal = 0;

        // start at data[offsetBytes]... spill into next byte as needed.
        for (int i = length; --i >= 0;) {
            final byte b = data[offsetBytes];
            final int test = indexBits == 7 ? 1 : 2 << (6 - indexBits);
            if ((b & test) != 0) {
                if (i == 0) {
                    rVal++;
                } else {
                    rVal += (2 << i - 1);
                }
            }
            indexBits++;
            if (indexBits == 8) {
                indexBits = 0;
                offsetBytes++;
            }
        }

        return (byte) rVal;
    }

    /**
     * <code>flipEndian</code> is used to flip the endian bit of the header file.
     * 
     * @param signedShort
     *            the bit to flip.
     * @return the flipped bit.
     */
    private static short flipEndian(final short signedShort) {
        final int input = signedShort & 0xFFFF;
        return (short) (input << 8 | (input & 0xFF00) >>> 8);
    }

    private static class ColorMapEntry {
        byte red, green, blue, alpha;

        @Override
        public String toString() {
            return "entry: " + red + "," + green + "," + blue + "," + alpha;
        }
    }
}
