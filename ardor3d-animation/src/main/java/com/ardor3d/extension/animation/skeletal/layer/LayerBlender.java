/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.layer;

import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationManager;

/**
 * Describes a class capable of blending together two AnimationLayers in some way.
 */
public interface LayerBlender {

    /**
     * @param layer
     *            our first layer
     */
    void setLayerA(AnimationLayer layer);

    /**
     * @param layer
     *            our second layer
     */
    void setLayerB(AnimationLayer layer);

    /**
     * @param key
     *            a String for retrieving the blend weight from the layer manager's values store.
     */
    void setBlendKey(String key);

    /**
     * @return the String for retrieving the blend weight from the layer manager's values store.
     */
    String getBlendKey();

    /**
     * @param manager
     *            the manager this is being called from
     * @return a key-value map representing the blended data from both animation layers.
     */
    Map<String, ? extends Object> getBlendedSourceData(AnimationManager manager);

}
