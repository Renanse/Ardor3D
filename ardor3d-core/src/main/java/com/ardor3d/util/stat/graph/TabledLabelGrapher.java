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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.Constants;
import com.ardor3d.util.stat.MultiStatSample;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;
import com.ardor3d.util.stat.StatValue;

public class TabledLabelGrapher extends AbstractStatGrapher {

  public enum ConfigKeys {
    TextColor, Name, FrameAverage, Decimals, FontScale, ValueScale, Abbreviate,
  }

  public static final int DEFAULT_DECIMALS = 2;

  protected Node _graphRoot = new Node("root");
  protected int _eventCount = 0;
  protected int _threshold = 1;
  protected int _columns = 1;

  protected Quad _bgQuad = new Quad("bgQuad", 1, 1);

  protected BlendState _defBlendState = null;

  private final HashMap<StatType, LabelEntry> _entries = new HashMap<>();

  private boolean _minimalBackground;

  private AbstractStatGrapher _linkedGraph;

  public TabledLabelGrapher(final int width, final int height, final Renderer renderer) {
    super(width, height, renderer);

    _defBlendState = new BlendState();
    _defBlendState.setEnabled(true);
    _defBlendState.setBlendEnabled(true);
    _defBlendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
    _defBlendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
    _graphRoot.setRenderState(_defBlendState);

    _bgQuad.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
    _bgQuad.setDefaultColor(new ColorRGBA(ColorRGBA.BLACK));
    _graphRoot.getSceneHints().setCullHint(CullHint.Never);
  }

  @Override
  public void statsUpdated() {
    if (!isEnabled() || !Constants.updateGraphs) {
      return;
    }

    // Turn off stat collection while we draw this graph.
    StatCollector.pause();

    // some basic stats:
    final int texWidth = _gWidth;
    final int texHeight = _gHeight;

    // On stat event:
    // - check if enough events have been triggered to cause an update.
    _eventCount++;
    if (_eventCount < _threshold) {
      return;
    } else {
      _eventCount = 0;
    }

    int col = 0;
    double lastY = texHeight - 3, maxY = 0;
    final float colSize = texWidth / (float) getColumns();

    // clear visitations
    for (final StatType type : _entries.keySet()) {
      _entries.get(type).visited = false;
    }

    // - We only care about the most recent stats
    synchronized (StatCollector.getHistorical()) {
      final MultiStatSample sample = StatCollector.getHistorical().get(StatCollector.getHistorical().size() - 1);
      // - go through things we are configured for
      for (final StatType type : _config.keySet()) {
        StatValue val = sample.getStatValue(type);
        if (val == null) {
          if (!StatCollector.hasHistoricalStat(type)) {
            continue;
          } else {
            val = new StatValue();
            val.incrementIterations();
          }
        }

        LabelEntry entry = _entries.get(type);
        // Prepare our entry object as needed.
        if (entry == null) {
          entry = new LabelEntry(type);
          _entries.put(type, entry);
          _graphRoot.attachChild(entry.text);
        }
        entry.visited = true;

        // Update text value
        final double value = getBooleanConfig(type, ConfigKeys.FrameAverage.name(), false) ? val.getAverageValue()
            : val.getAccumulatedValue();
        entry.text
            .setText(getStringConfig(type, ConfigKeys.Name.name(), type.getStatName()) + " " + stripVal(value, type));

        // Set font scale
        final float scale = getFloatConfig(type, ConfigKeys.FontScale.name(), .80f);
        entry.text.setScale(scale);

        // See if we have a defained color for this type, otherwise use
        // the corresponding color from a linked line grapher, or if
        // none, use white.
        entry.text.setTextColor(getColorConfig(type, ConfigKeys.TextColor.name(),
            _linkedGraph != null
                ? _linkedGraph.getColorConfig(type, LineGrapher.ConfigKeys.Color.name(), new ColorRGBA(ColorRGBA.WHITE))
                : new ColorRGBA(ColorRGBA.WHITE)));

        // Update text placement.
        final double labelHeight = entry.text.getHeight();
        if (maxY < labelHeight) {
          maxY = labelHeight;
        }
        entry.text.setTranslation(colSize * col, lastY, 0);

        // Update line key as needed
        if (_linkedGraph != null && _linkedGraph.hasConfig(type) && _linkedGraph instanceof TableLinkable) {
          // add line keys
          entry.lineKey = ((TableLinkable) _linkedGraph).updateLineKey(type, entry.lineKey);
          if (entry.lineKey.getParent() != _graphRoot) {
            _graphRoot.attachChild(entry.lineKey);
          }
          final ReadOnlyVector3 tLoc = entry.text.getTranslation();
          entry.lineKey.setTranslation((float) (tLoc.getX() + entry.text.getWidth() + 15),
              (float) (tLoc.getY() + (.5 * entry.text.getHeight())), 0);
        }

        // update column / row variables
        col++;
        col %= getColumns();
        if (col == 0) {
          lastY -= maxY;
          maxY = 0;
        }
      }

      for (final Iterator<StatType> i = _entries.keySet().iterator(); i.hasNext();) {
        final LabelEntry entry = _entries.get(i.next());
        // - Go through the entries list and remove any that were not
        // visited.
        if (!entry.visited) {
          entry.text.removeFromParent();
          if (entry.lineKey != null) {
            entry.lineKey.removeFromParent();
          }
          i.remove();
        }
      }
    }

    _graphRoot.updateGeometricState(0, true);

    final ColorRGBA bgColor = ColorRGBA.fetchTempInstance().set(_textureRenderer.getBackgroundColor());
    if (_minimalBackground) {
      bgColor.setAlpha(0);
      _textureRenderer.setBackgroundColor(bgColor);

      lastY -= 3;
      if (col != 0) {
        lastY -= maxY;
      }
      _bgQuad.resize(texWidth, texHeight - lastY);
      _bgQuad.setRenderState(_defBlendState);
      _bgQuad.setTranslation(texWidth / 2f, texHeight - (texHeight - lastY) / 2f, 0);
      _bgQuad.updateGeometricState(0, true);

      // - Draw our bg quad
      _textureRenderer.render(_bgQuad, _texture, Renderer.BUFFER_COLOR_AND_DEPTH);

      // - Now, draw to texture via a TextureRenderer
      _textureRenderer.renderSpatial(_graphRoot, _texture, Renderer.BUFFER_NONE);
    } else {
      bgColor.setAlpha(1);
      _textureRenderer.setBackgroundColor(bgColor);

      // - Now, draw to texture via a TextureRenderer
      _textureRenderer.renderSpatial(_graphRoot, _texture, Renderer.BUFFER_COLOR_AND_DEPTH);
    }
    ColorRGBA.releaseTempInstance(bgColor);

    // Turn stat collection back on.
    StatCollector.resume();
  }

