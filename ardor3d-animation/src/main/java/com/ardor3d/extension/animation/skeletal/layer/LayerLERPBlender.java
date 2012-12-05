/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.layer;

import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.BinaryLERPSource;

/**
 * <p>
 * A layer blender that uses linear interpolation to merge the results of two layers.
 * </p>
 * See also {@link BinaryLERPSource#combineSourceData(Map, Map, double)}
 */
public class LayerLERPBlender implements LayerBlender {

    /** A key into the related AnimationManager's values store for pulling blend weight. */
    private String _blendKey;

    /** Our first layer... generally the "prior" layer. */
    private AnimationLayer _layerA;

    /** Our second layer... generally the layer we were added to. */
    private AnimationLayer _layerB;

    public String getBlendKey() {
        return _blendKey;
    }

    public void setBlendKey(final String blendKey) {
        _blendKey = blendKey;
    }

    public AnimationLayer getLayerA() {
        return _layerA;
    }

    public void setLayerA(final AnimationLayer layer) {
        _layerA = layer;
    }

    public AnimationLayer getLayerB() {
        return _layerB;
    }

    public void setLayerB(final AnimationLayer layer) {
        _layerB = layer;
    }

    public Map<String, ? extends Object> getBlendedSourceData(final AnimationManager manager) {
        // grab our data maps from the two layers...
        // set A
        final Map<String, ? extends Object> sourceAData = getLayerA().getCurrentSourceData();
        // set B
        final Map<String, ? extends Object> sourceBData = getLayerB().getCurrentState() != null ? getLayerB()
                .getCurrentState().getCurrentSourceData(manager) : null;

        return BinaryLERPSource.combineSourceData(sourceAData, sourceBData, manager.getValuesStore().get(_blendKey));
    }
}
