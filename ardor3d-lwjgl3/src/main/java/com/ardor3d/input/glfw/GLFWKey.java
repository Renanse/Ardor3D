/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.glfw;

import org.lwjgl.glfw.GLFW;

import com.ardor3d.input.keyboard.Key;

public enum GLFWKey {

  SPACE(GLFW.GLFW_KEY_SPACE, Key.SPACE), //
  APOSTROPHE(GLFW.GLFW_KEY_APOSTROPHE, Key.APOSTROPHE), //
  COMMA(GLFW.GLFW_KEY_COMMA, Key.COMMA), //
  MINUS(GLFW.GLFW_KEY_MINUS, Key.MINUS), //
  PERIOD(GLFW.GLFW_KEY_PERIOD, Key.PERIOD), //
  SLASH(GLFW.GLFW_KEY_SLASH, Key.SLASH), //
  ZERO(GLFW.GLFW_KEY_0, Key.ZERO), //
  ONE(GLFW.GLFW_KEY_1, Key.ONE), //
  TWO(GLFW.GLFW_KEY_2, Key.TWO), //
  THREE(GLFW.GLFW_KEY_3, Key.THREE), //
  FOUR(GLFW.GLFW_KEY_4, Key.FOUR), //
  FIVE(GLFW.GLFW_KEY_5, Key.FIVE), //
  SIX(GLFW.GLFW_KEY_6, Key.SIX), //
  SEVEN(GLFW.GLFW_KEY_7, Key.SEVEN), //
  EIGHT(GLFW.GLFW_KEY_8, Key.EIGHT), //
  NINE(GLFW.GLFW_KEY_9, Key.NINE), //
  SEMICOLON(GLFW.GLFW_KEY_SEMICOLON, Key.SEMICOLON), //
  EQUAL(GLFW.GLFW_KEY_EQUAL, Key.EQUAL), //
  A(GLFW.GLFW_KEY_A, Key.A), //
  B(GLFW.GLFW_KEY_B, Key.B), //
  C(GLFW.GLFW_KEY_C, Key.C), //
  D(GLFW.GLFW_KEY_D, Key.D), //
  E(GLFW.GLFW_KEY_E, Key.E), //
  F(GLFW.GLFW_KEY_F, Key.F), //
  G(GLFW.GLFW_KEY_G, Key.G), //
  H(GLFW.GLFW_KEY_H, Key.H), //
  I(GLFW.GLFW_KEY_I, Key.I), //
  J(GLFW.GLFW_KEY_J, Key.J), //
  K(GLFW.GLFW_KEY_K, Key.K), //
  L(GLFW.GLFW_KEY_L, Key.L), //
  M(GLFW.GLFW_KEY_M, Key.M), //
  N(GLFW.GLFW_KEY_N, Key.N), //
  O(GLFW.GLFW_KEY_O, Key.O), //
  P(GLFW.GLFW_KEY_P, Key.P), //
  Q(GLFW.GLFW_KEY_Q, Key.Q), //
  R(GLFW.GLFW_KEY_R, Key.R), //
  S(GLFW.GLFW_KEY_S, Key.S), //
  T(GLFW.GLFW_KEY_T, Key.T), //
  U(GLFW.GLFW_KEY_U, Key.U), //
  V(GLFW.GLFW_KEY_V, Key.V), //
  W(GLFW.GLFW_KEY_W, Key.W), //
  X(GLFW.GLFW_KEY_X, Key.X), //
  Y(GLFW.GLFW_KEY_Y, Key.Y), //
  Z(GLFW.GLFW_KEY_Z, Key.Z), //
  LEFT_BRACKET(GLFW.GLFW_KEY_LEFT_BRACKET, Key.LEFT_BRACKET), //
  BACKSLASH(GLFW.GLFW_KEY_BACKSLASH, Key.BACKSLASH), //
  RIGHT_BRACKET(GLFW.GLFW_KEY_RIGHT_BRACKET, Key.RIGHT_BRACKET), //
  GRAVE_ACCENT(GLFW.GLFW_KEY_GRAVE_ACCENT, Key.GRAVE_ACCENT), //
  WORLD_1(GLFW.GLFW_KEY_WORLD_1, Key.WORLD_1), //
  WORLD_2(GLFW.GLFW_KEY_WORLD_2, Key.WORLD_2), //
  ESCAPE(GLFW.GLFW_KEY_ESCAPE, Key.ESCAPE), //
  ENTER(GLFW.GLFW_KEY_ENTER, Key.ENTER), //
  TAB(GLFW.GLFW_KEY_TAB, Key.TAB), //
  BACKSPACE(GLFW.GLFW_KEY_BACKSPACE, Key.BACKSPACE), //
  INSERT(GLFW.GLFW_KEY_INSERT, Key.INSERT), //
  DELETE(GLFW.GLFW_KEY_DELETE, Key.DELETE), //
  RIGHT(GLFW.GLFW_KEY_RIGHT, Key.RIGHT), //
  LEFT(GLFW.GLFW_KEY_LEFT, Key.LEFT), //
  DOWN(GLFW.GLFW_KEY_DOWN, Key.DOWN), //
  UP(GLFW.GLFW_KEY_UP, Key.UP), //
  PAGE_UP(GLFW.GLFW_KEY_PAGE_UP, Key.PAGE_UP), //
  PAGE_DOWN(GLFW.GLFW_KEY_PAGE_DOWN, Key.PAGE_DOWN), //
  HOME(GLFW.GLFW_KEY_HOME, Key.HOME), //
  END(GLFW.GLFW_KEY_END, Key.END), //
  CAPS_LOCK(GLFW.GLFW_KEY_CAPS_LOCK, Key.CAPS_LOCK), //
  SCROLL_LOCK(GLFW.GLFW_KEY_SCROLL_LOCK, Key.SCROLL_LOCK), //
  NUM_LOCK(GLFW.GLFW_KEY_NUM_LOCK, Key.NUM_LOCK), //
  PRINT_SCREEN(GLFW.GLFW_KEY_PRINT_SCREEN, Key.PRINT_SCREEN), //
  PAUSE(GLFW.GLFW_KEY_PAUSE, Key.PAUSE), //
  F1(GLFW.GLFW_KEY_F1, Key.F1), //
  F2(GLFW.GLFW_KEY_F2, Key.F2), //
  F3(GLFW.GLFW_KEY_F3, Key.F3), //
  F4(GLFW.GLFW_KEY_F4, Key.F4), //
  F5(GLFW.GLFW_KEY_F5, Key.F5), //
  F6(GLFW.GLFW_KEY_F6, Key.F6), //
  F7(GLFW.GLFW_KEY_F7, Key.F7), //
  F8(GLFW.GLFW_KEY_F8, Key.F8), //
  F9(GLFW.GLFW_KEY_F9, Key.F9), //
  F10(GLFW.GLFW_KEY_F10, Key.F10), //
  F11(GLFW.GLFW_KEY_F11, Key.F11), //
  F12(GLFW.GLFW_KEY_F12, Key.F12), //
  F13(GLFW.GLFW_KEY_F13, Key.F13), //
  F14(GLFW.GLFW_KEY_F14, Key.F14), //
  F15(GLFW.GLFW_KEY_F15, Key.F15), //
  F16(GLFW.GLFW_KEY_F16, Key.F16), //
  F17(GLFW.GLFW_KEY_F17, Key.F17), //
  F18(GLFW.GLFW_KEY_F18, Key.F18), //
  F19(GLFW.GLFW_KEY_F19, Key.F19), //
  F20(GLFW.GLFW_KEY_F20, Key.F20), //
  F21(GLFW.GLFW_KEY_F21, Key.F21), //
  F22(GLFW.GLFW_KEY_F22, Key.F22), //
  F23(GLFW.GLFW_KEY_F23, Key.F23), //
  F24(GLFW.GLFW_KEY_F24, Key.F24), //
  F25(GLFW.GLFW_KEY_F25, Key.F25), //
  KP_0(GLFW.GLFW_KEY_KP_0, Key.KP_0), //
  KP_1(GLFW.GLFW_KEY_KP_1, Key.KP_1), //
  KP_2(GLFW.GLFW_KEY_KP_2, Key.KP_2), //
  KP_3(GLFW.GLFW_KEY_KP_3, Key.KP_3), //
  KP_4(GLFW.GLFW_KEY_KP_4, Key.KP_4), //
  KP_5(GLFW.GLFW_KEY_KP_5, Key.KP_5), //
  KP_6(GLFW.GLFW_KEY_KP_6, Key.KP_6), //
  KP_7(GLFW.GLFW_KEY_KP_7, Key.KP_7), //
  KP_8(GLFW.GLFW_KEY_KP_8, Key.KP_8), //
  KP_9(GLFW.GLFW_KEY_KP_9, Key.KP_9), //
  KP_DECIMAL(GLFW.GLFW_KEY_KP_DECIMAL, Key.KP_DECIMAL), //
  KP_DIVIDE(GLFW.GLFW_KEY_KP_DIVIDE, Key.KP_DIVIDE), //
  KP_MULTIPLY(GLFW.GLFW_KEY_KP_MULTIPLY, Key.KP_MULTIPLY), //
  KP_SUBTRACT(GLFW.GLFW_KEY_KP_SUBTRACT, Key.KP_SUBTRACT), //
  KP_ADD(GLFW.GLFW_KEY_KP_ADD, Key.KP_ADD), //
  KP_ENTER(GLFW.GLFW_KEY_KP_ENTER, Key.KP_ENTER), //
  KP_EQUAL(GLFW.GLFW_KEY_KP_EQUAL, Key.KP_EQUAL), //
  LEFT_SHIFT(GLFW.GLFW_KEY_LEFT_SHIFT, Key.LEFT_SHIFT), //
  LEFT_CONTROL(GLFW.GLFW_KEY_LEFT_CONTROL, Key.LEFT_CONTROL), //
  LEFT_ALT(GLFW.GLFW_KEY_LEFT_ALT, Key.LEFT_ALT), //
  LEFT_SUPER(GLFW.GLFW_KEY_LEFT_SUPER, Key.LEFT_META), //
  RIGHT_SHIFT(GLFW.GLFW_KEY_RIGHT_SHIFT, Key.RIGHT_SHIFT), //
  RIGHT_CONTROL(GLFW.GLFW_KEY_RIGHT_CONTROL, Key.RIGHT_CONTROL), //
  RIGHT_ALT(GLFW.GLFW_KEY_RIGHT_ALT, Key.RIGHT_ALT), //
  RIGHT_SUPER(GLFW.GLFW_KEY_RIGHT_SUPER, Key.RIGHT_META), //
  MENU(GLFW.GLFW_KEY_MENU, Key.MENU); //

  private final int _glfwCode;
  private final Key _key;

  private GLFWKey(final int code, final Key key) {
    _glfwCode = code;
    _key = key;
  }

  /**
   * Locate a key, given a specific glfw key code.
   *
   * @param glfwCode
   *          the glfw key code.
   * @return the Ardor3D Key enum value.
   */
  public static Key findByCode(final int glfwCode) {
    for (final GLFWKey k : values()) {
      if (k.getGLFWCode() == glfwCode) {
        return k._key;
      }
    }

    return Key.UNKNOWN;
  }

  public int getGLFWCode() { return _glfwCode; }

}
