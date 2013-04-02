/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.lwjgl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.EXTTextureLODBias;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.util.geom.BufferUtils;

public class LwjglContextCapabilities extends ContextCapabilities {

    public LwjglContextCapabilities(final ContextCapabilities caps) {
        super(caps);
    }

    public LwjglContextCapabilities(final org.lwjgl.opengl.ContextCapabilities caps) {
        final IntBuffer buf = BufferUtils.createIntBuffer(16);

        _supportsVBO = caps.GL_ARB_vertex_buffer_object;
        _supportsGL1_2 = caps.OpenGL12;
        _supportsMultisample = caps.GL_ARB_multisample;
        _supportsDoubleCoefficientsInClipPlaneEquation = true;

        _supportsConstantColor = _supportsEq = caps.GL_ARB_imaging;
        _supportsSeparateFunc = caps.GL_EXT_blend_func_separate;
        _supportsSeparateEq = caps.GL_EXT_blend_equation_separate;
        _supportsMinMax = caps.GL_EXT_blend_minmax;
        _supportsSubtract = caps.GL_EXT_blend_subtract;

        _supportsFogCoords = caps.GL_EXT_fog_coord;
        _supportsFragmentProgram = caps.GL_ARB_fragment_program;
        _supportsVertexProgram = caps.GL_ARB_vertex_program;

        _supportsPointSprites = caps.GL_ARB_point_sprite;
        _supportsPointParameters = caps.GL_ARB_point_parameters;

        _supportsTextureLodBias = caps.GL_EXT_texture_lod_bias;
        if (_supportsTextureLodBias) {
            GL11.glGetInteger(EXTTextureLODBias.GL_MAX_TEXTURE_LOD_BIAS_EXT, buf);
            _maxTextureLodBias = buf.get(0);
        } else {
            _maxTextureLodBias = 0f;
        }

        GL11.glGetInteger(GL11.GL_MAX_CLIP_PLANES, buf);
        _maxUserClipPlanes = buf.get(0);

        _glslSupported = caps.GL_ARB_shader_objects && caps.GL_ARB_fragment_shader && caps.GL_ARB_vertex_shader
                && caps.GL_ARB_shading_language_100;

        _geometryShader4Supported = caps.GL_ARB_geometry_shader4 && _glslSupported;

        _geometryInstancingSupported = caps.GL_EXT_draw_instanced || caps.OpenGL30;

        if (_glslSupported) {
            GL11.glGetInteger(ARBVertexShader.GL_MAX_VERTEX_ATTRIBS_ARB, buf);
            _maxGLSLVertexAttribs = buf.get(0);
        }

        // Pbuffer
        _pbufferSupported = caps.GL_ARB_pixel_buffer_object;

        // FBO
        _fboSupported = caps.GL_EXT_framebuffer_object;
        if (_fboSupported) {

            _supportsFBOMultisample = caps.GL_EXT_framebuffer_multisample;
            _supportsFBOBlit = caps.GL_EXT_framebuffer_blit;

            if (caps.GL_ARB_draw_buffers) {
                GL11.glGetInteger(EXTFramebufferObject.GL_MAX_COLOR_ATTACHMENTS_EXT, buf);
                _maxFBOColorAttachments = buf.get(0);
            } else {
                _maxFBOColorAttachments = 1;
            }

            // Max multisample samples.
            if (caps.GL_EXT_framebuffer_multisample && caps.GL_EXT_framebuffer_blit) {
                GL11.glGetInteger(EXTFramebufferMultisample.GL_MAX_SAMPLES_EXT, buf);
                _maxFBOSamples = buf.get(0);
            } else {
                _maxFBOSamples = 0;
            }
        } else {
            _maxFBOColorAttachments = 0;
        }

        _twoSidedStencilSupport = caps.GL_EXT_stencil_two_side;
        _stencilWrapSupport = caps.GL_EXT_stencil_wrap;

        // number of available auxiliary draw buffers
        GL11.glGetInteger(GL11.GL_AUX_BUFFERS, buf);
        _numAuxDrawBuffers = buf.get(0);

        // max texture size.
        GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE, buf);
        _maxTextureSize = buf.get(0);

        // Check for support of multitextures.
        _supportsMultiTexture = caps.GL_ARB_multitexture;

        // Support for texture formats
        _supportsFloatTextures = caps.GL_ARB_texture_float;
        _supportsIntegerTextures = caps.GL_EXT_texture_integer;
        _supportsOneTwoComponentTextures = caps.GL_ARB_texture_rg;

