/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2ES3;
import javax.media.opengl.GLAutoDrawable;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.geom.jogl.DirectNioBuffersSet;

public class JoglContextCapabilities extends ContextCapabilities {

    public JoglContextCapabilities(final GLAutoDrawable autodrawable, final DirectNioBuffersSet directNioBuffersSet) {
        init(autodrawable.getGL(), directNioBuffersSet);
    }

    public JoglContextCapabilities(final GL gl, final DirectNioBuffersSet directNioBuffersSet) {
        init(gl, directNioBuffersSet);
    }

    public JoglContextCapabilities(final ContextCapabilities caps) {
        super(caps);
    }

    public void init(final GL gl, final DirectNioBuffersSet directNioBuffersSet) {
        final IntBuffer buf = directNioBuffersSet.getSingleIntBuffer();
        buf.clear();

        gl.glGetIntegerv(GL2ES3.GL_MAX_TEXTURE_LOD_BIAS, buf);
        _maxTextureLodBias = buf.get(0);

        gl.glGetIntegerv(GL2.GL_MAX_VERTEX_ATTRIBS_ARB, buf);
        _maxGLSLVertexAttribs = buf.get(0);

        if (gl.isExtensionAvailable("GL_ARB_draw_buffers")) {
            gl.glGetIntegerv(GL2ES2.GL_MAX_COLOR_ATTACHMENTS, buf);
            _maxFBOColorAttachments = buf.get(0);
        } else {
            _maxFBOColorAttachments = 1;
        }

        // Max multisample samples.
        if (gl.isExtensionAvailable("GL_EXT_framebuffer_multisample")
                && gl.isExtensionAvailable("GL_EXT_framebuffer_blit")) {
            gl.glGetIntegerv(GL2ES3.GL_MAX_SAMPLES, buf);
            _maxFBOSamples = buf.get(0);
        } else {
            _maxFBOSamples = 0;
        }

        // max texture size.
        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, buf);
        _maxTextureSize = buf.get(0);

        // max texture size.
        gl.glGetIntegerv(GL.GL_MAX_RENDERBUFFER_SIZE, buf);
        _maxRenderBufferSize = buf.get(0);

        // max viewport size.
        final IntBuffer dimsBuf = BufferUtils.createIntBuffer(2);
        gl.glGetIntegerv(GL.GL_MAX_VIEWPORT_DIMS, dimsBuf);
        _maxViewportWidth = dimsBuf.get(0);
        _maxViewportHeight = dimsBuf.get(1);

        // Find out how many textures we can handle.
        gl.glGetIntegerv(GL2ES2.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, buf);
        _numVertexTexUnits = buf.get(0);
        gl.glGetIntegerv(GL2ES2.GL_MAX_TEXTURE_IMAGE_UNITS, buf);
        _numFragmentTexUnits = buf.get(0);

        // Now determine the maximum number of supported texture units
        _numTotalTexUnits = Math.max(_numFragmentTexUnits, _numVertexTexUnits);

        // Check for S3 texture compression capability.
        _supportsS3TCCompression = gl.isExtensionAvailable("GL_EXT_texture_compression_s3tc");

        // Check for LA texture compression capability.
        _supportsLATCCompression = gl.isExtensionAvailable("GL_EXT_texture_compression_latc");

        // See if we support anisotropic filtering
        _supportsAniso = gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic");

        if (_supportsAniso) {
            final FloatBuffer max_a = directNioBuffersSet.getSingleFloatBuffer();
            max_a.clear();

            // Grab the maximum anisotropic filter.
            gl.glGetFloatv(GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max_a);

            // set max.
            _maxAnisotropic = max_a.get(0);
        }

        try {
            _displayVendor = gl.glGetString(GL.GL_VENDOR);
        } catch (final Exception e) {
            _displayVendor = "Unable to retrieve vendor.";
        }

        try {
            _displayRenderer = gl.glGetString(GL.GL_RENDERER);
        } catch (final Exception e) {
            _displayRenderer = "Unable to retrieve adapter details.";
        }

        try {
            _displayVersion = gl.glGetString(GL.GL_VERSION);
        } catch (final Exception e) {
            _displayVersion = "Unable to retrieve API version.";
        }

        try {
            _shadingLanguageVersion = gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION);
        } catch (final Exception e) {
            _shadingLanguageVersion = "Unable to retrieve shading language version.";
        }
    }
}
