/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export;

import java.lang.reflect.Array;

public final class CapsuleUtils {

    private CapsuleUtils() {}

    /**
     * Convert an object array to a Savable array for easier use during export.
     * 
     * @param values
     *            our object array, should be of a class type that implements Savable.
     * @return the array as Savable.
     */
    public static Savable[] asSavableArray(final Object[] values) {
        final Savable[] rVal = new Savable[values.length];
        for (int i = 0; i < values.length; i++) {
            rVal[i] = (Savable) values[i];
        }
        return rVal;
    }

    /**
     * Converts from a Savable array to a particular class type for import operations.
     * 
     * @param <T>
     *            The class type to convert to.
     * @param array
     *            our Savable array to convert.
     * @param clazz
     *            the class type value.
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] asArray(final Savable[] array, final Class<T> clazz) {
        final T[] values = (T[]) Array.newInstance(clazz, array.length);
        for (int i = 0; i < array.length; i++) {
            values[i] = (T) array[i];
        }
        return values;
    }

}
