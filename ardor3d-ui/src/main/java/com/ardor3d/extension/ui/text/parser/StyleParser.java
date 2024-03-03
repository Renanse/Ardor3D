/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text.parser;

import java.util.List;

import com.ardor3d.extension.ui.text.StyleSpan;

public interface StyleParser {

  /**
   * Parse the given text for markup indicating style spans and store the spans in the given store,
   * returning the remaining as plaintext.
   * 
   * @param text
   *          The text containing both plaintext and style markup.
   * @param store
   *          The store for our parsed style spans.
   * @return the plain text portion of our incoming text.
   */
  String parseStyleSpans(String text, List<StyleSpan> store);

  /**
   * Add markup describing the given spans, to the given plaintext. Note: spans with styles this
   * parser does not understand will be ignored.
   * 
   * @param plainText
   *          the plain text to decorate with markup.
   * @param spans
   *          our style spans
   * @return the marked up text.
   */
  String addStyleMarkup(String plainText, List<StyleSpan> spans);

}
