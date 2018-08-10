/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.text.parser;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ardor3d.extension.ui.text.StyleConstants;
import com.ardor3d.extension.ui.text.StyleSpan;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ForumLikeMarkupParserTest {

    private ForumLikeMarkupParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new ForumLikeMarkupParser();
    }

    @Test
    public void parseWithoutMarkup() {
        final String text = "A text without any markup what so ever [13] dum di dum [/23]";
        final List<StyleSpan> spans = Lists.newArrayList();
        final String result = parser.parseStyleSpans(text, spans);

        Assert.assertEquals(text, result);

        Assert.assertTrue(spans.isEmpty());
    }

    @Test
    public void parseWithSimpleStyle() throws Exception {
        final String text = "A text with [size=30]simple markup[/size] dum di dum";
        final List<StyleSpan> spans = Lists.newArrayList();
        final String result = parser.parseStyleSpans(text, spans);

        Assert.assertEquals("A text with simple markup dum di dum", result);

        Assert.assertEquals(1, spans.size());
        final StyleSpan span = spans.get(0);
        Assert.assertEquals(12, span.getSpanStart());
        Assert.assertEquals(13, span.getSpanLength());
        Assert.assertEquals(StyleConstants.KEY_SIZE, span.getStyle());
        Assert.assertEquals(30, span.getValue());
    }

    @Test
    public void parseWithNestedStyle() throws Exception {
        final String text = "A text [size=30]with [f=arial]simple markup[/f][/size] dum di dum";
        final List<StyleSpan> spans = Lists.newArrayList();
        final String result = parser.parseStyleSpans(text, spans);

        Assert.assertEquals("A text with simple markup dum di dum", result);

        Assert.assertEquals(2, spans.size());
        final SortedSet<StyleSpan> sortedSpans = Sets.newTreeSet(spans);
        final Iterator<StyleSpan> spanIterator = sortedSpans.iterator();
        final StyleSpan span1 = spanIterator.next();
        Assert.assertEquals(7, span1.getSpanStart());
        Assert.assertEquals(18, span1.getSpanLength());
        Assert.assertEquals(StyleConstants.KEY_SIZE, span1.getStyle());
        final StyleSpan span2 = spanIterator.next();
        Assert.assertEquals(12, span2.getSpanStart());
        Assert.assertEquals(13, span2.getSpanLength());
        Assert.assertEquals(StyleConstants.KEY_FAMILY, span2.getStyle());
    }

    @Test
    public void parseWithNestedSameStyleBackToBackTags() throws Exception {
        final String text = "[size=10][size=20]A text [/size]with simple markup[/size] dum di dum";
        final List<StyleSpan> spans = Lists.newArrayList();
        final String result = parser.parseStyleSpans(text, spans);

        Assert.assertEquals("A text with simple markup dum di dum", result);

        Assert.assertEquals(2, spans.size());
        final SortedSet<StyleSpan> sortedSpans = Sets.newTreeSet(spans);
        final Iterator<StyleSpan> spanIterator = sortedSpans.iterator();
        final StyleSpan span1 = spanIterator.next();
        Assert.assertEquals(0, span1.getSpanStart());
        Assert.assertEquals(25, span1.getSpanLength());
        Assert.assertEquals(StyleConstants.KEY_SIZE, span1.getStyle());
        Assert.assertEquals(10, span1.getValue());

        final StyleSpan span2 = spanIterator.next();
        Assert.assertEquals(0, span2.getSpanStart());
        Assert.assertEquals(7, span2.getSpanLength());
        Assert.assertEquals(StyleConstants.KEY_SIZE, span2.getStyle());
        Assert.assertEquals(20, span2.getValue());
    }

}
