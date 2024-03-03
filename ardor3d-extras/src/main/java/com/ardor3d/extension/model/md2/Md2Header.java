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
 *
 * @param magic            identifier of the file: magic number: "IDP2"
 * @param version          version number of the file (must be 8)
 * @param skinWidth        texture width in pixels
 * @param skinHeight       texture height in pixels
 * @param frameSize        size in bytes of a frame
 * @param numSkins         number of textures associated with the model
 * @param numVertices      number of vertices per frame
 * @param numTexCoords     number of texture coordinates
 * @param numTriangles     number of triangles
 * @param numGlCommands    number of gl commands
 * @param numFrames        number of animation frames
 * @param offsetSkins      offset in the file for the texture data
 * @param offsetTexCoords  offset in the file for the texture coords
 * @param offsetTriangles  offset in the file for the face data
 * @param offsetFrames     offset in the file for the frames data
 * @param offsetGlCommands offset in the file for the gl commands data
 * @param offsetEnd        offset of EOF
 */
record Md2Header(int magic, int version, int skinWidth, int skinHeight, int frameSize, int numSkins, int numVertices,
                 int numTexCoords, int numTriangles, int numGlCommands, int numFrames, int offsetSkins,
                 int offsetTexCoords, int offsetTriangles, int offsetFrames, int offsetGlCommands, int offsetEnd) {
}