  private String stripVal(double val, final StatType type) {
    // scale as needed
    val = val * getDoubleConfig(type, ConfigKeys.ValueScale.name(), 1.0);

    String post = "";
    // Break it down if needed.
    if (getBooleanConfig(type, ConfigKeys.Abbreviate.name(), true)) {
      if (val >= 1000000) {
        val /= 1000000;
        post = "m";
      } else if (val >= 1000) {
        val /= 1000;
        post = "k";
      }
    }

    int decimals = getIntConfig(type, ConfigKeys.Decimals.name(), DEFAULT_DECIMALS);
    if (!"".equals(post) && decimals == 0) {
      decimals = 1; // use 1 spot anyway.
    }

    final StringBuilder format = new StringBuilder(decimals > 0 ? "0.0" : "0");
    for (int x = 1; x < decimals; x++) {
      format.append('0');
    }

    return new DecimalFormat(format.toString()).format(val) + post;
  }

  public int getThreshold() { return _threshold; }

  public void setThreshold(final int threshold) { _threshold = threshold; }

  public int getColumns() { return _columns; }

  public void setColumns(final int columns) {
    if (columns < 1) {
      throw new IllegalArgumentException("columns must be >= 1 (" + columns + ")");
    }
    _columns = columns;
  }

  public boolean isMinimalBackground() { return _minimalBackground; }

  public void setMinimalBackground(final boolean minimalBackground) { _minimalBackground = minimalBackground; }

  public void linkTo(final AbstractStatGrapher grapher) {
    _linkedGraph = grapher;
  }

  class LabelEntry {
    BasicText text;
    Line lineKey;
    boolean visited;
    StatType _type;

    public LabelEntry(final StatType type) {
      _type = type;
      text =
          BasicText.createDefaultTextLabel("label", getStringConfig(type, ConfigKeys.Name.name(), type.getStatName()));
    }
  }

  @Override
  public void reset() {
    synchronized (StatCollector.getHistorical()) {
      for (final Iterator<StatType> i = _entries.keySet().iterator(); i.hasNext();) {
        final LabelEntry entry = _entries.get(i.next());
        entry.text.removeFromParent();
        entry.lineKey.removeFromParent();
        i.remove();
      }
    }
  }
}
