/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import com.ardor3d.extension.ui.text.StyleConstants;
import com.ardor3d.extension.ui.text.StyleSpan;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;

public class ForumLikeMarkupParser implements StyleParser {

    Comparator<StyleSpan> endSorter = new Comparator<StyleSpan>() {
        public int compare(final StyleSpan o1, final StyleSpan o2) {
            return o1.getSpanStart() + o1.getSpanLength() - (o2.getSpanStart() + o2.getSpanLength());
        }
    };

    @Override
    public String parseStyleSpans(final String text, final List<StyleSpan> store) {
        final StringBuilder rVal = new StringBuilder("");
        int index = 0;
        TagStatus tagStatus = TagStatus.NONE;
        String currTagText = "";
        final LinkedList<StyleSpan> buildingSpans = new LinkedList<>();
        final StringTokenizer st = new StringTokenizer(text, "[]\\", true);
        String token;
        while (st.hasMoreTokens()) {
            token = st.nextToken();
            // escape char
            if (tagStatus == TagStatus.NONE && "\\".equals(token)) {
                if (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if ("[".equals(token) || "]".equals(token)) {
                        rVal.append(token);
                        index++;
                        continue;
                    } else {
                        rVal.append('\\');
                        rVal.append(token);
                        index += token.length() + 1;
                        continue;
                    }
                } else {
                    rVal.append('\\');
                    index++;
                    continue;
                }
            }

            // start token
            else if (tagStatus == TagStatus.NONE && "[".equals(token)) {
                tagStatus = TagStatus.START_TAG;
                continue;
            }

            else if (tagStatus == TagStatus.START_TAG) {
                currTagText = token;
                tagStatus = TagStatus.IN_TAG;
                continue;
            }

            // end token
            else if (tagStatus == TagStatus.IN_TAG && "]".equals(token)) {
                tagStatus = TagStatus.NONE;
                // interpret tag:
                // BOLD
                if ("b".equalsIgnoreCase(currTagText)) {
                    // start a new bold span
                    buildingSpans.add(new StyleSpan(StyleConstants.KEY_BOLD, Boolean.TRUE, index, 0));
                } else if ("/b".equalsIgnoreCase(currTagText)) {
                    // find last BOLD entry and add length
                    endSpan(StyleConstants.KEY_BOLD, store, index, buildingSpans);
                }

                // ITALICS
                else if ("i".equalsIgnoreCase(currTagText)) {
                    // start a new italics span
                    buildingSpans.add(new StyleSpan(StyleConstants.KEY_ITALICS, Boolean.TRUE, index, 0));
                } else if ("/i".equalsIgnoreCase(currTagText)) {
                    // find last ITALICS entry and add length
                    endSpan(StyleConstants.KEY_ITALICS, store, index, buildingSpans);
                }

                // COLOR
                else if (currTagText.toLowerCase().startsWith("c=")) {
                    // start a new color span
                    try {
                        // parse a color
                        final String c = currTagText.substring(2);
                        buildingSpans
                                .add(new StyleSpan(StyleConstants.KEY_COLOR, ColorRGBA.parseColor(c, null), index, 0));
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                } else if ("/c".equalsIgnoreCase(currTagText)) {
                    // find last BOLD entry and add length
                    endSpan(StyleConstants.KEY_COLOR, store, index, buildingSpans);
                }

                // SIZE
                else if (currTagText.toLowerCase().startsWith("size=")) {
                    // start a new size span
                    try {
                        // parse a size
                        final int i = Integer.parseInt(currTagText.substring(5));
                        buildingSpans.add(new StyleSpan(StyleConstants.KEY_SIZE, i, index, 0));
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                } else if ("/size".equalsIgnoreCase(currTagText)) {
                    // find last SIZE entry and add length
                    endSpan(StyleConstants.KEY_SIZE, store, index, buildingSpans);
                }

                // FAMILY
                else if (currTagText.toLowerCase().startsWith("f=")) {
                    // start a new family span
                    final String family = currTagText.substring(2);
                    buildingSpans.add(new StyleSpan(StyleConstants.KEY_FAMILY, family, index, 0));
                } else if ("/f".equalsIgnoreCase(currTagText)) {
                    // find last FAMILY entry and add length
                    endSpan(StyleConstants.KEY_FAMILY, store, index, buildingSpans);
                } else {
                    // not really a tag, so put it back.
                    rVal.append('[');
                    rVal.append(currTagText);
                    rVal.append(']');
                    tagStatus = TagStatus.NONE;
                }

                currTagText = "";
                continue;
            }

            // anything else
            rVal.append(token);
            index += token.length();
        }

        // close any remaining open tags
        while (!buildingSpans.isEmpty()) {
            final StyleSpan span = buildingSpans.getLast();
            endSpan(span.getStyle(), store, index, buildingSpans);
        }

        // return plain text
        return rVal.toString();
    }

    private void endSpan(final String key, final List<StyleSpan> store, final int index,
            final LinkedList<StyleSpan> buildingSpans) {
        for (final Iterator<StyleSpan> it = buildingSpans.descendingIterator(); it.hasNext();) {
            final StyleSpan next = it.next();
            if (key.equals(next.getStyle())) {
                next.setSpanLength(index - next.getSpanStart());
                store.add(next);
                it.remove();
                break;
            }
        }
    }

    @Override
    public String addStyleMarkup(final String plainText, final List<StyleSpan> spans) {
        if (spans.isEmpty()) {
            return plainText;
        }

        // list of spans, sorted by start index
        final List<StyleSpan> starts = new ArrayList<>();
        starts.addAll(spans);
        Collections.sort(starts);

        // list of spans, to be sorted by end index
        final List<StyleSpan> ends = new LinkedList<>();

        final StringBuilder builder = new StringBuilder();

        // go through all chars and add starts and ends
        for (int index = 0, max = plainText.length(); index < max; index++) {
            // close markup
            while (!ends.isEmpty()) {
                final StyleSpan span = ends.get(0);
                if (span.getSpanStart() + span.getSpanLength() == index) {
                    builder.append(getMarkup(span, true));
                    ends.remove(0);
                } else {
                    break;
                }
            }

            // add starts
            while (!starts.isEmpty()) {
                final StyleSpan span = starts.get(0);
                if (span.getSpanStart() == index) {
                    builder.append(getMarkup(span, false));
                    ends.add(span);
                    starts.remove(0);
                    Collections.sort(ends, endSorter);
                } else {
                    break;
                }
            }

            builder.append(plainText.charAt(index));
        }

        // close any remaining markup:
        while (!ends.isEmpty()) {
            final StyleSpan span = ends.get(0);
            builder.append(getMarkup(span, true));
            ends.remove(0);
        }

        return builder.toString();
    }

    private String getMarkup(final StyleSpan span, final boolean end) {
        if (StyleConstants.KEY_BOLD.equalsIgnoreCase(span.getStyle())) {
            return end ? "[/b]" : "[b]";
        } else if (StyleConstants.KEY_ITALICS.equalsIgnoreCase(span.getStyle())) {
            return end ? "[/i]" : "[i]";
        } else if (StyleConstants.KEY_FAMILY.equalsIgnoreCase(span.getStyle())) {
            return end ? "[/f]" : "[f=" + span.getValue() + "]";
        } else if (StyleConstants.KEY_SIZE.equalsIgnoreCase(span.getStyle())) {
            return end ? "[/size]" : "[size=" + span.getValue() + "]";
        } else if (StyleConstants.KEY_COLOR.equalsIgnoreCase(span.getStyle()) && span.getValue() instanceof ColorRGBA) {
            return end ? "[/c]" : "[c=" + ((ReadOnlyColorRGBA) span.getValue()).asHexRRGGBBAA() + "]";
        }

        return "";
    }

    enum TagStatus {
        NONE, START_TAG, IN_TAG
    }
}
