/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

public class ContextCapabilities {

    protected boolean _supportsVBO = false;
    protected boolean _supportsGL1_2 = false;
    protected boolean _supportsMultisample = false;

    protected boolean _supportsDoubleCoefficientsInClipPlaneEquation = false;

    protected boolean _supportsConstantColor = false;
    protected boolean _supportsEq = false;
    protected boolean _supportsSeparateEq = false;
    protected boolean _supportsSeparateFunc = false;
    protected boolean _supportsMinMax = false;
    protected boolean _supportsSubtract = false;

    protected boolean _supportsFogCoords = false;

    protected boolean _supportsPointSprites = false;
    protected boolean _supportsPointParameters = false;

    protected boolean _supportsTextureLodBias = false;
    protected float _maxTextureLodBias = 0f;

    protected boolean _supportsFragmentProgram = false;
    protected boolean _supportsVertexProgram = false;

    protected boolean _glslSupported = false;
    protected boolean _geometryShader4Supported = false;
    protected boolean _geometryInstancingSupported = false;
    protected boolean _tessellationShadersSupported = false;
    protected int _maxGLSLVertexAttribs;

    protected boolean _pbufferSupported = false;

    protected boolean _fboSupported = false;
    protected boolean _supportsFBOMultisample = false;
    protected boolean _supportsFBOBlit = false;
    protected int _maxFBOColorAttachments = 1;
    protected int _maxFBOSamples = 0;

    protected int _maxUserClipPlanes = 0;

    protected boolean _twoSidedStencilSupport = false;
    protected boolean _stencilWrapSupport = false;

    /** The total number of available auxiliary draw buffers. */
    protected int _numAuxDrawBuffers = -1;

    /** The total number of supported texture units. */
    protected int _numTotalTexUnits = -1;

    /** The number of texture units availible for fixed functionality */
    protected int _numFixedTexUnits = -1;

    /** The number of texture units availible to vertex shader */
    protected int _numVertexTexUnits = -1;

    /** The number of texture units availible to fragment shader */
    protected int _numFragmentTexUnits = -1;

    /** The number of texture coordinate sets available */
    protected int _numFragmentTexCoordUnits = -1;

    /** The max side of a texture supported. */
    protected int _maxTextureSize = -1;

    protected float _maxAnisotropic = -1.0f;

    /** True if multitexturing is supported. */
    protected boolean _supportsMultiTexture = false;

    /** True if floating point textures are supported. */
    protected boolean _supportsFloatTextures = false;

    /** True if integer textures are supported. */
    protected boolean _supportsIntegerTextures = false;

    /** True if Red and RedGreen only textures are supported. */
    protected boolean _supportsOneTwoComponentTextures = false;

    /** True if combine dot3 is supported. */
    protected boolean _supportsEnvDot3 = false;

    /** True if combine dot3 is supported. */
    protected boolean _supportsEnvCombine = false;

    /** True if anisofiltering is supported. */
    protected boolean _supportsAniso = false;

    /** True if non pow 2 texture sizes are supported. */
    protected boolean _supportsNonPowerTwo = false;

    /** True if rectangular textures are supported (vs. only square textures) */
    protected boolean _supportsRectangular = false;

    /** True if S3TC compression is supported. */
    protected boolean _supportsS3TCCompression = false;

    /** True if LATC compression is supported. */
    protected boolean _supportsLATCCompression = false;

    /** True if generic (non-specific) texture compression is supported. */
    protected boolean _supportsGenericCompression = false;

    /** True if Texture3D is supported. */
    protected boolean _supportsTexture3D = false;

    /** True if TextureCubeMap is supported. */
    protected boolean _supportsTextureCubeMap = false;

    /** True if non-GLU mipmap generation (part of FBO) is supported. */
    protected boolean _automaticMipMaps = false;

    /** True if depth textures are supported */
    protected boolean _supportsDepthTexture = false;

    /** True if shadow mapping supported */
    protected boolean _supportsShadow = false;

