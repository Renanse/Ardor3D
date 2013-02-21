/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.bloom;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.pass.Pass;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * GLSL bloom effect pass. - Render supplied source to a texture - Extract intensity - Blur intensity - Blend with first
 * pass
 */
public class BloomRenderPass extends Pass {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(BloomRenderPass.class.getName());

    private static final long serialVersionUID = 1L;

    private double throttle = 0;
    private double sinceLast = 1;

    private TextureRenderer tRenderer = null;
    private TextureRenderer fullTRenderer = null;
    private Texture2D mainTexture = null;
    private Texture2D secondTexture = null;
    private Texture2D screenTexture = null;

    private Quad fullScreenQuad = null;

    private GLSLShaderObjectsState extractionShader = null;
    private GLSLShaderObjectsState blurShader = null;
    private GLSLShaderObjectsState blurShaderHorizontal = null;
    private GLSLShaderObjectsState blurShaderVertical = null;
    private GLSLShaderObjectsState finalShader = null;

    private final Camera cam;
    private final int renderScale;

    private int nrBlurPasses;
    private float blurSize;
    private float blurIntensityMultiplier;
    private float exposurePow;
    private float exposureCutoff;
    private boolean supported = true;
    private boolean useCurrentScene = false;

    private boolean useSeparateConvolution = false;

    public static String shaderDirectory = "com/ardor3d/extension/effect/bloom/";

    private boolean initialized = false;

    /**
     * Reset bloom parameters to default
     */
    public void resetParameters() {
        nrBlurPasses = 2;
        blurSize = 0.02f;
        blurIntensityMultiplier = 1.3f;
        exposurePow = 3.0f;
        exposureCutoff = 0.0f;
    }

    /**
     * Release pbuffers in TextureRenderer's. Preferably called from user cleanup method.
     */
    @Override
    public void cleanUp() {
        super.cleanUp();
        if (tRenderer != null) {
            tRenderer.cleanup();
        }
        if (fullTRenderer != null) {
            fullTRenderer.cleanup();
        }
    }

    public boolean isSupported() {
        return supported;
    }

    /**
     * Creates a new bloom renderpass
     * 
     * @param cam
     *            Camera used for rendering the bloomsource
     * @param renderScale
     *            Scale of bloom texture
     */
    public BloomRenderPass(final Camera cam, final int renderScale) {
        this.cam = cam;
        this.renderScale = renderScale;
        resetParameters();
    }

    @Override
    protected void doUpdate(final double tpf) {
        super.doUpdate(tpf);
        sinceLast += tpf;
    }

