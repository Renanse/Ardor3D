/**
 * Copyright (c) 2008-2019 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.swt;

public class SwtConstants {
    public static float PlatformDPIScale = 1.0f;
    static {
        final String os = System.getProperty("os.name");
        if (os.indexOf("nux") > 0 || os.indexOf("nix") > 0) {
            PlatformDPIScale = 2.0f;
        }
    }
}
