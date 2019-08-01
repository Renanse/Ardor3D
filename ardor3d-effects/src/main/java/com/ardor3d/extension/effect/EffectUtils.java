/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.effect;

import java.net.URISyntaxException;

import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

public abstract class EffectUtils {
    public static void addDefaultResourceLocators() {
        try {
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MATERIAL,
                    new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(EffectUtils.class,
                            "com/ardor3d/extension/effect/material")));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_SHADER,
                    new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(EffectUtils.class,
                            "com/ardor3d/extension/effect/shader")));
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }
    }
}
