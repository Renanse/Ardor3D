/**
 * Copyright  2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

/**
 * Keys supported by Ardor3D platforms. Note that all keys are not likely supported by any one platform.
 */
public enum Key {
    /**
     * Returned when a key character is not supported.
     */
    UNKNOWN,

    /**
     * escape key.
     */
    ESCAPE,

    /**
     * 1 key.
     */
    ONE,

    /**
     * 2 key.
     */
    TWO,

    /**
     * 3 key.
     */
    THREE,

    /**
     * 4 key.
     */
    FOUR,

    /**
     * 5 key.
     */
    FIVE,

    /**
     * 6 key.
     */
    SIX,

    /**
     * 7 key.
     */
    SEVEN,

    /**
     * 8 key.
     */
    EIGHT,

    /**
     * 9 key.
     */
    NINE,

    /**
     * 0 key.
     */
    ZERO,

    /**
     * - key.
     */
    MINUS,

    /**
     * = key.
     */
    EQUALS,

    /**
     * back key.
     */
    BACK,

    /**
     * tab key.
     */
    TAB,

    /**
     * q key.
     */
    Q,

    /**
     * w key.
     */
    W,

    /**
     * e key.
     */
    E,

    /**
     * r key.
     */
    R,

    /**
     * t key.
     */
    T,

    /**
     * y key.
     */
    Y,

    /**
     * u key.
     */
    U,

    /**
     * i key.
     */
    I,

    /**
     * o key.
     */
    O,

    /**
     * p key.
     */
    P,

    /**
     * [ key.
     */
    LBRACKET,

    /**
     * ] key.
     */
    RBRACKET,

    /**
     * enter key.
     */
    RETURN,

    /**
     * left control key.
     */
    LCONTROL,

    /**
     * a key.
     */
    A,

    /**
     * s key.
     */
    S,

    /**
     * d key.
     */
    D,

    /**
     * f key.
     */
    F,

    /**
     * g key.
     */
    G,

    /**
     * h key.
     */
    H,

    /**
     * j key.
     */
    J,

    /**
     * k key.
     */
    K,

    /**
     * l key.
     */
    L,

    /**
     * ; key.
     */
    SEMICOLON,

    /**
     * ' key.
     */
    APOSTROPHE,

    /**
     * Applications key.
     */
    APPS,

    /**
     * ` key.
     */
    GRAVE,

    /**
     * left shift key.
     */
    LSHIFT,

    /**
     * \ key.
     */
    BACKSLASH,

    /**
     * z key.
     */
    Z,

    /**
     * x key.
     */
    X,

    /**
     * c key.
     */
    C,

    /**
     * v key.
     */
    V,

    /**
     * b key.
     */
    B,

    /**
     * n key.
     */
    N,

    /**
     * m key.
     */
    M,

    /**
     * , key.
     */
    COMMA,

    /**
     * . key .
     */
    PERIOD,

    /**
     * / key .
     */
    SLASH,

    /**
     * right shift key.
     */
    RSHIFT,

    /**
     * * key .
     */
    MULTIPLY,

    /**
     * left alt key.
     */
    LMENU,

    /**
     * space key.
     */
    SPACE,

    /**
     * caps lock key.
     */
    CAPITAL,

    /**
     * F1 key.
     */
    F1,

    /**
     * F2 key.
     */
    F2,

    /**
     * F3 key.
     */
    F3,

    /**
     * F4 key.
     */
    F4,

    /**
     * F5 key.
     */
    F5,

    /**
     * F6 key.
     */
    F6,

    /**
     * F7 key.
     */
    F7,

    /**
     * F8 key.
     */
    F8,

    /**
     * F9 key.
     */
    F9,

    /**
     * F10 key.
     */
    F10,

    /**
     * NumLK key.
     */
    NUMLOCK,

    /**
     * Scroll lock key.
     */
    SCROLL,

    /**
     * 7 key .
     */
    NUMPAD7,

    /**
     * 8 key .
     */
    NUMPAD8,

    /**
     * 9 key .
     */
    NUMPAD9,

    /**
     * - key .
     */
    NUMPADSUBTRACT,

    /**
     * 4 key .
     */
    NUMPAD4,

    /**
     * 5 key .
     */
    NUMPAD5,

