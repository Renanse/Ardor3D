/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.providers.awt;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyTransform;

public class AwtImageElement extends AbstractAwtElement {

  private final Image _image;

  public AwtImageElement(final Image image) {
    this(image, Transform.IDENTITY, null);
    updateBounds();
  }

  public AwtImageElement(final Image image, final ReadOnlyTransform transform) {
    this(image, transform, null);
  }

  public AwtImageElement(final Image image, final ReadOnlyTransform transform, final Composite compositeOverride) {
    super(transform, compositeOverride);
    _image = image;
  }

  public Image getImage() { return _image; }

  @Override
  public void updateBoundsFromElement() {
    _awtBounds.set(0, 0, _image.getWidth(null), _image.getHeight(null));
  }

  @Override
  public void drawTo(final BufferedImage image, final ReadOnlyTransform localTransform, final int clipmapLevel) {
    // apply the two transforms together and then use result to scale/translate and rotate image
    final Transform trans = new Transform();
    localTransform.multiply(getTransform(), trans);

    // grab a copy of the graphics so we don't bleed state to next image
    final Graphics2D g2d = (Graphics2D) image.getGraphics().create();

    // apply hints
    for (final RenderingHints.Key key : hints.keySet()) {
      g2d.setRenderingHint(key, hints.get(key));
    }

    // set transform
    g2d.translate(trans.getTranslation().getX(), trans.getTranslation().getY());
    g2d.rotate(trans.getMatrix().toAngles(null)[2]); // rotation about z
    g2d.scale(trans.getScale().getX(), trans.getScale().getY());

    // set composite
    if (_compositeOverride != null) {
      g2d.setComposite(_compositeOverride);
    }

    // draw the image
    g2d.drawImage(_image, 0, 0, null);
  }
}
