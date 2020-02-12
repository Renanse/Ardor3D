/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.functions;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;

/**
 * Utility class containing a set of easy to use functions.
 */
public class Functions {

    /**
     * @param constant
     * @return a function that will always return the given constant value regardless of input
     */
    public static Function3D constant(final double constant) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                return constant;
            }
        };
    }

    /**
     * @param source
     * @param scale
     * @param bias
     * @return a function that returns (src.eval * scale) + bias.
     */
    public static Function3D scaleBias(final Function3D source, final double scale, final double bias) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                return source.eval(x, y, z) * scale + bias;
            }
        };
    }

    /**
     * @param source
     * @return a function that returns |src.eval|
     */
    public static Function3D abs(final Function3D source) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                return Math.abs(source.eval(x, y, z));
            }
        };
    }

    /**
     * @param source
     * @param min
     * @param max
     * @return a function that returns src.eval clamped to [min, max]
     */
    public static Function3D clamp(final Function3D source, final double min, final double max) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                return MathUtils.clamp(source.eval(x, y, z), min, max);
            }
        };
    }

    /**
     * @param source
     * @return a function that returns -(src.eval)
     */
    public static Function3D invert(final Function3D source) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                return -source.eval(x, y, z);
            }
        };
    }

    /**
     * @param sourceA
     * @param sourceB
     * @return a function the returns srcA.eval + srcB.eval
     */
    public static Function3D add(final Function3D sourceA, final Function3D sourceB) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                return sourceA.eval(x, y, z) + sourceB.eval(x, y, z);
            }
        };
    }

    /**
     * @param sourceA
     * @param sourceB
     * @return a function the returns srcA.eval * srcB.eval
     */
    public static Function3D multiply(final Function3D sourceA, final Function3D sourceB) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                return sourceA.eval(x, y, z) * sourceB.eval(x, y, z);
            }
        };
    }

    /**
     * @param sourceA
     * @param sourceB
     * @return a function the returns min(srcA.eval, srcB.eval)
     */
    public static Function3D min(final Function3D sourceA, final Function3D sourceB) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                return Math.min(sourceA.eval(x, y, z), sourceB.eval(x, y, z));
            }
        };
    }

    /**
     * @param sourceA
     * @param sourceB
     * @return a function the returns max(srcA.eval, srcB.eval)
     */
    public static Function3D max(final Function3D sourceA, final Function3D sourceB) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                return Math.max(sourceA.eval(x, y, z), sourceB.eval(x, y, z));
            }
        };
    }

    /**
     * (1-amount) * srcA.eval + (amount) * srcB.eval
     * 
     * @param sourceA
     * @param sourceB
     * @param amount
     * @return a function the linear interpolation of srcA.eval and srcB.eval using the given amount.
     */
    public static Function3D lerp(final Function3D sourceA, final Function3D sourceB, final double amount) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                return MathUtils.lerp(amount, sourceA.eval(x, y, z), sourceB.eval(x, y, z));
            }
        };
    }

    /**
     * @param source
     * @param rotation
     * @return a function that rotates the 3 value tuple as a vector using the given matrix before feeding it to
     *         src.eval.
     */
    public static Function3D rotateInput(final Function3D source, final ReadOnlyMatrix3 rotation) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                final Vector3 temp = Vector3.fetchTempInstance();
                temp.set(x, y, z);
                rotation.applyPost(temp, temp);
                final double val = source.eval(temp.getX(), temp.getY(), temp.getZ());
                Vector3.releaseTempInstance(temp);
                return val;
            }
        };
    }

    /**
     * @param source
     * @param scaleX
     * @param scaleY
     * @param scaleZ
     * @return a function that scales the 3 value tuple using the given values before feeding it to src.eval.
     */
    public static Function3D scaleInput(final Function3D source, final double scaleX, final double scaleY,
            final double scaleZ) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                return source.eval(x * scaleX, y * scaleY, z * scaleZ);
            }
        };
    }

    /**
     * @param source
     * @param transX
     * @param transY
     * @param transZ
     * @return a function that translates the 3 value tuple using the given values before feeding it to src.eval.
     */
    public static Function3D translateInput(final Function3D source, final double transX, final double transY,
            final double transZ) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                return source.eval(x + transX, y + transY, z + transZ);
            }
        };
    }

    /**
     * @param source
     * @param oldLow
     * @param oldHigh
     * @param newLow
     * @param newHigh
     * @return a function that maps src.eval from a given range onto a new range.
     */
    public static Function3D remap(final Function3D source, final double oldLow, final double oldHigh,
            final double newLow, final double newHigh) {
        return new Function3D() {
            public double eval(final double x, final double y, final double z) {
                double val = source.eval(x, y, z);
                // Zero out old domain
                val -= oldLow;
                val /= (oldHigh - oldLow);
                // Shift to new domain
                val *= (newHigh - newLow);
                val += newLow;
                return val;
            }
        };
    }

    /**
     * @return a function that returns simplex noise.
     */
    public static Function3D simplexNoise() {
        return new Function3D() {
            SimplexNoise noiseGenerator = new SimplexNoise();

            public double eval(final double x, final double y, final double z) {
                return noiseGenerator.noise(x, y, z);
            }
        };
    }
}