    /**
     * 6 key .
     */
    NUMPAD6,

    /**
     * + key .
     */
    NUMPADADD,

    /**
     * 1 key .
     */
    NUMPAD1,

    /**
     * 2 key .
     */
    NUMPAD2,

    /**
     * 3 key .
     */
    NUMPAD3,

    /**
     * 0 key .
     */
    NUMPAD0,

    /**
     * . key .
     */
    DECIMAL,

    /**
     * F11 key.
     */
    F11,

    /**
     * F12 key.
     */
    F12,

    /**
     * F13 key.
     */
    F13,

    /**
     * F14 key.
     */
    F14,

    /**
     * F15 key.
     */
    F15,

    /**
     * kana key .
     */
    KANA,

    /**
     * convert key .
     */
    CONVERT,

    /**
     * noconvert key .
     */
    NOCONVERT,

    /**
     * yen key .
     */
    YEN,

    /**
     * = on num pad .
     */
    NUMPADEQUALS,

    /**
     * circum flex key .
     */
    CIRCUMFLEX,

    /**
     * &#064; key .
     */
    AT,

    /**
     * : key
     */
    COLON,

    /**
     * _ key .
     */
    UNDERLINE,

    /**
     * kanji key .
     */
    KANJI,

    /**
     * stop key .
     */
    STOP,

    /**
     * ax key .
     */
    AX,

    /**
     * .
     */
    UNLABELED,

    /**
     * Enter key .
     */
    NUMPADENTER,

    /**
     * right control key.
     */
    RCONTROL,

    /**
     * , key on num pad .
     */
    NUMPADCOMMA,

    /**
     * / key .
     */
    DIVIDE,

    /**
     * SysRq key.
     */
    SYSRQ,

    /**
     * right alt key.
     */
    RMENU,

    /**
     * pause key.
     */
    PAUSE,

    /**
     * home key.
     */
    HOME,

    /**
     * up arrow key.
     */
    UP,

    /**
     * PageUp/Prior key.
     */
    PAGEUP_PRIOR,

    /**
     * left arrow key.
     */
    LEFT,

    /**
     * right arrow key.
     */
    RIGHT,

    /**
     * end key.
     */
    END,

    /**
     * down arrow key.
     */
    DOWN,

    /**
     * PageDown/Next key.
     */
    PAGEDOWN_NEXT,

    /**
     * insert key.
     */
    INSERT,

    /**
     * delete key.
     */
    DELETE,

    /**
     * Left Windows/Option key
     */
    LMETA,

    /**
     * Right Windows/Option key
     */
    RMETA,

    /**
     * power key.
     */
    POWER,

    /**
     * sleep key.
     */
    SLEEP,

    /**
     * mobile call button
     */
    CALL,

    /**
     * mobile camera button
     */
    CAMERA,

    /**
     * mobile clear button
     */
    CLEAR,

    /**
     * dpad center button
     */
    CENTER,

    /**
     * mobile end call button
     */
    ENDCALL,

    /**
     * mobile envelope button
     */
    ENVELOPE,

    /**
     * mobile explorer button
     */
    EXPLORER,

    /**
     * mobile focus button
     */
    FOCUS,

    /**
     * mobile headsethook button
     */
    HEADSETHOOK,

    /**
     * mobile fast fwd button
     */
    MEDIA_FAST_FORWARD,

    /**
     * mobile next button
     */
    MEDIA_NEXT,

    /**
     * mobile play/pause button
     */
    PLAY_PAUSE,

    /**
     * mobile previous button
     */
    MEDIA_PREVIOUS,

    /**
     * mobile rewind button
     */
    MEDIA_REWIND,

    /**
     * mobile stop button
     */
    MEDIA_STOP,

    /**
     * mobile menu button
     */
    MENU,

    /**
     * mobile mute button
     */
    MUTE,

    /**
     * mobile notification button
     */
    NOTIFICATION,

    /**
     * plus key
     */
    PLUS,

    /**
     * pound key
     */
    POUND,

    /**
     * mobile call button
     */
    SEARCH,

    /**
     * mobile star button
     */
    STAR,

    /**
     * mobile # button
     */
    SYM,

    /**
     * volume down button
     */
    VOLUME_DOWN,

    /**
     * volume up button
     */
    VOLUME_UP;
}
