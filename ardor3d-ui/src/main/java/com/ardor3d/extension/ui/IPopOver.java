/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.renderer.Renderer;

public interface IPopOver {

    public abstract void showAt(int x, int y);

    public abstract void setHud(UIHud hud);

    public abstract UIComponent getUIComponent(int hudX, int hudY);

    public abstract void onDraw(Renderer renderer);

    public abstract void updateGeometricState(double time, boolean initiator);

    public abstract void close();

    public abstract boolean isAttachedToHUD();

}