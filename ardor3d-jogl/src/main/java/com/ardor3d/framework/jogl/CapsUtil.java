/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.util.Ardor3dException;
import com.jogamp.newt.Display;

public class CapsUtil {

    /**
     * Profile for the default display.
     */
    public static GLProfile getProfile() {
        return getProfile(null);
    }

    public static GLProfile getProfile(final Display display) {
        // tries to get the most capable profile, programmable or fixed, desktop or embedded, forward or backward
        // compatible
        final AbstractGraphicsDevice device = display == null ? null : display.getGraphicsDevice();
        GLProfile profile = device == null ? GLProfile.getMaximum(true) : GLProfile.getMaximum(device, true);
        final boolean isForwardCompatible = (!profile.isGL4() && profile.isGL3() && !profile.isGL3bc())
                || (profile.isGL4() && !profile.isGL4bc());
        if (isForwardCompatible) {
            // Ardor3D doesn't support forward compatible yet
            profile = device == null ? GLProfile.getMaxFixedFunc(true) : GLProfile.getMaxFixedFunc(device, true);
        }
        return profile;
    }

    /**
     * Capabilities for the default display.
     */
    public static GLCapabilities getCapsForSettings(final DisplaySettings settings) {
        return getCapsForSettings(settings, true, false, false, false);
    }

    /**
     * Capabilities for the default display with support for offscreen rendering.
     */
    public static GLCapabilities getCapsForSettings(final DisplaySettings settings, final boolean onscreen,
            final boolean bitmapRequested, final boolean pbufferRequested, final boolean fboRequested) {
        return getCapsForSettings(null, settings, onscreen, bitmapRequested, pbufferRequested, fboRequested);
    }

    /**
     * Capabilities for the specified display with support for offscreen rendering.
     */
    public static GLCapabilities getCapsForSettings(final Display display, final DisplaySettings settings,
            final boolean onscreen, final boolean bitmapRequested, final boolean pbufferRequested,
            final boolean fboRequested) {

        // Validate window dimensions.
        if (settings.getWidth() <= 0 || settings.getHeight() <= 0) {
            throw new Ardor3dException("Invalid resolution values: " + settings.getWidth() + " " + settings.getHeight());
        }

        // Validate bit depth.
        if ((settings.getColorDepth() != 32) && (settings.getColorDepth() != 16) && (settings.getColorDepth() != 24)
                && (settings.getColorDepth() != -1)) {
            throw new Ardor3dException("Invalid pixel depth: " + settings.getColorDepth());
        }

        final GLCapabilities caps = new GLCapabilities(getProfile(display));
        caps.setHardwareAccelerated(true);
        caps.setDoubleBuffered(true);
        caps.setAlphaBits(settings.getAlphaBits());
        caps.setDepthBits(settings.getDepthBits());
        caps.setNumSamples(settings.getSamples());
        caps.setSampleBuffers(settings.getSamples() != 0);
        caps.setStereo(settings.isStereo());
        caps.setStencilBits(settings.getStencilBits());
        switch (settings.getColorDepth()) {
            case 32:
            case 24:
                caps.setRedBits(8);
                caps.setBlueBits(8);
                caps.setGreenBits(8);
                break;
            case 16:
                caps.setRedBits(4);
                caps.setBlueBits(4);
                caps.setGreenBits(4);
                break;
        }
        caps.setOnscreen(onscreen);
        if (!onscreen) {
            caps.setBitmap(bitmapRequested);
            caps.setPBuffer(pbufferRequested);
            caps.setFBO(fboRequested);
        }
        return caps;
    }

}