    @Override
    public void doRender(final Renderer r) {
        if (!initialized) {
            doInit(r);
        }

        if (!isSupported() || !useCurrentScene && _spatials.size() == 0) {
            return;
        }

        final BlendState blend = (BlendState) fullScreenQuad.getWorldRenderState(RenderState.StateType.Blend);

        if (sinceLast > throttle) {
            sinceLast = 0;

            tRenderer.getCamera().setLocation(cam.getLocation());
            tRenderer.getCamera().setDirection(cam.getDirection());
            tRenderer.getCamera().setUp(cam.getUp());
            tRenderer.getCamera().setLeft(cam.getLeft());

            blend.setEnabled(false);
            final TextureState ts = (TextureState) fullScreenQuad.getWorldRenderState(RenderState.StateType.Texture);

            // see if we should use the current scene to bloom, or only things added to the pass.
            if (useCurrentScene) {
                // grab backbuffer to texture
                if (screenTexture == null) {
                    final DisplaySettings settings = new DisplaySettings(cam.getWidth(), cam.getHeight(), 24, 0, 0, 8,
                            0, 0, false, false);
                    fullTRenderer = TextureRendererFactory.INSTANCE.createTextureRenderer(settings, false, r,
                            ContextManager.getCurrentContext().getCapabilities());
                    screenTexture = new Texture2D();
                    screenTexture.setWrap(Texture.WrapMode.Clamp);
                    screenTexture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
                    fullTRenderer.setupTexture(screenTexture);
                }
                fullTRenderer.copyToTexture(screenTexture, 0, 0, cam.getWidth(), cam.getHeight(), 0, 0);
                ts.setTexture(screenTexture, 0);
            } else {
                // Render scene to texture
                tRenderer.render(_spatials, mainTexture, Renderer.BUFFER_COLOR_AND_DEPTH);
                ts.setTexture(mainTexture, 0);
            }

            // Extract intensity
            extractionShader.setUniform("exposurePow", getExposurePow());
            extractionShader.setUniform("exposureCutoff", getExposureCutoff());

            fullScreenQuad.setRenderState(extractionShader);
            fullScreenQuad.updateWorldRenderStates(false);
            // fullScreenQuad.states[RenderState.StateType.GLSLShaderObjects.ordinal()] = extractionShader;
            tRenderer.render(fullScreenQuad, secondTexture, Renderer.BUFFER_NONE);

            if (!useSeparateConvolution) {
                blurShader.setUniform("sampleDist", getBlurSize());
                blurShader.setUniform("blurIntensityMultiplier", getBlurIntensityMultiplier());

                ts.setTexture(secondTexture, 0);
                fullScreenQuad.setRenderState(blurShader);
                fullScreenQuad.updateWorldRenderStates(false);
                // fullScreenQuad.states[RenderState.StateType.GLSLShaderObjects.ordinal()] = blurShader;
                tRenderer.render(fullScreenQuad, mainTexture, Renderer.BUFFER_NONE);

                // Extra blur passes
                for (int i = 1; i < getNrBlurPasses(); i++) {
                    blurShader.setUniform("sampleDist", getBlurSize() - i * getBlurSize() / getNrBlurPasses());
                    if (i % 2 == 1) {
                        ts.setTexture(mainTexture, 0);
                        tRenderer.render(fullScreenQuad, secondTexture, Renderer.BUFFER_NONE);
                    } else {
                        ts.setTexture(secondTexture, 0);
                        tRenderer.render(fullScreenQuad, mainTexture, Renderer.BUFFER_NONE);
                    }
                }
                if (getNrBlurPasses() % 2 == 1) {
                    ts.setTexture(mainTexture, 0);
                } else {
                    ts.setTexture(secondTexture, 0);
                    tRenderer.render(fullScreenQuad, mainTexture, Renderer.BUFFER_NONE);
                    ts.setTexture(mainTexture, 0);
                }
            } else {
                blurShaderVertical.setUniform("blurIntensityMultiplier", getBlurIntensityMultiplier());

                for (int i = 0; i < getNrBlurPasses(); i++) {
                    blurShaderHorizontal
                            .setUniform("sampleDist", getBlurSize() - i * getBlurSize() / getNrBlurPasses());
                    blurShaderVertical.setUniform("sampleDist", getBlurSize() - i * getBlurSize() / getNrBlurPasses());

                    ts.setTexture(secondTexture, 0);
                    fullScreenQuad.setRenderState(blurShaderHorizontal);
                    fullScreenQuad.updateWorldRenderStates(false);
                    // fullScreenQuad.states[RenderState.StateType.GLSLShaderObjects.ordinal()] = blurShaderHorizontal;
                    tRenderer.render(fullScreenQuad, mainTexture, Renderer.BUFFER_NONE);
                    ts.setTexture(mainTexture, 0);
                    fullScreenQuad.setRenderState(blurShaderVertical);
                    fullScreenQuad.updateWorldRenderStates(false);
                    // fullScreenQuad.states[RenderState.StateType.GLSLShaderObjects.ordinal()] = blurShaderVertical;
                    tRenderer.render(fullScreenQuad, secondTexture, Renderer.BUFFER_NONE);
                }
                ts.setTexture(secondTexture, 0);
            }
        }

        // Final blend
        blend.setEnabled(true);

        fullScreenQuad.setRenderState(finalShader);
        fullScreenQuad.updateWorldRenderStates(false);
        // fullScreenQuad.states[RenderState.StateType.GLSLShaderObjects.ordinal()] = finalShader;
        r.draw((Renderable) fullScreenQuad);
    }

