/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.md3;

/**
 * header of MD3: http://en.wikipedia.org/wiki/MD3_%28file_format%29#MD3_header
 */
final class Md3Header {
    /** identifier of the file: magic number: "IDP3" */
    final int _magic;
    /** version number of the file */
    final int _version;
    /** name, usually its pathname in the PK3. ASCII character string, NULL-terminated (C-style) */
    final String _name;
    /** flags, unused yet */
    final int _flags;
    /** Number of Frame objects, with a maximum of MD3_MAX_FRAMES. Current value of MD3_MAX_FRAMES is 1024. */
    final int _numFrames;
    /**
     * Number of Tag objects, with a maximum of MD3_MAX_TAGS. Current value of MD3_MAX_TAGS is 16. There is one set of
     * tags per frame so the total number of tags to read is (NUM_TAGS * NUM_FRAMES).
     */
    final int _numTags;
    /** Number of Surface objects, with a maximum of MD3_MAX_SURFACES. Current value of MD3_MAX_SURFACES is 32. */
    final int _numSurface;
    /** Number of Skin objects, unused */
    final int _numSkins;
    /** Relative offset from start of MD3 object where Frame objects start. The Frame objects are written sequentially, that is, when you read one Frame object, you do not need to seek() for the next object. */
    final int _offsetFrame;
    /** Relative offset from start of MD3 where Tag objects start. Similarly written sequentially. */
    final int _offsetTag;
    /** Relative offset from start of MD3 where Surface objects start. Again, written sequentially. */
    final int _offsetSurface;
    /** Relative offset from start of MD3 to the end of the MD3 object */
    final int _offsetEnd;

    Md3Header(final int magic, final int version, final String name, final int flags, final int numFrames,
            final int numTags, final int numSurface, final int numSkins, final int offsetFrame, final int offsetTag,
            final int offsetSurface, final int offsetEnd) {
        _magic = magic;
        _version = version;
        _name = name;
        _flags = flags;
        _numFrames = numFrames;
        _numTags = numTags;
        _numSurface = numSurface;
        _numSkins = numSkins;
        _offsetFrame = offsetFrame;
        _offsetTag = offsetTag;
        _offsetSurface = offsetSurface;
        _offsetEnd = offsetEnd;
    }
}
