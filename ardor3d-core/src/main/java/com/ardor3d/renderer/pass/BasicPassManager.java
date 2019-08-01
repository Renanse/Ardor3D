/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.pass;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.image.Texture;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.texture.TextureRenderer;

/**
 * <code>BasicPassManager</code> controls a set of passes and sends through calls to render and update.
 */
public class BasicPassManager {

    protected List<Pass> _passes = new ArrayList<Pass>();

    public void add(final Pass toAdd) {
        if (toAdd != null) {
            _passes.add(toAdd);
        }
    }

    public void insert(final Pass toAdd, final int index) {
        _passes.add(index, toAdd);
    }

    public boolean contains(final Pass s) {
        return _passes.contains(s);
    }

    public boolean remove(final Pass toRemove) {
        return _passes.remove(toRemove);
    }

    public Pass get(final int index) {
        return _passes.get(index);
    }

    public int passes() {
        return _passes.size();
    }

    public void clearAll() {
        cleanUp();
        _passes.clear();
    }

    public void cleanUp() {
        for (int i = 0, sSize = _passes.size(); i < sSize; i++) {
            final Pass p = _passes.get(i);
            p.cleanUp();
        }
    }

    public void renderPasses(final Renderer r) {
        for (int i = 0, sSize = _passes.size(); i < sSize; i++) {
            final Pass p = _passes.get(i);
            p.renderPass(r);
        }
    }

    public void renderPasses(final TextureRenderer r, final int clear, final List<Texture> texs) {
        for (int i = 0, sSize = _passes.size(); i < sSize; i++) {
            final Pass p = _passes.get(i);
            p.renderPass(r, clear, texs);
        }
    }

    public void updatePasses(final double tpf) {
        for (int i = 0, sSize = _passes.size(); i < sSize; i++) {
            final Pass p = _passes.get(i);
            p.updatePass(tpf);
        }
    }

}
