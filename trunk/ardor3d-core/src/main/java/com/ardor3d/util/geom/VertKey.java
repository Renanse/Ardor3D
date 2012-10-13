/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom;

import java.util.EnumSet;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.geom.GeometryTool.MatchCondition;

public class VertKey {

    private final Vector3 _vert;
    private final Vector3 _norm;
    private final ColorRGBA _color;
    private final Vector2[] _texs;
    private final long _smoothGroup;
    private final EnumSet<MatchCondition> _options;
    private int _hashCode = 0;

    public VertKey(final Vector3 vert, final Vector3 norm, final ColorRGBA color, final Vector2[] texs,
            final EnumSet<MatchCondition> options) {
        this(vert, norm, color, texs, options, 0);
    }

    public VertKey(final Vector3 vert, final Vector3 norm, final ColorRGBA color, final Vector2[] texs,
            final EnumSet<MatchCondition> options, final long smoothGroup) {
        _vert = vert;
        _options = options != null ? options : EnumSet.noneOf(MatchCondition.class);
        _norm = (_options.contains(MatchCondition.Normal)) ? norm : null;
        _color = (_options.contains(MatchCondition.Color)) ? color : null;
        _texs = (_options.contains(MatchCondition.UVs)) ? texs : null;
        _smoothGroup = (_options.contains(MatchCondition.Group)) ? smoothGroup : 0;
    }

    @Override
    public int hashCode() {
        if (_hashCode != 0) {
            return _hashCode;
        }
        _hashCode = _vert.hashCode();
        if (_options.contains(MatchCondition.Normal) && _norm != null) {
            final long x = Double.doubleToLongBits(_norm.getX());
            _hashCode += 31 * _hashCode + (int) (x ^ (x >>> 32));

            final long y = Double.doubleToLongBits(_norm.getY());
            _hashCode += 31 * _hashCode + (int) (y ^ (y >>> 32));

            final long z = Double.doubleToLongBits(_norm.getZ());
            _hashCode += 31 * _hashCode + (int) (z ^ (z >>> 32));
        }
        if (_options.contains(MatchCondition.Color) && _color != null) {
            final int r = Float.floatToIntBits(_color.getRed());
            _hashCode += 31 * _hashCode + r;

            final int g = Float.floatToIntBits(_color.getGreen());
            _hashCode += 31 * _hashCode + g;

            final int b = Float.floatToIntBits(_color.getBlue());
            _hashCode += 31 * _hashCode + b;

            final int a = Float.floatToIntBits(_color.getAlpha());
            _hashCode += 31 * _hashCode + a;
        }
        if (_options.contains(MatchCondition.UVs) && _texs != null) {
            for (int i = 0; i < _texs.length; i++) {
                if (_texs[i] != null) {
                    final long x = Double.doubleToLongBits(_texs[i].getX());
                    _hashCode += 31 * _hashCode + (int) (x ^ (x >>> 32));

                    final long y = Double.doubleToLongBits(_texs[i].getY());
                    _hashCode += 31 * _hashCode + (int) (y ^ (y >>> 32));
                }
            }
        }
        if (_options.contains(MatchCondition.Group)) {
            _hashCode += 31 * _hashCode + _smoothGroup;
        }
        return _hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof VertKey)) {
            return false;
        }

        final VertKey other = (VertKey) obj;

        if (other._options != _options) {
            return false;
        }
        if (!other._vert.equals(_vert)) {
            return false;
        }

        if (_options.contains(MatchCondition.Normal)) {
            if (_norm != null) {
                if (!_norm.equals(other._norm)) {
                    return false;
                }
            } else if (other._norm != null) {
                return false;
            }
        }

        if (_options.contains(MatchCondition.Color)) {
            if (_color != null) {
                if (!_color.equals(other._color)) {
                    return false;
                }
            } else if (other._color != null) {
                return false;
            }
        }

        if (_options.contains(MatchCondition.UVs)) {
            if (_texs != null) {
                if (other._texs == null || other._texs.length != _texs.length) {
                    return false;
                }
                for (int x = 0; x < _texs.length; x++) {
                    if (_texs[x] != null) {
                        if (!_texs[x].equals(other._texs[x])) {
                            return false;
                        }
                    } else if (other._texs[x] != null) {
                        return false;
                    }
                }
            } else if (other._texs != null) {
                return false;
            }
        }

        if (_options.contains(MatchCondition.Group)) {
            if (other._smoothGroup != _smoothGroup) {
                return false;
            }
        }

        return true;
    }
}
