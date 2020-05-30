/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.ardor3d.extension.ui.text.RenderedText.RenderedTextData;
import com.ardor3d.extension.ui.text.font.BMFontProvider;
import com.ardor3d.extension.ui.text.font.FontProvider;
import com.ardor3d.extension.ui.text.font.UIFont;
import com.ardor3d.extension.ui.text.parser.ForumLikeMarkupParser;
import com.ardor3d.extension.ui.text.parser.StyleParser;
import com.ardor3d.framework.IDpiScaleProvider;
import com.ardor3d.image.Texture2D;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.material.uniform.AlphaTestConsts;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.MeshData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public enum TextFactory {
  INSTANCE;

  private IDpiScaleProvider _scaleProvider;
  private FontProvider _fontProvider;
  private StyleParser _styleParser;
  private final static float[] WHITE = new float[] {1, 1, 1, 1};

  TextFactory() {
    final BMFontProvider fontProvider = new BMFontProvider();
    fontProvider.addFont("com/ardor3d/extension/ui/font/arial-12-regular", "Arial", 12, false, false);
    fontProvider.addFont("com/ardor3d/extension/ui/font/arial-16-bold", "Arial", 16, true, false);
    fontProvider.addFont("com/ardor3d/extension/ui/font/arial-18-regular", "Arial", 18, false, false);
    fontProvider.addFont("com/ardor3d/extension/ui/font/arial-18-bold", "Arial", 18, true, false);
    fontProvider.addFont("com/ardor3d/extension/ui/font/arial-18-bold-italic", "Arial", 18, true, true);
    fontProvider.addFont("com/ardor3d/extension/ui/font/arial-24-bold", "Arial", 24, false, false);
    fontProvider.addFont("com/ardor3d/ui/text/DroidSans-14-bold-regular", "DroidSans", 14, true, false);
    fontProvider.addFont("com/ardor3d/ui/text/DroidSans-16-bold-regular", "DroidSans", 16, true, false);
    fontProvider.addFont("com/ardor3d/ui/text/DroidSans-16-medium-regular", "DroidSans", 16, false, false);
    fontProvider.addFont("com/ardor3d/ui/text/DroidSans-20-bold-regular", "DroidSans", 20, true, false);
    fontProvider.addFont("com/ardor3d/ui/text/DroidSans-26-bold-regular", "DroidSans", 26, true, false);
    fontProvider.addFont("com/ardor3d/ui/text/DroidSans-40-bold-regular", "DroidSans", 40, true, false);
    fontProvider.addFont("com/ardor3d/ui/text/DroidSansMono-15-bold-regular", "DroidSansMono", 15, true, false);
    fontProvider.addFont("com/ardor3d/ui/text/DroidSansMono-20-bold-regular", "DroidSansMono", 20, true, false);
    fontProvider.addFont("com/ardor3d/ui/text/DroidSansMono-26-bold-regular", "DroidSansMono", 26, true, false);
    fontProvider.addFont("com/ardor3d/ui/text/DroidSansMono-40-bold-regular", "DroidSansMono", 40, true, false);
    setFontProvider(fontProvider);
    setStyleParser(new ForumLikeMarkupParser());
  }

  public void setDpiScaleProvider(final IDpiScaleProvider provider) { _scaleProvider = provider; }

  public IDpiScaleProvider getDpiScaleProvider() { return _scaleProvider; }

  public void setFontProvider(final FontProvider provider) { _fontProvider = provider; }

  public FontProvider getFontProvider() { return _fontProvider; }

  public void setStyleParser(final StyleParser parser) { _styleParser = parser; }

  public StyleParser getStyleParser() { return _styleParser; }

  // FIXME: Should reuse our buffers whenever possible! Particularly now that we are VBO only.
  public RenderedText generateText(final String rawText, final boolean styled, final Map<String, Object> defaultStyles,
      final RenderedText store, final int maxWidth) {
    RenderedText rVal = store;

    // Hold onto these in case they get switched out by another thread during generation.
    final FontProvider provider = _fontProvider;
    final StyleParser parser = _styleParser;

    if (rVal == null) {
      rVal = new RenderedText();
    } else {
      rVal.detachAllChildren();
    }

    rVal.setRawText(rawText);
    rVal.setStyled(styled);

    // note: spans must be in order by start index
    final LinkedList<StyleSpan> spans = new LinkedList<>();
    final String visibleText;
    if (styled && parser != null) {
      // parse text for style spans
      final List<StyleSpan> styleStore = new ArrayList<>();
      visibleText = parser.parseStyleSpans(rawText, styleStore);
      Collections.sort(styleStore);
      if (!styleStore.isEmpty()) {
        spans.addAll(styleStore);
      }
    } else {
      visibleText = rawText;
    }

    rVal.setVisibleText(visibleText);
    rVal.setParsedStyleSpans(spans);

    // push defaults onto head of list
    for (final String style : defaultStyles.keySet()) {
      spans.addFirst(new StyleSpan(style, defaultStyles.get(style), 0, visibleText.length()));
    }

    final RenderedTextData textData = rVal.getData();
    textData.reset();

    char prevChar = 0, c = 0;
    final List<StyleSpan> currentStyles = new LinkedList<>();
    // indexed by character offset
    final List<CharacterDescriptor> descs = textData._characters;
    final List<Integer> descXStarts = textData._xStarts;
    final List<Integer> fontHeights = textData._fontHeights;
    // indexed by line number
    final List<Integer> lineEnds = textData._lineEnds;
    final List<Integer> lineHeights = textData._lineHeights;
    // indexed by the used Texture
    final Multimap<Texture2D, Integer> descIndices = ArrayListMultimap.create();
    int maxLineHeight = 0, xOffset = 0, maxSizeHeight = 0;
    UIFont prevFont = null;
    double scale = 1, prevScale = 0;
    final Map<String, Object> stylesMap = new HashMap<>();

    final char[] chars = visibleText.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      c = chars[i];
      // update our list - remove spans we've passed by
      for (final Iterator<StyleSpan> it = currentStyles.iterator(); it.hasNext();) {
        final StyleSpan span = it.next();
        if (span.getSpanStart() + span.getSpanLength() <= i) {
          it.remove();
        }
      }

      // update our list - add spans we've entered
      while (!spans.isEmpty()) {
        final StyleSpan span = spans.peek();
        if (span.getSpanStart() <= i) {
          spans.removeFirst();
        } else {
          break;
        }

        if (span.getSpanStart() + span.getSpanLength() > i) {
          currentStyles.add(span);
        }
      }

      stylesMap.clear();
      for (final StyleSpan style : currentStyles) {
        stylesMap.put(style.getStyle(), style.getValue());
      }

      // find the UIFont related to the given font family & size & styles
      final AtomicReference<Double> scaleRef = new AtomicReference<>();
      final UIFont font = provider.getClosestMatchingFont(stylesMap, _scaleProvider, scaleRef);
      if (font == null) {
        return rVal;
      }

      scale = scaleRef.get();

      // check for new line
      if (c == '\n') {
        descs.add(CharacterDescriptor.CR);
        descXStarts.add(xOffset);

        // add current index to lineEnds
        lineEnds.add(descs.size() - 1);

        // check max height is valid
        if (maxLineHeight == 0) {
          maxLineHeight = (int) Math.round(scale * font.getFontSize());
        }

        // add current max height to heights
        lineHeights.add(maxLineHeight);
        maxSizeHeight += maxLineHeight;
        fontHeights.add(maxLineHeight);

        // reset tracking vars
        maxLineHeight = 0;
        prevChar = 0;
        prevFont = null;
        xOffset = 0;

        // go to next char
        continue;
      }

      // pull our char descriptor
      CharacterDescriptor desc = font.getDescriptor(c);
      if (desc == null) {
        // not a mapped char, so use '?'
        c = '?';
        desc = font.getDescriptor(c);
        if (desc == null) {
          continue;
        }
      }
      // clone so we can add custom values
      desc = new CharacterDescriptor(desc);
      desc.setTint((ReadOnlyColorRGBA) stylesMap.get(StyleConstants.KEY_COLOR));
      desc.setScale(scale);
      descs.add(desc);

      // add our kerning, if applicable (must be same font)
      if (prevFont == font && prevScale == scale) {
        xOffset += (int) Math.round(scale * font.getKerning(prevChar, c));
      }

      // check to see if we're past the edge of our allowed width
      if (maxWidth > 0 && xOffset + (int) Math.round(scale * (desc.getXOffset() + desc.getWidth())) > maxWidth) {
        // add current index to lineEnds
        lineEnds.add(descs.size() - 2);

        // add current max height to heights
        lineHeights.add(maxLineHeight);
        maxSizeHeight += maxLineHeight;

        // reset tracking vars
        maxLineHeight = 0;
        prevChar = 0;
        prevScale = 0;
        prevFont = null;
        xOffset = 0;

        // keep going...
      }

      // check against max line height
      maxLineHeight = Math.max((int) Math.round(scale * font.getFontSize()), maxLineHeight);

      // add a pointer to it for the associated texture
      descIndices.put(font.getFontTexture(), descs.size() - 1);

      // store our xOffset and line height
      descXStarts.add(xOffset);
      fontHeights.add((int) Math.round(scale * font.getFontSize()));

      // move forward for next char
      xOffset += (int) Math.round(scale * desc.getXAdvance());

      // update our previous vals
      prevChar = c;
      prevFont = font;
      prevScale = scale;
    }

    if (maxLineHeight != 0) {
      // add current index to lineEnds
      lineEnds.add(descs.size() - 1);

      // add current max height to heights
      lineHeights.add(maxLineHeight);
      maxSizeHeight += maxLineHeight;
    }

    // use parsed information to create our textmeshes
    Collection<Integer> indices;
    float t, b, l, r, inverseTextureHeight, inverseTextureWidth;
    CharacterDescriptor charDesc;
    int cursorY, lineHeight, maxSizeWidth = 0;
    for (final Texture2D tex : descIndices.keySet()) {
      inverseTextureHeight = 1f / tex.getImage().getHeight();
      inverseTextureWidth = 1f / tex.getImage().getWidth();

      final TextMesh tMesh = new TextMesh();
      tMesh.setRenderMaterial("ui/textured/vertex_color.yaml");

      // apply render states
      applyStates(tMesh, tex);

      indices = descIndices.get(tex);

      // setup buffers based on number of indices we have
      final MeshData mData = tMesh.getMeshData();
      final int verts = indices.size() * 6;
      mData.setVertexCoords(new FloatBufferData(verts * 3, 3));
      mData.setTextureCoords(new FloatBufferData(verts * 2, 2), 0);
      mData.setColorCoords(new FloatBufferData(verts * 4, 4));

      final FloatBuffer vertices = mData.getVertexBuffer();
      final FloatBuffer texs = mData.getTextureBuffer(0);
      final FloatBuffer colorBufs = mData.getColorBuffer();
      final float[] colorStore = new float[4];

      for (final int index : indices) {
        // grab which line we're on, line height, x offset and char descriptor
        charDesc = descs.get(index);
        lineHeight = lineHeights.get(0);
        cursorY = maxSizeHeight - lineHeight;
        for (int j = 0; j < lineEnds.size(); j++) {
          if (lineEnds.get(j) < index) {
            cursorY -= lineHeight;
            lineHeight = lineHeights.get(j);
          } else {
            break;
          }
        }

        maxSizeWidth = Math.max(descXStarts.get(index) + (int) Math.round(charDesc.getScale() * charDesc.getXAdvance()),
            maxSizeWidth);

        // add to buffers

        // -- vertices -----------------
        l = descXStarts.get(index) + (int) Math.round(charDesc.getScale() * charDesc.getXOffset());
        b = cursorY + lineHeight - Math.round(charDesc.getScale() * (charDesc.getHeight() + charDesc.getYOffset()));
        r = l + (int) Math.round(charDesc.getScale() * charDesc.getWidth());
        t = cursorY + lineHeight - Math.round(charDesc.getScale() * charDesc.getYOffset());

        vertices.put(l).put(t).put(0); // left top
        vertices.put(l).put(b).put(0); // left bottom
        vertices.put(r).put(t).put(0); // right top
        vertices.put(r).put(t).put(0); // right top
        vertices.put(l).put(b).put(0); // left bottom
        vertices.put(r).put(b).put(0); // right bottom

        final float[] color = charDesc.getTint() != null ? charDesc.getTint().toArray(colorStore) : TextFactory.WHITE;
        colorBufs.put(color);
        colorBufs.put(color);
        colorBufs.put(color);
        colorBufs.put(color);
        colorBufs.put(color);
        colorBufs.put(color);

        // -- tex coords ----------------
        l = charDesc.getX() * inverseTextureWidth;
        t = charDesc.getY() * inverseTextureHeight;
        r = (charDesc.getX() + charDesc.getWidth()) * inverseTextureWidth;
        b = (charDesc.getY() + charDesc.getHeight()) * inverseTextureHeight;

        texs.put(l).put(t); // left top
        texs.put(l).put(b); // left bottom
        texs.put(r).put(t); // right top
        texs.put(r).put(t); // right top
        texs.put(l).put(b); // left bottom
        texs.put(r).put(b); // right bottom
      }

      rVal.attachChild(tMesh);
    }

    // set maxWidth and maxHeight on rVal
    rVal.setWidth(maxSizeWidth);
    rVal.setHeight(maxSizeHeight);

    return rVal;
  }

  private void applyStates(final TextMesh mesh, final Texture2D tex) {
    final TextureState textureState = new TextureState();
    textureState.setTexture(tex);
    mesh.setRenderState(textureState);

    final BlendState blendState = new BlendState();
    blendState.setBlendEnabled(true);
    blendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
    blendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
    mesh.setRenderState(blendState);

    // set alpha testing
    mesh.setProperty(AlphaTestConsts.KEY_AlphaTestType, AlphaTestConsts.TestFunction.GreaterThan);
    mesh.setProperty(AlphaTestConsts.KEY_AlphaReference, 0f);

    mesh.updateWorldRenderStates(false);
  }

  public String getMarkedUpText(final String plainText, final List<StyleSpan> spans) {
    if (_styleParser == null) {
      return plainText;
    } else {
      return _styleParser.addStyleMarkup(plainText, spans);
    }
  }
}
