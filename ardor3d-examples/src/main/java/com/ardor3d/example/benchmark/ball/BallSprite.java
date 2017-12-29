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

import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Quad;

public class BallSprite extends Quad {

    private final Ball _ball;
    private int _areaWidth;
    private int _areaHeight;

    public BallSprite(final String name, final int areaWidth, final int areaHeight) {
        this(name, areaWidth, areaHeight, 1.0);
    }

    public BallSprite(final String name, final int areaWidth, final int areaHeight, final double scale) {
        super(name, Ball.radius * 2, Ball.radius * 2);
        _areaWidth = areaWidth;
        _areaHeight = areaHeight;
        _ball = new Ball(scale);
        _ball.setRandomPositionIn(_areaWidth, _areaHeight);
        setTranslation(_ball._x + _ball.getCurrentRadius(), _ball._y + _ball.getCurrentRadius(), 0);
        getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        setModelBound(null);
        getSceneHints().setCullHint(CullHint.Never);
    }

    @Override
    public void updateGeometricState(final double time, final boolean initiator) {
        super.updateGeometricState(time, initiator);
        _ball.move(_areaWidth, _areaHeight, time);
        setTranslation(_ball._x + _ball.getCurrentRadius(), _ball._y + _ball.getCurrentRadius(), 0);
    }

    public Ball getBall() {
        return _ball;
    }

    public void updateAreaDimensions(final int width, final int height) {
        _areaWidth = width;
        _areaHeight = height;
    }
}
