/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer;

import java.util.concurrent.Callable;

public abstract class RendererCallable<V> implements Callable<V> {

    private Renderer _renderer;

    public void setRenderer(final Renderer renderer) {
        _renderer = renderer;
    }

    public Renderer getRenderer() {
        return _renderer;
    }
}
