/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.light.Light;
import com.ardor3d.light.PointLight;
import com.ardor3d.light.SpotLight;
import com.ardor3d.math.Plane;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;

public abstract class LightUtil {
    private static class LightComparator implements Comparator<Light> {
        private Spatial _sp;

        public void setSpatial(final Spatial sp) {
            _sp = sp;
        }

        public int compare(final Light l1, final Light l2) {
            final double v1 = getValueFor(l1, _sp.getWorldBound());
            final double v2 = getValueFor(l2, _sp.getWorldBound());
            final double cmp = v1 - v2;
            if (0 > cmp) {
                return 1;
            } else if (0 < cmp) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private static LightComparator lightComparator = new LightComparator();

    public static void sort(final Mesh geometry, final List<Light> lights) {
        lightComparator.setSpatial(geometry);
        Collections.sort(lights, lightComparator);
    }

    protected static double getValueFor(final Light l, final BoundingVolume val) {
        if (l == null || !l.isEnabled()) {
            return 0;
        } else if (l.getType() == Light.Type.Directional) {
            return getColorValue(l);
        } else if (l.getType() == Light.Type.Point) {
            return getValueFor((PointLight) l, val);
        } else if (l.getType() == Light.Type.Spot) {
            return getValueFor((SpotLight) l, val);
        }
        // If a new type of light was added and this was not updated return .3
        return .3;
    }

    protected static double getValueFor(final PointLight l, final BoundingVolume val) {
        if (val == null) {
            return 0;
        }
        if (l.isAttenuate()) {
            final ReadOnlyVector3 location = l.getLocation();
            final double dist = val.distanceTo(location);

            final double color = getColorValue(l);
            final double amlat = l.getConstant() + l.getLinear() * dist + l.getQuadratic() * dist * dist;

            return color / amlat;
        }

        return getColorValue(l);
    }

    protected static double getValueFor(final SpotLight l, final BoundingVolume val) {
        if (val == null) {
            return 0;
        }
        final ReadOnlyVector3 direction = l.getDirection();
        final ReadOnlyVector3 location = l.getLocation();
        // direction is copied into Plane, not reused.
        final Plane p = new Plane(direction, direction.dot(location));
        if (val.whichSide(p) != Plane.Side.Inside) {
            return getValueFor((PointLight) l, val);
        }

        return 0;
    }

    protected static double getColorValue(final Light l) {
        return strength(l.getAmbient()) + strength(l.getDiffuse());
    }

    protected static double strength(final ReadOnlyColorRGBA color) {
        return Math.sqrt(color.getRed() * color.getRed() + color.getGreen() * color.getGreen() + color.getBlue()
                * color.getBlue());
    }
}