    private void doInit(final Renderer r) {
        initialized = true;

        cleanUp();

        // Test for glsl support
        final ContextCapabilities caps = ContextManager.getCurrentContext().getCapabilities();
        if (!caps.isGLSLSupported() || !(caps.isPbufferSupported() || caps.isFBOSupported())) {
            supported = false;
            return;
        }

        // Create texture renderers and rendertextures(alternating between two not to overwrite pbuffers)
        final DisplaySettings settings = new DisplaySettings(cam.getWidth() / renderScale, cam.getHeight()
                / renderScale, 24, 0, 0, 8, 0, 0, false, false);
        tRenderer = TextureRendererFactory.INSTANCE.createTextureRenderer(settings, false, r, ContextManager
                .getCurrentContext().getCapabilities());

        if (tRenderer == null) {
            supported = false;
            return;
        }
        tRenderer.setMultipleTargets(true);
        tRenderer.setBackgroundColor(new ColorRGBA(0.0f, 0.0f, 0.0f, 1.0f));
        tRenderer.getCamera().setFrustum(cam.getFrustumNear(), cam.getFrustumFar(), cam.getFrustumLeft(),
                cam.getFrustumRight(), cam.getFrustumTop(), cam.getFrustumBottom());

        mainTexture = new Texture2D();
        mainTexture.setWrap(Texture.WrapMode.Clamp);
        mainTexture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
        tRenderer.setupTexture(mainTexture);

        secondTexture = new Texture2D();
        secondTexture.setWrap(Texture.WrapMode.Clamp);
        secondTexture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
        tRenderer.setupTexture(secondTexture);

        extractionShader = new GLSLShaderObjectsState();
        try {
            extractionShader.setVertexShader(ResourceLocatorTool.getClassPathResourceAsStream(BloomRenderPass.class,
                    shaderDirectory + "bloom_extract.vert"));
            extractionShader.setFragmentShader(ResourceLocatorTool.getClassPathResourceAsStream(BloomRenderPass.class,
                    shaderDirectory + "bloom_extract.frag"));
        } catch (final IOException ex) {
            logger.logp(Level.SEVERE, getClass().getName(), "init(Renderer)", "Could not load shaders.", ex);
        }
        extractionShader.setUniform("RT", 0);

        // Create blur shader
        blurShader = new GLSLShaderObjectsState();
        try {
            blurShader.setVertexShader(ResourceLocatorTool.getClassPathResourceAsStream(BloomRenderPass.class,
                    shaderDirectory + "bloom_blur.vert"));
            blurShader.setFragmentShader(ResourceLocatorTool.getClassPathResourceAsStream(BloomRenderPass.class,
                    shaderDirectory + "bloom_blur.frag"));
        } catch (final IOException ex) {
            logger.logp(Level.SEVERE, getClass().getName(), "init(Renderer)", "Could not load shaders.", ex);
        }
        blurShader.setUniform("RT", 0);

        // Create blur shader horizontal
        blurShaderHorizontal = new GLSLShaderObjectsState();
        try {
            blurShaderHorizontal.setVertexShader(ResourceLocatorTool.getClassPathResourceAsStream(
                    BloomRenderPass.class, shaderDirectory + "bloom_blur.vert"));
            blurShaderHorizontal.setFragmentShader(ResourceLocatorTool.getClassPathResourceAsStream(
                    BloomRenderPass.class, shaderDirectory + "bloom_blur_horizontal7.frag"));
        } catch (final IOException ex) {
            logger.logp(Level.SEVERE, getClass().getName(), "init(Renderer)", "Could not load shaders.", ex);
        }
        blurShaderHorizontal.setUniform("RT", 0);

        // Create blur shader vertical
        blurShaderVertical = new GLSLShaderObjectsState();
        try {
            blurShaderVertical.setVertexShader(ResourceLocatorTool.getClassPathResourceAsStream(BloomRenderPass.class,
                    shaderDirectory + "bloom_blur.vert"));
            blurShaderVertical.setFragmentShader(ResourceLocatorTool.getClassPathResourceAsStream(
                    BloomRenderPass.class, shaderDirectory + "bloom_blur_vertical7.frag"));
        } catch (final IOException ex) {
            logger.logp(Level.SEVERE, getClass().getName(), "init(Renderer)", "Could not load shaders.", ex);
        }
        blurShaderVertical.setUniform("RT", 0);

        // Create final shader(basic texturing)
        finalShader = new GLSLShaderObjectsState();
        try {
            finalShader.setVertexShader(ResourceLocatorTool.getClassPathResourceAsStream(BloomRenderPass.class,
                    shaderDirectory + "bloom_final.vert"));
            finalShader.setFragmentShader(ResourceLocatorTool.getClassPathResourceAsStream(BloomRenderPass.class,
                    shaderDirectory + "bloom_final.frag"));
        } catch (final IOException ex) {
            logger.logp(Level.SEVERE, getClass().getName(), "init(Renderer)", "Could not load shaders.", ex);
        }

        // Create fullscreen quad
        fullScreenQuad = new Quad("FullScreenQuad", cam.getWidth() / 4, cam.getHeight() / 4);
        fullScreenQuad.setTranslation(cam.getWidth() / 2, cam.getHeight() / 2, 0);
        fullScreenQuad.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);

