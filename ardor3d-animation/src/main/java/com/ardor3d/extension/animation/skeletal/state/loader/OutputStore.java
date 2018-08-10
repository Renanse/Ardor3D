/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.state.loader;

import java.util.List;

import com.ardor3d.extension.animation.skeletal.AttachmentPoint;
import com.google.common.collect.Lists;

/**
 * Storage class for items created during Layer import.
 */
public class OutputStore {

    /** List of attachment points created during layer import. */
    private final List<AttachmentPoint> _attachments = Lists.newArrayList();

    /** List of animation clip sources encountered during layer import. */
    private final OutputClipSourceMap _usedClipSources = new OutputClipSourceMap();

    public void addAttachmentPoint(final AttachmentPoint attach) {
        _attachments.add(attach);
    }

    public List<AttachmentPoint> getAttachmentPoints() {
        return _attachments;
    }

    public AttachmentPoint findAttachmentPoint(final String name) {
        for (final AttachmentPoint attach : _attachments) {
            if (name.equals(attach.getName())) {
                return attach;
            }
        }
        return null;
    }

    public OutputClipSourceMap getClipSources() {
        return _usedClipSources;
    }
}
