/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>Image</code> defines a data format for a graphical image. The image is defined by a format, a height and width,
 * and the image data. The width and height must be greater than 0. The data is contained in a byte buffer, and should
 * be packed before creation of the image object.
 *
 */
public class Image implements Serializable, Savable {

    private static final long serialVersionUID = 1L;

    // image attributes
    protected ImageDataFormat _format = ImageDataFormat.RGBA;
    protected PixelDataType _type = PixelDataType.UnsignedByte;
    protected int _width, _height, _depth;
    protected int[] _mipMapSizes;
    protected List<ByteBuffer> _data;

    /**
     * Constructor instantiates a new <code>Image</code> object. All values are undefined.
     */
    public Image() {
        _data = new ArrayList<ByteBuffer>(1);
    }

    /**
     * Constructor instantiates a new <code>Image</code> object. The attributes of the image are defined during
     * construction.
     *
     * @param format
     *            the data format of the image. Must not be null.
     * @param type
     *            the data type of the image. Must not be null.
     * @param width
     *            the width of the image.
     * @param height
     *            the height of the image.
     * @param data
     *            the image data. Must not be null.
     * @param mipMapSizes
     *            the array of mipmap sizes, or null for no mipmaps.
     */
    public Image(final ImageDataFormat format, final PixelDataType type, final int width, final int height,
            final List<ByteBuffer> data, int[] mipMapSizes) {

        if (mipMapSizes != null && mipMapSizes.length <= 1) {
            mipMapSizes = null;
        }

        setDataFormat(format);
        setDataType(type);
        setData(data);
        _width = width;
        _height = height;
        _depth = data.size();
        _mipMapSizes = mipMapSizes;
    }

    /**
     * Constructor instantiates a new <code>Image</code> object. The attributes of the image are defined during
     * construction.
     *
     * @param format
     *            the data format of the image. Must not be null.
     * @param type
     *            the data type of the image. Must not be null.
     * @param width
     *            the width of the image.
     * @param height
     *            the height of the image.
     * @param data
     *            the image data. Must not be null.
     * @param mipMapSizes
     *            the array of mipmap sizes, or null for no mipmaps.
     */
    public Image(final ImageDataFormat format, final PixelDataType type, final int width, final int height,
            final ByteBuffer data, final int[] mipMapSizes) {
        this(format, type, width, height, Arrays.asList(data), mipMapSizes);
    }

    /**
     * <code>setData</code> sets the data that makes up the image. This data is packed into an array of
     * <code>ByteBuffer</code> objects.
     *
     * @param data
     *            the data that contains the image information. Must not be null.
     */
    public void setData(final List<ByteBuffer> data) {
        if (data == null) {
            throw new NullPointerException("data may not be null.");
        }
        _data = data;
    }

    /**
     * <code>setData</code> sets the data that makes up the image. This data is packed into a single
     * <code>ByteBuffer</code>.
     *
     * @param data
     *            the data that contains the image information.
     */
    public void setData(final ByteBuffer data) {
        _data = Arrays.asList(data);
    }

    /**
     * Adds the given buffer onto the current list of image data
     *
     * @param data
     *            the data that contains the image information.
     */
    public void addData(final ByteBuffer data) {
        if (_data == null) {
            _data = new ArrayList<ByteBuffer>(1);
        }
        _data.add(data);
    }

    public void setData(final int index, final ByteBuffer data) {
        if (index >= 0) {
            while (_data.size() <= index) {
                _data.add(null);
            }
            _data.set(index, data);
        } else {
            throw new IllegalArgumentException("index must be greater than or equal to 0.");
        }
    }

    /**
     * Sets the mipmap data sizes stored in this image's data buffer. Mipmaps are stored sequentially, and the first
     * mipmap is the main image data. To specify no mipmaps, pass null.
     *
     * @param mipMapSizes
     *            the mipmap sizes array, or null to indicate no mip maps.
     */
    public void setMipMapByteSizes(int[] mipMapSizes) {
        if (mipMapSizes != null && mipMapSizes.length <= 1) {
            mipMapSizes = null;
        }

        _mipMapSizes = mipMapSizes;
    }

    /**
     * <code>setHeight</code> sets the height value of the image. It is typically a good idea to try to keep this as a
     * multiple of 2.
     *
     * @param height
     *            the height of the image.
     */
    public void setHeight(final int height) {
        _height = height;
    }