        // Check for support of fixed function dot3 environment settings
        _supportsEnvDot3 = caps.GL_ARB_texture_env_dot3;

        // Check for support of fixed function dot3 environment settings
        _supportsEnvCombine = caps.GL_ARB_texture_env_combine;

        // Check for support of automatic mipmap generation
        _automaticMipMaps = caps.GL_SGIS_generate_mipmap;

        _supportsDepthTexture = caps.GL_ARB_depth_texture;
        _supportsShadow = caps.GL_ARB_shadow;

        // If we do support multitexturing, find out how many textures we
        // can handle.
        if (_supportsMultiTexture) {
            GL11.glGetInteger(ARBMultitexture.GL_MAX_TEXTURE_UNITS_ARB, buf);
            _numFixedTexUnits = buf.get(0);
        } else {
            _numFixedTexUnits = 1;
        }

        // Go on to check number of texture units supported for vertex and
        // fragment shaders
        if (caps.GL_ARB_shader_objects && caps.GL_ARB_vertex_shader && caps.GL_ARB_fragment_shader) {
            GL11.glGetInteger(ARBVertexShader.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS_ARB, buf);
            _numVertexTexUnits = buf.get(0);
            GL11.glGetInteger(ARBFragmentShader.GL_MAX_TEXTURE_IMAGE_UNITS_ARB, buf);
            _numFragmentTexUnits = buf.get(0);
            GL11.glGetInteger(ARBFragmentShader.GL_MAX_TEXTURE_COORDS_ARB, buf);
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
        // ARBShaderObjects.GL_OBJECT_ACTIVE_UNIFORM_MAX_LENGTH_ARB
        // caps.GL_EXT_bindable_uniform
        // EXTBindableUniform.GL_MAX_BINDABLE_UNIFORM_SIZE_EXT;

        // Now determine the maximum number of supported texture units
        _numTotalTexUnits = Math.max(_numFragmentTexCoordUnits,
                Math.max(_numFixedTexUnits, Math.max(_numFragmentTexUnits, _numVertexTexUnits)));

        // Check for S3 texture compression capability.
        _supportsS3TCCompression = caps.GL_EXT_texture_compression_s3tc;

        // Check for LA texture compression capability.
        _supportsLATCCompression = caps.GL_EXT_texture_compression_latc;

        // Check for generic texture compression capability.
        _supportsGenericCompression = caps.GL_ARB_texture_compression;

        // Check for 3D texture capability.
        _supportsTexture3D = caps.OpenGL12;

        // Check for cubemap capability.
        _supportsTextureCubeMap = caps.GL_ARB_texture_cube_map;

        // See if we support anisotropic filtering
        _supportsAniso = caps.GL_EXT_texture_filter_anisotropic;

        if (_supportsAniso) {
            // Due to LWJGL buffer check, you can't use smaller sized
            // buffers (min_size = 16 for glGetFloat()).
            final FloatBuffer max_a = BufferUtils.createFloatBuffer(16);
            max_a.rewind();

            // Grab the maximum anisotropic filter.
            GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max_a);

            // set max.
            _maxAnisotropic = max_a.get(0);
        }

        // See if we support textures that are not power of 2 in size.
        _supportsNonPowerTwo = caps.GL_ARB_texture_non_power_of_two;

        // See if we support textures that do not have width == height.
        _supportsRectangular = caps.GL_ARB_texture_rectangle;

        _supportsMirroredRepeat = caps.GL_ARB_texture_mirrored_repeat;
        _supportsMirrorClamp = _supportsMirrorEdgeClamp = _supportsMirrorBorderClamp = caps.GL_EXT_texture_mirror_clamp;
        _supportsBorderClamp = caps.GL_ARB_texture_border_clamp;
        _supportsEdgeClamp = _supportsGL1_2;

        try {
            _displayVendor = GL11.glGetString(GL11.GL_VENDOR);
        } catch (final Exception e) {
            _displayVendor = "Unable to retrieve vendor.";
        }

        try {
            _displayRenderer = GL11.glGetString(GL11.GL_RENDERER);
        } catch (final Exception e) {
            _displayRenderer = "Unable to retrieve adapter details.";
        }

        try {
            _displayVersion = GL11.glGetString(GL11.GL_VERSION);
        } catch (final Exception e) {
            _displayVersion = "Unable to retrieve API version.";
        }

        try {
            _shadingLanguageVersion = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
        } catch (final Exception e) {
            _shadingLanguageVersion = "Unable to retrieve shading language version.";
        }
    }
}
