/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.loader;

import java.io.IOException;
import java.io.InputStream;

import com.ardor3d.image.Image;

/**
 * Interface for image loaders. Implementing classes can be registered with the TextureManager to
 * decode image formats with a certain file extension.
 *
 * @see com.ardor3d.util.TextureManager#addHandler(String, ImageLoader)
 *
 */
public interface ImageLoader {

  /**
   * Decodes image data from an InputStream.
   * 
   * @param is
   *          The InputStream to create the image from. The inputstream should be closed before this
   *          method returns.
   * @return The decoded Image.
   * @throws IOException
   */
  Image load(InputStream is, boolean flipped) throws IOException;
}
