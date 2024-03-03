/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.md2;

/**
 * Header of MD2: see also http://tfc.duke.free.fr/coding/md2-specs-en.html
 */
final class Md2Header {
  /** identifier of the file: magic number: "IDP2" */
  final int magic;
  /** version number of the file (must be 8) */
  final int version;

  /** texture width in pixels */
  final int skinWidth;
  /** texture height in pixels */
  final int skinHeight;

  /** size in bytes of a frame */
  final int frameSize;

  /** number of textures associated with the model */
  final int numSkins;
  /** number of vertices per frame */
  final int numVertices;
  /** number of texture coordinates */
  final int numTexCoords;
  /** number of triangles */
  final int numTriangles;
  /** number of gl commands */
  final int numGlCommands;
  /** number of animation frames */
  final int numFrames;

  /** offset in the file for the texture data */
  final int offsetSkins;
  /** offset in the file for the texture coords */
  final int offsetTexCoords;
  /** offset in the file for the face data */
  final int offsetTriangles;
  /** offset in the file for the frames data */
  final int offsetFrames;
  /** offset in the file for the gl commands data */
  final int offsetGlCommands;
  /** offset of EOF */
  final int offsetEnd;

  Md2Header(final int magic, final int version, final int skinWidth, final int skinHeight, final int frameSize,
    final int numSkins, final int numVertices, final int numTexCoords, final int numTriangles, final int numGlCommands,
    final int numFrames, final int offsetSkins, final int offsetTexCoords, final int offsetTriangles,
    final int offsetFrames, final int offsetGlCommands, final int offsetEnd) {
    this.magic = magic;
    this.version = version;
    this.skinWidth = skinWidth;
    this.skinHeight = skinHeight;
    this.frameSize = frameSize;
    this.numSkins = numSkins;
    this.numVertices = numVertices;
    this.numTexCoords = numTexCoords;
    this.numTriangles = numTriangles;
    this.numGlCommands = numGlCommands;
    this.numFrames = numFrames;
    this.offsetSkins = offsetSkins;
    this.offsetTexCoords = offsetTexCoords;
    this.offsetTriangles = offsetTriangles;
    this.offsetFrames = offsetFrames;
    this.offsetGlCommands = offsetGlCommands;
    this.offsetEnd = offsetEnd;
  }
}
