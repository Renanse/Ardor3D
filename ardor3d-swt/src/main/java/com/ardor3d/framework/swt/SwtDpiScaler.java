/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.framework.swt;

import com.ardor3d.framework.IDpiScaleProvider;

public enum SwtDpiScaler implements IDpiScaleProvider {

    INSTANCE;

    @Override
    public double scaleToScreenDpi(final double size) {
        return ApplyScale ? org.eclipse.swt.internal.DPIUtil.autoScaleUp((int) Math.round(size)) : size;
    }

    @Override
    public double scaleFromScreenDpi(final double size) {
        return ApplyScale ? org.eclipse.swt.internal.DPIUtil.autoScaleDown((int) Math.round(size)) : size;
    }

    public int scaleToScreenDpiInt(final double size) {
        return (int) Math.round(scaleToScreenDpi(size));
    }

    public int scaleFromScreenDpiInt(final double size) {
        return (int) Math.round(scaleFromScreenDpi(size));
    }

    public static boolean ApplyScale = true;
    static {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("mac os x")) {
            ApplyScale = false;
        }
    }
}
