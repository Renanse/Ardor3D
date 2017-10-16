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
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2ES3;
import javax.media.opengl.GLAutoDrawable;

import com.ardor3d.renderer.ContextCapabilities;
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

        _supportsVBO = gl.isExtensionAvailable("GL_ARB_vertex_buffer_object");
        _supportsGL1_2 = gl.isExtensionAvailable("GL_VERSION_1_2");
        _supportsMultisample = gl.isExtensionAvailable("GL_ARB_multisample");
        _supportsDoubleCoefficientsInClipPlaneEquation = !gl.isGLES1();

        _supportsConstantColor = _supportsEq = gl.isExtensionAvailable("GL_ARB_imaging");
        _supportsSeparateFunc = gl.isExtensionAvailable("GL_EXT_blend_func_separate");
        _supportsSeparateEq = gl.isExtensionAvailable("GL_EXT_blend_equation_separate");
        _supportsMinMax = gl.isExtensionAvailable("GL_EXT_blend_minmax");
        _supportsSubtract = gl.isExtensionAvailable("GL_EXT_blend_subtract");

        _supportsFogCoords = gl.isExtensionAvailable("GL_EXT_fog_coord");
        _supportsFragmentProgram = gl.isExtensionAvailable("GL_ARB_fragment_program");
        _supportsVertexProgram = gl.isExtensionAvailable("GL_ARB_vertex_program");

        _supportsPointSprites = gl.isExtensionAvailable("GL_ARB_point_sprite");
        _supportsPointParameters = gl.isExtensionAvailable("GL_ARB_point_parameters");

        _supportsTextureLodBias = gl.isExtensionAvailable("GL_EXT_texture_lod_bias");
        if (_supportsTextureLodBias) {
            gl.glGetIntegerv(GL2ES3.GL_MAX_TEXTURE_LOD_BIAS, buf);
            _maxTextureLodBias = buf.get(0);
        } else {
            _maxTextureLodBias = 0f;
        }

        gl.glGetIntegerv(GL2ES1.GL_MAX_CLIP_PLANES, buf);
        _maxUserClipPlanes = buf.get(0);

        _glslSupported = gl.isExtensionAvailable("GL_ARB_shader_objects")
                && gl.isExtensionAvailable("GL_ARB_fragment_shader") && gl.isExtensionAvailable("GL_ARB_vertex_shader")
                && gl.isExtensionAvailable("GL_ARB_shading_language_100");

        _geometryShader4Supported = gl.isExtensionAvailable("GL_ARB_geometry_shader4") && _glslSupported;

        _geometryInstancingSupported = gl.isExtensionAvailable("GL_EXT_draw_instanced")
                || gl.isExtensionAvailable("GL_VERSION_3_1");

        _tessellationShadersSupported = gl.isExtensionAvailable("GL_ARB_tessellation_shader") && _glslSupported;

        if (_glslSupported) {
            gl.glGetIntegerv(GL2.GL_MAX_VERTEX_ATTRIBS_ARB, buf);
            _maxGLSLVertexAttribs = buf.get(0);
        }

        // Pbuffer
        _pbufferSupported = gl.isExtensionAvailable("GL_ARB_pixel_buffer_object");

        // FBO
        _fboSupported = gl.isExtensionAvailable("GL_EXT_framebuffer_object");
        if (_fboSupported) {

            _supportsFBOMultisample = gl.isExtensionAvailable("GL_EXT_framebuffer_multisample");
            _supportsFBOBlit = gl.isExtensionAvailable("GL_EXT_framebuffer_blit");

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
        } else {
            _maxFBOColorAttachments = 0;
        }

        _twoSidedStencilSupport = gl.isExtensionAvailable("GL_EXT_stencil_two_side");
        _stencilWrapSupport = gl.isExtensionAvailable("GL_EXT_stencil_wrap");

        // number of available auxiliary draw buffers
        gl.glGetIntegerv(GL2.GL_AUX_BUFFERS, buf);
        _numAuxDrawBuffers = buf.get(0);

        // max texture size.
        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, buf);
        _maxTextureSize = buf.get(0);

        // max texture size.
        gl.glGetIntegerv(GL.GL_MAX_RENDERBUFFER_SIZE, buf);
        _maxRenderBufferSize = buf.get(0);

        // max viewport size.
        gl.glGetIntegerv(GL.GL_MAX_VIEWPORT_DIMS, buf);
        _maxViewportWidth = buf.get(0);
        _maxViewportHeight = buf.get(1);

        // Check for support of multitextures.
        _supportsMultiTexture = gl.isExtensionAvailable("GL_ARB_multitexture");

        // Support for texture formats
        _supportsFloatTextures = gl.isExtensionAvailable("GL_ARB_texture_float");
        _supportsIntegerTextures = gl.isExtensionAvailable("GL_EXT_texture_integer");
        _supportsOneTwoComponentTextures = gl.isExtensionAvailable("GL_ARB_texture_rg");

        // Check for support of fixed function dot3 environment settings
        _supportsEnvDot3 = gl.isExtensionAvailable("GL_ARB_texture_env_dot3");

        // Check for support of fixed function dot3 environment settings
        _supportsEnvCombine = gl.isExtensionAvailable("GL_ARB_texture_env_combine");

        // Check for support of automatic mipmap generation
        _automaticMipMaps = gl.isExtensionAvailable("GL_SGIS_generate_mipmap");

        _supportsDepthTexture = gl.isExtensionAvailable("GL_ARB_depth_texture");
        _supportsShadow = gl.isExtensionAvailable("GL_ARB_shadow");

        // If we do support multitexturing, find out how many textures we
        // can handle.
        if (_supportsMultiTexture) {
            gl.glGetIntegerv(GL2ES1.GL_MAX_TEXTURE_UNITS, buf);
            _numFixedTexUnits = buf.get(0);
        } else {
            _numFixedTexUnits = 1;
        }

        // Go on to check number of texture units supported for vertex and
        // fragment shaders
        if (gl.isExtensionAvailable("GL_ARB_shader_objects") && gl.isExtensionAvailable("GL_ARB_vertex_shader")
                && gl.isExtensionAvailable("GL_ARB_fragment_shader")) {
            gl.glGetIntegerv(GL2ES2.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, buf);
            _numVertexTexUnits = buf.get(0);
            gl.glGetIntegerv(GL2ES2.GL_MAX_TEXTURE_IMAGE_UNITS, buf);
            _numFragmentTexUnits = buf.get(0);
            gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_COORDS, buf);
            _numFragmentTexCoordUnits = buf.get(0);
        } else {
            // based on nvidia dev doc:
            // http://developer.nvidia.com/object/General_FAQ.html#t6
            // "For GPUs that do not support GL_ARB_fragment_program and
            // GL_NV_fragment_program, those two limits are set equal to
            // GL_MAX_TEXTURE_UNITS."
            _numFragmentTexCoordUnits = _numFixedTexUnits;
            _numFragmentTexUnits = _numFixedTexUnits;

            // We'll set this to 0 for now since we do not know:
            _numVertexTexUnits = 0;
        }

        // Now determine the maximum number of supported texture units
        _numTotalTexUnits = Math.max(_numFragmentTexCoordUnits,
                Math.max(_numFixedTexUnits, Math.max(_numFragmentTexUnits, _numVertexTexUnits)));

        // Check for S3 texture compression capability.
        _supportsS3TCCompression = gl.isExtensionAvailable("GL_EXT_texture_compression_s3tc");

        // Check for LA texture compression capability.
        _supportsLATCCompression = gl.isExtensionAvailable("GL_EXT_texture_compression_latc");

        // Check for generic texture compression capability.
        _supportsGenericCompression = gl.isExtensionAvailable("GL_ARB_texture_compression");

        // Check for 3D texture capability.
        _supportsTexture3D = _supportsGL1_2;

        // Check for cubemap capability.
        _supportsTextureCubeMap = gl.isExtensionAvailable("GL_ARB_texture_cube_map");

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

        // See if we support textures that are not power of 2 in size.
        _supportsNonPowerTwo = gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two");

        // See if we support textures that do not have width == height.
        _supportsRectangular = gl.isExtensionAvailable("GL_ARB_texture_rectangle");

        _supportsMirroredRepeat = gl.isExtensionAvailable("GL_ARB_texture_mirrored_repeat");
        _supportsMirrorClamp = _supportsMirrorBorderClamp = _supportsMirrorEdgeClamp = gl
                .isExtensionAvailable("GL_EXT_texture_mirror_clamp");
        _supportsBorderClamp = gl.isExtensionAvailable("GL_ARB_texture_border_clamp");
        _supportsEdgeClamp = _supportsGL1_2;

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

        if (_glslSupported) {
            try {
                _shadingLanguageVersion = gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION);
            } catch (final Exception e) {
                _shadingLanguageVersion = "Unable to retrieve shading language version.";
            }
        } else {
            _shadingLanguageVersion = "Not supported.";
        }
    }
}
