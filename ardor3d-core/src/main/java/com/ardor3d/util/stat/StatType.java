/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.stat;

public class StatType implements Comparable<StatType> {

    public static final StatType STAT_FRAMES = new StatType("_frames");

    public static final StatType STAT_TRIANGLE_COUNT = new StatType("_triCount");
    public static final StatType STAT_LINE_COUNT = new StatType("_lineCount");
    public static final StatType STAT_POINT_COUNT = new StatType("_pointCount");
    public static final StatType STAT_VERTEX_COUNT = new StatType("_vertCount");
    public static final StatType STAT_MESH_COUNT = new StatType("_meshCount");
    public static final StatType STAT_TEXTURE_BINDS = new StatType("_texBind");
    public static final StatType STAT_SHADER_BINDS = new StatType("_shaderBind");

    public static final StatType STAT_UNSPECIFIED_TIMER = new StatType("_timedOther");
    public static final StatType STAT_RENDER_TIMER = new StatType("_timedRenderer");
    public static final StatType STAT_STATES_TIMER = new StatType("_timedStates");
    public static final StatType STAT_TEXTURE_STATE_TIMER = new StatType("_timedTextureState");
    public static final StatType STAT_SHADER_STATE_TIMER = new StatType("_timedShaderState");
    public static final StatType STAT_UPDATE_TIMER = new StatType("_timedUpdates");
    public static final StatType STAT_DISPLAYSWAP_TIMER = new StatType("_timedSwap");

    private String _statName = "-unknown-";

    public StatType(final String name) {
        _statName = name;
    }

    public String getStatName() {
        return _statName;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof StatType)) {
            return false;
        }
        final StatType other = (StatType) obj;
        if (!_statName.equals(other._statName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int hash = _statName.hashCode();
        return hash;
    }

    public int compareTo(final StatType obj) {
        final StatType other = obj;
        return _statName.compareTo(other._statName);
    }
}
