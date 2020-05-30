/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.Dimension;
import com.ardor3d.extension.ui.util.SubTex;

/**
 * A state useful for maintaining and setting label properties such as text, icon, gap, and
 * alignment.
 */
public class LabelState extends UIState {

  /** Text to set on component, or null if to leave unchanged. */
  protected String _text = null;
  /** Icon to set on component, or null if to leave unchanged. */
  protected SubTex _icon = null;
  /** Icon size to set on component, or null if to leave unchanged. */
  protected Dimension _iconDimensions = null;
  /** Alignment to set on component, or null if to leave unchanged. */
  protected Alignment _alignment = null;
  /** Gap to set on component, or null if to leave unchanged. */
  protected Integer _gap = null;
  /** If true, our text could be marked up with style information - null to leave unchanged. */
  protected Boolean _styled = null;

  @Override
  public void setupAppearance(final UIComponent component) {
    super.setupAppearance(component);

    // Add some extra settings if this is a label component.
    if (component instanceof AbstractLabelUIComponent) {
      final AbstractLabelUIComponent labelComponent = (AbstractLabelUIComponent) component;
      if (_text != null) {
        labelComponent.setText(_text);
      }
      if (_icon != null) {
        labelComponent.setIcon(_icon);
      }
      if (_iconDimensions != null) {
        labelComponent.setIconDimensions(_iconDimensions);
      }
      if (_gap != null) {
        labelComponent.setGap(_gap);
      }
      if (_alignment != null) {
        labelComponent.setAlignment(_alignment);
      }
    }
  }

  public Alignment getAlignment() { return _alignment; }

  public void setAlignment(final Alignment align) { _alignment = align; }

  public Integer getGap() { return _gap; }

  public void setGap(final int gap) { _gap = gap; }

  public SubTex getIcon() { return _icon; }

  public void setIcon(final SubTex icon) { _icon = icon; }

  public Dimension getIconDimensions() { return _iconDimensions; }

  public void setIconDimensions(final Dimension iconDimensions) { _iconDimensions = iconDimensions; }

  public String getText() { return _text; }

  public void setText(final String text) { _text = text; }

  public Boolean isStyledText() { return _styled; }

  public void setStyledText(final Boolean value) { _styled = value; }
}
