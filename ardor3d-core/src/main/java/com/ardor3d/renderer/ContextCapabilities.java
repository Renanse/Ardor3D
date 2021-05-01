/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer;

public class ContextCapabilities {
  protected float _maxTextureLodBias = 0f;

  protected boolean _tessellationShadersSupported = false;

  protected int _maxGLSLVertexAttribs;

  protected int _maxFBOColorAttachments = 1;
  protected int _maxFBOSamples = 0;

  /** The total number of supported texture units. */
  protected int _numTotalTexUnits = -1;

  /** The number of texture units availible to vertex shader */
  protected int _numVertexTexUnits = -1;

  /** The number of texture units availible to fragment shader */
  protected int _numFragmentTexUnits = -1;

  /** The max side of a texture supported. */
  protected int _maxTextureSize = -1;

  /** The max width of a viewport supported. */
  protected int _maxViewportWidth = -1;

  /** The max height of a viewport supported. */
  protected int _maxViewportHeight = -1;

  /** The max edge of a renderbuffer supported. */
  protected int _maxRenderBufferSize = -1;

  protected float _maxAnisotropic = -1.0f;

  /** True if anisofiltering is supported. */
  protected boolean _supportsAniso = false;

  /** True if S3TC compression is supported. */
  protected boolean _supportsS3TCCompression = false;

  /** True if LATC compression is supported. */
  protected boolean _supportsLATCCompression = false;

  protected String _displayVendor;
  protected String _displayRenderer;
  protected String _displayVersion;
  protected String _shadingLanguageVersion;

  public ContextCapabilities() {}

  public ContextCapabilities(final ContextCapabilities source) {
    _displayRenderer = source._displayRenderer;
    _displayVendor = source._displayVendor;
    _displayVersion = source._displayVersion;
    _tessellationShadersSupported = source._tessellationShadersSupported;
    _maxAnisotropic = source._maxAnisotropic;
    _maxFBOColorAttachments = source._maxFBOColorAttachments;
    _maxFBOSamples = source._maxFBOSamples;
    _maxGLSLVertexAttribs = source._maxGLSLVertexAttribs;
    _maxTextureLodBias = source._maxTextureLodBias;
    _maxTextureSize = source._maxTextureSize;
    _maxViewportWidth = source._maxViewportWidth;
    _maxViewportHeight = source._maxViewportHeight;
    _maxRenderBufferSize = source._maxRenderBufferSize;
    _numFragmentTexUnits = source._numFragmentTexUnits;
    _numTotalTexUnits = source._numTotalTexUnits;
    _numVertexTexUnits = source._numVertexTexUnits;
    _shadingLanguageVersion = source._shadingLanguageVersion;
    _supportsAniso = source._supportsAniso;
    _supportsLATCCompression = source._supportsLATCCompression;
    _supportsS3TCCompression = source._supportsS3TCCompression;
  }

  /**
   * @return the max amount of texture lod bias that this context supports.
   */
  public float getMaxLodBias() { return _maxTextureLodBias; }

  /**
   * @return <code>true</code> if the GLSL is supported and GL_ARB_tessellation_shader is supported by
   *         current graphics configuration
   */
  public boolean isTessellationShadersSupported() { return _tessellationShadersSupported; }

  /**
   * <code>getTotalNumberOfUnits</code> returns the total number of texture units this context
   * supports.
   *
   * @return the total number of texture units supported by the context.
   */
  public int getTotalNumberOfUnits() { return _numTotalTexUnits; }

  /**
   * <code>getNumberOfVertexUnits</code> returns the number of texture units available to a vertex
   * shader that this context supports.
   *
   * @return the number of units.
   */
  public int getNumberOfVertexUnits() { return _numVertexTexUnits; }

  /**
   * <code>getNumberOfFragmentUnits</code> returns the number of texture units available to a fragment
   * shader that this context supports.
   *
   * @return the number of units.
   */
  public int getNumberOfFragmentTextureUnits() { return _numFragmentTexUnits; }

  /**
   * @return the max size of texture (in terms of # pixels wide) that this context supports.
   */
  public int getMaxTextureSize() { return _maxTextureSize; }

  /**
   * @return the max width of viewport that this context supports.
   */
  public int getMaxViewportWidth() { return _maxViewportWidth; }

  /**
   * @return the max height of viewport that this context supports.
   */
  public int getMaxViewportHeight() { return _maxViewportHeight; }

  /**
   * @return the max size (in terms of # pixels) of renderbuffer that this context supports.
   */
  public int getMaxRenderBufferSize() { return _maxRenderBufferSize; }

  /**
   * <code>getNumberOfTotalUnits</code> returns the number of texture units this context supports.
   *
   * @return the number of units.
   */
  public int getNumberOfTotalTextureUnits() { return _numTotalTexUnits; }

  /**
   * <code>getMaxFBOColorAttachments</code> returns the MAX_COLOR_ATTACHMENTS for FBOs that this
   * context supports.
   *
   * @return the number of buffers.
   */
  public int getMaxFBOColorAttachments() { return _maxFBOColorAttachments; }

  /**
   * Returns the maximum anisotropic filter.
   *
   * @return The maximum anisotropic filter.
   */
  public float getMaxAnisotropic() { return _maxAnisotropic; }

  /**
   * Returns if S3TC compression is available for textures.
   *
   * @return true if S3TC is available.
   */
  public boolean isS3TCSupported() { return _supportsS3TCCompression; }

  /**
   * Returns if LATC compression is available for textures.
   *
   * @return true if LATC is available.
   */
  public boolean isLATCSupported() { return _supportsLATCCompression; }

  /**
   * @return if Anisotropic texture filtering is supported
   */
  public boolean isAnisoSupported() { return _supportsAniso; }

  public int getMaxGLSLVertexAttributes() { return _maxGLSLVertexAttribs; }

  public int getMaxFBOSamples() { return _maxFBOSamples; }

  /**
   * Returns the vendor of the graphics adapter
   *
   * @return The vendor of the graphics adapter
   */
  public String getDisplayVendor() { return _displayVendor; }

  /**
   * Returns renderer details of the adapter
   *
   * @return The adapter details
   */
  public String getDisplayRenderer() { return _displayRenderer; }

  /**
   * Returns the version supported
   *
   * @return The version supported
   */
  public String getDisplayVersion() { return _displayVersion; }

  /**
   * Returns the supported shading language version. Needs OpenGL 2.0 support to query.
   *
   * @return The shading language version supported
   */
  public String getShadingLanguageVersion() { return _shadingLanguageVersion; }
}