        fullScreenQuad.getSceneHints().setCullHint(CullHint.Never);
        fullScreenQuad.getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
        fullScreenQuad.getSceneHints().setLightCombineMode(LightCombineMode.Off);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        fullScreenQuad.setRenderState(ts);

        final BlendState as = new BlendState();
        as.setBlendEnabled(true);
        as.setSourceFunction(BlendState.SourceFunction.One);
        as.setDestinationFunction(BlendState.DestinationFunction.One);
        as.setEnabled(true);
        fullScreenQuad.setRenderState(as);

        fullScreenQuad.updateGeometricState(0.0f, true);
    }

    /**
     * @return The throttle amount - or in other words, how much time in seconds must pass before the bloom effect is
     *         updated.
     */
    public double getThrottle() {
        return throttle;
    }

    /**
     * @param throttle
     *            The throttle amount - or in other words, how much time in seconds must pass before the bloom effect is
     *            updated.
     */
    public void setThrottle(final float throttle) {
        this.throttle = throttle;
    }

    public float getBlurSize() {
        return blurSize;
    }

    public void setBlurSize(final float blurSize) {
        this.blurSize = blurSize;
    }

    public float getExposurePow() {
        return exposurePow;
    }

    public void setExposurePow(final float exposurePow) {
        this.exposurePow = exposurePow;
    }

    public float getExposureCutoff() {
        return exposureCutoff;
    }

    public void setExposureCutoff(final float exposureCutoff) {
        this.exposureCutoff = exposureCutoff;
    }

    public float getBlurIntensityMultiplier() {
        return blurIntensityMultiplier;
    }

    public void setBlurIntensityMultiplier(final float blurIntensityMultiplier) {
        this.blurIntensityMultiplier = blurIntensityMultiplier;
    }

    public int getNrBlurPasses() {
        return nrBlurPasses;
    }

    public void setNrBlurPasses(final int nrBlurPasses) {
        this.nrBlurPasses = nrBlurPasses;
    }

    public boolean useCurrentScene() {
        return useCurrentScene;
    }

    public void setUseCurrentScene(final boolean useCurrentScene) {
        this.useCurrentScene = useCurrentScene;
    }

    public void setUseSeparateConvolution(final boolean useSeparateConvolution) {
        this.useSeparateConvolution = useSeparateConvolution;
    }

    public boolean isUseSeparateConvolution() {
        return useSeparateConvolution;
    }

    public void markNeedsRefresh() {
        initialized = false;
    }
}
