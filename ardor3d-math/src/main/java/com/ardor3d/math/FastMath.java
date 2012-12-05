/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math;

/**
 * A "close approximation" class for Math operations.
 * 
 * References:
 * <ul>
 * <li>http://www.devmaster.net/forums/showthread.php?t=5784</li>
 * <li>http://www.lomont.org/Math/Papers/2003/InvSqrt.pdf</li>
 * <li>http://stackoverflow.com/questions/523531/fast-transcendent-trigonometric-functions-for-java</li>
 * <li>http://www.lightsoft.co.uk/PD/stu/stuchat37.html</li>
 * <li>http://wiki.java.net/bin/view/Games/JeffGems</li>
 * </ul>
 * 
 * NB: With current improvements in hardware, I don't recommend using these and they will likely be deprecated/removed
 * in the future.
 */
public enum FastMath {
    ;

    public final static double EPSILON_SIN = 0.0011d;
    public final static double EPSILON_COS = 0.0011d;
    public final static double EPSILON_SIN2COS2 = 0.002d;
    public final static double EPSILON_ASIN = 0.0014d;
    public final static double EPSILON_ACOS = 0.0014d;
    public final static double EPSILON_ATAN = 0.005d; // needs to be improved
    public final static double EPSILON_SQRT = 0.025d; // guess

    private final static double _sin_a = -4 / MathUtils.SQUARED_PI;
    private final static double _sin_b = 4 / MathUtils.PI;
    private final static double _sin_p = 9d / 40;

    private final static double _asin_a = -0.0481295276831013447d;
    private final static double _asin_b = -0.343835993947915197d;
    private final static double _asin_c = 0.962761848425913169d;
    private final static double _asin_d = 1.00138940860107040d;

    private final static double _atan_a = 0.280872d;

    /** sin: [-œÄ,œÄ] -> [-1,1] */
    public final static double sin(double x) {
        x = FastMath._sin_a * x * Math.abs(x) + FastMath._sin_b * x;
        return FastMath._sin_p * (x * Math.abs(x) - x) + x;
    }

    /** cos: [-œÄ,œÄ] -> [-1,1] */
    public final static double cos(final double x) {
        return sin(x + (x > MathUtils.HALF_PI ? -MathUtils.THREE_PI_HALVES : MathUtils.HALF_PI));
    }

    /** tan: [-œÄ,œÄ] \ {-œÄ/2,œÄ/2} -> R */
    public final static double tan(final double x) {
        return sin(x) / cos(x);
    }

    /** asin: [-1,1] -> [-œÄ/2,œÄ/2] */
    public final static double asin(final double x) {
        return x * (Math.abs(x) * (Math.abs(x) * FastMath._asin_a + FastMath._asin_b) + FastMath._asin_c)
                + Math.signum(x) * (FastMath._asin_d - Math.sqrt(1 - x * x));
    }

    /** acos: [-1,1] -> [0,œÄ] */
    public final static double acos(final double x) {
        return MathUtils.HALF_PI - asin(x);
    }

    /** atan: (-‚àû,‚àû) -> (-œÄ/2,œÄ/2) */
    public final static double atan(final double x) {
        return Math.abs(x) < 1 ? x / (1 + FastMath._atan_a * x * x) : Math.signum(x) * MathUtils.HALF_PI - x
                / (x * x + FastMath._atan_a);
    }

    /** inverseSqrt: (0,‚àû) -> (0,‚àû) **/
    public final static double inverseSqrt(double x) {
        final double xhalves = 0.5d * x;
        x = Double.longBitsToDouble(0x5FE6EB50C7B537AAl - (Double.doubleToRawLongBits(x) >> 1));
        return x * (1.5d - xhalves * x * x); // more iterations possible
    }

    public final static double sqrt(final double x) {
        return x * inverseSqrt(x);
    }
}
