
package com.ardor3d.extension.terrain.util;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * Utility class used by the mailbox update system.
 * 
 * @param <T>
 */
public class DoubleBufferedList<T> {
    private List<T> frontList = Lists.newArrayList();
    private List<T> backList = Lists.newArrayList();

    /**
     * The add method can be called at any point.
     * 
     * @param t
     */
    public synchronized void add(final T t) {
        if (!backList.contains(t)) {
            backList.add(t);
        }
    }

    /**
     * The switchAndGet call and it's returned list has to be accessed sequencially.
     * 
     * @return
     */
    public synchronized List<T> switchAndGet() {
        final List<T> tmp = backList;
        backList = frontList;
        frontList = tmp;
        backList.clear();
        return frontList;
    }
}
