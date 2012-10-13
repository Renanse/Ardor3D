/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.controller.interpolation;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;

/**
 * ColorRGBAInterpolationController class interpolates the {@link Mesh#getDefaultColor() default colour} of a mesh using
 * {@link ReadOnlyColorRGBA}s.
 * <p>
 * Note: The default colour only works if a {@link MeshData#getColorBuffer() colour buffer} has NOT been set on the
 * mesh.
 * </p>
 */
public class DefaultColorInterpolationController extends InterpolationController<ReadOnlyColorRGBA, Mesh> {

    /** Serial UID */
    private static final long serialVersionUID = 1L;

    /**
     * Interpolates between the given colors using the
     * {@link ColorRGBA#lerpLocal(ReadOnlyColorRGBA, ReadOnlyColorRGBA, float)} method.
     */
    @Override
    protected void interpolate(final ReadOnlyColorRGBA from, final ReadOnlyColorRGBA to, final double delta,
            final Mesh caller) {

        assert (null != from) : "parameter 'from' can not be null";
        assert (null != to) : "parameter 'to' can not be null";
        assert (null != caller) : "parameter 'caller' can not be null";

        final ColorRGBA color = ColorRGBA.fetchTempInstance().set(caller.getDefaultColor());

        color.lerpLocal(from, to, (float) delta);

        caller.setDefaultColor(color);

        ColorRGBA.releaseTempInstance(color);
    }

}
