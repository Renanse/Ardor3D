/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.atlas;

import com.ardor3d.math.Rectangle2;

public class AtlasNode {
    private boolean isLeaf = true;
    private boolean isSet = false;
    private final AtlasNode child[] = new AtlasNode[2];
    private Rectangle2 localRectangle;

    private AtlasNode() {}

    public AtlasNode(final int width, final int height) {
        localRectangle = new Rectangle2(0, 0, width, height);
    }

    public AtlasNode insert(final Rectangle2 rectangle) {
        if (!isLeaf) {
            final AtlasNode newNode = child[0].insert(rectangle);
            if (newNode != null) {
                return newNode;
            }

            return child[1].insert(rectangle);
        } else {
            if (isSet) {
                return null;
            }

            if (rectangle.getWidth() > localRectangle.getWidth() || rectangle.getHeight() > localRectangle.getHeight()) {
                return null;
            }

            if (rectangle.getWidth() == localRectangle.getWidth()
                    && rectangle.getHeight() == localRectangle.getHeight()) {
                isSet = true;
                return this;
            }

            isLeaf = false;

            child[0] = new AtlasNode();
            child[1] = new AtlasNode();

            final int dw = localRectangle.getWidth() - rectangle.getWidth();
            final int dh = localRectangle.getHeight() - rectangle.getHeight();

            if (dw > dh) {
                child[0].localRectangle = new Rectangle2(localRectangle.getX(), localRectangle.getY(),
                        rectangle.getWidth(), localRectangle.getHeight());
                child[1].localRectangle = new Rectangle2(localRectangle.getX() + rectangle.getWidth(),
                        localRectangle.getY(), dw, localRectangle.getHeight());
            } else {
                child[0].localRectangle = new Rectangle2(localRectangle.getX(), localRectangle.getY(),
                        localRectangle.getWidth(), rectangle.getHeight());
                child[1].localRectangle = new Rectangle2(localRectangle.getX(), localRectangle.getY()
                        + rectangle.getHeight(), localRectangle.getWidth(), dh);
            }

            return child[0].insert(rectangle);
        }
    }

    public AtlasNode getChild(final int childIndex) {
        return child[childIndex];
    }

    public Rectangle2 getRectangle() {
        return localRectangle;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public boolean isSet() {
        return isSet;
    }
}
