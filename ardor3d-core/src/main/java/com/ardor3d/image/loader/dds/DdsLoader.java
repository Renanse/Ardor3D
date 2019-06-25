/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.loader.dds;

import static com.ardor3d.image.loader.dds.DdsUtils.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.loader.ImageLoader;
import com.ardor3d.image.util.ImageUtils;
import com.ardor3d.util.LittleEndianDataInput;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <p>
 * <code>DdsLoader</code> is an image loader that reads in a DirectX DDS file.
 * </p>
 * Supports 2D images, volume images and cubemaps in the following formats:<br>
 * Compressed:<br>
 * <ul>
 * <li>DXT1A</li>
 * <li>DXT3</li>
 * <li>DXT5</li>
 * <li>LATC</li>
 * </ul>
 * Uncompressed:<br>
 * <ul>
 * <li>RGB</li>
 * <li>RGBA</li>
 * <li>Luminance</li>
 * <li>LuminanceAlpha</li>
 * <li>Alpha</li>
 * </ul>
 * Note that Cubemaps must have all 6 faces defined to load properly. FIXME: Needs a software inflater for compressed
 * formats in cases where support is not present? Maybe JSquish?
 */
public class DdsLoader implements ImageLoader {
    private static final Logger logger = Logger.getLogger(DdsLoader.class.getName());

    public Image load(final InputStream is, final boolean flipVertically) throws IOException {
        final LittleEndianDataInput in = new LittleEndianDataInput(is);

        // Read and check magic word...
        final int dwMagic = in.readInt();
        if (dwMagic != getInt("DDS ")) {
            throw new Error("Not a dds file.");
        }
        logger.finest("Reading DDS file.");

        // Create our data store;
        final DdsImageInfo info = new DdsImageInfo();

        info.flipVertically = flipVertically;

        // Read standard dds header
        info.header = DdsHeader.read(in);

        // if applicable, read DX10 header
        info.headerDX10 = info.header.ddpf.dwFourCC == getInt("DX10") ? DdsHeaderDX10.read(in) : null;

        // Create our new image
        final Image image = new Image();
        image.setWidth(info.header.dwWidth);
        image.setHeight(info.header.dwHeight);

        // update depth based on flags / header
        updateDepth(image, info);

        // add our format and image data.
        populateImage(image, info, in);

        // return the loaded image
        return image;
    }

    private static final void updateDepth(final Image image, final DdsImageInfo info) {
        if (isSet(info.header.dwCaps2, DdsHeader.DDSCAPS2_CUBEMAP)) {
            int depth = 0;
            if (isSet(info.header.dwCaps2, DdsHeader.DDSCAPS2_CUBEMAP_POSITIVEX)) {
                depth++;
            }
            if (isSet(info.header.dwCaps2, DdsHeader.DDSCAPS2_CUBEMAP_NEGATIVEX)) {
                depth++;
            }
            if (isSet(info.header.dwCaps2, DdsHeader.DDSCAPS2_CUBEMAP_POSITIVEY)) {
                depth++;
            }
            if (isSet(info.header.dwCaps2, DdsHeader.DDSCAPS2_CUBEMAP_NEGATIVEY)) {
                depth++;
            }
            if (isSet(info.header.dwCaps2, DdsHeader.DDSCAPS2_CUBEMAP_POSITIVEZ)) {
                depth++;
            }
            if (isSet(info.header.dwCaps2, DdsHeader.DDSCAPS2_CUBEMAP_NEGATIVEZ)) {
                depth++;
            }

            if (depth != 6) {
                throw new Error("Cubemaps without all faces defined are not currently supported.");
            }

            image.setDepth(depth);
        } else {
            // make sure we have at least depth of 1.
            image.setDepth(info.header.dwDepth > 0 ? info.header.dwDepth : 1);
        }
    }