    protected boolean _supportsMirroredRepeat;
    protected boolean _supportsMirrorClamp;
    protected boolean _supportsMirrorBorderClamp;
    protected boolean _supportsMirrorEdgeClamp;
    protected boolean _supportsBorderClamp;
    protected boolean _supportsEdgeClamp;

    protected String _displayVendor;
    protected String _displayRenderer;
    protected String _displayVersion;
    protected String _shadingLanguageVersion;

    public ContextCapabilities() {}

    public ContextCapabilities(final ContextCapabilities source) {
        _automaticMipMaps = source._automaticMipMaps;
        _displayRenderer = source._displayRenderer;
        _displayVendor = source._displayVendor;
        _displayVersion = source._displayVersion;
        _fboSupported = source._fboSupported;
        _geometryShader4Supported = source._geometryShader4Supported;
        _glslSupported = source._glslSupported;
        _geometryInstancingSupported = source._geometryInstancingSupported;
        _tessellationShadersSupported = source._tessellationShadersSupported;
        _maxAnisotropic = source._maxAnisotropic;
        _maxFBOColorAttachments = source._maxFBOColorAttachments;
        _maxFBOSamples = source._maxFBOSamples;
        _maxGLSLVertexAttribs = source._maxGLSLVertexAttribs;
        _maxTextureLodBias = source._maxTextureLodBias;
        _maxTextureSize = source._maxTextureSize;
        _maxUserClipPlanes = source._maxUserClipPlanes;
        _numAuxDrawBuffers = source._numAuxDrawBuffers;
        _numFixedTexUnits = source._numFixedTexUnits;
        _numFragmentTexCoordUnits = source._numFragmentTexCoordUnits;
        _numFragmentTexUnits = source._numFragmentTexUnits;
        _numTotalTexUnits = source._numTotalTexUnits;
        _numVertexTexUnits = source._numVertexTexUnits;
        _pbufferSupported = source._pbufferSupported;
        _shadingLanguageVersion = source._shadingLanguageVersion;
        _stencilWrapSupport = source._stencilWrapSupport;
        _supportsAniso = source._supportsAniso;
        _supportsBorderClamp = source._supportsBorderClamp;
        _supportsConstantColor = source._supportsConstantColor;
        _supportsDepthTexture = source._supportsDepthTexture;
        _supportsEdgeClamp = source._supportsEdgeClamp;
        _supportsEnvCombine = source._supportsEnvCombine;
        _supportsEnvDot3 = source._supportsEnvDot3;
        _supportsEq = source._supportsEq;
        _supportsFogCoords = source._supportsFogCoords;
        _supportsFragmentProgram = source._supportsFragmentProgram;
        _supportsGenericCompression = source._supportsGenericCompression;
        _supportsGL1_2 = source._supportsGL1_2;
        _supportsLATCCompression = source._supportsLATCCompression;
        _supportsMinMax = source._supportsMinMax;
        _supportsMirrorBorderClamp = source._supportsMirrorBorderClamp;
        _supportsMirrorClamp = source._supportsMirrorClamp;
        _supportsMirrorEdgeClamp = source._supportsMirrorEdgeClamp;
        _supportsMirroredRepeat = source._supportsMirroredRepeat;
        _supportsMultisample = source._supportsMultisample;
        _supportsDoubleCoefficientsInClipPlaneEquation = source._supportsDoubleCoefficientsInClipPlaneEquation;
        _supportsMultiTexture = source._supportsMultiTexture;
        _supportsNonPowerTwo = source._supportsNonPowerTwo;
        _supportsPointParameters = source._supportsPointParameters;
        _supportsPointSprites = source._supportsPointSprites;
        _supportsRectangular = source._supportsRectangular;
        _supportsS3TCCompression = source._supportsS3TCCompression;
        _supportsSeparateEq = source._supportsSeparateEq;
        _supportsSeparateFunc = source._supportsSeparateFunc;
        _supportsShadow = source._supportsShadow;
        _supportsSubtract = source._supportsSubtract;
        _supportsTexture3D = source._supportsTexture3D;
        _supportsTextureCubeMap = source._supportsTextureCubeMap;
        _supportsTextureLodBias = source._supportsTextureLodBias;
        _supportsVBO = source._supportsVBO;
        _supportsVertexProgram = source._supportsVertexProgram;
        _twoSidedStencilSupport = source._twoSidedStencilSupport;
        _supportsFloatTextures = source._supportsFloatTextures;
        _supportsIntegerTextures = source._supportsIntegerTextures;
        _supportsOneTwoComponentTextures = source._supportsOneTwoComponentTextures;
    }

