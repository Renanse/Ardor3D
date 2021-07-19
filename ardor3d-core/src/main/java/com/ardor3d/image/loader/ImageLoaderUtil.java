/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.image.Image;
import com.ardor3d.image.loader.dds.DdsLoader;
import com.ardor3d.image.loader.hdr.HdrLoader;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.util.resource.ResourceSource;

public abstract class ImageLoaderUtil {
  private static final Logger logger = Logger.getLogger(ImageLoaderUtil.class.getName());

  private static ImageLoader defaultLoader;
  private static Map<String, ImageLoader> loaders = Collections.synchronizedMap(new HashMap<String, ImageLoader>());

  static {
    registerHandler(new DdsLoader(), ".DDS"); // dxt image
    registerHandler(new HdrLoader(), ".HDR"); // hdr radiance image
    registerHandler(new TgaLoader(), ".TGA"); // targa image
    registerHandler(new AbiLoader(), ".ABI"); // ardor3d binary image
  }

  public static Image loadImage(final ResourceSource src, final boolean flipped) {
    if (src == null) {
      ImageLoaderUtil.logger.warning("loadImage(ResourceSource, boolean): file is null, defaultTexture used.");
      return TextureState.getDefaultTextureImage();
    }

    final String type = src.getType();
    if (type == null) {
      ImageLoaderUtil.logger.warning("loadImage(ResourceSource, boolean): type is null, defaultTexture used.");
      return TextureState.getDefaultTextureImage();
    }

    try (InputStream is = src.openStream()) {
      ImageLoaderUtil.logger.log(Level.FINER, "loadImage(ResourceSource, boolean) opened stream: {0}", src);
      return loadImage(type, is, flipped);
    } catch (final IOException e) {
      ImageLoaderUtil.logger.log(Level.WARNING, "loadImage(ResourceSource, boolean): defaultTexture used", e);
      return TextureState.getDefaultTextureImage();
    }
  }

  public static Image loadImage(final String type, final InputStream stream, final boolean flipped) {

    Image imageData = null;
    try {
      ImageLoader loader = ImageLoaderUtil.loaders.get(type.toLowerCase());
      if (loader == null) {
        loader = ImageLoaderUtil.defaultLoader;
      }
      if (loader != null) {
        imageData = loader.load(stream, flipped);
      } else {
        ImageLoaderUtil.logger.log(Level.WARNING, "Unable to read image of type: {0}", type);
      }
      if (imageData == null) {
        ImageLoaderUtil.logger
            .warning("loadImage(String, InputStream, boolean): no imageData found.  defaultTexture used.");
        imageData = TextureState.getDefaultTextureImage();
      }
    } catch (final IOException e) {
      ImageLoaderUtil.logger.log(Level.WARNING, "Could not load Image.", e);
      imageData = TextureState.getDefaultTextureImage();
    }
    return imageData;
  }

  /**
   * Register an ImageLoader to handle all files with a specific type. An ImageLoader can be
   * registered to handle several formats without problems.
   *
   * @param handler
   *          the handler to use
   * @param types
   *          The type or types for the format this ImageLoader will handle. This value is case
   *          insensitive. Examples include ".jpeg", ".gif", ".dds", etc.
   */
  public static void registerHandler(final ImageLoader handler, final String... types) {
    for (final String type : types) {
      ImageLoaderUtil.loaders.put(type.toLowerCase(), handler);
    }
  }

  public static void unregisterHandler(final String... types) {
    for (final String type : types) {
      ImageLoaderUtil.loaders.remove(type.toLowerCase());
    }
  }

  public static void registerDefaultHandler(final ImageLoader handler) {
    ImageLoaderUtil.defaultLoader = handler;
  }
}
