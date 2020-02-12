/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.stat.graph;

import java.nio.FloatBuffer;

import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture2D;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.stat.StatCollector;

/**
 * Factory class useful for setting up various types of graphs.
 */
public abstract class GraphFactory {

    /**
     * Makes a new line grapher and sets up a quad to display it.
     *
     * @param width
     *            the width in pixels of the graph
     * @param height
     *            the height in pixels of the graph
     * @param quad
     *            the quad on whose surface we'll display our graph.
     * @param renderer
     *            the Renderer to use
     * @return the new LineGrapher
     */
    public static LineGrapher makeLineGraph(final int width, final int height, final Quad quad,
            final Renderer renderer) {
        final LineGrapher grapher = new LineGrapher(width, height, renderer);
        grapher.setThreshold(1);
        StatCollector.addStatListener(grapher);
        final Texture2D graphTex = setupGraphTexture(grapher);

        final float dW = (float) width / grapher._textureRenderer.getWidth();
        final float dH = (float) height / grapher._textureRenderer.getHeight();

        setupGraphQuad(quad, graphTex, dW, dH);

        return grapher;
    }

    /**
     * Makes a new area grapher and sets up a quad to display it.
     *
     * @param width
     *            the width in pixels of the graph
     * @param height
     *            the height in pixels of the graph
     * @param quad
     *            the quad on whose surface we'll display our graph.
     * @param renderer
     *            the Renderer to use
     * @return the new TimedAreaGrapher
     */
    public static TimedAreaGrapher makeTimedGraph(final int width, final int height, final Quad quad,
            final Renderer renderer) {
        final TimedAreaGrapher grapher = new TimedAreaGrapher(width, height, renderer);
        grapher.setThreshold(1);
        StatCollector.addStatListener(grapher);
        final Texture2D graphTex = setupGraphTexture(grapher);
        final float dW = (float) width / grapher._textureRenderer.getWidth();
        final float dH = (float) height / grapher._textureRenderer.getHeight();

        setupGraphQuad(quad, graphTex, dW, dH);

        return grapher;
    }

    /**
     * Makes a new label grapher and sets up a quad to display it.
     *
     * @param width
     *            the width in pixels of the graph
     * @param height
     *            the height in pixels of the graph
     * @param quad
     *            the quad on whose surface we'll display our graph.
     * @param renderer
     *            the Renderer to use
     * @return the new TabledLabelGrapher
     */
    public static TabledLabelGrapher makeTabledLabelGraph(final int width, final int height, final Quad quad,
            final Renderer renderer) {
        final TabledLabelGrapher grapher = new TabledLabelGrapher(width, height, renderer);
        grapher.setThreshold(1);
        StatCollector.addStatListener(grapher);
        final Texture2D graphTex = setupGraphTexture(grapher);
        final float dW = (float) width / grapher._textureRenderer.getWidth();
        final float dH = (float) height / grapher._textureRenderer.getHeight();

        setupGraphQuad(quad, graphTex, dW, dH);

        return grapher;
    }

    /**
     * Creates and sets up a texture to be used as the texture for a given grapher. Also applies appropriate texture
     * filter modes. (NearestNeighborNoMipMaps and Bilinear)
     *
     * @param grapher
     *            the grapher to associate the texture with
     * @return the texture
     */
    private static Texture2D setupGraphTexture(final AbstractStatGrapher grapher) {
        final Texture2D graphTex = new Texture2D();
        graphTex.setMinificationFilter(MinificationFilter.NearestNeighborNoMipMaps);
        graphTex.setMagnificationFilter(MagnificationFilter.Bilinear);
        grapher.setTexture(graphTex);
        return graphTex;
    }

    /**
     * Sets up a Quad to be used as the display surface for a grapher. Puts it in the ortho mode, sets up UVs, and sets
     * up a TextureState and an alpha transparency BlendState.
     *
     * @param quad
     *            the Quad to use
     * @param graphTexture
     *            the texture to use
     * @param maxU
     *            the maximum value along the U axis to use in the texture for UVs
     * @param maxV
     *            the maximum value along the V axis to use in the texture for UVs
     */
    private static void setupGraphQuad(final Quad quad, final Texture2D graphTexture, final float maxU,
            final float maxV) {
        quad.getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
        quad.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        quad.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
        quad.getSceneHints().setOrthoOrder(-1);

        final FloatBuffer tbuf = quad.getMeshData().getTextureCoords(0).getBuffer();
        tbuf.clear();
        tbuf.put(0).put(0);
        tbuf.put(0).put(maxV);
        tbuf.put(maxU).put(maxV);
        tbuf.put(maxU).put(0);
        tbuf.rewind();

        final TextureState texState = new TextureState();
        texState.setTexture(graphTexture);
        quad.setRenderState(texState);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(SourceFunction.SourceAlpha);
        blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
        quad.setRenderState(blend);
    }
}