    /**
     * @return true if we support Vertex Buffer Objects.
     */
    public boolean isVBOSupported() {
        return _supportsVBO;
    }

    /**
     * @return true if we support all of OpenGL 1.2
     */
    public boolean isOpenGL1_2Supported() {
        return _supportsGL1_2;
    }

    /**
     * @return true if we support multisampling (antialiasing)
     */
    public boolean isMultisampleSupported() {
        return _supportsMultisample;
    }

    /**
     * @return true if double coefficients are supported in clip plane equation
     */
    public boolean areDoubleCoefficientsInClipPlaneEquationSupported() {
        return _supportsDoubleCoefficientsInClipPlaneEquation;
    }

    /**
     * @return true if we support fbo multisampling (antialiasing)
     */
    public boolean isFBOMultisampleSupported() {
        return _supportsFBOMultisample;
    }

    /**
     * @return true if we support fbo blitting
     */
    public boolean isFBOBlitSupported() {
        return _supportsFBOBlit;
    }

    /**
     * @return true if we support setting a constant color for use with *Constant* type BlendFunctions.
     */
    public boolean isConstantBlendColorSupported() {
        return _supportsConstantColor;
    }

    /**
     * @return true if we support setting rgb and alpha functions separately for source and destination.
     */
    public boolean isSeparateBlendFunctionsSupported() {
        return _supportsSeparateFunc;
    }

    /**
     * @return true if we support setting the blend equation
     */
    public boolean isBlendEquationSupported() {
        return _supportsEq;
    }

    /**
     * @return true if we support setting the blend equation for alpha and rgb separately
     */
    public boolean isSeparateBlendEquationsSupported() {
        return _supportsSeparateEq;
    }

    /**
     * @return true if we support using min and max blend equations
     */
    public boolean isMinMaxBlendEquationsSupported() {
        return _supportsMinMax;
    }

    /**
     * @return true if we support using subtract blend equations
     */
    public boolean isSubtractBlendEquationsSupported() {
        return _supportsSubtract;
    }

    /**
     * @return true if mesh based fog coords are supported
     */
    public boolean isFogCoordinatesSupported() {
        return _supportsFogCoords;
    }

    /**
     * @return true if point sprites are supported
     */
    public boolean isPointSpritesSupported() {
        return _supportsPointSprites;
    }

    /**
     * @return true if point parameters are supported
     */
    public boolean isPointParametersSupported() {
        return _supportsPointParameters;
    }

    /**
     * @return true if texture lod bias is supported
     */
    public boolean isTextureLodBiasSupported() {
        return _supportsTextureLodBias;
    }

    /**
     * @return the max amount of texture lod bias that this context supports.
     */
    public float getMaxLodBias() {
        return _maxTextureLodBias;
    }

    /**
     * @return <code>true</code> if the GLSL is supported and GL_ARB_tessellation_shader is supported by current
     *         graphics configuration
     */
    public boolean isTessellationShadersSupported() {
        return _tessellationShadersSupported;
    }

    /**
     * @return true if the ARB_shader_objects extension is supported by current graphics configuration.
     */
    public boolean isGLSLSupported() {
        return _glslSupported;
    }

    /**
     * @return true if the GLSL is supported and GL_EXT_draw_instanced is supported by the current graphics
     *         configuration configuration.
     */
    public boolean isGeometryInstancingSupported() {
        return _geometryInstancingSupported;
    }

    /**
     * @return true if the GLSL is supported and ARB_geometry_shader4 extension is supported by current graphics
     *         configuration.
     */
    public boolean isGeometryShader4Supported() {
        return _geometryShader4Supported;
    }

