/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.stat.graph;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.CullHint;

/**
 * <p>
 * A controller that changes over time the alpha value of the default color of a given Geometry. When coupled with an
 * appropriate BlendState, this can be used to fade in and out unlit objects.
 * </p>
 * 
 * <p>
 * An example of an appropriate BlendState to use with this class:
 * </p>
 * 
 * <pre>
 * BlendState blend = new BlendState();
 * blend.setBlendEnabled(true);
 * blend.setSourceFunction(SourceFunction.SourceAlpha);
 * blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
 * </pre>
 */
public class DefColorFadeController implements SpatialController<Spatial> {

    private Mesh _target;
    private final float _targetAlpha;
    private final double _rate;
    private final boolean _dir;

    /**
     * Sets up a new instance of the controller. The
     * 
     * @param target
     *            the object whose default color we want to change the alpha on.
     * @param targetAlpha
     *            the alpha value we want to end up at.
     * @param rate
     *            the amount, per second, to change the alpha. This value will be have its sign flipped if it is not the
     *            appropriate direction given the current default color's alpha.
     */
    public DefColorFadeController(final Mesh target, final float targetAlpha, double rate) {
        _target = target;
        _targetAlpha = targetAlpha;
        _dir = target.getDefaultColor().getAlpha() > targetAlpha;
        if ((_dir && rate > 0) || (!_dir && rate < 0)) {
            rate *= -1;
        }
        _rate = rate;
    }

    public void update(final double time, final Spatial caller) {
        if (_target == null) {
            return;
        }
        final ColorRGBA color = ColorRGBA.fetchTempInstance().set(_target.getDefaultColor());
        float alpha = color.getAlpha();

        alpha += _rate * time;
        if (_dir && alpha <= _targetAlpha) {
            alpha = _targetAlpha;
        } else if (!_dir && alpha >= _targetAlpha) {
            alpha = _targetAlpha;
        }

        if (alpha != 0) {
            _target.getSceneHints().setCullHint(CullHint.Inherit);
        } else {
            _target.getSceneHints().setCullHint(CullHint.Always);
        }

        color.setAlpha(alpha);
        _target.setDefaultColor(color);
        ColorRGBA.releaseTempInstance(color);

        if (alpha == _targetAlpha) {
            _target.removeController(this);

            // enable gc
            _target = null;
        }
    }

}
