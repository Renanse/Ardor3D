/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.material.uniform;

public abstract class BlinnPhongKeys {

    public static final String DiffuseColor = "diffuse_color";
    public static final String DiffuseColorBack = "diffuse_color_back";
    public static final String AmbientColor = "ambient_color";
    public static final String AmbientColorBack = "ambient_color_back";
    public static final String SpecularColor = "specular_color";
    public static final String SpecularColorBack = "specular_color_back";
    public static final String EmissiveColor = "emissive_color";
    public static final String EmissiveColorBack = "emissive_color_back";
    public static final String Shininess = "shininess";
    public static final String ShininessBack = "shininess_back";

    private BlinnPhongKeys() {}
}