    /**
     * @return true if the ARB_shader_objects extension is supported by current graphics configuration.
     */
    public boolean isPbufferSupported() {
        return _pbufferSupported;
    }

    /**
     * @return true if the EXT_framebuffer_object extension is supported by current graphics configuration.
     */
    public boolean isFBOSupported() {
        return _fboSupported;
    }

    /**
     * @return true if we can handle doing separate stencil operations for front and back facing polys in a single pass.
     */
    public boolean isTwoSidedStencilSupported() {
        return _twoSidedStencilSupport;
    }

    /**
     * @return true if we can handle wrapping increment/decrement operations.
     */
    public boolean isStencilWrapSupported() {
        return _stencilWrapSupport;
    }

    /**
     * <code>getNumberOfAuxiliaryDrawBuffers</code> returns the total number of available auxiliary draw buffers this
     * context supports.
     * 
     * @return the number of available auxiliary draw buffers supported by the context.
     */
    public int getNumberOfAuxiliaryDrawBuffers() {
        return _numAuxDrawBuffers;
    }

    /**
     * <code>getTotalNumberOfUnits</code> returns the total number of texture units this context supports.
     * 
     * @return the total number of texture units supported by the context.
     */
    public int getTotalNumberOfUnits() {
        return _numTotalTexUnits;
    }

    /**
     * <code>getNumberOfFixedUnits</code> returns the number of texture units this context supports, for use in the
     * fixed pipeline.
     * 
     * @return the number units.
     */
    public int getNumberOfFixedTextureUnits() {
        return _numFixedTexUnits;
    }

    /**
     * <code>getNumberOfVertexUnits</code> returns the number of texture units available to a vertex shader that this
     * context supports.
     * 
     * @return the number of units.
     */
    public int getNumberOfVertexUnits() {
        return _numVertexTexUnits;
    }

    /**
     * <code>getNumberOfFragmentUnits</code> returns the number of texture units available to a fragment shader that
     * this context supports.
     * 
     * @return the number of units.
     */
    public int getNumberOfFragmentTextureUnits() {
        return _numFragmentTexUnits;
    }

    /**
     * <code>getNumberOfFragmentTexCoordUnits</code> returns the number of texture coordinate sets available that this
     * context supports.
     * 
     * @return the number of units.
     */
    public int getNumberOfFragmentTexCoordUnits() {
        return _numFragmentTexCoordUnits;
    }

    /**
     * @return the max size of texture (in terms of # pixels wide) that this context supports.
     */
    public int getMaxTextureSize() {
        return _maxTextureSize;
    }

    /**
     * <code>getNumberOfTotalUnits</code> returns the number of texture units this context supports.
     * 
     * @return the number of units.
     */
    public int getNumberOfTotalTextureUnits() {
        return _numTotalTexUnits;
    }

    /**
     * <code>getMaxFBOColorAttachments</code> returns the MAX_COLOR_ATTACHMENTS for FBOs that this context supports.
     * 
     * @return the number of buffers.
     */
    public int getMaxFBOColorAttachments() {
        return _maxFBOColorAttachments;
    }

    /**
     * Returns the maximum anisotropic filter.
     * 
     * @return The maximum anisotropic filter.
     */
    public float getMaxAnisotropic() {
        return _maxAnisotropic;
    }

    /**
     * @return true if multi-texturing is supported in fixed function
     */
    public boolean isMultitextureSupported() {
        return _supportsMultiTexture;
    }

    /**
     * @return true if floating point textures are supported by this context.
     */
    public boolean isFloatingPointTexturesSupported() {
        return _supportsFloatTextures;
    }

    /**
     * @return true if integer textures are supported by this context.
     */
    public boolean isIntegerTexturesSupported() {
        return _supportsIntegerTextures;
    }

    /**
     * @return true if one- and two-component textures are supported by this context.
     */
    public boolean isOneTwoComponentTexturesSupported() {
        return _supportsOneTwoComponentTextures;
    }