    private static final void populateImage(final Image image, final DdsImageInfo info, final LittleEndianDataInput in)
            throws IOException {
        final int flags = info.header.ddpf.dwFlags;

        final boolean compressedFormat = isSet(flags, DdsPixelFormat.DDPF_FOURCC);
        final boolean rgb = isSet(flags, DdsPixelFormat.DDPF_RGB);
        final boolean alphaPixels = isSet(flags, DdsPixelFormat.DDPF_ALPHAPIXELS);
        final boolean lum = isSet(flags, DdsPixelFormat.DDPF_LUMINANCE);
        final boolean alpha = isSet(flags, DdsPixelFormat.DDPF_ALPHA);

        if (compressedFormat) {
            final int fourCC = info.header.ddpf.dwFourCC;
            // DXT1 format
            if (fourCC == getInt("DXT1")) {
                info.bpp = 4;
                // if (isSet(flags, DdsPixelFormat.DDPF_ALPHAPIXELS)) {
                // XXX: many authoring tools do not set alphapixels, so we'll error on the side of alpha
                logger.finest("DDS format: DXT1A");
                image.setDataFormat(ImageDataFormat.PrecompressedDXT1A);
                // } else {
                // logger.finest("DDS format: DXT1");
                // image.setDataFormat(ImageDataFormat.PrecompressedDXT1);
                // }
            }

            // DXT3 format
            else if (fourCC == getInt("DXT3")) {
                logger.finest("DDS format: DXT3");
                info.bpp = 8;
                image.setDataFormat(ImageDataFormat.PrecompressedDXT3);
            }

            // DXT5 format
            else if (fourCC == getInt("DXT5")) {
                logger.finest("DDS format: DXT5");
                info.bpp = 8;
                image.setDataFormat(ImageDataFormat.PrecompressedDXT5);
            }

            // DXT10 info present...
            else if (fourCC == getInt("DX10")) {
                switch (info.headerDX10.dxgiFormat) {
                    case DXGI_FORMAT_BC4_UNORM:
                        logger.finest("DXGI format: BC4_UNORM");
                        info.bpp = 4;
                        image.setDataFormat(ImageDataFormat.PrecompressedLATC_L);
                        break;
                    case DXGI_FORMAT_BC5_UNORM:
                        logger.finest("DXGI format: BC5_UNORM");
                        info.bpp = 8;
                        image.setDataFormat(ImageDataFormat.PrecompressedLATC_LA);
                        break;
                    default:
                        throw new Error("dxgiFormat not supported: " + info.headerDX10.dxgiFormat);
                }
            }

            // DXT2 format - unsupported
            else if (fourCC == getInt("DXT2")) {
                logger.finest("DDS format: DXT2");
                throw new Error("DXT2 is not supported.");
            }

            // DXT4 format - unsupported
            else if (fourCC == getInt("DXT4")) {
                logger.finest("DDS format: DXT4");
                throw new Error("DXT4 is not supported.");
            }

            // Unsupported compressed type.
            else {
                throw new Error("unsupported compressed dds format found (" + fourCC + ")");
            }
        }

        // not a compressed format
        else {
            // TODO: more use of bit masks?
            // TODO: Use bit size instead of hardcoded 8 bytes? (need to also implement in readUncompressed)
            image.setDataType(PixelDataType.UnsignedByte);

            info.bpp = info.header.ddpf.dwRGBBitCount;

            // One of the RGB formats?
            if (rgb) {
                if (alphaPixels) {
                    logger.finest("DDS format: uncompressed rgba");
                    image.setDataFormat(ImageDataFormat.RGBA);
                } else {
                    logger.finest("DDS format: uncompressed rgb ");
                    image.setDataFormat(ImageDataFormat.RGB);
                }
            }

            // A luminance or alpha format
            else if (lum || alphaPixels) {
                if (lum && alphaPixels) {
                    logger.finest("DDS format: uncompressed LumAlpha");
                    image.setDataFormat(ImageDataFormat.RG);
                }

                else if (lum) {
                    logger.finest("DDS format: uncompressed Lum");
                    image.setDataFormat(ImageDataFormat.Red);
                }

                else if (alpha) {
                    logger.finest("DDS format: uncompressed Alpha");
                    image.setDataFormat(ImageDataFormat.Alpha);
                }
            } // end luminance/alpha type

            // Unsupported type.
            else {
                throw new Error("unsupported uncompressed dds format found.");
            }
        }

        info.calcMipmapSizes(compressedFormat);
        image.setMipMapByteSizes(info.mipmapByteSizes);

        // Add up total byte size of single depth layer
        int totalSize = 0;
        for (final int size : info.mipmapByteSizes) {
            totalSize += size;
        }

        // Go through and load in image data
        final List<ByteBuffer> imageData = new ArrayList<>();
        for (int i = 0; i < image.getDepth(); i++) {
            // read in compressed data
            if (compressedFormat) {
                imageData.add(readDXT(in, totalSize, info, image));
            }

            // read in uncompressed data
            else if (rgb || lum || alpha) {
                imageData.add(readUncompressed(in, totalSize, rgb, lum, alpha, alphaPixels, info, image));
            }
        }

        // set on image
        image.setData(imageData);
    }

