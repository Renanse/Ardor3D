/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.lwjgl3;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.opengl.GLCapabilities;

import com.ardor3d.renderer.ContextCapabilities;

public class Lwjgl3ContextCapabilities extends ContextCapabilities {

    public Lwjgl3ContextCapabilities(final ContextCapabilities caps) {
        super(caps);
    }

    public Lwjgl3ContextCapabilities(final GLCapabilities caps) {

        _maxTextureLodBias = GL11C.glGetInteger(GL14C.GL_MAX_TEXTURE_LOD_BIAS);

        _maxGLSLVertexAttribs = GL11C.glGetInteger(GL20C.GL_MAX_VERTEX_ATTRIBS);

        if (caps.GL_ARB_draw_buffers) {
            _maxFBOColorAttachments = GL11C.glGetInteger(GL30C.GL_MAX_COLOR_ATTACHMENTS);
        } else {
            _maxFBOColorAttachments = 1;
        }

        // Max multisample samples.
        if (caps.GL_EXT_framebuffer_multisample && caps.GL_EXT_framebuffer_blit) {
            _maxFBOSamples = GL11C.glGetInteger(GL30C.GL_MAX_SAMPLES);
        } else {
            _maxFBOSamples = 0;
        }

        // max texture size.
        _maxTextureSize = GL11C.glGetInteger(GL11C.GL_MAX_TEXTURE_SIZE);

        // max texture size.
        _maxRenderBufferSize = GL11C.glGetInteger(GL30C.GL_MAX_RENDERBUFFER_SIZE);

        // max viewport size.
        final int[] dims = new int[2];
        GL11C.glGetIntegerv(GL11C.GL_MAX_VIEWPORT_DIMS, dims);
        _maxViewportWidth = dims[0];
        _maxViewportHeight = dims[1];

        // Go on to check number of texture units supported for vertex and
        // fragment shaders
        _numVertexTexUnits = GL11C.glGetInteger(GL20C.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS);
        _numFragmentTexUnits = GL11C.glGetInteger(GL20C.GL_MAX_TEXTURE_IMAGE_UNITS);

        // ARBShaderObjects.GL_OBJECT_ACTIVE_UNIFORM_MAX_LENGTH_ARB
        // caps.GL_EXT_bindable_uniform
        // EXTBindableUniform.GL_MAX_BINDABLE_UNIFORM_SIZE_EXT;

        // Now determine the maximum number of supported texture units
        _numTotalTexUnits = Math.max(_numFragmentTexUnits, _numVertexTexUnits);

        // Check for S3 texture compression capability.
        _supportsS3TCCompression = caps.GL_EXT_texture_compression_s3tc;

        // Check for LA texture compression capability.
        _supportsLATCCompression = caps.GL_EXT_texture_compression_latc;

        // See if we support anisotropic filtering
        _supportsAniso = caps.GL_EXT_texture_filter_anisotropic;

        if (_supportsAniso) {
            _maxAnisotropic = GL33C.glGetFloat(GL46C.GL_MAX_TEXTURE_MAX_ANISOTROPY);
        }

        try {
            _displayVendor = GL11C.glGetString(GL11C.GL_VENDOR);
        } catch (final Exception e) {
            _displayVendor = "Unable to retrieve vendor.";
        }

        try {
            _displayRenderer = GL11C.glGetString(GL11C.GL_RENDERER);
        } catch (final Exception e) {
            _displayRenderer = "Unable to retrieve adapter details.";
        }

        try {
            _displayVersion = GL11C.glGetString(GL11C.GL_VERSION);
        } catch (final Exception e) {
            _displayVersion = "Unable to retrieve API version.";
        }

        try {
            _shadingLanguageVersion = GL11C.glGetString(GL20C.GL_SHADING_LANGUAGE_VERSION);
        } catch (final Exception e) {
            _shadingLanguageVersion = "Unable to retrieve shading language version.";
        }
    }
}