    /**
     * @return true we support dot3 environment texture settings
     */
    public boolean isEnvDot3TextureCombineSupported() {
        return _supportsEnvDot3;
    }

    /**
     * @return true we support combine environment texture settings
     */
    public boolean isEnvCombineSupported() {
        return _supportsEnvCombine;
    }

    /**
     * Returns if S3TC compression is available for textures.
     * 
     * @return true if S3TC is available.
     */
    public boolean isS3TCSupported() {
        return _supportsS3TCCompression;
    }

    /**
     * Returns if LATC compression is available for textures.
     * 
     * @return true if LATC is available.
     */
    public boolean isLATCSupported() {
        return _supportsLATCCompression;
    }

    /**
     * Returns if generic (non-specific) compression is available for textures.
     * 
     * @return true if available.
     */
    public boolean isGenericTCSupported() {
        return _supportsGenericCompression;
    }

    /**
     * Returns if Texture3D is available for textures.
     * 
     * @return true if Texture3D is available.
     */
    public boolean isTexture3DSupported() {
        return _supportsTexture3D;
    }

    /**
     * Returns if TextureCubeMap is available for textures.
     * 
     * @return true if TextureCubeMap is available.
     */
    public boolean isTextureCubeMapSupported() {
        return _supportsTextureCubeMap;
    }

    /**
     * Returns if AutomaticMipmap generation is available for textures.
     * 
     * @return true if AutomaticMipmap generation is available.
     */
    public boolean isAutomaticMipmapsSupported() {
        return _automaticMipMaps;
    }

    /**
     * @return if Anisotropic texture filtering is supported
     */
    public boolean isAnisoSupported() {
        return _supportsAniso;
    }

    /**
     * @return true if non pow 2 texture sizes are supported
     */
    public boolean isNonPowerOfTwoTextureSupported() {
        return _supportsNonPowerTwo;
    }

    /**
     * @return true if rectangular texture sizes are supported (width != height)
     */
    public boolean isRectangularTextureSupported() {
        return _supportsRectangular;
    }

    public boolean isFragmentProgramSupported() {
        return _supportsFragmentProgram;
    }

    public boolean isVertexProgramSupported() {
        return _supportsVertexProgram;
    }

    public int getMaxGLSLVertexAttributes() {
        return _maxGLSLVertexAttribs;
    }

    public boolean isDepthTextureSupported() {
        return _supportsDepthTexture;
    }

    public boolean isARBShadowSupported() {
        return _supportsShadow;
    }

    public boolean isTextureMirroredRepeatSupported() {
        return _supportsMirroredRepeat;
    }

    public boolean isTextureMirrorClampSupported() {
        return _supportsMirrorClamp;
    }

    public boolean isTextureMirrorEdgeClampSupported() {
        return _supportsMirrorEdgeClamp;
    }

    public boolean isTextureMirrorBorderClampSupported() {
        return _supportsMirrorBorderClamp;
    }

    public boolean isTextureBorderClampSupported() {
        return _supportsBorderClamp;
    }

    public boolean isTextureEdgeClampSupported() {
        return _supportsEdgeClamp;
    }

    public int getMaxFBOSamples() {
        return _maxFBOSamples;
    }

    public int getMaxUserClipPlanes() {
        return _maxUserClipPlanes;
    }

    /**
     * Returns the vendor of the graphics adapter
     * 
     * @return The vendor of the graphics adapter
     */
    public String getDisplayVendor() {
        return _displayVendor;
    }

    /**
     * Returns renderer details of the adapter
     * 
     * @return The adapter details
     */
    public String getDisplayRenderer() {
        return _displayRenderer;
    }

    /**
     * Returns the version supported
     * 
     * @return The version supported
     */
    public String getDisplayVersion() {
        return _displayVersion;
    }

    /**
     * Returns the supported shading language version. Needs OpenGL 2.0 support to query.
     * 
     * @return The shading language version supported
     */
    public String getShadingLanguageVersion() {
        return _shadingLanguageVersion;
    }
}