    static final ByteBuffer readDXT(final LittleEndianDataInput in, final int totalSize, final DdsImageInfo info,
            final Image image) throws IOException {
        int mipWidth = info.header.dwWidth;
        int mipHeight = info.header.dwHeight;

        final ByteBuffer buffer = BufferUtils.createByteBuffer(totalSize);
        for (int mip = 0; mip < info.header.dwMipMapCount; mip++) {
            final byte[] data = new byte[info.mipmapByteSizes[mip]];
            in.readFully(data);
            if (!info.flipVertically) {
                buffer.put(data);
            } else {
                final byte[] flipped = flipDXT(data, mipWidth, mipHeight, image.getDataFormat());
                buffer.put(flipped);

                mipWidth = Math.max(mipWidth / 2, 1);
                mipHeight = Math.max(mipHeight / 2, 1);
            }
        }
        buffer.rewind();
        return buffer;
    }

    private static ByteBuffer readUncompressed(final LittleEndianDataInput in, final int totalSize,
            final boolean useRgb, final boolean useLum, final boolean useAlpha, final boolean useAlphaPixels,
            final DdsImageInfo info, final Image image) throws IOException {
        final int redLumShift = shiftCount(info.header.ddpf.dwRBitMask);
        final int greenShift = shiftCount(info.header.ddpf.dwGBitMask);
        final int blueShift = shiftCount(info.header.ddpf.dwBBitMask);
        final int alphaShift = shiftCount(info.header.ddpf.dwABitMask);

        final int sourcebytesPP = info.header.ddpf.dwRGBBitCount / 8;
        final int targetBytesPP = ImageUtils.getPixelByteSize(image.getDataFormat(), image.getDataType());

        final ByteBuffer dataBuffer = BufferUtils.createByteBuffer(totalSize);

        int mipWidth = info.header.dwWidth;
        int mipHeight = info.header.dwHeight;
        int offset = 0;

        for (int mip = 0; mip < info.header.dwMipMapCount; mip++) {
            for (int y = 0; y < mipHeight; y++) {
                for (int x = 0; x < mipWidth; x++) {
                    final byte[] b = new byte[sourcebytesPP];
                    in.readFully(b);

                    final int i = getInt(b);

                    final byte redLum = (byte) (((i & info.header.ddpf.dwRBitMask) >> redLumShift));
                    final byte green = (byte) (((i & info.header.ddpf.dwGBitMask) >> greenShift));
                    final byte blue = (byte) (((i & info.header.ddpf.dwBBitMask) >> blueShift));
                    final byte alpha = (byte) (((i & info.header.ddpf.dwABitMask) >> alphaShift));

                    if (info.flipVertically) {
                        dataBuffer.position(offset + ((mipHeight - y - 1) * mipWidth + x) * targetBytesPP);
                    }

                    if (useAlpha) {
                        dataBuffer.put(alpha);
                    } else if (useLum) {
                        if (useAlphaPixels) {
                            dataBuffer.put(redLum).put(alpha);
                        } else {
                            dataBuffer.put(redLum);
                        }
                    } else if (useRgb) {
                        if (useAlphaPixels) {
                            dataBuffer.put(redLum).put(green).put(blue).put(alpha);
                        } else {
                            dataBuffer.put(redLum).put(green).put(blue);
                        }
                    }
                }
            }

            offset += mipWidth * mipHeight * targetBytesPP;

            mipWidth = Math.max(mipWidth / 2, 1);
            mipHeight = Math.max(mipHeight / 2, 1);
        }

        return dataBuffer;
    }

    private final static class DdsImageInfo {
        boolean flipVertically;
        int bpp = 0;
        DdsHeader header;
        DdsHeaderDX10 headerDX10;
        int mipmapByteSizes[];

        void calcMipmapSizes(final boolean compressed) {
            int width = header.dwWidth;
            int height = header.dwHeight;
            int size = 0;

            mipmapByteSizes = new int[header.dwMipMapCount];

            for (int i = 0; i < header.dwMipMapCount; i++) {
                if (compressed) {
                    size = ((width + 3) / 4) * ((height + 3) / 4) * bpp * 2;
                } else {
                    size = width * height * bpp / 8;
                }

                mipmapByteSizes[i] = ((size + 3) / 4) * 4;

                width = Math.max(width / 2, 1);
                height = Math.max(height / 2, 1);
            }
        }
    }
}
