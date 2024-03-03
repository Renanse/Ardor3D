/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */

package com.ardor3d.example.ui.nuklear;

import static org.lwjgl.nuklear.Nuklear.NK_EDIT_SIMPLE;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_BORDER;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_MOVABLE;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_NO_SCROLLBAR;
import static org.lwjgl.nuklear.Nuklear.nk_begin;
import static org.lwjgl.nuklear.Nuklear.nk_button_label;
import static org.lwjgl.nuklear.Nuklear.nk_button_text;
import static org.lwjgl.nuklear.Nuklear.nk_edit_string;
import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_rect;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memASCII;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkPluginFilter;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import com.ardor3d.extension.ui.nuklear.NuklearWindow;

/**
 * Java port of <a href=
 * "https://github.com/vurtun/nuklear/blob/master/demo/calculator.c">https://github.com/vurtun/nuklear/blob/master/demo/calculator.c</a>.
 */
class CalculatorWindow extends NuklearWindow {

  private static final String NUMS = "789456123";
  private static final String OPS = "+-*/";

  private char prev, op;

  private boolean set;

  private final double[] a = new double[1], b = new double[1];

  private double[] current = a;

  private final DecimalFormat format = new DecimalFormat();

  final NkPluginFilter numberFilter;

  CalculatorWindow() {
    format.setGroupingUsed(false);
    format.setDecimalSeparatorAlwaysShown(true);

    format.setMinimumFractionDigits(2);
    format.setMaximumFractionDigits(2);

    final DecimalFormatSymbols dfs = format.getDecimalFormatSymbols();
    dfs.setDecimalSeparator('.');
    format.setDecimalFormatSymbols(dfs);

    numberFilter = NkPluginFilter.create(Nuklear::nnk_filter_float);
  }

  @Override
  public void layout(final NkContext ctx) {
    try (MemoryStack stack = stackPush()) {
      final NkRect rect = NkRect.malloc(stack);
      if (nk_begin(ctx, "Calculator", nk_rect(getXOffset(), getYOffset(), 180, 250, rect),
          NK_WINDOW_BORDER | NK_WINDOW_NO_SCROLLBAR | NK_WINDOW_MOVABLE)) {
        nk_layout_row_dynamic(ctx, 35, 1);
        {
          final ByteBuffer buffer = stack.calloc(256);

          final int length = memASCII(format.format(current[0]), false, buffer);

          final IntBuffer len = stack.ints(length);
          nk_edit_string(ctx, NK_EDIT_SIMPLE, buffer, len, 255, numberFilter);
          try {
            current[0] = format.parse(memASCII(buffer, len.get(0))).doubleValue();
          } catch (final ParseException e) {
            e.printStackTrace();
          }
        }

        nk_layout_row_dynamic(ctx, 35, 4);

        boolean solve = false;
        for (int i = 0; i < 16; ++i) {
          if (i >= 12 && i < 15) {
            if (i > 12) {
              continue;
            }
            if (nk_button_label(ctx, "C")) {
              a[0] = b[0] = op = 0;
              current = a;
              set = false;
            }
            if (nk_button_label(ctx, "0")) {
              current[0] *= 10.0f;
              set = false;
            }
            if (nk_button_label(ctx, "=")) {
              solve = true;
              prev = op;
              op = 0;
            }
          } else if (((i + 1) % 4) != 0) {
            var c = NUMS.charAt((i / 4) * 3 + i % 4);
            if (nk_button_text(ctx, Character.toString(c))) {
              current[0] = current[0] * 10.0f + (c - '0');
              set = false;
            }
          } else if (nk_button_text(ctx, Character.toString(OPS.charAt(i / 4)))) {
            if (!set) {
              if (current != b) {
                current = b;
              } else {
                prev = op;
                solve = true;
              }
            }
            op = OPS.charAt(i / 4);
            set = true;
          }
        }
        if (solve) {
          if (prev == '+') {
            a[0] += b[0];
          }
          if (prev == '-') {
            a[0] -= b[0];
          }
          if (prev == '*') {
            a[0] *= b[0];
          }
          if (prev == '/') {
            a[0] /= b[0];
          }
          current = a;
          if (set) {
            current = b;
          }
          b[0] = 0;
          set = false;
        }
      }
      nk_end(ctx);
    }
  }

}
