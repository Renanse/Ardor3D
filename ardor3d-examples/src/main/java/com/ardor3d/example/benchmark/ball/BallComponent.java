/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.benchmark.ball;

import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.util.Dimension;
import com.ardor3d.extension.ui.util.SubTex;

class BallComponent extends UILabel {

    private final Ball _ball;
    private final int _areaWidth;
    private final int _areaHeight;

    public BallComponent(final String name, final SubTex tex, final int width, final int height, final int areaWidth,
            final int areaHeight) {
        super("", tex);
        setName(name);
        setIconDimensions(new Dimension(width, height));
        _areaWidth = areaWidth;
        _areaHeight = areaHeight;
        _ball = new Ball();
        _ball.setRandomPositionIn(_areaWidth, _areaHeight);
        setLocalXY((int) _ball._x, (int) _ball._y);
    }

    @Override
    public void updateGeometricState(final double time, final boolean initiator) {
        super.updateGeometricState(time, initiator);
        _ball.move(_areaWidth, _areaHeight);
        setLocalXY((int) _ball._x, (int) _ball._y);
    }

    public Ball getBall() {
        return _ball;
    }
}