    /**
     * <code>setDepth</code> sets the depth value of the image. It is typically a good idea to try to keep this as a
     * multiple of 2. This is used for 3d images.
     *
     * @param depth
     *            the depth of the image.
     */
    public void setDepth(final int depth) {
        _depth = depth;
    }

    /**
     * <code>setWidth</code> sets the width value of the image. It is typically a good idea to try to keep this as a
     * multiple of 2.
     *
     * @param width
     *            the width of the image.
     */
    public void setWidth(final int width) {
        _width = width;
    }

    /**
     * @param format
     *            the image data format.
     * @throws NullPointerException
     *             if format is null
     * @see ImageDataFormat
     */
    public void setDataFormat(final ImageDataFormat format) {
        if (format == null) {
            throw new NullPointerException("format may not be null.");
        }

        _format = format;
    }

    /**
     * @return the image data format.
     * @see ImageDataFormat
     */
    public ImageDataFormat getDataFormat() {
        return _format;
    }

    /**
     * @param type
     *            the image data type.
     * @throws NullPointerException
     *             if type is null
     * @see PixelDataType
     */
    public void setDataType(final PixelDataType type) {
        if (type == null) {
            throw new NullPointerException("type may not be null.");
        }

        _type = type;
    }

    /**
     * @return the image data type.
     * @see PixelDataType
     */
    public PixelDataType getDataType() {
        return _type;
    }

    /**
     * @return the width of this image.
     */
    public int getWidth() {
        return _width;
    }

    /**
     * @return the height of this image.
     */
    public int getHeight() {
        return _height;
    }

    /**
     * @return the depth of this image (used for 3d textures and 2d texture arrays)
     */
    public int getDepth() {
        return _depth;
    }

    /**
     * <code>getData</code> returns the data for this image. If the data is undefined, null will be returned.
     *
     * @return the data for this image.
     */
    public List<ByteBuffer> getData() {
        return _data;
    }

    /**
     * @return the number of individual data buffers or slices in this Image.
     */
    public int getDataSize() {
        if (_data == null) {
            return 0;
        } else {
            return _data.size();
        }
    }

    /**
     * <code>getData</code> returns the data for this image at a given index. If the data is undefined, null will be
     * returned.
     *
     * @return the data for this image.
     */
    public ByteBuffer getData(final int index) {
        if (_data.size() > index) {
            return _data.get(index);
        } else {
            return null;
        }
    }

    /**
     * Returns whether the image data contains mipmaps.
     *
     * @return true if the image data contains mipmaps, false if not.
     */
    public boolean hasMipmaps() {
        return _mipMapSizes != null;
    }

    /**
     * Returns the mipmap sizes for this image.
     *
     * @return the mipmap sizes for this image.
     */
    public int[] getMipMapByteSizes() {
        return _mipMapSizes;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Image)) {
            return false;
        }
        final Image that = (Image) other;
        if (getDataFormat() != that.getDataFormat()) {
            return false;
        }
        if (getDataType() != that.getDataType()) {
            return false;
        }
        if (getWidth() != that.getWidth()) {
            return false;
        }
        if (getHeight() != that.getHeight()) {
            return false;
        }
        if (!this.getData().equals(that.getData())) {
            return false;
        }
        if (getMipMapByteSizes() != null && !Arrays.equals(getMipMapByteSizes(), that.getMipMapByteSizes())) {
            return false;
        }
        if (getMipMapByteSizes() == null && that.getMipMapByteSizes() != null) {
            return false;
        }

        return true;
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_format, "dataformat", ImageDataFormat.RGBA);
        capsule.write(_type, "datatype", PixelDataType.UnsignedByte);
        capsule.write(_width, "width", 0);
        capsule.write(_height, "height", 0);
        capsule.write(_depth, "depth", 0);
        capsule.write(_mipMapSizes, "mipMapSizes", null);
        capsule.writeByteBufferList(_data, "data", null);
    }

    public void read(final InputCapsule capsule) throws IOException {
        _format = capsule.readEnum("dataformat", ImageDataFormat.class, ImageDataFormat.RGBA);
        _type = capsule.readEnum("datatype", PixelDataType.class, PixelDataType.UnsignedByte);
        _width = capsule.readInt("width", 0);
        _height = capsule.readInt("height", 0);
        _depth = capsule.readInt("depth", 0);
        _mipMapSizes = capsule.readIntArray("mipMapSizes", null);
        _data = capsule.readByteBufferList("data", null);
    }

    public Class<? extends Image> getClassTag() {
        return this.getClass();
    }
}
